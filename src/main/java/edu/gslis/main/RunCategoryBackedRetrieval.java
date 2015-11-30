package edu.gslis.main;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;

import edu.gslis.docscoring.support.CollectionStats;
import edu.gslis.docscoring.support.IndexBackedCollectionStats;
import edu.gslis.entities.DocumentEntities;
import edu.gslis.entities.EntityCategories;
import edu.gslis.entities.categories.CategoryModel;
import edu.gslis.entities.docscoring.ScorerDirichletCategory;
import edu.gslis.entities.docscoring.support.CategoryProbability;
import edu.gslis.entities.docscoring.support.IDFCategoryProbability;
import edu.gslis.entities.utils.Configuration;
import edu.gslis.entities.utils.SimpleConfiguration;
import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.output.FormattedOutputTrecEval;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.queries.GQuery;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.utils.Stopper;

public class RunCategoryBackedRetrieval {
	
	public static void main(String[] args) {
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
		
		CategoryProbability cp = new IDFCategoryProbability(de, ec, cm);
		
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
			
			SearchHits hits = index.runQuery(query, 1000);
			Iterator<SearchHit> hitIt = hits.iterator();
			while (hitIt.hasNext()) {
				SearchHit hit = hitIt.next();
				hit.setFeatureVector(index.getDocVector(hit.getDocno(), null));
				hit.setScore(scorer.score(hit));
			}
			hits.rank();
			output.write(hits, query.getTitle());
		}
	}

}
