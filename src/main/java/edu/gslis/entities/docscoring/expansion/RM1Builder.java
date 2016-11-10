package edu.gslis.entities.docscoring.expansion;

import java.util.Iterator;

import edu.gslis.docscoring.support.CollectionStats;
import edu.gslis.entities.docscoring.DirichletDocScorer;
import edu.gslis.entities.docscoring.DocScorer;
import edu.gslis.entities.docscoring.QueryLikelihoodQueryScorer;
import edu.gslis.entities.docscoring.QueryScorer;
import edu.gslis.indexes.IndexWrapper;
import edu.gslis.queries.GQuery;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;

public class RM1Builder {
	
	public static final int DEFAULT_FEEDBACK_DOCS = 20;
	public static final int DEFAULT_FEEDBACK_TERMS = 20;
	
	private int feedbackDocs;
	private int feedbackTerms;
	
	private GQuery query;
	private SearchHits initialHits;
	private CollectionStats collectionStats;
	
	public RM1Builder(GQuery query, SearchHits initialHits, int feedbackDocs, int feedbackTerms, CollectionStats collectionStats) {
		setFeedbackDocs(feedbackDocs);
		setFeedbackTerms(feedbackTerms);
		setQuery(query, initialHits);
		this.collectionStats = collectionStats;
	}
	
	public RM1Builder(GQuery query, SearchHits initialHits, CollectionStats collectionStats) {
		this(query, initialHits, DEFAULT_FEEDBACK_DOCS, DEFAULT_FEEDBACK_TERMS, collectionStats);
	}
	
	public RM1Builder(GQuery query, IndexWrapper index, int feedbackDocs, int feedbackTerms, CollectionStats collectionStats) {
		setFeedbackDocs(feedbackDocs);
		setFeedbackTerms(feedbackTerms);
		setQuery(query, index);
		this.collectionStats = collectionStats;
	}

	public RM1Builder(GQuery query, IndexWrapper index, CollectionStats collectionStats) {
		this(query, index, DEFAULT_FEEDBACK_DOCS, DEFAULT_FEEDBACK_TERMS, collectionStats);
	}
	
	public void setFeedbackDocs(int feedbackDocs) {
		this.feedbackDocs = feedbackDocs;
	}
	
	public void setFeedbackTerms(int feedbackTerms) {
		this.feedbackTerms = feedbackTerms;
	}
	
	public void setQuery(GQuery query, SearchHits initialHits) {
		this.query = query;
		this.initialHits = initialHits;
	}
	
	public void setQuery(GQuery query, IndexWrapper index) {
		this.query = query;
		this.initialHits = index.runQuery(query, feedbackDocs);
	}
	
	public FeatureVector buildRelevanceModel() {
		return buildRelevanceModel(null);
	}

	public FeatureVector buildRelevanceModel(Stopper stopper) {
		FeatureVector termScores = new FeatureVector(stopper);
		
		initialHits.crop(feedbackDocs);
		Iterator<SearchHit> hitIt = initialHits.iterator();
		while (hitIt.hasNext()) {
			SearchHit hit = hitIt.next();
			
			// Prep the scorers
			QueryScorer queryScorer = new QueryLikelihoodQueryScorer(new DirichletDocScorer(hit, collectionStats));
			DocScorer rmScorer = new RelevanceModelScorer(new DirichletDocScorer(0, hit, collectionStats),
					Math.exp(queryScorer.scoreQuery(query)));
				
			// Score each term
			for (String term : hit.getFeatureVector().getFeatures()) {
				if (stopper != null && stopper.isStopWord(term)) {
					continue;
				}
				termScores.addTerm(term, rmScorer.scoreTerm(term)/initialHits.size());
			}
		}
		termScores.clip(feedbackTerms);
		return termScores;
	}

}