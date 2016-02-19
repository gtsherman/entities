package edu.gslis.main;

import java.util.Iterator;

import edu.gslis.docscoring.support.CollectionStats;
import edu.gslis.docscoring.support.IndexBackedCollectionStats;
import edu.gslis.entities.docscoring.ScorerDirichlet;
import edu.gslis.entities.readers.DocumentEntityReader;
import edu.gslis.entities.utils.Configuration;
import edu.gslis.entities.utils.SimpleConfiguration;
import edu.gslis.patches.IndexWrapperIndriImpl;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.queries.GQuery;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;

public class ProduceEntityQueryLikelihood {

	/***
	 * Outputs entity query likelihood scores:
	 * Format:	query,entity,score
	 * @param args	a configuration file
	 */
	public static void main(String[] args) {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		IndexWrapperIndriImpl index = new IndexWrapperIndriImpl(config.get("wiki-index"));
		
		Stopper stopper = null;
		if (config.get("stoplist") != null)
			stopper = new Stopper(config.get("stoplist"));
		
		GQueriesJsonImpl queries = new GQueriesJsonImpl();
		queries.read(config.get("queries"));
		
		DocumentEntityReader de = new DocumentEntityReader();
		de.readFileAbsolute(config.get("document-entities-file"));
		
		CollectionStats cs = new IndexBackedCollectionStats();
		cs.setStatSource(config.get("wiki-index"));
		
		double mu = 2500;
		if (config.get("mu") != null) {
			mu = Double.parseDouble(config.get("mu"));
		}

		ScorerDirichlet scorer = new ScorerDirichlet();
		scorer.setCollectionStats(cs);
		scorer.setParameter(scorer.PARAMETER_NAME, mu);

		Iterator<String> entityIt = de.getAllEntities().iterator();
		while (entityIt.hasNext()) {
			String entity = entityIt.next();
			FeatureVector entityVec = index.getDocVector(entity, stopper);

			Iterator<GQuery> queryIt = queries.iterator();
			while (queryIt.hasNext()) {
				GQuery query = queryIt.next();

				if (stopper != null)
					query.applyStopper(stopper);

				scorer.setQuery(query);
				
				SearchHit hit = new SearchHit();
				hit.setFeatureVector(entityVec);
				hit.setLength(entityVec.getLength());
				hit.setDocno(entity);

				double score = scorer.score(hit);
				System.out.println(entity+","+query.getTitle()+","+score);
			}
		}
	}

}
