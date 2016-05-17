package edu.gslis.evaluation.evaluators;

import java.util.Iterator;

import edu.gslis.eval.Qrels;
import edu.gslis.evaluation.SearchHitsBatch;
import edu.gslis.queries.GQuery;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;

public class MAPEvaluator implements Evaluator {

	public double evaluate(SearchHitsBatch batchResults, Qrels qrels) {
		return meanAveragePrecision(batchResults, qrels);
	}
	
	public double meanAveragePrecision(SearchHitsBatch batchResults, Qrels qrels) {
		double map = 0.0;

		Iterator<String> queryIt = batchResults.queryIterator();
		while (queryIt.hasNext()) {
			String query = queryIt.next();
			SearchHits results = batchResults.getSearchHits(query);
			double ap = averagePrecision(query, results, qrels);
			System.err.println(query+" ap: "+ap);
			map += ap;
		}
		
		System.err.println("Number of queries: "+batchResults.getNumQueries());
		map /= batchResults.getNumQueries();
		return map;
	}
	
	public double averagePrecision(GQuery query, SearchHits results, Qrels qrels) {
		return averagePrecision(query.getTitle(), results, qrels);
	}
	
	public double averagePrecision(String query, SearchHits results, Qrels qrels) {
		results.rank();

		double ap = 0.0;

		int rels = 0;
		int seen = 0;

		Iterator<SearchHit> resultIt = results.iterator();
		while (resultIt.hasNext()) {
			SearchHit result = resultIt.next();
			seen++;
			
			if (qrels.isRel(query, result.getDocno())) {
				rels++;
				ap += rels / (double)seen;
			}
		}
	
		try {
			ap /= qrels.getRelDocs(query).size();
		} catch (NullPointerException e) {
			System.err.println("Error with AP division for "+query+".)");
			if (ap > 0) {
				System.err.println("Somehow we have seen rel docs but don't have rel docs now.");
				System.err.println("AP="+ap);
				System.exit(-1);
			} else {
				System.err.println("At least we don't have a positive AP.");
				return ap;
			}
		}

		if (Double.isNaN(ap))
			ap = 0.0;
		return ap;
	}
}
