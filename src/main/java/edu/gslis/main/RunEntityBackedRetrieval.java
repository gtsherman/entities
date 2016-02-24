package edu.gslis.main;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.gslis.docscoring.support.CollectionStats;
import edu.gslis.docscoring.support.IndexBackedCollectionStats;
import edu.gslis.entities.docscoring.ScorerDirichletEntityInterpolated;
import edu.gslis.entities.docscoring.support.EntityExpectedProbability;
import edu.gslis.entities.docscoring.support.EntityProbability;
import edu.gslis.entities.docscoring.support.EntityPseudoDocumentProbability;
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

public class RunEntityBackedRetrieval {

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
		
		EntityProbability cp = new EntityExpectedProbability(de, wikiIndex, stopper);

		TopEntitiesReader topEntities = new TopEntitiesReader();
		if (config.get("top-entities") != null) {
			topEntities.readFileAbsolute(config.get("top-entities"));
		}
		
		CollectionStats cs = new IndexBackedCollectionStats();
		cs.setStatSource(config.get("index"));
		
		if (cp instanceof EntityPseudoDocumentProbability) {
			((EntityPseudoDocumentProbability) cp).setCollectionStats(cs);
		}
		if (cp instanceof EntityExpectedProbability) {
			((EntityExpectedProbability) cp).setCollectionStats(cs);
			if (config.get("top-entities") != null) {
				((EntityExpectedProbability) cp).setTopEntities(topEntities);
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

		ScorerDirichletEntityInterpolated scorer = new ScorerDirichletEntityInterpolated();
		scorer.setCollectionStats(cs);
		scorer.setCategoryProbability(cp);
		scorer.setParameter(scorer.BACKGROUND_MIX, backgroundMix);
		scorer.setParameter(scorer.PARAMETER_NAME, mu);

		Writer outputWriter = new BufferedWriter(new OutputStreamWriter(System.out));
		FormattedOutputTrecEval output = FormattedOutputTrecEval.getInstance("entities", outputWriter);
		
		Iterator<GQuery> queryIt = queries.iterator();
		while (queryIt.hasNext()) {
			GQuery query = queryIt.next();
			
			/*FeedbackRelevanceModel rm = new FeedbackRelevanceModel();
			rm.setIndex(index);
			rm.setDocCount(20);
			rm.setTermCount(20);
			rm.setStopper(stopper);
			rm.setOriginalQuery(query);
			
			rm.build();
			FeatureVector rm3 = FeatureVector.interpolate(rm.asFeatureVector(), query.getFeatureVector(), 0.5);
			GQuery rmQuery = new GQuery();
			rmQuery.setFeatureVector(rm3);
			rmQuery.setTitle(query.getTitle());;
			*/
			
			scorer.setQuery(query);
			
			if (cp instanceof EntityExpectedProbability) {
				((EntityExpectedProbability) cp).setQuery(query.getTitle());
			}
			
			SearchHits hits = index.runQuery(query, 1000);
			//List<String> retrievedDocs = docs.getDocsForQuery(query.getTitle());
			Map<Double, SearchHits> lambdaToSearchHits = new HashMap<Double, SearchHits>();
			//Iterator<String> docIt = retrievedDocs.iterator();
			//while (docIt.hasNext()) {
			Iterator<SearchHit> hitIt = hits.iterator();
			while (hitIt.hasNext()) {
				//String doc = docIt.next();
				SearchHit hit = hitIt.next();
				
				//SearchHit hit = new SearchHit();
				//hit.setDocno(doc);
				FeatureVector dv = index.getDocVector(hit.getDocno(), null);
				hit.setFeatureVector(dv);
				hit.setLength(dv.getLength());

				scorer.score(hit); // produces scores for all values of lambda

				for (double lambda : scorer.getLambdaToScore().keySet()) {
					SearchHit newHit = new SearchHit();
					newHit.setDocno(hit.getDocno());
					newHit.setScore(scorer.getScore(lambda));
					
					if (!lambdaToSearchHits.containsKey(lambda)) {
						lambdaToSearchHits.put(lambda, new SearchHits());
					}
					lambdaToSearchHits.get(lambda).add(newHit);
				}
			}
			for (double lambda : lambdaToSearchHits.keySet()) {
				output.setRunId(Double.toString(lambda));
				SearchHits theseHits = lambdaToSearchHits.get(lambda);
				theseHits.rank();
				theseHits.crop(1000);
				output.write(theseHits, query.getTitle());
			}
		}
		output.close();
	}

}
