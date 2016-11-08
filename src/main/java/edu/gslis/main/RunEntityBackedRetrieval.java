package edu.gslis.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.gslis.docscoring.support.CollectionStats;
import edu.gslis.docscoring.support.IndexBackedCollectionStats;
import edu.gslis.entities.docscoring.DirichletDocScorer;
import edu.gslis.entities.docscoring.DocScorer;
import edu.gslis.entities.docscoring.ExpansionDocsDocScorer;
import edu.gslis.entities.docscoring.InterpolatedDocScorer;
import edu.gslis.entities.docscoring.QueryScorer;
import edu.gslis.entities.docscoring.QueryScorerQueryLikelihood;
import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.output.FormattedOutputTrecEval;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.queries.GQuery;
import edu.gslis.related_docs.DocumentClusterReader;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.utils.Stopper;
import edu.gslis.utils.config.Configuration;
import edu.gslis.utils.config.SimpleConfiguration;

public class RunEntityBackedRetrieval {
	
	public static void main(String[] args) {
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
		
		int limit = Integer.parseInt(args[1]);
		DocumentClusterReader clusters = new DocumentClusterReader(new File(config.get("document-entities-file")), limit);
		
		CollectionStats cs = new IndexBackedCollectionStats();
		cs.setStatSource(config.get("index"));
		
		double mu = 2500;
		if (config.get("mu") != null) {
			mu = Double.parseDouble(config.get("mu"));
		}
		
		int numDocs = 1000;
		if (config.get("num-docs") != null) {
			numDocs = Integer.parseInt(config.get("num-docs"));
		}
		
		double origQueryWeight = 0.5;
		if (config.get("original-query-weight") != null) {
			origQueryWeight = Double.parseDouble(config.get("original-query-weight"));
		}

		Writer outputWriter = new BufferedWriter(new OutputStreamWriter(System.out));
		FormattedOutputTrecEval output = FormattedOutputTrecEval.getInstance("entities", outputWriter);
		
		Iterator<GQuery> queryIt = queries.iterator();
		while (queryIt.hasNext()) {
			GQuery query = queryIt.next();
			query.applyStopper(stopper);

			System.err.println("Query " + query.getTitle());
			
			SearchHits hits = index.runQuery(query, numDocs);

			Iterator<SearchHit> hitIt = hits.iterator();
			while (hitIt.hasNext()) {
				SearchHit hit = hitIt.next();
				hit.setFeatureVector(index.getDocVector(hit.getDocno(), null));
			
				DocScorer hitScorer = new DirichletDocScorer(mu, hit, cs);
				DocScorer expansionScorer = new ExpansionDocsDocScorer(hit, wikiIndex, clusters);

				Map<DocScorer, Double> scorerWeights = new HashMap<DocScorer, Double>();
				scorerWeights.put(hitScorer, origQueryWeight);
				scorerWeights.put(expansionScorer, 1-origQueryWeight);

				InterpolatedDocScorer docScorer = new InterpolatedDocScorer(scorerWeights);
				
				QueryScorer scorer = new QueryScorerQueryLikelihood(docScorer);
				hit.setScore(scorer.scoreQuery(query));
			}
			hits.rank();
			output.write(hits, query.getTitle());
		}
		output.close();
	}

}
