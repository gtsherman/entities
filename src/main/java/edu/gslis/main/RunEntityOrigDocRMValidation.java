package edu.gslis.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;

import edu.gslis.docscoring.support.CollectionStats;
import edu.gslis.docscoring.support.IndexBackedCollectionStats;
import edu.gslis.entities.docscoring.ExpansionDocsDocScorer;
import edu.gslis.eval.Qrels;
import edu.gslis.evaluation.evaluators.Evaluator;
import edu.gslis.evaluation.evaluators.MAPEvaluator;
import edu.gslis.evaluation.evaluators.NDCGEvaluator;
import edu.gslis.evaluation.running.runners.EntityOrigDocRMRunner;
import edu.gslis.evaluation.validators.KFoldValidator;
import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.output.FormattedOutputTrecEval;
import edu.gslis.queries.GQueries;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.related_docs.DocumentClusterDataInterpreter;
import edu.gslis.related_docs.DocumentClusterDataSource;
import edu.gslis.related_docs.RelatedDocs;
import edu.gslis.scoring.CachedDocScorer;
import edu.gslis.scoring.DirichletDocScorer;
import edu.gslis.scoring.DocScorer;
import edu.gslis.searchhits.SearchHitsBatch;
import edu.gslis.utils.Stopper;
import edu.gslis.utils.config.Configuration;
import edu.gslis.utils.config.SimpleConfiguration;
import edu.gslis.utils.data.factory.DataSourceFactory;
import edu.gslis.utils.data.interpreters.SearchResultsDataInterpreter;
import edu.gslis.utils.data.sources.DataSource;

public class RunEntityOrigDocRMValidation {
	
	public static void main(String[] args) throws InterruptedException {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		IndexWrapperIndriImpl index = new IndexWrapperIndriImpl(config.get("index"));
		IndexWrapperIndriImpl wikiIndex = new IndexWrapperIndriImpl(config.get("wiki-index"));

		Stopper stopper = new Stopper(config.get("stoplist"));

		GQueries queries = new GQueriesJsonImpl();
		queries.read(config.get("queries"));

		Qrels qrels = new Qrels(config.get("qrels"), false, 1);

		String targetMetric = config.get("target-metric");
		
		String rmDir = config.get("rms-dir");
		
		RelatedDocs expansionClusters = (new DocumentClusterDataInterpreter()).build(
				new DocumentClusterDataSource(new File(config.get("document-entities-file"))));

		CollectionStats cs = new IndexBackedCollectionStats();
		cs.setStatSource(config.get("index"));

		Evaluator evaluator = new MAPEvaluator(qrels);
		if (targetMetric.equalsIgnoreCase("ndcg")) {
			evaluator = new NDCGEvaluator(qrels);
		}
		
		DataSource data = DataSourceFactory.getDataSource(config.get("intial-hits"),
				config.get("database"), SearchResultsDataInterpreter.DATA_NAME);
		SearchResultsDataInterpreter dataInterpreter = new SearchResultsDataInterpreter(index);
		SearchHitsBatch initialHitsBatch = dataInterpreter.build(data);

		long seed = Long.parseLong(args[1]);
		
		DocScorer docScorer = new CachedDocScorer(new DirichletDocScorer(cs));
		DocScorer expansionScorer = new ExpansionDocsDocScorer(wikiIndex, expansionClusters);

		EntityOrigDocRMRunner runner = new EntityOrigDocRMRunner(initialHitsBatch, stopper, rmDir, docScorer, expansionScorer);
		KFoldValidator validator = new KFoldValidator(runner, 10);
		
		SearchHitsBatch batchResults = validator.evaluate(seed, queries, evaluator);
		
		Writer outputWriter = new BufferedWriter(new OutputStreamWriter(System.out));
		FormattedOutputTrecEval output = FormattedOutputTrecEval.getInstance("origDocRM3WithExp", outputWriter);
		
		Iterator<String> qit = batchResults.queryIterator();
		while (qit.hasNext()) {
			String query = qit.next();
			output.write(batchResults.getSearchHits(query), query);			
		}
	}

}
