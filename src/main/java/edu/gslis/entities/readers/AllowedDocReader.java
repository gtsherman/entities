package edu.gslis.entities.readers;

import java.io.File;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class AllowedDocReader extends AbstractReader implements Reader {
	private String thisClass = "[AllowedDocReader] ";

	private Set<String> allowedDocs;
	
	@Override
	public void readFile(File file) {
		allowedDocs = new HashSet<String>();
		try {
			Scanner scanner = new Scanner(file);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				
				try {
					String doc = line.trim();
					allowedDocs.add(doc);
				} catch (ArrayIndexOutOfBoundsException e) {
					System.err.println(thisClass+"Error reading line: "+line);
				}
			}
			scanner.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		System.err.println(thisClass+allowedDocs.size()+" allowed docs.");
	}
	
	public boolean contains(String doc) {
		return allowedDocs.contains(doc);
	}

}
