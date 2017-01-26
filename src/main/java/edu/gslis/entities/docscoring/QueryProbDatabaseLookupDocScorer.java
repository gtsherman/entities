package edu.gslis.entities.docscoring;

import edu.gslis.readers.QueryProbabilityDataInterpreter;
import edu.gslis.scoring.DocScorer;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.data.interpreters.RelevanceModelDataInterpreter;
import edu.gslis.utils.data.sources.DatabaseDataSource;

public class QueryProbDatabaseLookupDocScorer implements DocScorer {
	
	private DatabaseDataSource data;
	private QueryProbabilityDataInterpreter interpreter;

	public QueryProbDatabaseLookupDocScorer(DatabaseDataSource data,
			QueryProbabilityDataInterpreter interpreter) {
		this.data = data;
		this.interpreter = interpreter;
	}

	@Override
	public double scoreTerm(String term, SearchHit doc) {
		data.read(1, RelevanceModelDataInterpreter.TERM_FIELD + "=\"" + term + "\"",
				"DOCUMENT=\"" + doc.getDocno() + "\"",
				"QUERY=\"" + doc.getQueryName() + "\"");
		FeatureVector termScores = interpreter.build(data);
		if (!termScores.contains(term)) {
			//System.err.println("No score for " + term + " in " + doc.getDocno());
			return 0.0;
		}
		return termScores.getFeatureWeight(term);
	}

}
