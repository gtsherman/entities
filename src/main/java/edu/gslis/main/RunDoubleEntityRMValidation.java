package edu.gslis.main;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.gslis.docscoring.support.CollectionStats;
import edu.gslis.docscoring.support.IndexBackedCollectionStats;
import edu.gslis.eval.Qrels;
import edu.gslis.evaluation.evaluators.Evaluator;
import edu.gslis.evaluation.evaluators.MAPEvaluator;
import edu.gslis.evaluation.evaluators.NDCGEvaluator;
import edu.gslis.evaluation.running.runners.DoubleEntityRMRunner;
import edu.gslis.evaluation.running.runners.DoubleEntityRunner;
import edu.gslis.evaluation.validators.KFoldValidator;
import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.output.FormattedOutputTrecEval;
import edu.gslis.queries.GQueries;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.readers.DocumentEntityReader;
import edu.gslis.readers.QueryProbabilityReader;
import edu.gslis.searchhits.SearchHitsBatch;
import edu.gslis.utils.Stopper;
import edu.gslis.utils.config.Configuration;
import edu.gslis.utils.config.SimpleConfiguration;

public class RunDoubleEntityRMValidation {
	
	public static void main(String[] args) throws InterruptedException {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		IndexWrapperIndriImpl index = new IndexWrapperIndriImpl(config.get("index"));
		CollectionStats csSelf = new IndexBackedCollectionStats();
		csSelf.setStatSource(config.get("index"));
		IndexWrapperIndriImpl wikiIndex = new IndexWrapperIndriImpl(config.get("wiki-index"));
		CollectionStats csWiki = new IndexBackedCollectionStats();
		csSelf.setStatSource(config.get("wiki-index"));
		Stopper stopper = new Stopper(config.get("stoplist"));
		GQueries queries = new GQueriesJsonImpl();
		queries.read(config.get("queries"));
		Qrels qrels = new Qrels(config.get("qrels"), false, 1);
		String forQueryProbs = config.get("for-query-probs");
		String targetMetric = config.get("target-metric");
		
		int numEntities = 10;
		if (config.get("num-entities") != null) {
			numEntities = Integer.parseInt(config.get("num-entities"));
		}
		if (args.length > 2) {
			numEntities = Integer.parseInt(args[2]);
		}
		
		DocumentEntityReader deSelf = new DocumentEntityReader();
		deSelf.setLimit(numEntities);
		deSelf.readFileAbsolute(config.get("document-entities-file-self"));

		DocumentEntityReader deWiki = new DocumentEntityReader();
		deWiki.setLimit(numEntities);
		deWiki.readFileAbsolute(config.get("document-entities-file-wiki"));


		Evaluator evaluator = new MAPEvaluator();
		if (targetMetric.equalsIgnoreCase("ndcg")) {
			evaluator = new NDCGEvaluator();
		}
		
		QueryProbabilityReader qpreader = new QueryProbabilityReader();
		qpreader.setBasePath(forQueryProbs);
		
		long seed = Long.parseLong(args[1]);

		Map<IndexWrapperIndriImpl, String> expansionIndexes = new HashMap<IndexWrapperIndriImpl, String>();
		expansionIndexes.put(index, DoubleEntityRunner.SELF_WEIGHT);
		expansionIndexes.put(wikiIndex, DoubleEntityRunner.WIKI_WEIGHT);

		Map<IndexWrapperIndriImpl, DocumentEntityReader> de = new HashMap<IndexWrapperIndriImpl, DocumentEntityReader>();
		de.put(index, deSelf);
		de.put(wikiIndex, deWiki);
		
		Map<String, CollectionStats> cs = new HashMap<String, CollectionStats>();
		cs.put(DoubleEntityRunner.WIKI_WEIGHT, csWiki);
		cs.put(DoubleEntityRunner.SELF_WEIGHT, csSelf);
		
		DoubleEntityRMRunner runner = new DoubleEntityRMRunner(index, stopper, de, cs, expansionIndexes);
		KFoldValidator validator = new KFoldValidator(runner, 10);
		
		SearchHitsBatch batchResults = validator.evaluate(seed, queries, evaluator, qrels);
		
		Writer outputWriter = new BufferedWriter(new OutputStreamWriter(System.out));
		FormattedOutputTrecEval output = FormattedOutputTrecEval.getInstance("entities", outputWriter);
		
		Iterator<String> qit = batchResults.queryIterator();
		while (qit.hasNext()) {
			String query = qit.next();
			output.write(batchResults.getSearchHits(query), query);			
		}
	}

}
