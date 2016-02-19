package edu.gslis.entities;

import java.io.File;
import java.util.Scanner;

import edu.gslis.entities.categories.CategoryModel;
import edu.gslis.entities.readers.AbstractReader;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;

public class DocumentModel extends AbstractReader implements CategoryModel {
	
	private String thisClass = "[DocumentModel] ";

	private Stopper stopper;
	private FeatureVector model;
	
	public DocumentModel(Stopper stopper) {
		this.stopper = stopper;
	}
	
	public double getScore(String term) {
		return model.getFeatureWeight(term) / model.getLength();
	}
	
	public double getTermFreq(String term) {
		return model.getFeatureWeight(term);
	}
	
	public double getLength() {
		return model.getLength();
	}

	@Override
	public void readFile(File file) {
		model = new FeatureVector(stopper);
		try {
			Scanner scanner = new Scanner(file);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				String[] parts = line.split(" ");

				String term = parts[0];
				double freq = Double.parseDouble(parts[1]);
				
				if (stopper != null && stopper.isStopWord(term))
					continue;
				
				model.addTerm(term, freq);
			}
			scanner.close();
		} catch (Exception e) {
			System.err.println(thisClass+"Error reading file: "+file.getAbsolutePath());
			e.printStackTrace();
		}
	}

}
