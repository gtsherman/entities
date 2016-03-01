package edu.gslis.main;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.gslis.docscoring.support.CollectionStats;
import edu.gslis.docscoring.support.IndexBackedCollectionStats;
import edu.gslis.entities.utils.Configuration;
import edu.gslis.entities.utils.SimpleConfiguration;
import edu.gslis.patches.FormattedOutputTrecEval;
import edu.gslis.patches.IndexWrapperIndriImpl;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.queries.GQuery;
import edu.gslis.queries.expansion.FeedbackRelevanceModel;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;

public class RunKBRelevanceModel {
	
	final static Logger logger = LoggerFactory.getLogger(RunKBRelevanceModel.class);

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
		
		CollectionStats cs = new IndexBackedCollectionStats();
		cs.setStatSource(config.get("index"));

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

		Writer outputWriter = new BufferedWriter(new OutputStreamWriter(System.out));
		FormattedOutputTrecEval output = FormattedOutputTrecEval.getInstance("entities", outputWriter);
		
		Iterator<GQuery> queryIt = queries.iterator();
		int i = 0;
		while (queryIt.hasNext()) {
			GQuery query = queryIt.next();
			
			i++;
			logger.info("Working on query "+query.getTitle()+". ("+i+"/"+queries.numQueries()+")");
			
			FeedbackRelevanceModel rm = new FeedbackRelevanceModel();
			rm.setIndex(wikiIndex);
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
			
			
			SearchHits hits = index.runQuery(rmQuery, numDocs);
			hits.rank();
			hits.crop(1000);
			output.write(hits, query.getTitle());
		}
		output.close();
	}

}
