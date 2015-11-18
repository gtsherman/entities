package edu.gslis.entities;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import edu.gslis.entities.readers.AbstractReader;

public class DocumentEntities extends AbstractReader {

	private Set<String> entities;
	
	@Override
	public void readFile(File file) {
		Set<String> entities = new HashSet<String>();
		try {
			Scanner scanner = new Scanner(file);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				String[] parts = line.split("\\t");

				entities.add(parts[parts.length-1]);
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			System.err.println("Couldn't find TSV file: "+file.getName());
		}
	}
	
	public Set<String> getEntities() {
		return this.entities;
	}

}
