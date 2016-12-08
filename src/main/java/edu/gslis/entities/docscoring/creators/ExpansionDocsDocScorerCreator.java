package edu.gslis.entities.docscoring.creators;

import edu.gslis.entities.docscoring.ExpansionDocsDocScorer;
import edu.gslis.indexes.IndexWrapper;
import edu.gslis.related_docs.RelatedDocs;
import edu.gslis.scoring.StoredDocScorer;
import edu.gslis.scoring.creators.DocScorerCreator;
import edu.gslis.searchhits.SearchHit;

public class ExpansionDocsDocScorerCreator extends DocScorerCreator {
	
	public static final double DEFAULT_MU = -1;
	
	private IndexWrapper expansionIndex;
	private RelatedDocs clusters;
	private double mu = DEFAULT_MU;
	private boolean useStoredScorers = true;
	
	public ExpansionDocsDocScorerCreator(IndexWrapper expansionIndex, RelatedDocs clusters) {
		this.expansionIndex = expansionIndex;
		this.clusters = clusters;
	}
	
	public ExpansionDocsDocScorerCreator(IndexWrapper expansionIndex, RelatedDocs clusters, boolean useStoredScorers) {
		this(expansionIndex, clusters);
		useStoredScorers(useStoredScorers);
	}
	
	public ExpansionDocsDocScorerCreator(double mu, IndexWrapper expansionIndex, RelatedDocs clusters) {
		this(expansionIndex, clusters);
		setMu(mu);
	}
	
	public ExpansionDocsDocScorerCreator(double mu, IndexWrapper expansionIndex, RelatedDocs clusters, boolean useStoredScorers) {
		this(mu, expansionIndex, clusters);
		useStoredScorers(useStoredScorers);
	}

	public IndexWrapper getIndex() {
		return expansionIndex;
	}
	
	public RelatedDocs getClusters() {
		return clusters;
	}
	
	public double getMu() {
		return mu;
	}
	
	public void setMu(double mu) {
		this.mu = mu;
	}
	
	public void useStoredScorers(boolean useStoredScorers) {
		this.useStoredScorers = useStoredScorers;
	}

	@Override
	protected void createIfNecessary(SearchHit doc) {
		String docKey = docKey(doc);
		if (!storedScorers.containsKey(docKey)) {
			ExpansionDocsDocScorer docScorer;
			if (mu != DEFAULT_MU) {
				docScorer = new ExpansionDocsDocScorer(mu, doc, expansionIndex, clusters);
			} else {
				docScorer = new ExpansionDocsDocScorer(doc, expansionIndex, clusters);
			}
			if (useStoredScorers) {
				storedScorers.put(docKey, new StoredDocScorer(docScorer));
			} else {
				storedScorers.put(docKey, docScorer);
			}
		}
	}
	
	@Override
	protected String docKey(SearchHit doc) {
		return doc.getDocno() + mu;
	}

}
