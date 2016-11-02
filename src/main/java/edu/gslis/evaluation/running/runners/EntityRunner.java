package edu.gslis.evaluation.running.runners;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.gslis.evaluation.evaluators.Evaluator;
import edu.gslis.evaluation.running.QueryRunner;
import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.queries.GQueries;
import edu.gslis.queries.GQuery;
import edu.gslis.readers.QueryProbabilityReader;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.searchhits.SearchHitsBatch;
import edu.gslis.utils.Stopper;

public class EntityRunner implements QueryRunner {
	
	private static final Logger logger = LoggerFactory.getLogger(EntityRunner.class);
	
	public static final String DOCUMENT_WEIGHT = "document";
	public static final String WIKI_WEIGHT = "wiki";
	public static final String WIKI_MODEL = "Wiki";
	public static final String SELF_MODEL = "Self";

	private IndexWrapperIndriImpl index;
	private QueryProbabilityReader qpreader;
	private Stopper stopper;

	private int numEntities = 10;
	private String model = WIKI_MODEL;
	
	private Map<GQuery, SearchHits> initialHitsPerQuery;
	
	public EntityRunner(IndexWrapperIndriImpl index, QueryProbabilityReader qpreader, Stopper stopper) {
		this.index = index;
		this.qpreader = qpreader;
		this.stopper = stopper;
		
		this.initialHitsPerQuery = new HashMap<GQuery, SearchHits>();
	}
	
	public Map<String, Double> sweep(GQueries queries, Evaluator evaluator) {
		double maxMetric = 0.0;

		Map<String, Double> bestParams = new HashMap<String, Double>();
		Map<String, Double> currentParams = new HashMap<String, Double>();
		
		for (int origW = 0; origW <= 10; origW++) {
			double origWeight = origW / 10.0;
			currentParams.put(EntityRunner.DOCUMENT_WEIGHT, origWeight);
			for (int wikiW = 0; wikiW <= 10-origW; wikiW++) {
				double wikiWeight = wikiW / 10.0;
				
				currentParams.put(EntityRunner.WIKI_WEIGHT, wikiWeight);

				SearchHitsBatch batchResults = run(queries, 100, currentParams);
				
				double metricVal = evaluator.evaluate(batchResults);
				if (metricVal > maxMetric) {
					maxMetric = metricVal;
					bestParams.putAll(currentParams);
				}
			}
		}
		
		System.err.println("Best parameters:");
		for (String param : bestParams.keySet()) {
			System.err.println(param+": "+bestParams.get(param));
		}
		return bestParams;
	}

	public void setNumEntities(int numEntities) {
		this.numEntities = numEntities;
	}
	
	public void setModel(String model) {
		this.model = model;
	}

	public SearchHitsBatch run(GQueries queries, int numResults, Map<String, Double> params) {
		SearchHitsBatch batchResults = new SearchHitsBatch();
		Iterator<GQuery> queryIt = queries.iterator();
		while (queryIt.hasNext()) {
			GQuery query = queryIt.next();
			query.applyStopper(stopper);
			
			SearchHits initialHits = getInitialHits(query, numResults);

			Iterator<SearchHit> hitIt = initialHits.iterator();
			while (hitIt.hasNext()) {
				SearchHit doc = hitIt.next();

				qpreader.readFileRelative("docProbs/"+query.getTitle()+"/"+doc.getDocno());
				Map<String, Double> termProbsDoc = qpreader.getTermProbs();
				qpreader.readFileRelative("entityProbs"+model+"."+numEntities+"/"+query.getTitle()+"/"+doc.getDocno());
				Map<String, Double> termProbsWiki = qpreader.getTermProbs();

				double logLikelihood = 0.0;
				Iterator<String> queryIterator = query.getFeatureVector().iterator();
				while(queryIterator.hasNext()) {
					String feature = queryIterator.next();
					logger.debug("Scoring feature: "+feature);

					double docProb = termProbsDoc.get(feature);

					double entityWikiProb = termProbsWiki.get(feature);
					logger.debug("Probability for term "+feature+" in wiki: "+entityWikiProb);

					double pr = params.get(DOCUMENT_WEIGHT)*docProb +
							params.get(WIKI_WEIGHT)*entityWikiProb;
					double queryWeight = query.getFeatureVector().getFeatureWeight(feature);
					logLikelihood += queryWeight * Math.log(pr);
				}
			
				doc.setScore(logLikelihood);
			}
			
			initialHits.rank();
			batchResults.setSearchHits(query.getTitle(), initialHits);
		}
		return batchResults;
	}
	
	private SearchHits getInitialHits(GQuery query, int numResults) {
		if (initialHitsPerQuery.containsKey(query)) {
			return initialHitsPerQuery.get(query);
		}
		SearchHits hits = index.runQuery(query, numResults);
		initialHitsPerQuery.put(query, hits);
		return hits;
	}

}
