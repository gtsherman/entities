package edu.gslis.readers;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.gslis.utils.readers.Reader;
import edu.gslis.utils.readers.RelevanceModelReader;

public class QueryProbabilityReader extends Reader {
	
	private Map<String, Double> termProbs;
	
	public QueryProbabilityReader(File file) {
		super(Arrays.asList(RelevanceModelReader.TERM_FIELD, RelevanceModelReader.SCORE_FIELD));
		read(file);
		createTermProbs();
	}
	
	public Map<String, Double> getTermProbs() {
		return termProbs;
	}
	
	public void createTermProbs() {
		termProbs = new HashMap<String, Double>();
		Iterator<Map<String, String>> tupleIt = results.iterator();
		while (tupleIt.hasNext()) {
			Map<String, String> termTuple = tupleIt.next();
			termProbs.put(termTuple.get(RelevanceModelReader.TERM_FIELD),
					Double.parseDouble(termTuple.get(RelevanceModelReader.SCORE_FIELD)));
		}
	}

}
