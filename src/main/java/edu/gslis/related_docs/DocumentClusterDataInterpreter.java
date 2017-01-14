package edu.gslis.related_docs;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import edu.gslis.utils.data.interpreters.DataInterpreter;
import edu.gslis.utils.data.sources.DataSource;

public class DocumentClusterDataInterpreter extends DataInterpreter {
	
	public static final String ORIGINAL_DOCUMENT = "ORIG_DOC";
	public static final String RELATED_DOCUMENT = "REL_DOC";
	public static final String RELATED_DOCUMENT_SCORE = "REL_DOC_SCORE";
	public static final List<String> ALL_FIELDS = Arrays.asList(
			ORIGINAL_DOCUMENT, RELATED_DOCUMENT, RELATED_DOCUMENT_SCORE);

	public DocumentClusterDataInterpreter() {
		super(ALL_FIELDS);
	}
	
	@Override
	public RelatedDocs build(DataSource dataSource) {
		RelatedDocs relatedDocs = new RelatedDocs();
		Iterator<String[]> tupleIt = dataSource.iterator();
		while (tupleIt.hasNext()) {
			String[] tuple = tupleIt.next();
			relatedDocs.setRelatedDocScore(valueOfField(ORIGINAL_DOCUMENT, tuple),
					valueOfField(RELATED_DOCUMENT, tuple),
					Double.parseDouble(valueOfField(RELATED_DOCUMENT_SCORE, tuple)));
		}
		return relatedDocs;
	}

}
