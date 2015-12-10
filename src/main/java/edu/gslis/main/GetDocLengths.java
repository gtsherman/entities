package edu.gslis.main;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import edu.gslis.entities.EntityCategories;
import edu.gslis.indexes.IndexWrapper;
import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.utils.Stopper;

public class GetDocLengths {
	
	public static void main(String[] args) {
		String indexFile = args[0];
		String categoryList = args[1];
		String categoryEntitiesFile = args[2];
		String stopperFile = args[3];
		
		EntityCategories ec = new EntityCategories();
		ec.readFileAbsolute(categoryEntitiesFile);
		
		Stopper stopper = new Stopper(stopperFile);
		
		IndexWrapper index = new IndexWrapperIndriImpl(indexFile);
		
		Map<String, Double> entityLengths = new HashMap<String, Double>();
		
		try {
			Scanner scanner = new Scanner(new File(categoryList));
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine().trim();
				
				double length = 0;
				for (String entity : ec.getEntities(line)) {
					if (!entityLengths.containsKey(entity)) {
						entityLengths.put(entity, index.getDocVector(entity, stopper).getLength());
					}
					length += entityLengths.get(entity);
				}
				System.out.println(length+"|"+line);
			}
			scanner.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		
	}

}
