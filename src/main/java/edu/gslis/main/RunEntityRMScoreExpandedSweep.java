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
import edu.gslis.entities.docscoring.ExpansionDocsDocScorer;
import edu.gslis.entities.docscoring.RMDatabaseLookupDocScorer;
import edu.gslis.evaluation.running.runners.EntityRunner;
import edu.gslis.evaluation.running.runners.RMRunner;
import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.output.FormattedOutputTrecEval;
import edu.gslis.queries.GQueries;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.queries.GQuery;
import edu.gslis.related_docs.DocumentClusterDataInterpreter;
import edu.gslis.related_docs.DocumentClusterDataSource;
import edu.gslis.related_docs.RelatedDocs;
import edu.gslis.scoring.CachedDocScorer;
import edu.gslis.scoring.DirichletDocScorer;
import edu.gslis.scoring.DocScorer;
import edu.gslis.searchhits.SearchHitsBatch;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;
import edu.gslis.utils.config.Configuration;
import edu.gslis.utils.config.SimpleConfiguration;
import edu.gslis.utils.data.interpreters.RelevanceModelDataInterpreter;
import edu.gslis.utils.data.interpreters.SearchResultsDataInterpreter;
import edu.gslis.utils.data.sources.DatabaseDataSource;
import edu.gslis.utils.data.sources.FileDataSource;

public class RunEntityRMScoreExpandedSweep {
	
	public static void main(String[] args) throws InterruptedException, IOException {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		IndexWrapperIndriImpl index = new IndexWrapperIndriImpl(config.get("index"));
		IndexWrapperIndriImpl wikiIndex = new IndexWrapperIndriImpl(config.get("wiki-index"));

		Stopper stopper = new Stopper(config.get("stoplist"));

		GQueries queries = new GQueriesJsonImpl();
		queries.read(config.get("queries"));
		
		String rmsDir = config.get("rms-dir");
		RelevanceModelDataInterpreter rmInterpreter = new RelevanceModelDataInterpreter();
		
		System.err.println("Reading clusters");
		DocumentClusterDataInterpreter clusterInterpreter = new DocumentClusterDataInterpreter();
		RelatedDocs clusters = clusterInterpreter.build(
				new DocumentClusterDataSource(
						new File(config.get("document-entities-file"))));

		System.err.println("Prefetching collection stats");
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
		
		System.err.println("Reading initial results");
		DatabaseDataSource data = new DatabaseDataSource(dbCon, SearchResultsDataInterpreter.DATA_NAME);
		data.read();
		SearchResultsDataInterpreter dataInterpreter = new SearchResultsDataInterpreter(index);
		SearchHitsBatch initialHitsBatch = dataInterpreter.build(data);

		DatabaseDataSource expansionData = new DatabaseDataSource(
				dbCon, "expansion_rms_" + config.get("expansion-collection"));
		
		// expansion docs scorer
		DocScorer expansionScorer = new CachedDocScorer(new RMDatabaseLookupDocScorer(expansionData,
				new RelevanceModelDataInterpreter(RelevanceModelDataInterpreter.SCORE_FIELD,
						RelevanceModelDataInterpreter.TERM_FIELD,
						"ORIGINAL_DOCUMENT")));
		
		DocScorer docScorerForRescore = new CachedDocScorer(new DirichletDocScorer(cs));
		DocScorer expansionScorerForRescore = new CachedDocScorer(
				new DatabaseLookupDocScorer(
						expansionScorer,
						new ExpansionDocsDocScorer(wikiIndex, clusters)));

		EntityRunner runner = new EntityRunner(initialHitsBatch,
				stopper, docScorerForRescore, expansionScorerForRescore);

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
				
				GQueries rm3Queries = new GQueriesJsonImpl();
				for (GQuery query : queries) {
					query.applyStopper(stopper);
					
					GQuery rm3Version = new GQuery();
					rm3Version.setTitle(query.getTitle());
					
					FileDataSource rmFile = new FileDataSource(
							new File(
									rmsDir + File.separator + origWeight +
									"_" + expansionWeight + "_" + query.getTitle()));
					rm3Version.setFeatureVector(FeatureVector.interpolate(query.getFeatureVector(), 
							rmInterpreter.build(rmFile), lam));
					rm3Queries.addQuery(rm3Version);
				}

				System.err.println("\t\tParameters: "+origWeight+" (doc), "+expansionWeight+" (expansion), "+lam+" (mixing)");
				SearchHitsBatch batchResults = runner.run(rm3Queries, 1000, currentParams);
				
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
