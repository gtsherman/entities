package edu.gslis.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;

import edu.gslis.docscoring.support.CollectionStats;
import edu.gslis.docscoring.support.IndexBackedCollectionStats;
import edu.gslis.entities.docscoring.DirichletDocScorer;
import edu.gslis.entities.docscoring.QueryScorer;
import edu.gslis.entities.docscoring.QueryScorerQueryLikelihood;
import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.output.FormattedOutputTrecEval;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.queries.GQuery;
import edu.gslis.searchhits.IndexBackedSearchHit;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.searchhits.SearchHitsBatch;
import edu.gslis.utils.Stopper;
import edu.gslis.utils.config.Configuration;
import edu.gslis.utils.config.SimpleConfiguration;
import edu.gslis.utils.readers.SearchResultsReader;

public class RunBaselineRetrieval {
	
	public static void main(String[] args) {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		Stopper stopper = null;
		if (config.get("stoplist") != null)
			stopper = new Stopper(config.get("stoplist"));
		
		IndexWrapperIndriImpl index = new IndexWrapperIndriImpl(config.get("index"));
		
		GQueriesJsonImpl queries = new GQueriesJsonImpl();
		queries.read(config.get("queries"));
		
		CollectionStats cs = new IndexBackedCollectionStats();
		cs.setStatSource(config.get("index"));
		
		SearchResultsReader results = new SearchResultsReader(new File(config.get("initial-hits")));
		SearchHitsBatch batchResults = results.getBatchResults();

		Writer outputWriter = new BufferedWriter(new OutputStreamWriter(System.out));
		FormattedOutputTrecEval output = FormattedOutputTrecEval.getInstance("baseline", outputWriter);
		
		Iterator<GQuery> queryIt = queries.iterator();
		while (queryIt.hasNext()) {
			GQuery query = queryIt.next();
			query.applyStopper(stopper);
			
			System.err.println("Query "+query.getTitle());
			
			SearchHits hits = batchResults.getSearchHits(query);
			SearchHits rescored = new SearchHits();

			Iterator<SearchHit> hitIt = hits.iterator();
			while (hitIt.hasNext()) {
				SearchHit hit = new IndexBackedSearchHit(index, hitIt.next());

				QueryScorer scorer = new QueryScorerQueryLikelihood(new DirichletDocScorer(hit, cs));
				double score = scorer.scoreQuery(query);
				
				hit.setScore(score);
				rescored.add(hit);
			}
			rescored.rank();
			output.write(rescored, query.getTitle());
		}
		output.close();
	}

}
