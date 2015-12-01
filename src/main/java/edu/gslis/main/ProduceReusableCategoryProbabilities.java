package edu.gslis.main;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.gslis.docscoring.support.CollectionStats;
import edu.gslis.docscoring.support.IndexBackedCollectionStats;
import edu.gslis.entities.DocumentEntities;
import edu.gslis.entities.EntityCategories;
import edu.gslis.entities.categories.CategoryModel;
import edu.gslis.entities.docscoring.support.CategoryProbability;
import edu.gslis.entities.utils.Configuration;
import edu.gslis.entities.utils.SimpleConfiguration;
import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.queries.GQuery;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.utils.Stopper;

public class ProduceReusableCategoryProbabilities {
	
	public static void main(String[] args) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		IndexWrapperIndriImpl index = new IndexWrapperIndriImpl(config.get("index"));
		
		Stopper stopper = null;
		if (config.get("stoplist") != null)
			stopper = new Stopper(config.get("stoplist"));
		
		GQueriesJsonImpl queries = new GQueriesJsonImpl();
		queries.read(config.get("queries"));
		
		String entityCategories = config.get("entity-categories");
		EntityCategories ec = new EntityCategories();
		ec.readFileAbsolute(entityCategories);

		String categoryModelsDir = config.get("category-models-directory");
		CategoryModel cm = new CategoryModel();
		cm.setBasePath(categoryModelsDir);
		
		String entityDocumentsDir = config.get("entity-documents-directory");
		DocumentEntities de = new DocumentEntities();
		de.setBasePath(entityDocumentsDir);
		
		String categoryProbabilityClass = config.get("category-probability-class");
		
		CategoryProbability cp = (CategoryProbability) Class.forName(categoryProbabilityClass).getConstructor(DocumentEntities.class, EntityCategories.class, CategoryModel.class).newInstance(de, ec, cm);
		
		CollectionStats cs = new IndexBackedCollectionStats();
		cs.setStatSource(config.get("index"));

		Iterator<GQuery> queryIt = queries.iterator();
		while (queryIt.hasNext()) {
			GQuery query = queryIt.next();

			if (stopper != null)
				query.applyStopper(stopper);
			
			List<String> queryList = new ArrayList<String>();
			Iterator<String> qIt = query.getFeatureVector().iterator();
			while (qIt.hasNext()) {
				queryList.add(qIt.next());
			}
			
			SearchHits hits = index.runQuery(query, 1000);
			Iterator<SearchHit> hitIt = hits.iterator();
			while (hitIt.hasNext()) {
				SearchHit hit = hitIt.next();

				cp.setDocument(hit);
				Map<String, Double> termProbs = new HashMap<String, Double>();
				try {
					termProbs = cp.getProbability(queryList);
				} catch (Exception e) {
					e.printStackTrace();
				}

				for (String term : termProbs.keySet()) {
					System.out.println(query.getTitle()+"|"+hit.getDocno()+"|"+hit.getDocID()+"|"+term+"|"+termProbs.get(term));
				}
			}
		}
	}

}
