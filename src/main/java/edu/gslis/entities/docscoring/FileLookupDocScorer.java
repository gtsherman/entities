package edu.gslis.entities.docscoring;

import java.io.File;
import java.util.Map;

import edu.gslis.readers.QueryProbabilityReader;
import edu.gslis.scoring.DocScorer;

public class FileLookupDocScorer implements DocScorer {
	
	private Map<String, Double> termProbs;
	
	public FileLookupDocScorer(String filePath) {
		this(new File(filePath));
	}
	
	public FileLookupDocScorer(File file) {
		QueryProbabilityReader qpreader = new QueryProbabilityReader(file);
		termProbs = qpreader.getTermProbs();
	}

	@Override
	public double scoreTerm(String term) {
		if (!termProbs.containsKey(term)) {
			termProbs.put(term, 0.0);
		}
		return termProbs.get(term);
	}

}
