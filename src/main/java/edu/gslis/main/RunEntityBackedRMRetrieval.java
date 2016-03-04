package edu.gslis.main;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.gslis.docscoring.support.CollectionStats;
import edu.gslis.docscoring.support.IndexBackedCollectionStats;
import edu.gslis.entities.docscoring.ScorerDirichletEntityInterpolated;
import edu.gslis.entities.docscoring.support.EntityExpectedProbability;
import edu.gslis.entities.docscoring.support.EntityProbability;
import edu.gslis.entities.docscoring.support.EntityPseudoDocumentProbability;
import edu.gslis.patches.FormattedOutputTrecEval;
import edu.gslis.patches.IndexWrapperIndriImpl;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.queries.GQuery;
import edu.gslis.queries.expansion.FeedbackRelevanceModel;
import edu.gslis.readers.DocumentEntityReader;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Configuration;
import edu.gslis.utils.SimpleConfiguration;
import edu.gslis.utils.Stopper;

public class RunEntityBackedRMRetrieval {
	
	final static Logger logger = LoggerFactory.getLogger(RunEntityBackedRMRetrieval.class);

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

		CollectionStats cs = new IndexBackedCollectionStats();
		cs.setStatSource(config.get("index"));
		
		if (cp instanceof EntityPseudoDocumentProbability) {
			((EntityPseudoDocumentProbability) cp).setCollectionStats(cs);
		}
		if (cp instanceof EntityExpectedProbability) {
			((EntityExpectedProbability) cp).setCollectionStats(cs);
		}
		
		double mu = 2500;
		if (config.get("mu") != null) {
			mu = Double.parseDouble(config.get("mu"));
		}

		int numDocs = 1000;
		if (config.get("num-docs") != null) {
			numDocs = Integer.parseInt(config.get("num-docs"));
		}
		
		int fbDocs = 20;
		if (config.get("fb-docs") != null) {
			fbDocs = Integer.parseInt(config.get("fb-docs"));
		}
		
		int fbTerms = 20;
		if (config.get("fb-terms") != null) {
			fbTerms = Integer.parseInt(config.get("fb-terms"));
		}
		
		double origQueryWeight = 0.5;
		if (config.get("original-query-weight") != null) {
			origQueryWeight = Double.parseDouble(config.get("original-query-weight"));
		}

		ScorerDirichletEntityInterpolated scorer = new ScorerDirichletEntityInterpolated();
		scorer.setCollectionStats(cs);
		scorer.setCategoryProbability(cp);
		scorer.setParameter(scorer.PARAMETER_NAME, mu);

		Writer outputWriter = new BufferedWriter(new OutputStreamWriter(System.out));
		FormattedOutputTrecEval output = FormattedOutputTrecEval.getInstance("entities", outputWriter);
		
		Iterator<GQuery> queryIt = queries.iterator();
		int i = 0;
		while (queryIt.hasNext()) {
			GQuery query = queryIt.next();
			
			i++;
			logger.info("Working on query "+query.getTitle()+". ("+i+"/"+queries.numQueries()+")");

			FeedbackRelevanceModel rm = new FeedbackRelevanceModel();
			rm.setIndex(index);
			rm.setDocCount(fbDocs);
			rm.setTermCount(fbTerms);
			rm.setStopper(stopper);
			rm.setOriginalQuery(query);
			
			rm.build();
			FeatureVector rmVec = rm.asGquery().getFeatureVector();
			rmVec.normalize();

			FeatureVector rm3 = FeatureVector.interpolate(query.getFeatureVector(), rmVec, origQueryWeight);
			GQuery rmQuery = new GQuery();
			rmQuery.setFeatureVector(rm3);
			rmQuery.setTitle(query.getTitle());
			
			scorer.setQuery(rmQuery);
		
			SearchHits hits = index.runQuery(rmQuery, numDocs);
			Map<Double, SearchHits> lambdaToSearchHits = new HashMap<Double, SearchHits>();
			Iterator<SearchHit> hitIt = hits.iterator();
			while (hitIt.hasNext()) {
				SearchHit hit = hitIt.next();
				
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
				output.write(theseHits, rmQuery.getTitle());
			}
		}
		output.close();
	}

}
