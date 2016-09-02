package edu.gslis.main;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.queries.GQuery;
import edu.gslis.queries.expansion.FeedbackRelevanceModel;
import edu.gslis.readers.QueryEntitiesReader;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;
import edu.gslis.utils.config.Configuration;
import edu.gslis.utils.config.SimpleConfiguration;

public class CreateRMs {

	static final Logger logger = LoggerFactory.getLogger(CreateRMs.class);

	public static void main(String[] args) throws IOException {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		IndexWrapperIndriImpl index = new IndexWrapperIndriImpl(config.get("index"));
		index.setTimeFieldName(null);
		Stopper stopper = new Stopper(config.get("stoplist"));
		GQueriesJsonImpl queries = new GQueriesJsonImpl();
		queries.read(config.get("queries"));
		String outDir = config.get("rms-dir");
		
		QueryEntitiesReader qdocs = new QueryEntitiesReader();
		String queryEntities = config.get("query-entities");
		if (queryEntities != null) {
			qdocs.readFileAbsolute(queryEntities);
		}
		
		
		int fbDocs = 20;
		if (config.get("fb-docs") != null) {
			fbDocs = Integer.parseInt(config.get("fb-docs"));
		}
		
		int fbTerms = 20;
		if (config.get("fb-terms") != null) {
			fbTerms = Integer.parseInt(config.get("fb-terms"));
		}
		
		Iterator<GQuery> queryIt = queries.iterator();
		int i = 1;
		while (queryIt.hasNext()) {
			GQuery query = queryIt.next();
			query.applyStopper(stopper);
			
			logger.info("Working on query "+query.getTitle()+". ("+i+++"/"+queries.numQueries()+")");

			if (query.getFeatureVector().getLength() == 0) {
				continue;
			}

			// RM built on target index
			FeedbackRelevanceModel rm = new FeedbackRelevanceModel();
			rm.setIndex(index);
			rm.setDocCount(fbDocs);
			rm.setTermCount(fbTerms);
			rm.setStopper(stopper);
			rm.setOriginalQuery(query);
			
			if (queryEntities != null) {
				SearchHits hits = qdocs.getEntitiesForQuery(query);
				hits.crop(fbDocs);
				Iterator<SearchHit> hitit = hits.iterator();
				while (hitit.hasNext()) {
					SearchHit hit = hitit.next();
					hit.setDocID(index.getDocId(hit.getDocno()));
				}
				rm.setRes(hits);
			}

			rm.build();

			FeatureVector rmVec = rm.asGquery().getFeatureVector();
			rmVec.normalize();
			
			FileWriter out = new FileWriter(outDir+"/"+query.getTitle());
			out.write(rmVec.toString(50));
			out.close();
		}
	}

}
