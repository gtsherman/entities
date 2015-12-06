package edu.gslis.entities.categories;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.gslis.entities.DocumentEntities;
import edu.gslis.entities.EntityCategories;
import edu.gslis.indexes.IndexWrapper;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;

public class MLECategoryModel implements CategoryModel {
	private static String thisClass = "[MLECategoryModel] ";

	private Stopper stopper = null;
	
	private IndexWrapper index;
	private DocumentEntities de;
	private EntityCategories ec;
	
	private FeatureVector backgroundVector;
	
	/**
	 * Constructor
	 * @param index	The Wikipedia index
	 * @param de	DocumentEntities object with basepath specified
	 * @param ec	EntityCategories object with data already read in
	 */
	public MLECategoryModel(IndexWrapper index, DocumentEntities de, EntityCategories ec) {
		setIndex(index);
		setDocumentEntities(de);
		setEntityCategories(ec);
	}
	
	public MLECategoryModel() {
		this(null, null, null);
	}
	
	public void setIndex(IndexWrapper index) {
		this.index = index;
	}

	public void setDocumentEntities(DocumentEntities de) {
		this.de = de;
	}
	
	public void setEntityCategories(EntityCategories ec) {
		this.ec = ec;
	}
	
	public void setDocument(SearchHit doc) {
		de.readFileRelative(doc.getDocno()+".tsv");
		
		readCategories(doc);
	}

	public double getScore(String term) {
		return backgroundVector.getFeatureWeight(term) / backgroundVector.getLength();
	}

	private void readCategories(SearchHit document) {
		FeatureVector backgroundVector = new FeatureVector(stopper);
		FeatureVector categoryVector = null;
		Set<String> seenCategories = new HashSet<String>();
		Iterator<String> vectorIt;
		FeatureVector docVector;
		String term;
		for (String entity : de.getEntities()) {  // each entity in the document
			System.err.println("\t"+thisClass+"Entity: "+entity);

			for (String category : ec.getCategories(entity)) {  // each category per entity
				if (seenCategories.contains(category))
					continue;
				seenCategories.add(category);

				System.err.println("\t\t"+thisClass+"Category: "+category);
				categoryVector = new FeatureVector(stopper);
				
				for (String page : ec.getEntities(category)) {  // each page within the category
					docVector = index.getDocVector(page, stopper);

					vectorIt = docVector.iterator();
					while (vectorIt.hasNext()) {
						term = vectorIt.next();
						
						categoryVector.addTerm(term, docVector.getFeatureWeight(term));  // add the page text to the category vector
					}
				}
			}
			
			// Category vector has been constructed
			vectorIt = categoryVector.iterator();
			while (vectorIt.hasNext()) {
				term = vectorIt.next();
				
				backgroundVector.addTerm(term, categoryVector.getFeatureWeight(term));  // add the category vector to the background vector
			}
		}
		
		this.backgroundVector = backgroundVector;
	}
}
