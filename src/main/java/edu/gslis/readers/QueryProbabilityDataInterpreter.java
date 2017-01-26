package edu.gslis.readers;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.data.interpreters.DataInterpreter;
import edu.gslis.utils.data.interpreters.RelevanceModelDataInterpreter;
import edu.gslis.utils.data.sources.DataSource;

public class QueryProbabilityDataInterpreter extends DataInterpreter {
	
	public static final String DATA_NAME = "query_probability";
	
	public QueryProbabilityDataInterpreter(List<String> fields) {
		super(fields);
	}

	public QueryProbabilityDataInterpreter(String... fields) {
		this(Arrays.asList(fields));
	}
	
	public QueryProbabilityDataInterpreter() {
		this(Arrays.asList(RelevanceModelDataInterpreter.TERM_FIELD,
				RelevanceModelDataInterpreter.SCORE_FIELD));
	}
	
	@Override
	public FeatureVector build(DataSource dataSource) {
		FeatureVector termProbs = new FeatureVector(null);
		Iterator<String[]> tupleIt = dataSource.iterator();
		while (tupleIt.hasNext()) {
			String[] termTuple = tupleIt.next();
			termProbs.addTerm(valueOfField(RelevanceModelDataInterpreter.TERM_FIELD, termTuple),
					Double.parseDouble(valueOfField(RelevanceModelDataInterpreter.SCORE_FIELD, termTuple)));
		}
		return termProbs;
	}

}
