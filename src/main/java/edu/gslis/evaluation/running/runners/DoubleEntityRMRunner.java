package edu.gslis.evaluation.running.runners;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.gslis.docscoring.support.CollectionStats;
import edu.gslis.eval.Qrels;
import edu.gslis.evaluation.evaluators.Evaluator;
import edu.gslis.evaluation.running.QueryRunner;
import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.main.PrecomputeExpansionRMs;
import edu.gslis.queries.GQueries;
import edu.gslis.queries.GQuery;
import edu.gslis.queries.expansion.FeedbackRelevanceModel;
import edu.gslis.readers.DocumentEntityReader;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.searchhits.SearchHitsBatch;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;
import edu.gslis.utils.readers.RelevanceModelReader;

public class DoubleEntityRMRunner implements QueryRunner {
	
	private static final int mu = 2500;
	
	private IndexWrapperIndriImpl index;
	private IndexWrapperIndriImpl wikiIndex;
	private Stopper stopper;
	private DocumentEntityReader deSelf;
	private DocumentEntityReader deWiki;
	private Map<String, CollectionStats> cs;
	private String expansionRMsDir;
	
	private Map<GQuery, SearchHits> initialHitsPerQuery;
	
	public DoubleEntityRMRunner(IndexWrapperIndriImpl index,
			IndexWrapperIndriImpl wikiIndex,
			Stopper stopper,
			DocumentEntityReader deSelf,
			DocumentEntityReader deWiki,
			Map<String, CollectionStats> cs,
			String expansionRMsDir) {
		this.index = index;
		this.wikiIndex = wikiIndex;
		this.stopper = stopper;
		this.deSelf = deSelf;
		this.deWiki = deWiki;
		this.cs = cs;
		this.expansionRMsDir = expansionRMsDir;
	}

