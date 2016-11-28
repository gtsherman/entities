package edu.gslis.entities.docscoring.creators;

import java.io.File;

import edu.gslis.entities.docscoring.DocScorer;
import edu.gslis.entities.docscoring.FileLookupDocScorer;
import edu.gslis.entities.docscoring.StoredDocScorer;
import edu.gslis.searchhits.SearchHit;

/**
 * DocScorerCreator for FileLookupDocScorer.
 * Note: It is very important to set the SearchHit's query name property if you
 * need that to be included in the file lookup path.
 * 
 * @author Garrick
 *
 */
public class FileLookupDocScorerCreator extends DocScorerCreator {
	
	private String basePath;
	
	public FileLookupDocScorerCreator(String basePath) {
		this.basePath = basePath;
	}
	
	public String getBasePath() {
		return basePath;
	}

	@Override
	protected void createIfNecessary(SearchHit doc) {
		if (doc.getQueryName() == null) {
			doc.setQueryName("");
		}

		String docKey = docKey(doc);
		if (!storedScorers.containsKey(docKey)) {
			DocScorer docScorer = new FileLookupDocScorer(basePath + File.separator +
					doc.getQueryName() + File.separator + doc.getDocno());
			storedScorers.put(docKey, new StoredDocScorer(docScorer));
		}
	}
	
	@Override
	protected String docKey(SearchHit doc) {
		return doc.getQueryName() + File.separator + doc.getDocno();
	}

}
