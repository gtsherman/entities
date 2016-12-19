package edu.gslis.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.gslis.docscoring.support.PrefetchedCollectionStats;
import edu.gslis.entities.docscoring.ExpansionDocsDocScorer;
import edu.gslis.eval.Qrels;
import edu.gslis.evaluation.evaluators.Evaluator;
import edu.gslis.evaluation.evaluators.MAPEvaluator;
import edu.gslis.evaluation.evaluators.NDCGEvaluator;
import edu.gslis.evaluation.running.runners.EntityRMRunner;
import edu.gslis.evaluation.validators.KFoldValidator;
import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.output.FormattedOutputTrecEval;
import edu.gslis.queries.GQueries;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.queries.GQuery;
import edu.gslis.related_docs.DocumentClusterReader;
import edu.gslis.related_docs.RelatedDocs;
import edu.gslis.scoring.DirichletDocScorer;
import edu.gslis.searchhits.SearchHitsBatch;
import edu.gslis.utils.Stopper;
import edu.gslis.utils.config.Configuration;
import edu.gslis.utils.config.SimpleConfiguration;
import edu.gslis.utils.readers.SearchResultsReader;

public class RunEntityRMValidation {
	
	public static void main(String[] args) throws InterruptedException {
		System.err.println("Begin: "+memUse());
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		System.err.println("After reading config "+memUse());
		
		IndexWrapperIndriImpl index = new IndexWrapperIndriImpl(config.get("index"));
		System.err.println("After loading index: "+memUse());
		IndexWrapperIndriImpl wikiIndex = new IndexWrapperIndriImpl(config.get("wiki-index"));
		System.err.println("After loading wiki-index: "+memUse());

		Stopper stopper = new Stopper(config.get("stoplist"));
		System.err.println("After loading stoplist: "+memUse());

		GQueries queries = new GQueriesJsonImpl();
		queries.read(config.get("queries"));
		System.err.println("After loading queries: "+memUse());

		Qrels qrels = new Qrels(config.get("qrels"), false, 1);
		System.err.println("After loading qrels: "+memUse());

		String targetMetric = config.get("target-metric");
		
		RelatedDocs expansionClusters = (new DocumentClusterReader(new File(config.get("document-entities-file")))).getClusters();
		System.err.println("After loading clusters: "+memUse());

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
		System.err.println("After loading col stats: "+memUse());

		Evaluator evaluator = new MAPEvaluator(qrels);
		if (targetMetric.equalsIgnoreCase("ndcg")) {
			evaluator = new NDCGEvaluator(qrels);
		}
		System.err.println("After instantiating evaluator: "+memUse());
		
		SearchHitsBatch initialHitsBatch = (new SearchResultsReader(new File(config.get("initial-hits")), index)).getBatchResults();
		System.err.println("After reading initial hits: "+memUse());

		long seed = Long.parseLong(args[1]);
		
		DirichletDocScorer docScorer = new DirichletDocScorer(cs);
		ExpansionDocsDocScorer expansionScorer = new ExpansionDocsDocScorer(wikiIndex, expansionClusters);
		System.err.println("After instantiating docscorers: "+memUse());

		EntityRMRunner runner = new EntityRMRunner(index, initialHitsBatch, stopper, docScorer, expansionScorer);
		System.err.println("After instantiating runner: "+memUse());
		KFoldValidator validator = new KFoldValidator(runner, 10);
		System.err.println("After instantiating validator: "+memUse());
		
		SearchHitsBatch batchResults = validator.evaluate(seed, queries, evaluator);
	
		Writer outputWriter = new BufferedWriter(new OutputStreamWriter(System.out));
		FormattedOutputTrecEval output = FormattedOutputTrecEval.getInstance("entityRM3", outputWriter);
		
		Iterator<String> qit = batchResults.queryIterator();
		while (qit.hasNext()) {
			String query = qit.next();
			output.write(batchResults.getSearchHits(query), query);			
		}
	}
	
	public static double memUse() {
		return ((double)((double)(Runtime.getRuntime().totalMemory()/1024)/1024)) - ((double)((double)(Runtime.getRuntime().freeMemory()/1024)/1024));
		//return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
	}

}
