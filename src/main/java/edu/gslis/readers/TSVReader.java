package edu.gslis.readers;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class TSVReader {

	public List<List<String>> readFile(String fileName) {
		File file = new File(fileName);
		return this.readFile(file);
	}
	
	public List<List<String>> readFile(File file) {
		List<List<String>> lines = new ArrayList<List<String>>();
		try {
			Scanner scanner = new Scanner(file);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				String[] parts = line.split("\\t");

				List<String> lineList = new ArrayList<String>();
				for (String part : parts) {
					lineList.add(part.trim());
				}
				
				lines.add(lineList);
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			System.err.println("Couldn't find TSV file: "+file.getName());
		}
		return lines;
	}

}