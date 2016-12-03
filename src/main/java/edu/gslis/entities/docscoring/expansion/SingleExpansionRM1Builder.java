package edu.gslis.entities.docscoring.expansion;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.gslis.entities.docscoring.creators.ExpansionDocsDocScorerCreator;
import edu.gslis.evaluation.running.runners.EntityRunner;
import edu.gslis.queries.GQuery;
import edu.gslis.scoring.DocScorer;
import edu.gslis.scoring.InterpolatedDocScorer;
import edu.gslis.scoring.creators.DirichletDocScorerCreator;
import edu.gslis.scoring.expansion.RelevanceModelScorer;
import edu.gslis.scoring.queryscoring.QueryLikelihoodQueryScorer;
import edu.gslis.scoring.queryscoring.QueryScorer;
import edu.gslis.searchhits.IndexBackedSearchHit;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;

public class SingleExpansionRM1Builder {
	
	public static final int DEFAULT_FEEDBACK_DOCS = 20;
	public static final int DEFAULT_FEEDBACK_TERMS = 20;
	
	private int feedbackDocs;
	private int feedbackTerms;
	
	private GQuery query;
	private SearchHits initialHits;
	private DirichletDocScorerCreator docScorerCreator;
	private ExpansionDocsDocScorerCreator expansionScorerCreator;
	
	public SingleExpansionRM1Builder(GQuery query,
			SearchHits initialHits, 
			DirichletDocScorerCreator docScorerCreator, 
			ExpansionDocsDocScorerCreator expansionScorerCreator,
			int feedbackDocs,
			int feedbackTerms) {
		this.docScorerCreator = docScorerCreator;
		this.expansionScorerCreator = expansionScorerCreator;
		setFeedbackDocs(feedbackDocs);
		setFeedbackTerms(feedbackTerms);
		setQuery(query, initialHits);
	}
	
	public SingleExpansionRM1Builder(GQuery query,
			SearchHits initialHits,
			DirichletDocScorerCreator docScorerCreator,
			ExpansionDocsDocScorerCreator expansionScorerCreator) {
		this(query, initialHits, docScorerCreator, expansionScorerCreator,
				DEFAULT_FEEDBACK_DOCS, DEFAULT_FEEDBACK_TERMS);
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
	
	public FeatureVector buildRelevanceModel(Map<String, Double> params) {
		return buildRelevanceModel(null, params);
	}

	public FeatureVector buildRelevanceModel(Stopper stopper, Map<String, Double> params) {
		FeatureVector termScores = new FeatureVector(stopper);
		
		int i = 0;
		Iterator<SearchHit> hitIt = initialHits.iterator();
		while (hitIt.hasNext() && i < feedbackDocs) {
			SearchHit hit = hitIt.next();
			i++;
			
			// Prep the scorers
			// Set mu to whatever the client provided, since it will be set to 0 after each hit is scored
			docScorerCreator.setMu(-1);
			expansionScorerCreator.setMu(-1);
			DocScorer docScorer = docScorerCreator.getDocScorer(hit);
			DocScorer expansionScorer = expansionScorerCreator.getDocScorer(hit);
			
			// Combine into an interpolated scorer
			Map<DocScorer, Double> docScorers = new HashMap<DocScorer, Double>();
			docScorers.put(docScorer, params.get(EntityRunner.DOCUMENT_WEIGHT)); // P(q|D)
			docScorers.put(expansionScorer, params.get(EntityRunner.EXPANSION_WEIGHT)); // P(q|E)
			DocScorer interpolatedScorer = new InterpolatedDocScorer(docScorers); // lambda
			
			// Score the query
			QueryScorer queryScorer = new QueryLikelihoodQueryScorer(interpolatedScorer);
			double queryScore = Math.exp(queryScorer.scoreQuery(query)); // P(Q|D)
			
			// Prep the 0-mu scorers
			// This is important because RM1 performs much better with mu == 0 for non-query terms
			docScorerCreator.setMu(0);
			expansionScorerCreator.setMu(0);
			docScorer = docScorerCreator.getDocScorer(hit);
			expansionScorer = expansionScorerCreator.getDocScorer(hit);
			
			// Combine into an interpolated scorer
			docScorers = new HashMap<DocScorer, Double>();
			docScorers.put(docScorer, params.get(EntityRunner.DOCUMENT_WEIGHT)); // P(w|D)
			docScorers.put(expansionScorer, params.get(EntityRunner.EXPANSION_WEIGHT)); // P(w|E)
			interpolatedScorer = new InterpolatedDocScorer(docScorers);	// lambda
			
			DocScorer rmScorer = new RelevanceModelScorer(interpolatedScorer, queryScore);
				
			FeatureVector terms = new FeatureVector(stopper);
			for (String term : hit.getFeatureVector().getFeatures()) {
				if (stopper != null && stopper.isStopWord(term)) {
					continue;
				}
				terms.addTerm(term, hit.getFeatureVector().getFeatureWeight(term));
			}
			for (String docno : expansionScorerCreator.getClusters().getDocsRelatedTo(hit).keySet()) {
				SearchHit expansionHit = new IndexBackedSearchHit(expansionScorerCreator.getIndex());
				expansionHit.setDocno(docno);
				for (String term : expansionHit.getFeatureVector().getFeatures()) {
					if (stopper != null && stopper.isStopWord(term)) {
						continue;
					}
					terms.addTerm(term, expansionHit.getFeatureVector().getFeatureWeight(term));
				}
			}
			terms.clip(50);
			
			Iterator<String> termit = terms.iterator();
			while (termit.hasNext()) {
				String term = termit.next();
				termScores.addTerm(term, rmScorer.scoreTerm(term)/initialHits.size());
			}
		}

		termScores.clip(feedbackTerms);
		return termScores;
	}

}
