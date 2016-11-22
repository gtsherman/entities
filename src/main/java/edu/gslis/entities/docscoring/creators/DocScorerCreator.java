package edu.gslis.entities.docscoring.creators;

import java.util.HashMap;
import java.util.Map;

import edu.gslis.entities.docscoring.DocScorer;
import edu.gslis.entities.docscoring.StoredDocScorer;
import edu.gslis.searchhits.SearchHit;

/**
 * Provides a DocScorer using previously specified parameters.
 * Classes extending this one should require reusable parameters in their 
 * creation. These parameters are then used to build new DocScorers as needed. 
 * If a DocScorer has been created already, return the prior instance.
 * 
 * @author Garrick
 *
 */
public abstract class DocScorerCreator {
	
	protected Map<String, StoredDocScorer> storedScorers = new HashMap<String, StoredDocScorer>();
	
	public DocScorer getDocScorer(SearchHit doc) {
		createIfNecessary(doc);
		return storedScorers.get(doc.getDocno());
	}
	
	protected abstract void createIfNecessary(SearchHit doc);

}
