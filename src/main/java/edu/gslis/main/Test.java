package edu.gslis.main;

import java.util.Iterator;

import edu.gslis.eval.Qrels;
import edu.gslis.evaluation.SearchHitsBatch;
import edu.gslis.evaluation.evaluators.Evaluator;
import edu.gslis.evaluation.evaluators.MAPEvaluator;
import edu.gslis.patches.IndexWrapperIndriImpl;
import edu.gslis.queries.GQueries;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.queries.GQuery;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.utils.Configuration;
import edu.gslis.utils.SimpleConfiguration;

public class Test {

	public static void main(String[] args) {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		IndexWrapperIndriImpl index = new IndexWrapperIndriImpl(config.get("index"));
		GQueries queries = new GQueriesJsonImpl();
		queries.read(config.get("queries"));
		Qrels qrels = new Qrels(config.get("qrels"), false, 1);
		
		SearchHitsBatch batch = new SearchHitsBatch();
		Iterator<GQuery> qIt = queries.iterator();
		while (qIt.hasNext()) {
			GQuery query = qIt.next();
			SearchHits results = index.runQuery(query, 1000);
			batch.setSearchHits(query.getTitle(), results);
		}

		Evaluator eval = new MAPEvaluator();
		double map = eval.evaluate(batch, qrels);
		System.out.println(map);
	}

}
