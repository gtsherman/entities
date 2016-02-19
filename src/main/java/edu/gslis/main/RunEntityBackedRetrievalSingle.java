package edu.gslis.main;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;

import edu.gslis.docscoring.support.CollectionStats;
import edu.gslis.docscoring.support.IndexBackedCollectionStats;
import edu.gslis.entities.DocumentModel;
import edu.gslis.entities.docscoring.ScorerDirichletCategory2Single;
import edu.gslis.entities.docscoring.support.CategoryProbability;
import edu.gslis.entities.docscoring.support.EntityCategoryProbability;
import edu.gslis.entities.docscoring.support.EntityQueryWeightedProbability;
import edu.gslis.entities.readers.DocumentEntityReader;
import edu.gslis.entities.readers.TopEntitiesReader;
import edu.gslis.entities.utils.Configuration;
import edu.gslis.entities.utils.SimpleConfiguration;
import edu.gslis.patches.FormattedOutputTrecEval;
import edu.gslis.patches.IndexWrapperIndriImpl;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.queries.GQuery;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;

public class RunEntityBackedRetrievalSingle {

	public static void main(String[] args) {
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
		
		DocumentEntityReader de = new DocumentEntityReader();
		de.readFileAbsolute(config.get("document-entities-file"));
		
		DocumentModel dm = new DocumentModel(stopper);
		if (config.get("document-models-dir") != null)
			dm.setBasePath(config.get("document-models-dir"));
		
		CategoryProbability cp;
		if (config.get("document-models-dir") == null) {
			System.err.println("Using EntityQueryWeightedProbability");
			cp = new EntityQueryWeightedProbability(de, wikiIndex, stopper);
		} else {
			System.err.println("Using EntityCategoryProbability");
			cp = new EntityCategoryProbability(de, dm, stopper);
		}

		TopEntitiesReader topEntities = new TopEntitiesReader();
		if (config.get("top-entities") != null) {
			topEntities.readFileAbsolute(config.get("top-entities"));
		}
		
		CollectionStats cs = new IndexBackedCollectionStats();
		cs.setStatSource(config.get("index"));
		
		if (cp instanceof EntityCategoryProbability) {
			((EntityCategoryProbability) cp).setCollectionStats(cs);
		}
		if (cp instanceof EntityQueryWeightedProbability) {
			((EntityQueryWeightedProbability) cp).setCollectionStats(cs);
			if (config.get("top-entities") != null) {
				((EntityQueryWeightedProbability) cp).setTopEntities(topEntities);
			}
		}
		
		double backgroundMix = 0.5;
		if (config.get("background-mix") != null) {
			backgroundMix = Double.parseDouble(config.get("background-mix"));
		}
		
		double mu = 2500;
		if (config.get("mu") != null) {
			mu = Double.parseDouble(config.get("mu"));
		}

		ScorerDirichletCategory2Single scorer = new ScorerDirichletCategory2Single();
		scorer.setCollectionStats(cs);
		scorer.setCategoryProbability(cp);
		scorer.setParameter(scorer.BACKGROUND_MIX, backgroundMix);
		scorer.setParameter(scorer.PARAMETER_NAME, mu);

		Writer outputWriter = new BufferedWriter(new OutputStreamWriter(System.out));
		FormattedOutputTrecEval output = FormattedOutputTrecEval.getInstance("entities", outputWriter);
		
		Iterator<GQuery> queryIt = queries.iterator();
		while (queryIt.hasNext()) {
			GQuery query = queryIt.next();

			if (stopper != null)
				query.applyStopper(stopper);
			scorer.setQuery(query);
			
			if (cp instanceof EntityQueryWeightedProbability) {
				((EntityQueryWeightedProbability) cp).setQuery(query.getTitle());
			}
			
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
			hits.crop(1000);
			output.write(hits, query.getTitle());
		}
		output.close();
	}

}
