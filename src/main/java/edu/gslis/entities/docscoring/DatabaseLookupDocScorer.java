package edu.gslis.entities.docscoring;

import edu.gslis.scoring.DocScorer;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.data.interpreters.FeatureVectorBuilder;
import edu.gslis.utils.data.interpreters.RelevanceModelDataInterpreter;
import edu.gslis.utils.data.sources.DatabaseDataSource;

public class DatabaseLookupDocScorer implements DocScorer {
	
	private DatabaseDataSource data;
	private FeatureVectorBuilder interpreter;

	public DatabaseLookupDocScorer(DatabaseDataSource data,
			FeatureVectorBuilder interpreter) {
		this.data = data;
		this.interpreter = interpreter;
	}

	@Override
	public double scoreTerm(String term, SearchHit doc) {
		String docColumnName = "DOCUMENT=\"";
		if (interpreter instanceof RelevanceModelDataInterpreter) {
			docColumnName = "ORIGINAL_" + docColumnName;
		}
		data.read(RelevanceModelDataInterpreter.TERM_FIELD + "=\"" + term + "\"",
				docColumnName + doc.getDocno() + "\"");
		FeatureVector termScores = interpreter.build(data);
		if (!termScores.contains(term)) {
			//System.err.println("No score for " + term + " in " + doc.getDocno());
			return 0.0;
		}
		return termScores.getFeatureWeight(term);
	}

}
