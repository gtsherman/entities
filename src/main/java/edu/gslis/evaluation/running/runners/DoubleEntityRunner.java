package edu.gslis.evaluation.running.runners;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.gslis.entities.docscoring.DocScorer;
import edu.gslis.entities.docscoring.FileLookupDocScorer;
import edu.gslis.entities.docscoring.InterpolatedDocScorer;
import edu.gslis.entities.docscoring.QueryLikelihoodQueryScorer;
import edu.gslis.entities.docscoring.QueryScorer;
import edu.gslis.evaluation.evaluators.Evaluator;
import edu.gslis.evaluation.running.QueryRunner;
import edu.gslis.evaluation.running.runners.support.ParameterizedResults;
import edu.gslis.queries.GQueries;
import edu.gslis.queries.GQuery;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.searchhits.SearchHitsBatch;
import edu.gslis.utils.Stopper;

public class DoubleEntityRunner implements QueryRunner {
	
	public static final String DOCUMENT_WEIGHT = "document";
	public static final String WIKI_WEIGHT = "wiki";
	public static final String SELF_WEIGHT = "self";

	private SearchHitsBatch initialResultsBatch;
	private String basePath;
	private Stopper stopper;
	
	private ParameterizedResults processedQueries = new ParameterizedResults();
	
	private int numEntities = 10;
	
	public DoubleEntityRunner(SearchHitsBatch initialResultsBatch, String basePath, Stopper stopper) {
		this.initialResultsBatch = initialResultsBatch;
		this.basePath = basePath;
		this.stopper = stopper;
	}
	
	public Map<String, Double> sweep(GQueries queries, Evaluator evaluator) {
		double maxMetric = 0.0;

		Map<String, Double> bestParams = new HashMap<String, Double>();
		Map<String, Double> currentParams = new HashMap<String, Double>();

		for (int origW = 0; origW <= 10; origW++) {
			double origWeight = origW / 10.0;
			currentParams.put(DoubleEntityRunner.DOCUMENT_WEIGHT, origWeight);
			for (int wikiW = 0; wikiW <= 10-origW; wikiW++) {
				double wikiWeight = wikiW / 10.0;
				double selfWeight = (10-(origW+wikiW)) / 10.0;
				
				currentParams.put(DoubleEntityRunner.WIKI_WEIGHT, wikiWeight);
				currentParams.put(DoubleEntityRunner.SELF_WEIGHT, selfWeight);

				System.err.println("\t\tParameters: " + origWeight + " (doc), " + wikiWeight + " (wiki), " + selfWeight + " (self)");
				SearchHitsBatch batchResults = run(queries, 1000, currentParams);
				
				double metricVal = evaluator.evaluate(batchResults);
				if (metricVal > maxMetric) {
					maxMetric = metricVal;
					bestParams.putAll(currentParams);
				}
			}
		}
	
		System.err.println("\tBest parameters:");
		for (String param : bestParams.keySet()) {
			System.err.println("\t\t" + param + ": "+bestParams.get(param));
		}
		return bestParams;
	}

	public void setNumEntities(int numEntities) {
		this.numEntities = numEntities;
	}
	
	public SearchHitsBatch run(GQueries queries, int numResults, Map<String, Double> params) {
		SearchHitsBatch batchResults = new SearchHitsBatch();
		Iterator<GQuery> queryIt = queries.iterator();
		while (queryIt.hasNext()) {
			GQuery query = queryIt.next();
			SearchHits processedHits = getProcessedQueryResults(query, numResults, params);			
			batchResults.setSearchHits(query.getTitle(), processedHits);
		}
		return batchResults;
	}
	
	private SearchHits getProcessedQueryResults(GQuery query, int numResults, Map<String, Double> params) {
		double[] paramVals = {params.get(DOCUMENT_WEIGHT),
				params.get(SELF_WEIGHT),
				params.get(WIKI_WEIGHT),
				numResults};

		if (!processedQueries.resultsExist(query, paramVals)) {
			query.applyStopper(stopper);
			
			SearchHits initialHits = getInitialHits(query, numResults);

			Iterator<SearchHit> hitIt = initialHits.iterator();
			while (hitIt.hasNext()) {
				SearchHit doc = hitIt.next();

				DocScorer docScorer = new FileLookupDocScorer(basePath + File.separator + "docProbsNew" +
						File.separator + query.getTitle() + File.separator + doc.getDocno());
				DocScorer wikiDocScorer = new FileLookupDocScorer(basePath + File.separator + "entityProbsWikiNew" +
						"." + numEntities + File.separator + query.getTitle() + File.separator + doc.getDocno());
				DocScorer selfDocScorer = new FileLookupDocScorer(basePath + File.separator + "entityProbsSelfNew" +
						"." + numEntities + File.separator + query.getTitle() + File.separator + doc.getDocno());
				
				Map<DocScorer, Double> scorerWeights = new HashMap<DocScorer, Double>();
				scorerWeights.put(docScorer, params.get(DOCUMENT_WEIGHT));
				scorerWeights.put(wikiDocScorer, params.get(WIKI_WEIGHT));
				scorerWeights.put(selfDocScorer, params.get(SELF_WEIGHT));
				
				DocScorer interpolatedScorer = new InterpolatedDocScorer(scorerWeights);
				
				QueryScorer queryScorer = new QueryLikelihoodQueryScorer(interpolatedScorer);
			
				doc.setScore(queryScorer.scoreQuery(query));
			}
			
			initialHits.rank();
			processedQueries.addResults(initialHits, query, paramVals);
		}

		return processedQueries.getResults(query, paramVals);
	}
	
	private SearchHits getInitialHits(GQuery query, int numResults) {
		SearchHits hits = initialResultsBatch.getSearchHits(query);
		SearchHits cropped = new SearchHits(new ArrayList<SearchHit>(hits.hits()));
		if (numResults < hits.size()) {
			cropped.crop(numResults);
		}
		return cropped;
	}

}
