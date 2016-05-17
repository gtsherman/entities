package edu.gslis.queryrunning;

import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.gslis.docscoring.support.CollectionStats;
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
	private CollectionStats cs;
	private Stopper stopper;
	
	public DoubleEntityRunner(IndexWrapperIndriImpl index, QueryProbabilityReader qpreader, CollectionStats cs, Stopper stopper) {
		this.index = index;
		this.qpreader = qpreader;
		this.cs = cs;
		this.stopper = stopper;
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

				qpreader.readFileRelative("entityProbsWiki/"+query.getTitle()+"/"+doc.getDocno());
				Map<String, Double> termProbsWiki = qpreader.getTermProbs();
				qpreader.readFileRelative("entityProbsSelf/"+query.getTitle()+"/"+doc.getDocno());
				Map<String, Double> termProbsSelf = qpreader.getTermProbs();

				double logLikelihood = 0.0;
				Iterator<String> queryIterator = query.getFeatureVector().iterator();
				while(queryIterator.hasNext()) {
					String feature = queryIterator.next();
					logger.debug("Scoring feature: "+feature);

					double docFreq = doc.getFeatureVector().getFeatureWeight(feature);
					double docLength = doc.getLength();

					double entityWikiProb = termProbsWiki.get(feature);
					double entitySelfProb = termProbsSelf.get(feature);
					System.err.println("Probability for term "+feature+" in wiki: "+entityWikiProb);
					System.err.println("Probability for term "+feature+" in self: "+entitySelfProb);

					double collectionProb = (1.0 + cs.termCount(feature)) / cs.getTokCount();

					double pr = params.get(DOCUMENT_WEIGHT)*((docFreq + 
							2500*collectionProb) / (docLength + 2500)) +
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
