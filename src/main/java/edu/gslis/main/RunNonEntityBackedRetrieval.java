package edu.gslis.main;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;

import edu.gslis.docscoring.support.CollectionStats;
import edu.gslis.docscoring.support.IndexBackedCollectionStats;
import edu.gslis.entities.docscoring.ScorerDirichlet;
import edu.gslis.entities.docscoring.ScorerDirichletCategory2;
import edu.gslis.entities.docscoring.support.BackgroundProbability;
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

public class RunNonEntityBackedRetrieval {

	public static void main(String[] args) {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		IndexWrapperIndriImpl index = new IndexWrapperIndriImpl(config.get("index"));
		
		Stopper stopper = null;
		if (config.get("stoplist") != null)
			stopper = new Stopper(config.get("stoplist"));
		
		GQueriesJsonImpl queries = new GQueriesJsonImpl();
		queries.read(config.get("queries"));
		
		CollectionStats cs = new IndexBackedCollectionStats();
		cs.setStatSource(config.get("index"));
		
		DocumentEntityReader de = new DocumentEntityReader();
		de.readFileAbsolute(config.get("document-entities-file"));
		
		BackgroundProbability cp = new BackgroundProbability();
		cp.setCollectionStats(cs);
		
		double backgroundMix = 0.5;
		if (config.get("background-mix") != null) {
			backgroundMix = Double.parseDouble(config.get("background-mix"));
		}
		
		double mu = 2500;
		if (config.get("mu") != null) {
			mu = Double.parseDouble(config.get("mu"));
		}

		ScorerDirichletCategory2 scorer = new ScorerDirichletCategory2();
		scorer.setCollectionStats(cs);
		scorer.setCategoryProbability(cp);
		scorer.setParameter(scorer.BACKGROUND_MIX, backgroundMix);
		scorer.setParameter(scorer.PARAMETER_NAME, mu);
		
		ScorerDirichlet scorerStandard = new ScorerDirichlet();
		scorerStandard.setCollectionStats(cs);
		scorerStandard.setParameter(scorer.PARAMETER_NAME, mu);
		
		Writer outputWriter = new BufferedWriter(new OutputStreamWriter(System.out));
		FormattedOutputTrecEval output = FormattedOutputTrecEval.getInstance("entities", outputWriter);
		
		Iterator<GQuery> queryIt = queries.iterator();
		while (queryIt.hasNext()) {
			GQuery query = queryIt.next();

			if (stopper != null)
				query.applyStopper(stopper);
			scorer.setQuery(query);
			scorerStandard.setQuery(query);
			
			SearchHits hits = index.runQuery(query, 3000);
			Iterator<SearchHit> hitIt = hits.iterator();
			while (hitIt.hasNext()) {
				SearchHit hit = hitIt.next();
				FeatureVector dv = index.getDocVector(hit.getDocID(), null);
				hit.setFeatureVector(dv);
				hit.setLength(dv.getLength());
				
				if (de.getDocuments().contains(hit.getDocno())) {
					hit.setScore(scorerStandard.score(hit));
				} else {
					hit.setScore(scorer.score(hit));
				}
			}
			hits.rank();
			hits.crop(1000);
			output.write(hits, query.getTitle());
		}
	}

}
