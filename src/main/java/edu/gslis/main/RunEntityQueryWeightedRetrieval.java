package edu.gslis.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.gslis.docscoring.support.CollectionStats;
import edu.gslis.docscoring.support.IndexBackedCollectionStats;
import edu.gslis.entities.docscoring.ScorerDirichlet;
import edu.gslis.entities.docscoring.support.EntityQueryWeight;
import edu.gslis.entities.docscoring.support.EntityQueryWeightedProbability;
import edu.gslis.entities.readers.DocumentEntityReader;
import edu.gslis.entities.utils.Configuration;
import edu.gslis.entities.utils.SimpleConfiguration;
import edu.gslis.output.FormattedOutputTrecEval;
import edu.gslis.patches.IndexWrapperIndriImpl;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.queries.GQuery;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;

public class RunEntityQueryWeightedRetrieval {

	public static void main(String[] args) throws FileNotFoundException {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		IndexWrapperIndriImpl index = new IndexWrapperIndriImpl(config.get("index"));
		IndexWrapperIndriImpl wikiIndex = null;
		if (config.get("wiki-index") != null)
			wikiIndex = new IndexWrapperIndriImpl(config.get("wiki-index"));
		
		Stopper stopper = null;
		if (config.get("stoplist") != null)
			stopper = new Stopper(config.get("stoplist"));
		
		GQueriesJsonImpl queries = new GQueriesJsonImpl();
		queries.read(config.get("queries"));
		
		DocumentEntityReader de = new DocumentEntityReader();
		de.readFileAbsolute(config.get("document-entities-file"));
		
		CollectionStats cs = new IndexBackedCollectionStats();
		cs.setStatSource(config.get("index"));

		EntityQueryWeightedProbability cp = new EntityQueryWeightedProbability(de, wikiIndex, stopper);
		cp.setCollectionStats(cs);
		
		String outDir = config.get("outdir");
	
		double backgroundMix = 0.5;
		if (config.get("background-mix") != null) {
			backgroundMix = Double.parseDouble(config.get("background-mix"));
		}
		
		double mu = 2500;
		if (config.get("mu") != null) {
			mu = Double.parseDouble(config.get("mu"));
		}

		ScorerDirichlet scorer = new ScorerDirichlet();
		scorer.setCollectionStats(cs);
		scorer.setParameter(scorer.PARAMETER_NAME, mu);

		Iterator<GQuery> queryIt = queries.iterator();
		while (queryIt.hasNext()) {
			GQuery query = queryIt.next();

			if (stopper != null)
				query.applyStopper(stopper);
			scorer.setQuery(query);
			
			List<String> terms = new ArrayList<String>();
			Iterator<String> qit = query.getFeatureVector().iterator();
			while (qit.hasNext()) {
				terms.add(qit.next());
			}
			
			SearchHits hits = index.runQuery(query, 1000);

			EntityQueryWeight eqw = new EntityQueryWeight(hits, de);
			cp.setEntityQueryWeight(eqw);
			
			Map<String, Map<String, Double>> scores = new HashMap<String, Map<String, Double>>();

			Iterator<SearchHit> hitIt = hits.iterator();
			while (hitIt.hasNext()) {
				SearchHit hit = hitIt.next();
				
				scores.put(hit.getDocno(), new HashMap<String, Double>());
				
				FeatureVector dv = index.getDocVector(hit.getDocID(), null);
				hit.setFeatureVector(dv);
				hit.setLength(dv.getLength());
				
				double qlscore = scorer.score(hit);
				scores.get(hit.getDocno()).put("ql", qlscore);
				
				cp.setDocument(hit);
				double entityscore = cp.getProbability(terms);
				scores.get(hit.getDocno()).put("entity", entityscore);
			}
			
			for (int i = 0; i <= 10; i++) {
				double lambda = backgroundMix+i/10.0;
				System.err.println("Lambda: "+lambda);

				hitIt = hits.iterator();
				while (hitIt.hasNext()) {
					SearchHit hit = hitIt.next();

					double qlscore = scores.get(hit.getDocno()).get("ql");
					double entityScore = scores.get(hit.getDocno()).get("entity");

					double combinedScore = lambda*qlscore + (1-lambda)*entityScore;

					System.err.println("Setting score of "+hit.getDocno()+": "+combinedScore);

					hit.setScore(combinedScore);
				}

				hits.rank();
				hits.crop(1000);

				Writer outputWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outDir+"/"+lambda), true)));
				FormattedOutputTrecEval output = FormattedOutputTrecEval.getInstance("entities", outputWriter);
		
				output.write(hits, query.getTitle());
			}
		}
	}

}
