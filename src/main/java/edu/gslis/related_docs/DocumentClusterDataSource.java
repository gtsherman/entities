package edu.gslis.related_docs;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import edu.gslis.utils.data.sources.FileDataSource;

public class DocumentClusterDataSource extends FileDataSource {

	// how many related docs to read per original doc
	public static final int DEFAULT_LIMIT = 10;
	
	public DocumentClusterDataSource(File file) {
		this(file, DEFAULT_LIMIT);
	}
	
	public DocumentClusterDataSource(File file, int limit) {
		super(file, limit);
	}
	
	@Override
	public boolean read(File file) {
		return read(file, DEFAULT_LIMIT);
	}
	
	@Override
	public boolean read(File file, int limit) {
		flush();
		
		boolean success = true;
		
		try {
			int origDocIndex = DocumentClusterDataInterpreter.ALL_FIELDS.indexOf(
					DocumentClusterDataInterpreter.ORIGINAL_DOCUMENT);
			int lineCount = 0;
			String currentDoc = "";
			Scanner scanner = new Scanner(file);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				lineCount++;

				String[] parts = line.split(getDelimiter());
				if (!parts[origDocIndex].equals(currentDoc)) {
					lineCount = 1;
					currentDoc = parts[origDocIndex];
				} else if (lineCount > limit) { // it is the same document as last line
					continue; // skip this line, we've reached our limit for this document
				}

				addTuple(parts);
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			System.err.println("File not found: "+file.getAbsolutePath());
			success = false;
		}
		
		return success;
	}

}
