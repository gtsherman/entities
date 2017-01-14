package edu.gslis.entities.docscoring.expansion;

import java.io.File;
import java.util.Iterator;

import edu.gslis.scoring.expansion.RM1Builder;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;
import edu.gslis.utils.data.interpreters.RelevanceModelDataInterpreter;
import edu.gslis.utils.data.sources.DataSource;
import edu.gslis.utils.data.sources.FileDataSource;
import edu.gslis.utils.retrieval.QueryResults;

public class FileLookupRM1Builder implements RM1Builder {
	
	private String basePath;
	private int fbTerms = 20;
	
	public FileLookupRM1Builder(String basePath) {
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
		DataSource data = new FileDataSource(new File(basePath + File.separator + queryResults.getQuery().getTitle()));
		RelevanceModelDataInterpreter rmReader = new RelevanceModelDataInterpreter();
		FeatureVector rmVec = rmReader.build(data);
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
