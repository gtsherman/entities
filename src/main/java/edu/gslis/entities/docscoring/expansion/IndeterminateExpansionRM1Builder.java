package edu.gslis.entities.docscoring.expansion;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import edu.gslis.queries.GQuery;
import edu.gslis.related_docs.term_collectors.TermCollector;
import edu.gslis.scoring.DocScorer;
import edu.gslis.scoring.InterpolatedDocScorer;
import edu.gslis.scoring.expansion.RM1Builder;
import edu.gslis.scoring.expansion.RelevanceModelScorer;
import edu.gslis.scoring.queryscoring.QueryLikelihoodQueryScorer;
import edu.gslis.scoring.queryscoring.QueryScorer;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;

public class IndeterminateExpansionRM1Builder implements RM1Builder {
	
	public static final int DEFAULT_FEEDBACK_DOCS = 20;
	public static final int DEFAULT_FEEDBACK_TERMS = 20;
	
	private int feedbackDocs;
	private int feedbackTerms;
	
	private TermCollector termCollector;
	private Map<DocScorer, Double> docScorers;
	private Map<DocScorer, Double> queryScorers;
	
	private LoadingCache<ExpansionKey, Set<String>> terms = CacheBuilder.newBuilder()
			.softValues()
			.build(
					new CacheLoader<ExpansionKey, Set<String>>() {
						public Set<String> load(ExpansionKey key) throws Exception {
							return collectTerms(key.getDoc(),
									key.getStopper(), key.getLimit());
						}
					});
	
	public IndeterminateExpansionRM1Builder(GQuery query,
			SearchHits initialHits, 
			Map<DocScorer, Double> docScorers, 
			Map<DocScorer, Double> queryScorers,
			TermCollector termCollector,
			int feedbackDocs,
			int feedbackTerms) {

		this.docScorers = docScorers;
		this.queryScorers = queryScorers;
		
		this.termCollector = termCollector;

		setFeedbackDocs(feedbackDocs);
		setFeedbackTerms(feedbackTerms);
	}
	
	public IndeterminateExpansionRM1Builder(GQuery query,
			SearchHits initialHits,
			Map<DocScorer, Double> docScorers,
			Map<DocScorer, Double> queryScorers,
			TermCollector termCollector) {
		this(query, initialHits, docScorers, queryScorers, termCollector,
				DEFAULT_FEEDBACK_DOCS, DEFAULT_FEEDBACK_TERMS);
	}
	
	public void setFeedbackDocs(int feedbackDocs) {
		this.feedbackDocs = feedbackDocs;
	}
	
	public void setFeedbackTerms(int feedbackTerms) {
		this.feedbackTerms = feedbackTerms;
	}

	@Override
	public FeatureVector buildRelevanceModel(GQuery query, SearchHits initialHits) {
		return buildRelevanceModel(query, initialHits, null);		
	}

	@Override
	public FeatureVector buildRelevanceModel(GQuery query, SearchHits initialHits, Stopper stopper) {
		FeatureVector termScores = new FeatureVector(stopper);
		
		int i = 0;
		Iterator<SearchHit> hitIt = initialHits.iterator();
		while (hitIt.hasNext() && i < feedbackDocs) {
			SearchHit d = hitIt.next();
			i++;
			
			// Set up interpolated query scorer
			DocScorer queryScorersDocs = new InterpolatedDocScorer(queryScorers);
			QueryScorer queryScorer = new QueryLikelihoodQueryScorer(queryScorersDocs);
			
			// Get P(Q|D)
			double queryScore = Math.exp(queryScorer.scoreQuery(query, d));
			
			// Set up interpolated doc scorer
			DocScorer docScorersInterp = new InterpolatedDocScorer(docScorers);

			// Set up RM scorer
			DocScorer rmScorer = new RelevanceModelScorer(docScorersInterp, queryScore);
			
			// Collect terms for this document and its expansion documents
			Set<String> terms;
			try {
				terms = this.terms.get(new ExpansionKey(d, stopper, 200));
			} catch (ExecutionException e) {
				System.err.println("Error getting terms");
				terms = new HashSet<String>();
			}
			
			// Add this document's contribution to the overall RM
			for (String term : terms) {
				termScores.addTerm(term, rmScorer.scoreTerm(term, d));
			}
		}

		termScores.clip(feedbackTerms);
		return termScores;
	}
	
	private Set<String> collectTerms(SearchHit hit, Stopper stopper, int limit) {
		// Collect non-stop words from this document's expansion documents
		FeatureVector terms = termCollector.getTerms(hit, stopper);
		
		// Collect non-stop words from this document
		for (String term : hit.getFeatureVector().getFeatures()) {
			if (stopper != null && stopper.isStopWord(term)) {
				continue;
			}
			terms.addTerm(term, hit.getFeatureVector().getFeatureWeight(term));
		}
		
		if (limit > 0) {
			terms.clip(limit);
		}

		return terms.getFeatures();
	}

}
