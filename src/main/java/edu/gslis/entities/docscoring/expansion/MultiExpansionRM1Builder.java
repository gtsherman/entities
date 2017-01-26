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

import edu.gslis.evaluation.running.runners.DoubleEntityRunner;
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

public class MultiExpansionRM1Builder implements ExpansionRM1Builder {
	
	public static final int DEFAULT_FEEDBACK_DOCS = 20;
	public static final int DEFAULT_FEEDBACK_TERMS = 20;
	
	private int feedbackDocs;
	private int feedbackTerms;
	
	private GQuery query;
	private SearchHits initialHits;
	private DocScorer docScorer;
	private DocScorer selfExpansionScorer;
	private DocScorer wikiExpansionScorer;
	private QueryScorer docScorerQuery;
	private QueryScorer selfExpansionScorerQuery;
	private QueryScorer wikiExpansionScorerQuery;
	private RelatedDocs selfClusters;
	private RelatedDocs wikiClusters;
	private IndexWrapper selfIndex;
	private IndexWrapper wikiIndex;
	
	private LoadingCache<ExpansionKey, Set<String>> terms = CacheBuilder.newBuilder()
			.softValues()
			.build(
					new CacheLoader<ExpansionKey, Set<String>>() {
						public Set<String> load(ExpansionKey key) throws Exception {
							return collectTerms(key.getDoc(),
									key.getStopper(), key.getLimit());
						}
					});
	
	public MultiExpansionRM1Builder(GQuery query, SearchHits initialHits, 
			DocScorer docScorer, DocScorer selfExpansionScorer,
			DocScorer wikiExpansionScorer, QueryScorer docScorerQuery,
			QueryScorer selfExpansionScorerQuery, QueryScorer wikiExpansionScorerQuery,
			RelatedDocs selfClusters, RelatedDocs wikiClusters,
			IndexWrapper selfIndex, IndexWrapper wikiIndex,
			int feedbackDocs, int feedbackTerms) {
		this.docScorer = docScorer;
		this.selfExpansionScorer = selfExpansionScorer;
		this.wikiExpansionScorer = wikiExpansionScorer;
		this.docScorerQuery = docScorerQuery;
		this.selfExpansionScorerQuery = selfExpansionScorerQuery;
		this.wikiExpansionScorerQuery = wikiExpansionScorerQuery;
		this.selfClusters = selfClusters;
		this.wikiClusters = wikiClusters;
		this.selfIndex = selfIndex;
		this.wikiIndex = wikiIndex;
		setFeedbackDocs(feedbackDocs);
		setFeedbackTerms(feedbackTerms);
		setQuery(query, initialHits);
	}
	
	public MultiExpansionRM1Builder(GQuery query, SearchHits initialHits,
			DocScorer docScorer, DocScorer selfExpansionScorer,
			DocScorer wikiExpansionScorer, QueryScorer docScorerQuery,
			QueryScorer selfExpansionScorerQuery, QueryScorer wikiExpansionScorerQuery,
			RelatedDocs selfClusters, RelatedDocs wikiClusters,
			IndexWrapper selfIndex, IndexWrapper wikiIndex) {
		this(query, initialHits, docScorer, selfExpansionScorer,
				wikiExpansionScorer, docScorerQuery, selfExpansionScorerQuery,
				wikiExpansionScorerQuery, selfClusters, wikiClusters,
				selfIndex, wikiIndex, DEFAULT_FEEDBACK_DOCS, DEFAULT_FEEDBACK_TERMS);
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
			hit.setQueryName(query.getTitle());
			i++;
			
			// Get P(Q|D)
			double docQueryScore = Math.exp(docScorerQuery.scoreQuery(query, hit));
			
			// Set up orig doc RM scorer
			DocScorer docRMScorer = new RelevanceModelScorer(docScorer, docQueryScore);
			
			// Get P(Q|E_D) (expansion documents query likelihood) for self
			double expDocQueryScoreSelf = Math.exp(selfExpansionScorerQuery.scoreQuery(query, hit));
			
			// Set up exp doc RM scorer for self
			DocScorer expDocRMScorerSelf = new RelevanceModelScorer(selfExpansionScorer, expDocQueryScoreSelf);

			// Get P(Q|E_D) (expansion documents query likelihood) for wiki
			double expDocQueryScoreWiki = Math.exp(wikiExpansionScorerQuery.scoreQuery(query, hit));
			
			// Set up exp doc RM scorer for wiki
			DocScorer expDocRMScorerWiki = new RelevanceModelScorer(wikiExpansionScorer, expDocQueryScoreWiki);

			// Combine RM scorers
			Map<DocScorer, Double> docScorers = new HashMap<DocScorer, Double>();
			docScorers.put(docRMScorer, params.get(DoubleEntityRunner.DOCUMENT_WEIGHT));
			docScorers.put(expDocRMScorerSelf, params.get(DoubleEntityRunner.SELF_WEIGHT));
			docScorers.put(expDocRMScorerWiki, params.get(DoubleEntityRunner.WIKI_WEIGHT));
			DocScorer rmScorer = new InterpolatedDocScorer(docScorers);
			
			Set<String> terms;
			try {
				terms = this.terms.get(new ExpansionKey(hit, stopper, 200));
			} catch (ExecutionException e) {
				System.err.println("Error getting terms");
				terms = new HashSet<String>();
			}
			
			for (String term : terms) {
				termScores.addTerm(term, rmScorer.scoreTerm(term, hit));
			}
		}

		termScores.clip(feedbackTerms);
		return termScores;
	}
	
	private Set<String> collectTerms(SearchHit hit, Stopper stopper, int limit) {
		FeatureVector terms = new FeatureVector(stopper);

		for (String term : hit.getFeatureVector().getFeatures()) {
			if (stopper != null && stopper.isStopWord(term)) {
				continue;
			}
			terms.addTerm(term, hit.getFeatureVector().getFeatureWeight(term));
		}

		if (wikiClusters.getDocsRelatedTo(hit) != null) {
			for (String docno : wikiClusters.getDocsRelatedTo(hit).keySet()) {
				SearchHit expansionHit = new IndexBackedSearchHit(wikiIndex);
				expansionHit.setDocno(docno);
				for (String term : expansionHit.getFeatureVector().getFeatures()) {
					if (stopper != null && stopper.isStopWord(term)) {
						continue;
					}
					terms.addTerm(term, expansionHit.getFeatureVector().getFeatureWeight(term));
				}
			}
		}

		if (selfClusters.getDocsRelatedTo(hit) != null) {
			for (String docno : selfClusters.getDocsRelatedTo(hit).keySet()) {
				SearchHit expansionHit = new IndexBackedSearchHit(selfIndex);
				expansionHit.setDocno(docno);
				for (String term : expansionHit.getFeatureVector().getFeatures()) {
					if (stopper != null && stopper.isStopWord(term)) {
						continue;
					}
					terms.addTerm(term, expansionHit.getFeatureVector().getFeatureWeight(term));
				}
			}
		}
		
		if (limit > 0) {
			terms.clip(limit);
		}

		return terms.getFeatures();
	}

}
