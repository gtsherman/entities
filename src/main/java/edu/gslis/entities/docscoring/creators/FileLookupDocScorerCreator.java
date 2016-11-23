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

		if (!storedScorers.containsKey(doc.getQueryName() + File.separator + doc.getDocno())) {
			DocScorer docScorer = new FileLookupDocScorer(basePath + File.separator +
					doc.getQueryName() + File.separator + doc.getDocno());
			storedScorers.put(doc.getDocno(), new StoredDocScorer(docScorer));
		}
	}

}
