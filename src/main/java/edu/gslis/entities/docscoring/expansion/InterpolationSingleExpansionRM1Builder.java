package edu.gslis.entities.docscoring.expansion;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import edu.gslis.evaluation.running.runners.EntityRunner;
import edu.gslis.indexes.IndexWrapper;
import edu.gslis.queries.GQuery;
import edu.gslis.related_docs.RelatedDocs;
import edu.gslis.scoring.DocScorer;
import edu.gslis.scoring.InterpolatedDocScorer;
import edu.gslis.scoring.expansion.RelevanceModelScorer;
import edu.gslis.scoring.queryscoring.QueryScorer;
import edu.gslis.searchhits.IndexBackedSearchHit;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;

public class InterpolationSingleExpansionRM1Builder implements ExpansionRM1Builder {
	
	public static final int DEFAULT_FEEDBACK_DOCS = 20;
	public static final int DEFAULT_FEEDBACK_TERMS = 20;
	
	private int feedbackDocs;
	private int feedbackTerms;
	
	private GQuery query;
	private SearchHits initialHits;
	private RelatedDocs clusters;
	private IndexWrapper expansionIndex;
	private DocScorer docScorer;
	private QueryScorer docScorerQueryProb;
	private DocScorer expansionScorer;
	private QueryScorer expansionScorerQueryProb;
	
	private LoadingCache<ExpansionKey, Set<String>> terms = CacheBuilder.newBuilder()
			.softValues()
			.build(
					new CacheLoader<ExpansionKey, Set<String>>() {
						public Set<String> load(ExpansionKey key) throws Exception {
							return collectTerms(key.getDoc(),
									key.getStopper(), key.getLimit());
						}
					});
	
	public InterpolationSingleExpansionRM1Builder(GQuery query,
			SearchHits initialHits, 
			DocScorer docScorer, 
			QueryScorer docScorerQueryProb,
			DocScorer expansionScorer,
			QueryScorer expansionScorerQueryProb,
			RelatedDocs clusters,
			IndexWrapper expansionIndex,
			int feedbackDocs,
			int feedbackTerms) {

		this.docScorer = docScorer;
		this.docScorerQueryProb = docScorerQueryProb;
		this.expansionScorer = expansionScorer;
		this.expansionScorerQueryProb = expansionScorerQueryProb;

		this.clusters = clusters;
		this.expansionIndex = expansionIndex;
		
		setFeedbackDocs(feedbackDocs);
		setFeedbackTerms(feedbackTerms);
		setQuery(query, initialHits);
	}
	
	public InterpolationSingleExpansionRM1Builder(GQuery query,
			SearchHits initialHits,
			DocScorer docScorer,
			QueryScorer docScorerQueryProb,
			DocScorer expansionScorer,
			QueryScorer expansionScorerQueryProb,
			RelatedDocs clusters,
			IndexWrapper expansionIndex) {
		this(query, initialHits, docScorer, docScorerQueryProb,
				expansionScorer, expansionScorerQueryProb, clusters,
				expansionIndex, DEFAULT_FEEDBACK_DOCS, DEFAULT_FEEDBACK_TERMS);
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
			SearchHit d = hitIt.next();
			i++;
			
			// Get P(Q|D)
			double docQueryScore = Math.exp(docScorerQueryProb.scoreQuery(query, d));
			
			// Set up orig doc RM scorer
			DocScorer docRMScorer = new RelevanceModelScorer(docScorer, docQueryScore);
			
			// Get P(Q|E_D) (expansion documents query likelihood)
			double expDocQueryScore = Math.exp(expansionScorerQueryProb.scoreQuery(query, d));
			
			// Set up exp doc RM scorer
			DocScorer expDocRMScorer = new RelevanceModelScorer(expansionScorer, expDocQueryScore);
			
			// Combine RM scorers
			Map<DocScorer, Double> docScorers = new HashMap<DocScorer, Double>();
			docScorers.put(docRMScorer, params.get(EntityRunner.DOCUMENT_WEIGHT));
			docScorers.put(expDocRMScorer, params.get(EntityRunner.EXPANSION_WEIGHT));
			DocScorer rmScorer = new InterpolatedDocScorer(docScorers);
				
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
		FeatureVector terms = new FeatureVector(null);
		
		// Collect non-stop words from this document
		for (String term : hit.getFeatureVector().getFeatures()) {
			if (stopper != null && stopper.isStopWord(term)) {
				continue;
			}
			terms.addTerm(term, hit.getFeatureVector().getFeatureWeight(term));
		}
		
		// Collect non-stop words from this document's expansion documents
		if (clusters.getDocsRelatedTo(hit) != null) {
			for (String docno : clusters.getDocsRelatedTo(hit).keySet()) {
				SearchHit expansionHit = new IndexBackedSearchHit(expansionIndex);
				expansionHit.setDocno(docno);
				for (String term : expansionHit.getFeatureVector().getFeatures()) {
					if (stopper != null && stopper.isStopWord(term)) {
						continue;
					}
					terms.addTerm(term, hit.getFeatureVector().getFeatureWeight(term));
				}
			}
		}
		
		if (limit > 0) {
			terms.clip(limit);
		}

		return terms.getFeatures();
	}

}
