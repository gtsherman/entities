package edu.gslis.entities.docscoring.support;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import edu.gslis.entities.readers.AbstractReader;
import edu.gslis.entities.readers.Reader;

public class CategoryLength extends AbstractReader implements Reader {

	private String thisClass = "[CategoryLength] ";

	private Map<String, Double> categoryLength;
	
	@Override
	public void readFile(File file) {
		System.err.println(thisClass+"Reading file "+file.getAbsolutePath());
		categoryLength = new HashMap<String, Double>();
		try {
			Scanner scanner = new Scanner(file);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				String[] parts = line.split("\\|");
				double length = Double.parseDouble(parts[0]);
				String category = line.substring(line.indexOf('|')+1).trim();
				categoryLength.put(category, length);
			}
			scanner.close();
		} catch (Exception e) {
			System.err.println(thisClass+"Error reading category lengths file");
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	public double getLength(String category) {
		Double length = categoryLength.get(category);
		if (length == null) {
			length = 0.0;
		}
		return length;
	}

}
