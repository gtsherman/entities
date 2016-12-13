package edu.gslis.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import edu.gslis.docscoring.support.PrefetchedCollectionStats;
import edu.gslis.entities.docscoring.creators.ExpansionDocsDocScorerCreator;
import edu.gslis.evaluation.running.runners.EntityRMRunner;
import edu.gslis.evaluation.running.runners.EntityRunner;
import edu.gslis.evaluation.running.runners.RMRunner;
import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.output.FormattedOutputTrecEval;
import edu.gslis.queries.GQueries;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.queries.GQuery;
import edu.gslis.related_docs.DocumentClusterReader;
import edu.gslis.related_docs.RelatedDocs;
import edu.gslis.scoring.creators.DirichletDocScorerCreator;
import edu.gslis.searchhits.SearchHitsBatch;
import edu.gslis.utils.Stopper;
import edu.gslis.utils.config.Configuration;
import edu.gslis.utils.config.SimpleConfiguration;
import edu.gslis.utils.readers.SearchResultsReader;

public class RunEntityRMSweep {
	
	public static void main(String[] args) throws InterruptedException, IOException {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		IndexWrapperIndriImpl index = new IndexWrapperIndriImpl(config.get("index"));
		IndexWrapperIndriImpl wikiIndex = new IndexWrapperIndriImpl(config.get("wiki-index"));

		Stopper stopper = new Stopper(config.get("stoplist"));

		GQueries queries = new GQueriesJsonImpl();
		queries.read(config.get("queries"));

		RelatedDocs expansionClusters = (new DocumentClusterReader(new File(config.get("document-entities-file")))).getClusters();

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

		SearchHitsBatch initialHitsBatch = (new SearchResultsReader(new File(config.get("initial-hits")), index)).getBatchResults();

		DirichletDocScorerCreator docScorerCreator = new DirichletDocScorerCreator(cs);
		ExpansionDocsDocScorerCreator expansionScorerCreator = new ExpansionDocsDocScorerCreator(wikiIndex, expansionClusters);

		Writer outputWriter = new BufferedWriter(new OutputStreamWriter(System.out));
		FormattedOutputTrecEval output = FormattedOutputTrecEval.getInstance("entityRM3", outputWriter);
				
		EntityRMRunner runner = new EntityRMRunner(index, initialHitsBatch, stopper, docScorerCreator, expansionScorerCreator);
		
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
