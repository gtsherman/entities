package edu.gslis.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.Connection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import edu.gslis.docscoring.support.PrefetchedCollectionStats;
import edu.gslis.entities.docscoring.CachedFileLookupDocScorer;
import edu.gslis.entities.docscoring.FileLookupDocScorer;
import edu.gslis.entities.docscoring.QueryProbDatabaseLookupDocScorer;
import edu.gslis.entities.docscoring.RMDatabaseLookupDocScorer;
import edu.gslis.evaluation.running.runners.DoubleEntityRMRunner;
import edu.gslis.evaluation.running.runners.DoubleEntityRunner;
import edu.gslis.evaluation.running.runners.RMRunner;
import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.output.FormattedOutputTrecEval;
import edu.gslis.queries.GQueries;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.queries.GQuery;
import edu.gslis.readers.QueryProbabilityDataInterpreter;
import edu.gslis.related_docs.term_collectors.DatabaseTermCollector;
import edu.gslis.related_docs.term_collectors.MultiSourceTermCollector;
import edu.gslis.related_docs.term_collectors.TermCollector;
import edu.gslis.scoring.CachedDocScorer;
import edu.gslis.scoring.DirichletDocScorer;
import edu.gslis.scoring.DocScorer;
import edu.gslis.searchhits.SearchHitsBatch;
import edu.gslis.utils.Stopper;
import edu.gslis.utils.config.Configuration;
import edu.gslis.utils.config.SimpleConfiguration;
import edu.gslis.utils.data.interpreters.RelevanceModelDataInterpreter;
import edu.gslis.utils.data.interpreters.SearchResultsDataInterpreter;
import edu.gslis.utils.data.sources.DatabaseDataSource;

public class RunDoubleEntityRMSweep {
	
