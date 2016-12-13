package edu.gslis.related_docs;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Scanner;

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
	
	public RelatedDocs getClusters() {
		return relatedDocs;
	}
	
	public void read(File file, int limit) {
		try {
			int lineCount = 0;
			String currentDoc = "";
			Scanner scanner = new Scanner(file);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				lineCount++;

				String[] parts = line.split(delimiter);
				for (int i = 0; i < parts.length; i++) {
					parts[i] = parts[i].trim();

					String fieldName = fieldAtIndex(i);
					
					if (fieldName.equals(ORIGINAL_DOCUMENT)) {
						if (!parts[i].equals(currentDoc)) {
							lineCount = 1;
							currentDoc = parts[i];
						} else if (lineCount > limit) { // it is the same document as last line
							continue; // skip this line, we've reached our limit for this document
						}
					}
				}
				
				results.add(parts);
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			System.err.println("File not found: "+file.getAbsolutePath());
		}
	}
	
	private void createRelatedDocs() {
		relatedDocs = new RelatedDocs();
		Iterator<String[]> tupleIt = results.iterator();
		while (tupleIt.hasNext()) {
			String[] tuple = tupleIt.next();
			relatedDocs.setRelatedDocScore(valueOfField(ORIGINAL_DOCUMENT, tuple),
					valueOfField(RELATED_DOCUMENT, tuple),
					Double.parseDouble(valueOfField(RELATED_DOCUMENT_SCORE, tuple)));
		}
	}

}
