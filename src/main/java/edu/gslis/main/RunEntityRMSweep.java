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
import edu.gslis.entities.docscoring.DatabaseLookupDocScorer;
import edu.gslis.entities.docscoring.FileLookupDocScorer;
import edu.gslis.entities.docscoring.FileLookupQueryLikelihoodQueryScorer;
import edu.gslis.evaluation.running.runners.EntityRMRunner;
import edu.gslis.evaluation.running.runners.EntityRunner;
import edu.gslis.evaluation.running.runners.RMRunner;
import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.output.FormattedOutputTrecEval;
import edu.gslis.queries.GQueries;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.queries.GQuery;
import edu.gslis.readers.QueryProbabilityDataInterpreter;
import edu.gslis.related_docs.DocumentClusterDataInterpreter;
import edu.gslis.related_docs.DocumentClusterDataSource;
import edu.gslis.related_docs.RelatedDocs;
import edu.gslis.scoring.CachedDocScorer;
import edu.gslis.scoring.DirichletDocScorer;
import edu.gslis.scoring.DocScorer;
import edu.gslis.scoring.queryscoring.QueryLikelihoodQueryScorer;
import edu.gslis.scoring.queryscoring.QueryScorer;
import edu.gslis.searchhits.SearchHitsBatch;
import edu.gslis.utils.Stopper;
import edu.gslis.utils.config.Configuration;
import edu.gslis.utils.config.SimpleConfiguration;
import edu.gslis.utils.data.interpreters.RelevanceModelDataInterpreter;
import edu.gslis.utils.data.interpreters.SearchResultsDataInterpreter;
import edu.gslis.utils.data.sources.DataSource;
import edu.gslis.utils.data.sources.DatabaseDataSource;

public class RunEntityRMSweep {
	
	public static void main(String[] args) throws InterruptedException, IOException {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		IndexWrapperIndriImpl index = new IndexWrapperIndriImpl(config.get("index"));
		IndexWrapperIndriImpl wikiIndex = new IndexWrapperIndriImpl(config.get("wiki-index"));

		Stopper stopper = new Stopper(config.get("stoplist"));

		GQueries queries = new GQueriesJsonImpl();
		queries.read(config.get("queries"));

		RelatedDocs expansionClusters = (new DocumentClusterDataInterpreter()).build(
				new DocumentClusterDataSource(new File(config.get("document-entities-file"))));

		Set<String> terms = new HashSet<String>();
		Iterator<GQuery> queryIt = queries.iterator();
		while (queryIt.hasNext()) {
			GQuery query = queryIt.next();
			Iterator<String> featureIt = query.getFeatureVector().iterator();
			while (featureIt.hasNext()) {
				terms.add(featureIt.next());
			}
		}
		PrefetchedCollectionStats cs = new PrefetchedCollectionStats(config.get("index"), terms);
		
		String outDir = config.get("single-entity-rm-sweep-dir"); 

		Connection dbCon = DatabaseDataSource.getConnection(config.get("database"));
		
		DataSource data = new DatabaseDataSource(dbCon, SearchResultsDataInterpreter.DATA_NAME);
		SearchResultsDataInterpreter dataInterpreter = new SearchResultsDataInterpreter(index);
		SearchHitsBatch initialHitsBatch = dataInterpreter.build(data);

		DatabaseDataSource expansionData = new DatabaseDataSource(
				dbCon, "expansion_rms_" + config.get("expansion-collection"));
		DatabaseDataSource expansionQueryData = new DatabaseDataSource(
				dbCon, "expansion_rms_" + config.get("expansion-collection"));
		
		// fbDocs scorer
		DocScorer docScorer = new CachedDocScorer(new DirichletDocScorer(0, cs));
		
		// fbDocs query scorer
		FileLookupDocScorer docScorerQueryProb = new FileLookupDocScorer(config.get("document-probability-data-dir"));
		QueryScorer docQueryScorer = new FileLookupQueryLikelihoodQueryScorer(docScorerQueryProb);
		
		// expansion docs scorer
		DocScorer expansionScorer = new DatabaseLookupDocScorer(expansionData,
				new RelevanceModelDataInterpreter(RelevanceModelDataInterpreter.SCORE_FIELD,
						RelevanceModelDataInterpreter.TERM_FIELD,
						"ORIGINAL_DOCUMENT"));
		
		// expansion docs query scorer
		DocScorer expansionScorerQueryProb = new DatabaseLookupDocScorer(expansionQueryData,
				new QueryProbabilityDataInterpreter(RelevanceModelDataInterpreter.TERM_FIELD,
						RelevanceModelDataInterpreter.SCORE_FIELD,
						"QUERY", "DOCUMENT"));
		QueryScorer expansionQueryScorer = new QueryLikelihoodQueryScorer(expansionScorerQueryProb);

		EntityRMRunner runner = new EntityRMRunner(index, initialHitsBatch,
				stopper, docScorer, docQueryScorer, expansionScorer,
				expansionQueryScorer, expansionClusters, wikiIndex);

		Writer outputWriter = new BufferedWriter(new OutputStreamWriter(System.out));
		FormattedOutputTrecEval output = FormattedOutputTrecEval.getInstance("entityRM3", outputWriter);
				
		Map<String, Double> currentParams = new HashMap<String, Double>();
		for (int origW = 0; origW <= 10; origW++) {
			double origWeight = origW / 10.0;
			currentParams.put(EntityRunner.DOCUMENT_WEIGHT, origWeight);

			double expansionWeight = (10 - origW) / 10.0;
			currentParams.put(EntityRunner.EXPANSION_WEIGHT, expansionWeight);
				
			for (int lambda = 0; lambda <= 10; lambda += 1) {
				double lam = lambda / 10.0;
				currentParams.put(RMRunner.ORIG_QUERY_WEIGHT, lam);

				System.err.println("\t\tParameters: "+origWeight+" (doc), "+expansionWeight+" (expansion), "+lam+" (mixing)");
				SearchHitsBatch batchResults = runner.run(queries, 1000, currentParams);
				
				String run = origWeight + "_" + expansionWeight + "_" + lam;
				output.setRunId(run);
				output.setWriter(new FileWriter(outDir + File.separator + run));
	
				Iterator<String> qit = batchResults.queryIterator();
				while (qit.hasNext()) {
					String query = qit.next();
					output.write(batchResults.getSearchHits(query), query);			
				}
			}
		}
	}

}