	public static void main(String[] args) throws InterruptedException, IOException {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		IndexWrapperIndriImpl index = new IndexWrapperIndriImpl(config.get("index"));
		Stopper stopper = new Stopper(config.get("stoplist"));
		GQueries queries = new GQueriesJsonImpl();
		queries.read(config.get("queries"));

		Set<String> terms = new HashSet<String>();
		Iterator<GQuery> queryIt = queries.iterator();
		while (queryIt.hasNext()) {
			GQuery query = queryIt.next();
			Iterator<String> featureIt = query.getFeatureVector().iterator();
			while (featureIt.hasNext()) {
				terms.add(featureIt.next());
			}
		}
		PrefetchedCollectionStats csSelf = new PrefetchedCollectionStats(config.get("index"), terms);

		String outDir = config.get("double-entity-rm-sweep-dir"); 
		
		Connection dbCon = DatabaseDataSource.getConnection(config.get("database"));
		
		DatabaseDataSource data = new DatabaseDataSource(dbCon, SearchResultsDataInterpreter.DATA_NAME);
		data.read();
		SearchResultsDataInterpreter dataInterpreter = new SearchResultsDataInterpreter(index);
		SearchHitsBatch initialResults = dataInterpreter.build(data);

		DatabaseDataSource expansionDataSelf = new DatabaseDataSource(
				dbCon, "expansion_rms_self");
		DatabaseDataSource expansionQueryDataSelf = new DatabaseDataSource(
				dbCon, "expansion_probabilities_self");
		DatabaseDataSource expansionDataWiki = new DatabaseDataSource(
				dbCon, "expansion_rms_wiki");
		DatabaseDataSource expansionQueryDataWiki = new DatabaseDataSource(
				dbCon, "expansion_probabilities_wiki");
		
		// Term collector
		TermCollector termCollectorSelf = new DatabaseTermCollector(expansionDataSelf,
				new RelevanceModelDataInterpreter(RelevanceModelDataInterpreter.SCORE_FIELD,
						RelevanceModelDataInterpreter.TERM_FIELD,
						"ORIGINAL_DOCUMENT"));
		TermCollector termCollectorWiki = new DatabaseTermCollector(expansionDataWiki,
				new RelevanceModelDataInterpreter(RelevanceModelDataInterpreter.SCORE_FIELD,
						RelevanceModelDataInterpreter.TERM_FIELD,
						"ORIGINAL_DOCUMENT"));
		TermCollector termCollector = new MultiSourceTermCollector(termCollectorSelf, termCollectorWiki);
		
		// fbDocs scorer
		DocScorer docScorer = new CachedDocScorer(new DirichletDocScorer(0, csSelf));
		
		// fbDocs query scorer
		FileLookupDocScorer docScorerQueryProb = new CachedFileLookupDocScorer(config.get("document-probability-data-dir"));
		
		// self expansion docs scorer
		DocScorer expansionScorerSelf = new CachedDocScorer(new RMDatabaseLookupDocScorer(expansionDataSelf,
				new RelevanceModelDataInterpreter(RelevanceModelDataInterpreter.SCORE_FIELD,
						RelevanceModelDataInterpreter.TERM_FIELD,
						"ORIGINAL_DOCUMENT")));
		
		// self expansion docs query scorer
		DocScorer expansionScorerQueryProbSelf = new CachedDocScorer(new QueryProbDatabaseLookupDocScorer(expansionQueryDataSelf,
				new QueryProbabilityDataInterpreter(RelevanceModelDataInterpreter.TERM_FIELD,
						RelevanceModelDataInterpreter.SCORE_FIELD,
						"QUERY", "DOCUMENT")));

		// wiki expansion docs scorer
		DocScorer expansionScorerWiki = new CachedDocScorer(new RMDatabaseLookupDocScorer(expansionDataWiki,
				new RelevanceModelDataInterpreter(RelevanceModelDataInterpreter.SCORE_FIELD,
						RelevanceModelDataInterpreter.TERM_FIELD,
						"ORIGINAL_DOCUMENT")));
		
		// wiki expansion docs query scorer
		DocScorer expansionScorerQueryProbWiki = new CachedDocScorer(new QueryProbDatabaseLookupDocScorer(expansionQueryDataWiki,
				new QueryProbabilityDataInterpreter(RelevanceModelDataInterpreter.TERM_FIELD,
						RelevanceModelDataInterpreter.SCORE_FIELD,
						"QUERY", "DOCUMENT")));

		DoubleEntityRMRunner runner = new DoubleEntityRMRunner(index, initialResults, stopper,
				docScorer, expansionScorerSelf, expansionScorerWiki,
				docScorerQueryProb, expansionScorerQueryProbSelf,
				expansionScorerQueryProbWiki, termCollector);
		
		Writer outputWriter = new BufferedWriter(new OutputStreamWriter(System.out));
		FormattedOutputTrecEval output = FormattedOutputTrecEval.getInstance("rm3", outputWriter);
		
		Map<String, Double> currentParams = new HashMap<String, Double>();

		for (int origW = 0; origW <= 10; origW++) {
			double origWeight = origW / 10.0;
			currentParams.put(DoubleEntityRunner.DOCUMENT_WEIGHT, origWeight);
			for (int wikiW = 0; wikiW <= 10-origW; wikiW++) {
				double wikiWeight = wikiW / 10.0;
				double selfWeight = (10-(origW+wikiW)) / 10.0;
				
				currentParams.put(DoubleEntityRunner.WIKI_WEIGHT, wikiWeight);
				currentParams.put(DoubleEntityRunner.SELF_WEIGHT, selfWeight);
				
				for (int lambda = 0; lambda <= 10; lambda += 1) {
					double lam = lambda / 10.0;
					currentParams.put(RMRunner.ORIG_QUERY_WEIGHT, lam);

					System.err.println("\t\tParameters: "+origWeight+" (doc), "+wikiWeight+" (wiki), "+selfWeight+" (self), "+lam+" (mixing)");
					
					String run = origWeight + "_" + wikiWeight + "_" + selfWeight + "_" + lam;
					output.setRunId(run);
					output.setWriter(new FileWriter(outDir + File.separator + run));
					
					SearchHitsBatch batchResults = runner.run(queries, 1000, currentParams);
					
					Iterator<String> qit = batchResults.queryIterator();
					while (qit.hasNext()) {
						String query = qit.next();
						output.write(batchResults.getSearchHits(query), query);			
					}
				}
			}
		}
	}

}
