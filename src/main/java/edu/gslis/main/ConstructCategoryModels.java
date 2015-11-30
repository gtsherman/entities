package edu.gslis.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Scanner;

import edu.gslis.entities.categories.Category;
import edu.gslis.entities.categories.UniformCategoryModel;
import edu.gslis.entities.utils.Configuration;
import edu.gslis.entities.utils.NameToFileNameConverter;
import edu.gslis.entities.utils.SimpleConfiguration;
import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.utils.Stopper;

public class ConstructCategoryModels {

	public static void main(String[] args) throws FileNotFoundException {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		IndexWrapperIndriImpl index = new IndexWrapperIndriImpl(config.get("index"));
		
		Stopper stopper = null;
		if (config.get("stoplist") != null)
			stopper = new Stopper(config.get("stoplist"));
		
		String outputDir = config.get("outdir");

		String categoryPages = config.get("category-pages");
		String categorySubcats = config.get("category-subcategories");
		System.err.println("Loading categories...");
		
		Scanner scanner = new Scanner(new File(categoryPages));
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			
			String[] parts = line.split("\\|");
			
			String catName = parts[0];
			Category cat = new Category(catName);
			for (int i = 1; i < parts.length; i++) {
				String part = parts[i];
				cat.addPage(part.trim());
			}

			UniformCategoryModel model;

			System.err.println("Constructing category model for "+cat.getName());

			model = new UniformCategoryModel(cat);
			model.setIndex(index);
			model.setStopper(stopper);
			model.build();
			
			NameToFileNameConverter nc = new NameToFileNameConverter(cat.getName());
			String c1 = nc.getFirstChar();
			String c2 = nc.getSecondChar();
			
			File d1 = new File(outputDir+"/"+c1);
			if (!d1.exists()) {
				d1.mkdir();
			}
			File d2 = new File(d1+"/"+c2);
			if (!d2.exists()) {
				d2.mkdir();
			}
			
			try {
				PrintWriter out = new PrintWriter(d2+"/"+cat.getName());
				out.println(model.getModel().toString(1000));
				out.close();
			} catch (FileNotFoundException e) {
				System.err.println("Can't write "+d2+"/"+cat.getName());
			}
		}
		scanner.close();
	}

}