	public Map<String, Double> sweep(GQueries queries, Evaluator evaluator, Qrels qrels) {
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
				
				for (int lambda = 0; lambda <= 10; lambda += 1) {
					double lam = lambda / 10.0;
					currentParams.put(RMRunner.ORIG_QUERY_WEIGHT, lam);

					System.err.println("\t\tParameters: "+origWeight+" (doc), "+wikiWeight+" (wiki), "+selfWeight+" (self), "+lam+" (mixing)");
					SearchHitsBatch batchResults = run(queries, 100, currentParams);

					double metricVal = evaluator.evaluate(batchResults, qrels);
					if (metricVal > maxMetric) {
						maxMetric = metricVal;
						bestParams.putAll(currentParams);
					}
				}
			}
		}
		
		return bestParams;
	}

	public SearchHitsBatch run(GQueries queries, int numResults, Map<String, Double> params) {
		SearchHitsBatch batchResults = new SearchHitsBatch();
		
		Iterator<GQuery> qIt = queries.iterator();
		while (qIt.hasNext()) {
			GQuery query = qIt.next();
			query.applyStopper(stopper);
			
			if (!query.getTitle().equals("462"))
				continue;
			
			int fbDocs = 20;
			try {
				fbDocs = params.get(RMRunner.FEEDBACK_DOCUMENTS).intValue();
			} catch (NullPointerException e) {
				
			}
			int fbTerms = 20;
			try {
				params.get(RMRunner.FEEDBACK_TERMS).intValue();
			} catch (NullPointerException e) {
				
			}
			
			SearchHits initialHits = getInitialHits(query, numResults);
			
			Iterator<SearchHit> hitIt = initialHits.iterator();
			while (hitIt.hasNext()) {
				SearchHit hit = hitIt.next();
				hit.setFeatureVector(index.getDocVector(hit.getDocID(), null));
				
				File wikiFile = new File(expansionRMsDir+"/"+hit.getDocno()+"/wiki");
				if (!wikiFile.exists()) {
					System.err.println("Document "+hit.getDocno()+" not expanded. Expanding...");
					try {
						PrecomputeExpansionRMs.compute(query, index, wikiIndex, stopper, deSelf, deWiki, expansionRMsDir);
					} catch (IOException e) {
						System.err.println("Error creating file "+wikiFile.getAbsolutePath());
					}
				}
				RelevanceModelReader wikiReader = new RelevanceModelReader(wikiFile);
				FeatureVector wiki = wikiReader.getFeatureVector();
				
				File selfFile = new File(expansionRMsDir+"/"+hit.getDocno()+"/self");
				RelevanceModelReader selfReader = new RelevanceModelReader(selfFile);
				FeatureVector self = selfReader.getFeatureVector();
				FeatureVector doc = hit.getFeatureVector();
				
				double wikiWeight = params.get(DoubleEntityRunner.WIKI_WEIGHT);
				double selfWeight = params.get(DoubleEntityRunner.SELF_WEIGHT);
				double docWeight = params.get(DoubleEntityRunner.DOCUMENT_WEIGHT);
				
				double logLikelihood = 0.0;
				Iterator<String> queryIterator = query.getFeatureVector().iterator();
				while(queryIterator.hasNext()) {
					String feature = queryIterator.next();
					
					double collectionScoreSelf = (1.0 + cs.get(DoubleEntityRunner.SELF_WEIGHT).termCount(feature)) / cs.get(DoubleEntityRunner.SELF_WEIGHT).getTokCount();
					double collectionScoreWiki = (1.0 + cs.get(DoubleEntityRunner.SELF_WEIGHT).termCount(feature)) / cs.get(DoubleEntityRunner.SELF_WEIGHT).getTokCount();
					double docProb = (doc.getFeatureWeight(feature) + mu*collectionScoreSelf) / (doc.getLength() + mu);
					double entityWikiProb = (wiki.getFeatureWeight(feature) + mu*collectionScoreWiki) / (wiki.getLength() + mu);
					double entitySelfProb = (self.getFeatureWeight(feature) + mu*collectionScoreSelf) / (self.getLength() + mu);

					double pr = docWeight*docProb +
							wikiWeight*entityWikiProb +
							selfWeight*entitySelfProb;
					double queryWeight = query.getFeatureVector().getFeatureWeight(feature);
					logLikelihood += queryWeight * Math.log(pr);
				}
				hit.setScore(logLikelihood);
				
				FeatureVector wikiSelf = FeatureVector.interpolate(wiki, self, wikiWeight/(wikiWeight+selfWeight));
				FeatureVector combined = FeatureVector.interpolate(doc, wikiSelf, docWeight);
				hit.setFeatureVector(combined);
			}
			
			initialHits.rank();
			initialHits.crop(fbDocs);
			
			FeedbackRelevanceModel rm = new FeedbackRelevanceModel();
			rm.setDocCount(fbDocs);
			rm.setTermCount(fbTerms);
			rm.setIndex(index);
			rm.setRes(initialHits);
			rm.setStopper(stopper);
			rm.setOriginalQuery(query);
			rm.build();
			FeatureVector rmVec = rm.asGquery().getFeatureVector();
			rmVec.normalize();	
			
			FeatureVector rm3 = FeatureVector.interpolate(query.getFeatureVector(), rmVec, params.get(RMRunner.ORIG_QUERY_WEIGHT));
			rm3.normalize();
			
			GQuery newQuery = new GQuery();
			newQuery.setTitle(query.getTitle());
			newQuery.setFeatureVector(rm3);
			
			try {
				SearchHits results = index.runQuery(newQuery, numResults);
				batchResults.setSearchHits(query.getTitle(), results);	
			} catch (Exception e) {
				System.err.println("Problem with RM:");
				System.err.println(rm3.toString(5));
				System.err.println(e.getStackTrace());
			}
		}
		return batchResults;
	}
	
	private SearchHits getInitialHits(GQuery query, int count) {
		if (initialHitsPerQuery == null) {
			initialHitsPerQuery = new HashMap<GQuery, SearchHits>();
		}
		if (initialHitsPerQuery.containsKey(query)) {
			return initialHitsPerQuery.get(query);
		}
		SearchHits hits = index.runQuery(query, count);
		initialHitsPerQuery.put(query, hits);
		return hits;
	}

}
