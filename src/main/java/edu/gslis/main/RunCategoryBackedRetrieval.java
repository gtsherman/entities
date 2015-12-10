package edu.gslis.main;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

import edu.gslis.docscoring.support.CollectionStats;
import edu.gslis.docscoring.support.IndexBackedCollectionStats;
import edu.gslis.entities.DocumentEntities;
import edu.gslis.entities.EntityCategories;
import edu.gslis.entities.categories.CategoryModel;
import edu.gslis.entities.categories.MLECategoryModel;
import edu.gslis.entities.categories.PrecomputedCategoryModel;
import edu.gslis.entities.docscoring.ScorerDirichletCategory;
import edu.gslis.entities.docscoring.support.CategoryLength;
import edu.gslis.entities.docscoring.support.CategoryProbability;
import edu.gslis.entities.docscoring.support.PrecomputedCategoryProbability;
import edu.gslis.entities.docscoring.support.PrecomputedMLECategoryProbability;
import edu.gslis.entities.utils.Configuration;
import edu.gslis.entities.utils.SimpleConfiguration;
import edu.gslis.patches.IndexWrapperIndriImpl;
import edu.gslis.output.FormattedOutputTrecEval;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.queries.GQuery;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;

public class RunCategoryBackedRetrieval {
	
	public static void main(String[] args) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		IndexWrapperIndriImpl index = new IndexWrapperIndriImpl(config.get("index"));
		IndexWrapperIndriImpl wikiIndex = null;
		if (config.get("wiki-index") != null)
			wikiIndex = new IndexWrapperIndriImpl(config.get("wiki-index"));
		
		Stopper stopper = null;
		if (config.get("stoplist") != null)
			stopper = new Stopper(config.get("stoplist"));
		
		GQueriesJsonImpl queries = new GQueriesJsonImpl();
		queries.read(config.get("queries"));
		
		String entityCategories = config.get("entity-categories");
		EntityCategories ec = new EntityCategories();
		ec.readFileAbsolute(entityCategories);
		
		CategoryLength cl = new CategoryLength();
		if (config.get("category-lengths") != null) {
			cl.readFileAbsolute(config.get("category-lengths"));
		}

		String categoryModelsDir = config.get("category-models-directory");
		String categoryModelClass = config.get("category-model-class");
		CategoryModel cm = (CategoryModel) Class.forName(categoryModelClass).getConstructor().newInstance();
		if (cm instanceof PrecomputedCategoryModel) {
			((PrecomputedCategoryModel) cm).setBasePath(categoryModelsDir);
		} else if (cm instanceof MLECategoryModel) {
			((MLECategoryModel) cm).setIndex(wikiIndex);
		}
		
		String entityDocumentsDir = config.get("entity-documents-directory");
		DocumentEntities de = new DocumentEntities();
		de.setBasePath(entityDocumentsDir);
		
		String categoryProbabilityClass = config.get("category-probability-class");
		String precomputedProbabilityFile = config.get("precomputed-probabilities");

		CategoryProbability cp;
		if (precomputedProbabilityFile == null) {
			cp = (CategoryProbability) Class.forName(categoryProbabilityClass).getConstructor(DocumentEntities.class, EntityCategories.class, CategoryModel.class).newInstance(de, ec, cm);
		} else {
			cp = new PrecomputedCategoryProbability(precomputedProbabilityFile);
		}
		
		if (cp instanceof PrecomputedMLECategoryProbability)
			((PrecomputedMLECategoryProbability) cp).setCategoryLength(cl);
		
		CollectionStats cs = new IndexBackedCollectionStats();
		cs.setStatSource(config.get("index"));

		ScorerDirichletCategory scorer = new ScorerDirichletCategory();
		scorer.setCategoryProbability(cp);
		scorer.setCollectionStats(cs);
		scorer.setParameter(scorer.BACKGROUND_MIX, 0.5);
		scorer.setParameter(scorer.PARAMETER_NAME, 2500);

		Writer outputWriter = new BufferedWriter(new OutputStreamWriter(System.out));
		FormattedOutputTrecEval output = FormattedOutputTrecEval.getInstance("categories", outputWriter);
		
		Iterator<GQuery> queryIt = queries.iterator();
		while (queryIt.hasNext()) {
			GQuery query = queryIt.next();

			if (stopper != null)
				query.applyStopper(stopper);
			scorer.setQuery(query);
			
			if (cp instanceof PrecomputedCategoryProbability)
				((PrecomputedCategoryProbability) cp).setQuery(query);
			
			SearchHits hits = index.runQuery(query, 1000);
			Iterator<SearchHit> hitIt = hits.iterator();
			while (hitIt.hasNext()) {
				SearchHit hit = hitIt.next();
				FeatureVector dv = index.getDocVector(hit.getDocID(), null);
				hit.setFeatureVector(dv);
				hit.setLength(dv.getLength());
				hit.setScore(scorer.score(hit));
			}
			hits.rank();
			output.write(hits, query.getTitle());
		}
	}

}
