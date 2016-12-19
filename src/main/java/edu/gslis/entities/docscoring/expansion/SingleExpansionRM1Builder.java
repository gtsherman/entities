package edu.gslis.entities.docscoring.expansion;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import edu.gslis.entities.docscoring.ExpansionDocsDocScorer;
import edu.gslis.evaluation.running.runners.EntityRunner;
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

public class SingleExpansionRM1Builder implements ExpansionRM1Builder {
	
	public static final int DEFAULT_FEEDBACK_DOCS = 20;
	public static final int DEFAULT_FEEDBACK_TERMS = 20;
	
	private int feedbackDocs;
	private int feedbackTerms;
	
	private GQuery query;
	private SearchHits initialHits;
	private DirichletDocScorer docScorer;
	private ExpansionDocsDocScorer expansionScorer;
	private DirichletDocScorer docScorerZeroMu;
	private ExpansionDocsDocScorer expansionScorerZeroMu;
	
	public SingleExpansionRM1Builder(GQuery query,
			SearchHits initialHits, 
			DirichletDocScorer docScorer, 
			ExpansionDocsDocScorer expansionScorer,
			int feedbackDocs,
			int feedbackTerms) {
		this.docScorer = docScorer;
		this.expansionScorer = expansionScorer;
		this.docScorerZeroMu = new DirichletDocScorer(0, docScorer.getCollectionStats());
		this.expansionScorerZeroMu = new ExpansionDocsDocScorer(0, expansionScorer.getExpansionIndex(), expansionScorer.getClusters());
		setFeedbackDocs(feedbackDocs);
		setFeedbackTerms(feedbackTerms);
		setQuery(query, initialHits);
	}
	
	public SingleExpansionRM1Builder(GQuery query,
			SearchHits initialHits,
			DirichletDocScorer docScorer,
			ExpansionDocsDocScorer expansionScorer) {
		this(query, initialHits, docScorer, expansionScorer,
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
			
			// Combine into an interpolated scorer
			Map<DocScorer, Double> docScorers = new HashMap<DocScorer, Double>();
			docScorers.put(docScorer, params.get(EntityRunner.DOCUMENT_WEIGHT)); // P(q|D)
			docScorers.put(expansionScorer, params.get(EntityRunner.EXPANSION_WEIGHT)); // P(q|E)
			DocScorer interpolatedScorer = new InterpolatedDocScorer(docScorers); // lambda
			
			// Score the query
			QueryScorer queryScorer = new QueryLikelihoodQueryScorer(interpolatedScorer);
			double queryScore = Math.exp(queryScorer.scoreQuery(query, hit)); // P(Q|D)
			
			// Combine into an interpolated scorer
			docScorers = new HashMap<DocScorer, Double>();
			docScorers.put(docScorerZeroMu, params.get(EntityRunner.DOCUMENT_WEIGHT)); // P(w|D)
			docScorers.put(expansionScorerZeroMu, params.get(EntityRunner.EXPANSION_WEIGHT)); // P(w|E)
			interpolatedScorer = new InterpolatedDocScorer(docScorers);	// lambda
			
			DocScorer rmScorer = new RelevanceModelScorer(interpolatedScorer, queryScore);
				
			Set<String> terms = new HashSet<String>();
			for (String term : hit.getFeatureVector().getFeatures()) {
				if (stopper != null && stopper.isStopWord(term)) {
					continue;
				}
				terms.add(term);
			}
			if (expansionScorer.getClusters().getDocsRelatedTo(hit) != null) {
				for (String docno : expansionScorer.getClusters().getDocsRelatedTo(hit).keySet()) {
					SearchHit expansionHit = new IndexBackedSearchHit(expansionScorer.getExpansionIndex());
					expansionHit.setDocno(docno);
					for (String term : expansionHit.getFeatureVector().getFeatures()) {
						if (stopper != null && stopper.isStopWord(term)) {
							continue;
						}
						terms.add(term);
					}
				}
			}
			
			for (String term : terms) {
				termScores.addTerm(term, rmScorer.scoreTerm(term, hit));
			}
		}

		termScores.clip(feedbackTerms);
		return termScores;
	}

}
