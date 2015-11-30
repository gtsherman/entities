package edu.gslis.entities.categories;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class Categories implements Iterable<Category> {
	
	public static final String PAGE = "PAGE";
	public static final String SUBCATEGORY = "SUBCATEGORY";
	
	private Map<String, Category> categories = new HashMap<String, Category>();

	
	public Categories() {}
	
	public Categories(String pagesFile, String subcategoriesFile) {
		System.err.println("Reading category pages...");
		this.readPages(pagesFile);
		System.err.println("Reading subcategories...");
		this.readSubcategories(subcategoriesFile);
	}
	
	public void readPages(String file) {
		this.read(file, PAGE);
	}
	
	public void readSubcategories(String file) {
		this.read(file, SUBCATEGORY);
	}

	private void read(String file, String addType) {
		try {
			int l = 0;
			System.err.println(l);
			Scanner scanner = new Scanner(new File(file));
			while (scanner.hasNextLine()) {
				System.err.println("\r"+l++);
				String line = scanner.nextLine();
				
				String[] parts = line.split("\\|");

				String catName = parts[0];
				Category category = this.getCategoryByName(catName);
				for (int i = 1; i < parts.length; i++) {
					String part = parts[i];
					if (addType.equals(PAGE)) {
						category.addPage(part);
					} else if (addType.equals(SUBCATEGORY)) {
						category.addSubcategory(this.getCategoryByName(part));
					}
				}
				this.categories.put(catName, category);
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			System.err.println("Unable to read categories file.");
		}
	}
	
	public int size() {
		return this.categories.size();
	}
	
	public void addCategory(Category newCat) {
		this.categories.put(newCat.getName(), newCat);
	}
	
	public void addCategories(List<Category> categories) {
		for (Category cat : categories) {
			this.categories.put(cat.getName(), cat);
		}
	}

	public Iterator<Category> iterator() {
		Set<Category> categories = (Set<Category>) this.categories.values();
		return categories.iterator();
	}
	
	public Category getCategoryByName(String name) {
		return this.categories.keySet().contains(name) ? this.categories.get(name) : new Category(name);
	}
	
	public void setCategory(Category cat) {
		this.categories.put(cat.getName(), cat);
	}
}
