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
		System.err.println("Reading file "+file.getAbsolutePath());
		entities = new HashSet<String>();
		try {
			Scanner scanner = new Scanner(file);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if (line.length() < 1) {
					continue;
				}
				String[] parts = line.split("\\t");

				String entity = parts[parts.length-1];
				entities.add(entity);
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
