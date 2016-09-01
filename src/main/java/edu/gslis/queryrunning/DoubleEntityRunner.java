package edu.gslis.queryrunning;

import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.gslis.evaluation.SearchHitsBatch;
import edu.gslis.patches.IndexWrapperIndriImpl;
import edu.gslis.queries.GQuery;
import edu.gslis.readers.QueryProbabilityReader;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.utils.Stopper;

public class DoubleEntityRunner extends GenericRunner {
	
	private static final Logger logger = LoggerFactory.getLogger(DoubleEntityRunner.class);
	
	public static final String DOCUMENT_WEIGHT = "document";
	public static final String WIKI_WEIGHT = "wiki";
	public static final String SELF_WEIGHT = "self";

	private IndexWrapperIndriImpl index;
	private QueryProbabilityReader qpreader;
	private Stopper stopper;
	
	private int numEntities = 10;
	
	public DoubleEntityRunner(IndexWrapperIndriImpl index, QueryProbabilityReader qpreader, Stopper stopper) {
		this.index = index;
		this.qpreader = qpreader;
		this.stopper = stopper;
	}
	
	public void setNumEntities(int numEntities) {
		this.numEntities = numEntities;
	}
	
	public void run(int numResults) {
		batchResults = new SearchHitsBatch();
		Iterator<GQuery> queryIt = queries.iterator();
		while (queryIt.hasNext()) {
			GQuery query = queryIt.next();
			query.applyStopper(stopper);
			
			SearchHits initialHits = index.runQuery(query, numResults);
			Iterator<SearchHit> hitIt = initialHits.iterator();
			while (hitIt.hasNext()) {
				SearchHit doc = hitIt.next();

				qpreader.readFileRelative("docProbs/"+query.getTitle()+"/"+doc.getDocno());
				Map<String, Double> termProbsDoc = qpreader.getTermProbs();
				qpreader.readFileRelative("entityProbsWiki."+numEntities+"/"+query.getTitle()+"/"+doc.getDocno());
				Map<String, Double> termProbsWiki = qpreader.getTermProbs();
				qpreader.readFileRelative("entityProbsSelf."+numEntities+"/"+query.getTitle()+"/"+doc.getDocno());
				Map<String, Double> termProbsSelf = qpreader.getTermProbs();

				double logLikelihood = 0.0;
				Iterator<String> queryIterator = query.getFeatureVector().iterator();
				while(queryIterator.hasNext()) {
					String feature = queryIterator.next();
					logger.debug("Scoring feature: "+feature);

					double docProb = termProbsDoc.get(feature);
					double entityWikiProb = termProbsWiki.get(feature);
					double entitySelfProb = termProbsSelf.get(feature);
					logger.debug("Probability for term "+feature+" in wiki: "+entityWikiProb);
					logger.debug("Probability for term "+feature+" in self: "+entitySelfProb);

					double pr = params.get(DOCUMENT_WEIGHT)*docProb +
							params.get(WIKI_WEIGHT)*entityWikiProb +
							params.get(SELF_WEIGHT)*entitySelfProb;
					double queryWeight = query.getFeatureVector().getFeatureWeight(feature);
					logLikelihood += queryWeight * Math.log(pr);
				}
			
				doc.setScore(logLikelihood);
			}
			
			initialHits.rank();
			batchResults.setSearchHits(query.getTitle(), initialHits);
		}
	}

}
