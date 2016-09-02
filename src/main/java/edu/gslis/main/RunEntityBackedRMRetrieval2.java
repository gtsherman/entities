package edu.gslis.main;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.gslis.docscoring.support.CollectionStats;
import edu.gslis.docscoring.support.IndexBackedCollectionStats;
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
import edu.gslis.utils.Stopper;
import edu.gslis.utils.config.Configuration;
import edu.gslis.utils.config.SimpleConfiguration;

public class RunEntityBackedRMRetrieval2 {
	
	final static Logger logger = LoggerFactory.getLogger(RunEntityBackedRMRetrieval2.class);

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
		if (args.length >= 2) {
			origQueryWeight = Double.parseDouble(args[1]);
		}

		Iterator<GQuery> queryIt = queries.iterator();
		int i = 0;
		while (queryIt.hasNext()) {
			GQuery query = queryIt.next();
			
			i++;
			logger.info("Working on query "+query.getTitle()+". ("+i+"/"+queries.numQueries()+")");
			
			FeatureVector finalRM = new FeatureVector(stopper);
			
			SearchHits initialDocs = index.runQuery(query, fbDocs);
			Iterator<SearchHit> docIt = initialDocs.iterator();
			while (docIt.hasNext()) {
				SearchHit document = docIt.next();
				logger.info("Document: "+document.getDocno());
				
				SearchHits entityHits = new SearchHits();
				List<String> entities = de.getEntities(document.getDocno());
				for (String entity : entities) {
					SearchHit entityHit = new SearchHit();

					logger.info("Entity: "+entity);
					FeatureVector entityVec = wikiIndex.getDocVector(entity, stopper);
					entityHit.setDocno(entity);
					entityHit.setDocID(wikiIndex.getDocId(entity));
					entityHit.setFeatureVector(entityVec);
					entityHit.setScore(de.getEntityConfidence(document.getDocno(), entity));
					
					entityHits.add(entityHit);
				}
				
				logger.info("Setting up entity RM");
				FeedbackRelevanceModel rm = new FeedbackRelevanceModel();
				rm.setIndex(wikiIndex);
				rm.setDocCount(fbDocs);
				rm.setTermCount(fbTerms);
				rm.setStopper(stopper);
				rm.setOriginalQuery(query);
				rm.setRes(entityHits);
				
				logger.info("Building entity RM");
				rm.build();
				FeatureVector rmVec = rm.asGquery().getFeatureVector();
				rmVec.normalize();
				
				logger.info("Updating final RM");
				Iterator<String> termIt = rmVec.iterator();
				while (termIt.hasNext()) {
					String term = termIt.next();
					finalRM.addTerm(term, rmVec.getFeatureWeight(term)*document.getScore());
				}
			}

			FeatureVector rm3 = FeatureVector.interpolate(query.getFeatureVector(), finalRM, origQueryWeight);
			GQuery rmQuery = new GQuery();
			rmQuery.setFeatureVector(rm3);
			rmQuery.setTitle(query.getTitle());
			
			SearchHits hits = index.runQuery(rmQuery, numDocs);
			
			Writer outputWriter = new BufferedWriter(new OutputStreamWriter(System.out));
			FormattedOutputTrecEval output = FormattedOutputTrecEval.getInstance("entityRM3", outputWriter);
			output.write(hits, rmQuery.getTitle());
		}
	}
}
