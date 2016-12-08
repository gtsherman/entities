package edu.gslis.entities.docscoring.expansion;

import java.io.File;
import java.util.Iterator;

import edu.gslis.queries.GQuery;
import edu.gslis.scoring.expansion.RM1Builder;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;
import edu.gslis.utils.readers.RelevanceModelReader;

public class FileLookupRM1Builder implements RM1Builder {
	
	private String basePath;
	private GQuery query;
	
	public FileLookupRM1Builder(GQuery query, String basePath) {
		setQuery(query);
		setBasePath(basePath);
	}

	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}
	
	public void setQuery(GQuery query) {
		this.query = query;
	}
	
	public FeatureVector buildRelevanceModel() {
		return buildRelevanceModel(null);
	}
	
	public FeatureVector buildRelevanceModel(Stopper stopper) {
		RelevanceModelReader rmReader = new RelevanceModelReader(new File(basePath + File.separator + query.getTitle()));
		FeatureVector rmVec = rmReader.getFeatureVector();
		if (stopper != null) {
			Iterator<String> termit = rmVec.iterator();
			while (termit.hasNext()) {
				String term = termit.next();
				if (stopper.isStopWord(term)) {
					rmVec.removeTerm(term);
				}
			}
			rmVec.normalize();
		}
		return rmVec;
	}

}
