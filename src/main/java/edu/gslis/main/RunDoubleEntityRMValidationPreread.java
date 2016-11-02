package edu.gslis.main;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.gslis.docscoring.support.PrefetchedCollectionStats;
import edu.gslis.eval.Qrels;
import edu.gslis.evaluation.evaluators.Evaluator;
import edu.gslis.evaluation.evaluators.MAPEvaluator;
import edu.gslis.evaluation.evaluators.NDCGEvaluator;
import edu.gslis.evaluation.running.runners.DoubleEntityRMRunner;
import edu.gslis.evaluation.validators.KFoldValidator;
import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.output.FormattedOutputTrecEval;
import edu.gslis.queries.GQueries;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.queries.GQuery;
import edu.gslis.readers.DocumentEntityReader;
import edu.gslis.readers.QueryProbabilityReader;
import edu.gslis.readers.RMPrereader;
import edu.gslis.searchhits.SearchHitsBatch;
import edu.gslis.utils.Stopper;
import edu.gslis.utils.config.Configuration;
import edu.gslis.utils.config.SimpleConfiguration;

public class RunDoubleEntityRMValidationPreread {
	
	public static void main(String[] args) throws InterruptedException {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		IndexWrapperIndriImpl index = new IndexWrapperIndriImpl(config.get("index"));
		IndexWrapperIndriImpl wikiIndex = new IndexWrapperIndriImpl(config.get("wiki-index"));
		Stopper stopper = new Stopper(config.get("stoplist"));
		GQueries queries = new GQueriesJsonImpl();
		queries.read(config.get("queries"));
		Qrels qrels = new Qrels(config.get("qrels"), false, 1);
		String forQueryProbs = config.get("for-query-probs");
		String targetMetric = config.get("target-metric");
		String expansionRMsDir = config.get("expansion-rms-dir");
		
		DocumentEntityReader deSelf = new DocumentEntityReader();
		deSelf.setLimit(10);
		deSelf.readFileAbsolute(config.get("document-entities-file-self"));

		DocumentEntityReader deWiki = new DocumentEntityReader();
		deWiki.setLimit(10);
		deWiki.readFileAbsolute(config.get("document-entities-file-wiki"));
		
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
		PrefetchedCollectionStats csWiki = new PrefetchedCollectionStats(config.get("wiki-index"), terms);

		Evaluator evaluator = new MAPEvaluator();
		if (targetMetric.equalsIgnoreCase("ndcg")) {
			evaluator = new NDCGEvaluator();
		}
		
		QueryProbabilityReader qpreader = new QueryProbabilityReader();
		qpreader.setBasePath(forQueryProbs);
		
		RMPrereader rmpr = new RMPrereader(expansionRMsDir);
		
		long seed = Long.parseLong(args[1]);
		
		DoubleEntityRMRunner runner = new DoubleEntityRMRunner(wikiIndex, wikiIndex, stopper, deWiki, deWiki, csSelf, csWiki, expansionRMsDir);
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
