package edu.gslis.entities.docscoring;

import edu.gslis.scoring.DocScorer;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.data.interpreters.RelevanceModelDataInterpreter;
import edu.gslis.utils.data.sources.DatabaseDataSource;

public class RMDatabaseLookupDocScorer implements DocScorer {
	
	private DatabaseDataSource data;
	private RelevanceModelDataInterpreter interpreter;

	public RMDatabaseLookupDocScorer(DatabaseDataSource data,
			RelevanceModelDataInterpreter interpreter) {
		this.data = data;
		this.interpreter = interpreter;
	}

	@Override
	public double scoreTerm(String term, SearchHit doc) {
		data.read(1, RelevanceModelDataInterpreter.TERM_FIELD + "=\"" + term + "\"",
				"ORIGINAL_DOCUMENT=\"" + doc.getDocno() + "\"");
		FeatureVector termScores = interpreter.build(data);
		if (!termScores.contains(term)) {
			//System.err.println("No score for " + term + " in " + doc.getDocno());
			return 0.0;
		}
		return termScores.getFeatureWeight(term);
	}

}
