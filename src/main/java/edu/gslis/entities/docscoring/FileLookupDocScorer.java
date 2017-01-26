package edu.gslis.entities.docscoring;

import java.io.File;

import edu.gslis.readers.QueryProbabilityDataInterpreter;
import edu.gslis.scoring.DocScorer;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.data.sources.DataSource;
import edu.gslis.utils.data.sources.FileDataSource;

public class FileLookupDocScorer implements DocScorer {
	
	private String basePath;
	private FeatureVector termProbs;
	private String currentDoc = "";
	
	public FileLookupDocScorer(String basePath) {
		this.basePath = basePath;
	}
	
	public void build(String fileName) {
		build(new FileDataSource(new File(basePath + File.separator + fileName)));
	}

	public void build(DataSource ds) {
		QueryProbabilityDataInterpreter qpreader = new QueryProbabilityDataInterpreter();
		termProbs = qpreader.build(ds);
	}

	@Override
	public double scoreTerm(String term, SearchHit doc) {
		String docno = doc.getDocno();
		if (!currentDoc.equals(docno)) {
			if (doc.getQueryName() != null) {
				build(doc.getQueryName() + File.separator + docno);
			} else {
				build(docno);
			}
			currentDoc = docno;
		}
		if (!termProbs.contains(term)) {
			return 0.0;
		}
		return termProbs.getFeatureWeight(term);
	}

}
