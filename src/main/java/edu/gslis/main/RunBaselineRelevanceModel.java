package edu.gslis.main;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;

import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.output.FormattedOutputTrecEval;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.queries.GQuery;
import edu.gslis.queries.expansion.FeedbackRelevanceModel;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;
import edu.gslis.utils.config.Configuration;
import edu.gslis.utils.config.SimpleConfiguration;

public class RunBaselineRelevanceModel {
	
	public static void main(String[] args) {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		IndexWrapperIndriImpl index = new IndexWrapperIndriImpl(config.get("index"));
		
		Stopper stopper = null;
		if (config.get("stoplist") != null)
			stopper = new Stopper(config.get("stoplist"));
		
		GQueriesJsonImpl queries = new GQueriesJsonImpl();
		queries.read(config.get("queries"));
		
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
 		if (args.length > 1) {
 			origQueryWeight = Double.parseDouble(args[1]);
 		}
		
		Writer outputWriter = new BufferedWriter(new OutputStreamWriter(System.out));
		FormattedOutputTrecEval output = FormattedOutputTrecEval.getInstance("entities", outputWriter);
		
		Iterator<GQuery> queryIt = queries.iterator();
		int i = 0;
		while (queryIt.hasNext()) {
			GQuery query = queryIt.next();
			query.applyStopper(stopper);
			query.getFeatureVector().normalize();
			
			i++;
			System.err.println("Working on query "+query.getTitle()+". ("+i+"/"+queries.numQueries()+")");
			
			FeedbackRelevanceModel rm = new FeedbackRelevanceModel();
			rm.setIndex(index);
			rm.setDocCount(fbDocs);
			rm.setTermCount(fbTerms);
			rm.setStopper(stopper);
			rm.setOriginalQuery(query);
			
			rm.build();
			FeatureVector rmVec = rm.asGquery().getFeatureVector();
			rmVec.normalize();

			rmVec = FeatureVector.interpolate(query.getFeatureVector(), rmVec, origQueryWeight);
			GQuery rmQuery = new GQuery();
			rmQuery.setFeatureVector(rmVec);
			rmQuery.setTitle(query.getTitle());
			
			SearchHits hits = index.runQuery(rmQuery, numDocs);
			output.write(hits, query.getTitle());
		}
		output.close();
	}

}
