package edu.gslis.entities.docscoring.expansion;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.gslis.entities.docscoring.ExpansionDocsDocScorer;
import edu.gslis.evaluation.running.runners.DoubleEntityRunner;
import edu.gslis.queries.GQuery;
import edu.gslis.scoring.DirichletDocScorer;
import edu.gslis.scoring.DocScorer;
import edu.gslis.scoring.InterpolatedDocScorer;
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
	private DirichletDocScorer docScorer;
	private ExpansionDocsDocScorer selfExpansionScorer;
	private ExpansionDocsDocScorer wikiExpansionScorer;
	private DirichletDocScorer docScorerZeroMu;
	private ExpansionDocsDocScorer selfExpansionScorerZeroMu;
	private ExpansionDocsDocScorer wikiExpansionScorerZeroMu;
	
	public MultiExpansionRM1Builder(GQuery query, SearchHits initialHits, 
			DirichletDocScorer docScorer, ExpansionDocsDocScorer selfExpansionScorer,
			ExpansionDocsDocScorer wikiExpansionScorer, int feedbackDocs, int feedbackTerms) {
		this.docScorer = docScorer;
		this.selfExpansionScorer = selfExpansionScorer;
		this.wikiExpansionScorer = wikiExpansionScorer;
		this.docScorerZeroMu = new DirichletDocScorer(0, docScorer.getCollectionStats());
		this.selfExpansionScorerZeroMu = new ExpansionDocsDocScorer(0, selfExpansionScorer.getExpansionIndex(), selfExpansionScorer.getClusters());
		this.wikiExpansionScorerZeroMu = new ExpansionDocsDocScorer(0, wikiExpansionScorer.getExpansionIndex(), wikiExpansionScorer.getClusters());
		setFeedbackDocs(feedbackDocs);
		setFeedbackTerms(feedbackTerms);
		setQuery(query, initialHits);
	}
	
	public MultiExpansionRM1Builder(GQuery query, SearchHits initialHits,
			DirichletDocScorer docScorer, ExpansionDocsDocScorer selfExpansionScorer,
			ExpansionDocsDocScorer wikiExpansionScorer) {
		this(query, initialHits, docScorer, selfExpansionScorer,
				wikiExpansionScorer, DEFAULT_FEEDBACK_DOCS, DEFAULT_FEEDBACK_TERMS);
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
			
			// Combine into an interpolated scorer
			Map<DocScorer, Double> docScorers = new HashMap<DocScorer, Double>();
			docScorers.put(docScorer, params.get(DoubleEntityRunner.DOCUMENT_WEIGHT));
			docScorers.put(selfExpansionScorer, params.get(DoubleEntityRunner.SELF_WEIGHT));
			docScorers.put(wikiExpansionScorer, params.get(DoubleEntityRunner.WIKI_WEIGHT));
			DocScorer interpolatedScorer = new InterpolatedDocScorer(docScorers);
			
			// Score the query
			QueryScorer queryScorer = new QueryLikelihoodQueryScorer(interpolatedScorer);
			double queryScore = Math.exp(queryScorer.scoreQuery(query, hit));
			
			// Combine into an interpolated scorer
			docScorers = new HashMap<DocScorer, Double>();
			docScorers.put(docScorerZeroMu, params.get(DoubleEntityRunner.DOCUMENT_WEIGHT));
			docScorers.put(selfExpansionScorerZeroMu, params.get(DoubleEntityRunner.SELF_WEIGHT));
			docScorers.put(wikiExpansionScorerZeroMu, params.get(DoubleEntityRunner.WIKI_WEIGHT));
			interpolatedScorer = new InterpolatedDocScorer(docScorers);	
			
			DocScorer rmScorer = new RelevanceModelScorer(interpolatedScorer, queryScore);
				
			FeatureVector terms = new FeatureVector(stopper);
			for (String term : hit.getFeatureVector().getFeatures()) {
				if (stopper != null && stopper.isStopWord(term)) {
					continue;
				}
				terms.addTerm(term, hit.getFeatureVector().getFeatureWeight(term));
			}
			if (wikiExpansionScorer.getClusters().getDocsRelatedTo(hit) != null) {
				for (String docno : wikiExpansionScorer.getClusters().getDocsRelatedTo(hit).keySet()) {
					SearchHit expansionHit = new IndexBackedSearchHit(wikiExpansionScorer.getExpansionIndex());
					expansionHit.setDocno(docno);
					for (String term : expansionHit.getFeatureVector().getFeatures()) {
						if (stopper != null && stopper.isStopWord(term)) {
							continue;
						}
						terms.addTerm(term, expansionHit.getFeatureVector().getFeatureWeight(term));
					}
				}
			}
			if (selfExpansionScorer.getClusters().getDocsRelatedTo(hit) != null) {
				for (String docno : selfExpansionScorer.getClusters().getDocsRelatedTo(hit).keySet()) {
					SearchHit expansionHit = new IndexBackedSearchHit(selfExpansionScorer.getExpansionIndex());
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
				termScores.addTerm(term, rmScorer.scoreTerm(term, hit));
			}
		}

		termScores.clip(feedbackTerms);
		return termScores;
	}

}
