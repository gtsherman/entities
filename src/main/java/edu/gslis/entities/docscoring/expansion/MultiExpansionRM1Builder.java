package edu.gslis.entities.docscoring.expansion;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.gslis.entities.docscoring.creators.ExpansionDocsDocScorerCreator;
import edu.gslis.evaluation.running.runners.DoubleEntityRunner;
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

public class MultiExpansionRM1Builder implements ExpansionRM1Builder {
	
	public static final int DEFAULT_FEEDBACK_DOCS = 20;
	public static final int DEFAULT_FEEDBACK_TERMS = 20;
	
	private int feedbackDocs;
	private int feedbackTerms;
	
	private GQuery query;
	private SearchHits initialHits;
	private DirichletDocScorerCreator docScorerCreator;
	private ExpansionDocsDocScorerCreator selfExpansionScorerCreator;
	private ExpansionDocsDocScorerCreator wikiExpansionScorerCreator;
	
	public MultiExpansionRM1Builder(GQuery query, SearchHits initialHits, 
			DirichletDocScorerCreator docScorerCreator, ExpansionDocsDocScorerCreator selfExpansionScorerCreator,
			ExpansionDocsDocScorerCreator wikiExpansionScorerCreator, int feedbackDocs, int feedbackTerms) {
		this.docScorerCreator = docScorerCreator;
		this.selfExpansionScorerCreator = selfExpansionScorerCreator;
		this.wikiExpansionScorerCreator = wikiExpansionScorerCreator;
		setFeedbackDocs(feedbackDocs);
		setFeedbackTerms(feedbackTerms);
		setQuery(query, initialHits);
	}
	
	public MultiExpansionRM1Builder(GQuery query, SearchHits initialHits,
			DirichletDocScorerCreator docScorerCreator, ExpansionDocsDocScorerCreator selfExpansionScorerCreator,
			ExpansionDocsDocScorerCreator wikiExpansionScorerCreator) {
		this(query, initialHits, docScorerCreator, selfExpansionScorerCreator,
				wikiExpansionScorerCreator, DEFAULT_FEEDBACK_DOCS, DEFAULT_FEEDBACK_TERMS);
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
			selfExpansionScorerCreator.setMu(-1);
			wikiExpansionScorerCreator.setMu(-1);
			DocScorer docScorer = docScorerCreator.getDocScorer(hit);
			DocScorer selfScorer = selfExpansionScorerCreator.getDocScorer(hit);
			DocScorer wikiScorer = wikiExpansionScorerCreator.getDocScorer(hit);
			
			// Combine into an interpolated scorer
			Map<DocScorer, Double> docScorers = new HashMap<DocScorer, Double>();
			docScorers.put(docScorer, params.get(DoubleEntityRunner.DOCUMENT_WEIGHT));
			docScorers.put(selfScorer, params.get(DoubleEntityRunner.SELF_WEIGHT));
			docScorers.put(wikiScorer, params.get(DoubleEntityRunner.WIKI_WEIGHT));
			DocScorer interpolatedScorer = new InterpolatedDocScorer(docScorers);
			
			// Score the query
			QueryScorer queryScorer = new QueryLikelihoodQueryScorer(interpolatedScorer);
			double queryScore = Math.exp(queryScorer.scoreQuery(query));
			
			// Prep the 0-mu scorers
			// This is important because RM1 performs much better with mu == 0 for non-query terms
			docScorerCreator.setMu(0);
			selfExpansionScorerCreator.setMu(0);
			wikiExpansionScorerCreator.setMu(0);
			docScorer = docScorerCreator.getDocScorer(hit);
			selfScorer = selfExpansionScorerCreator.getDocScorer(hit);
			wikiScorer = wikiExpansionScorerCreator.getDocScorer(hit);
			
			// Combine into an interpolated scorer
			docScorers = new HashMap<DocScorer, Double>();
			docScorers.put(docScorer, params.get(DoubleEntityRunner.DOCUMENT_WEIGHT));
			docScorers.put(selfScorer, params.get(DoubleEntityRunner.SELF_WEIGHT));
			docScorers.put(wikiScorer, params.get(DoubleEntityRunner.WIKI_WEIGHT));
			interpolatedScorer = new InterpolatedDocScorer(docScorers);	
			
			DocScorer rmScorer = new RelevanceModelScorer(interpolatedScorer, queryScore);
				
			FeatureVector terms = new FeatureVector(stopper);
			for (String term : hit.getFeatureVector().getFeatures()) {
				if (stopper != null && stopper.isStopWord(term)) {
					continue;
				}
				terms.addTerm(term, hit.getFeatureVector().getFeatureWeight(term));
			}
			if (wikiExpansionScorerCreator.getClusters().getDocsRelatedTo(hit) != null) {
				for (String docno : wikiExpansionScorerCreator.getClusters().getDocsRelatedTo(hit).keySet()) {
					SearchHit expansionHit = new IndexBackedSearchHit(wikiExpansionScorerCreator.getIndex());
					expansionHit.setDocno(docno);
					for (String term : expansionHit.getFeatureVector().getFeatures()) {
						if (stopper != null && stopper.isStopWord(term)) {
							continue;
						}
						terms.addTerm(term, expansionHit.getFeatureVector().getFeatureWeight(term));
					}
				}
			}
			if (selfExpansionScorerCreator.getClusters().getDocsRelatedTo(hit) != null) {
				for (String docno : selfExpansionScorerCreator.getClusters().getDocsRelatedTo(hit).keySet()) {
					SearchHit expansionHit = new IndexBackedSearchHit(selfExpansionScorerCreator.getIndex());
					expansionHit.setDocno(docno);
					for (String term : expansionHit.getFeatureVector().getFeatures()) {
						if (stopper != null && stopper.isStopWord(term)) {
							continue;
						}
						terms.addTerm(term, expansionHit.getFeatureVector().getFeatureWeight(term));
					}
				}
			}
			
			Iterator<String> termit = terms.iterator();
			while (termit.hasNext()) {
				String term = termit.next();
				termScores.addTerm(term, rmScorer.scoreTerm(term));
			}
		}

		termScores.clip(feedbackTerms);
		return termScores;
	}

}
