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
import edu.gslis.evaluation.running.runners.DoubleEntityRMRunner;
import edu.gslis.evaluation.running.runners.DoubleEntityRunner;
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

public class RunDoubleEntityRMSweep {
	
	public static void main(String[] args) throws InterruptedException, IOException {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		IndexWrapperIndriImpl index = new IndexWrapperIndriImpl(config.get("index"));
		IndexWrapperIndriImpl wikiIndex = new IndexWrapperIndriImpl(config.get("wiki-index"));
		Stopper stopper = new Stopper(config.get("stoplist"));
		GQueries queries = new GQueriesJsonImpl();
		queries.read(config.get("queries"));
		
		RelatedDocs selfClusters = (new DocumentClusterReader(new File(config.get("document-entities-file-self")))).getClusters();
		RelatedDocs wikiClusters = (new DocumentClusterReader(new File(config.get("document-entities-file-wiki")))).getClusters();

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
		
		SearchHitsBatch initialHitsBatch = (new SearchResultsReader(new File(config.get("initial-hits")), index)).getBatchResults();

		DirichletDocScorerCreator docScorerCreator = new DirichletDocScorerCreator(csSelf);
		ExpansionDocsDocScorerCreator selfScorerCreator = new ExpansionDocsDocScorerCreator(index, selfClusters);
		ExpansionDocsDocScorerCreator wikiScorerCreator = new ExpansionDocsDocScorerCreator(wikiIndex, wikiClusters);

		DoubleEntityRMRunner runner = new DoubleEntityRMRunner(index, initialHitsBatch, stopper,
				docScorerCreator, selfScorerCreator, wikiScorerCreator);
		
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
