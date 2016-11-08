package edu.gslis.related_docs;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

import edu.gslis.searchhits.SearchHit;
import edu.gslis.utils.readers.Reader;

public class DocumentClusterReader extends Reader {
	
	public static final String ORIGINAL_DOCUMENT = "ORIG_DOC";
	public static final String RELATED_DOCUMENT = "REL_DOC";
	public static final String RELATED_DOCUMENT_SCORE = "REL_DOC_SCORE";

	public static final int DEFAULT_LIMIT = 10; // how many related docs to read per original doc
	
	private RelatedDocs relatedDocs;
	
	public DocumentClusterReader(File file) {
		this(file, DEFAULT_LIMIT);
	}
	
	public DocumentClusterReader(File file, int limit) {
		super(Arrays.asList(ORIGINAL_DOCUMENT, RELATED_DOCUMENT, RELATED_DOCUMENT_SCORE));
		read(file, limit);
		createRelatedDocs();
	}
	
	public Map<String, Double> getRelatedDocs(SearchHit doc) {
		return getRelatedDocs(doc.getDocno());
	}
	
	public Map<String, Double> getRelatedDocs(String docno) {
		return relatedDocs.getDocsRelatedTo(docno);
	}
	
	public void read(File file, int limit) {
		try {
			int l = 0;
			Scanner scanner = new Scanner(file);
			while (scanner.hasNextLine()) {
				l++;
				if (l > limit) {
					break;
				}

				String line = scanner.nextLine();
				
				Map<String, String> field = new HashMap<String, String>();

				String[] parts = line.split(delimiter);
				for (int i = 0; i < parts.length; i++) {
					String fieldValue = parts[i].trim();
					String fieldName;
					try {
						fieldName = fields.get(i);
					} catch (IndexOutOfBoundsException e) {
						fieldName = "Field"+i;
					}
					field.put(fieldName, fieldValue);
				}
				
				results.add(field);
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			System.err.println("File not found: "+file.getAbsolutePath());
		}
	}
	
	private void createRelatedDocs() {
		relatedDocs = new RelatedDocs();
		Iterator<Map<String, String>> tupleIt = results.iterator();
		while (tupleIt.hasNext()) {
			Map<String, String> tuple = tupleIt.next();
			relatedDocs.setRelatedDocScore(tuple.get(ORIGINAL_DOCUMENT),
					tuple.get(RELATED_DOCUMENT),
					Double.parseDouble(tuple.get(RELATED_DOCUMENT_SCORE)));
		}
	}

}
