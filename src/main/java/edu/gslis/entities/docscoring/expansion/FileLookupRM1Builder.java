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
	private int fbTerms = 20;
	
	public FileLookupRM1Builder(GQuery query, String basePath) {
		setQuery(query);
		setBasePath(basePath);
	}

	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}
	
	public void setFeedbackTerms(int fbTerms) {
		this.fbTerms = fbTerms;
	}
	
	public FeatureVector buildRelevanceModel(QueryResults queryResults) {
		return buildRelevanceModel(queryResults, null);
	}
	
	public FeatureVector buildRelevanceModel(QueryResults queryResults, Stopper stopper) {
		RelevanceModelReader rmReader = new RelevanceModelReader(new File(basePath +
				File.separator + queryResults.getQuery().getTitle()));
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
		rmVec.clip(fbTerms);
		return rmVec;
	}

}
