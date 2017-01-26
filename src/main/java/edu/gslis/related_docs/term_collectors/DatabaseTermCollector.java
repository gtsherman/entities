package edu.gslis.related_docs.term_collectors;

import edu.gslis.searchhits.SearchHit;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;
import edu.gslis.utils.data.interpreters.RelevanceModelDataInterpreter;
import edu.gslis.utils.data.sources.DatabaseDataSource;

public class DatabaseTermCollector implements TermCollector {
	
	private DatabaseDataSource data;
	private RelevanceModelDataInterpreter interpreter;
	
	public DatabaseTermCollector(DatabaseDataSource data, RelevanceModelDataInterpreter interpreter) {
		this.data = data;
		this.interpreter = interpreter;
	}
	
	@Override
	public FeatureVector getTerms(SearchHit doc, Stopper stopper) {
		data.read(500, "ORIGINAL_DOCUMENT=\"" + doc.getDocno() + "\"");
		FeatureVector terms = interpreter.build(data);
		return terms;
	}

}
