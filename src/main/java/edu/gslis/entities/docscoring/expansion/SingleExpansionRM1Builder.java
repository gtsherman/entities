package edu.gslis.entities.docscoring.expansion;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.gslis.entities.docscoring.creators.ExpansionDocsDocScorerCreator;
import edu.gslis.evaluation.running.runners.DoubleEntityRunner;
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
		
		// Store the current parameters of each DocScorerCreator so we can reuse them later
		double docScorerMu = docScorerCreator.getMu();
		double expansionScorerMu = expansionScorerCreator.getMu();
		
		int i = 0;
		Iterator<SearchHit> hitIt = initialHits.iterator();
		while (hitIt.hasNext() && i < feedbackDocs) {
			SearchHit hit = hitIt.next();
			i++;
			System.err.println("Scoring doc "+i+" of "+feedbackDocs);
			
			// Prep the scorers
			// Set mu to whatever the client provided, since it will be set to 0 after each hit is scored
			System.err.println("\tPrepping scorers");
			docScorerCreator.setMu(docScorerMu);
			expansionScorerCreator.setMu(expansionScorerMu);
			DocScorer docScorer = docScorerCreator.getDocScorer(hit);
			DocScorer expansionScorer = expansionScorerCreator.getDocScorer(hit);
			
			// Combine into an interpolated scorer
			Map<DocScorer, Double> docScorers = new HashMap<DocScorer, Double>();
			docScorers.put(docScorer, params.get(DoubleEntityRunner.DOCUMENT_WEIGHT));
			docScorers.put(expansionScorer, params.get(DoubleEntityRunner.WIKI_WEIGHT));
			DocScorer interpolatedScorer = new InterpolatedDocScorer(docScorers);
			
			// Score the query
			System.err.println("\tScoring query");
			QueryScorer queryScorer = new QueryLikelihoodQueryScorer(interpolatedScorer);
			double queryScore = Math.exp(queryScorer.scoreQuery(query));
			
			// Prep the 0-mu scorers
			// This is important because RM1 performs much better with mu == 0 for non-query terms
			docScorerCreator.setMu(0);
			expansionScorerCreator.setMu(0);
			docScorer = docScorerCreator.getDocScorer(hit);
			expansionScorer = expansionScorerCreator.getDocScorer(hit);
			
			// Combine into an interpolated scorer
			docScorers = new HashMap<DocScorer, Double>();
			docScorers.put(docScorer, params.get(EntityRunner.DOCUMENT_WEIGHT));
			docScorers.put(expansionScorer, params.get(EntityRunner.EXPANSION_WEIGHT));
			interpolatedScorer = new InterpolatedDocScorer(docScorers);	
			
			DocScorer rmScorer = new RelevanceModelScorer(interpolatedScorer, queryScore);
				
			System.err.println("\tCollecting terms");
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
			terms.clip(100);
			
			System.err.println("\tScoring terms ("+terms.getFeatureCount()+")");
			Iterator<String> termit = terms.iterator();
			while (termit.hasNext()) {
				String term = termit.next();
				termScores.addTerm(term, rmScorer.scoreTerm(term));
			}
		}

		termScores.clip(feedbackTerms);
		return termScores;
	}
	
	private void scoreAllTerms(SearchHit hit,
			FeatureVector accumulatedTermScores,
			Stopper stopper,
			DocScorer rmScorer) {
		for (String term : hit.getFeatureVector().getFeatures()) {
			if (stopper != null && stopper.isStopWord(term)) {
				continue;
			}
			accumulatedTermScores.addTerm(term, rmScorer.scoreTerm(term));
		}
	}
	
	private void scoreTermsInExpansionDocuments(SearchHit originalHit,
			FeatureVector termScores,
			Stopper stopper,
			DocScorer rmScorer,
			ExpansionDocsDocScorerCreator expansionScorerCreator) {
		for (String docno : expansionScorerCreator.getClusters().getDocsRelatedTo(originalHit).keySet()) {
			SearchHit hit = new IndexBackedSearchHit(expansionScorerCreator.getIndex());
			hit.setDocno(docno);
			
			scoreAllTerms(hit, termScores, stopper, rmScorer);
		}
	}

}
