package edu.gslis.main;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;

import edu.gslis.patches.FormattedOutputTrecEval;
import edu.gslis.patches.IndexWrapperIndriImpl;
import edu.gslis.queries.GQueries;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.queries.GQuery;
import edu.gslis.queries.expansion.FeedbackRelevanceModel;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.similarity.KLScorer;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;
import edu.gslis.utils.config.Configuration;
import edu.gslis.utils.config.SimpleConfiguration;

public class RunQueryColKLRMs {

	public static void main(String[] args) throws FileNotFoundException {

		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		IndexWrapperIndriImpl index = new IndexWrapperIndriImpl(config.get("index"));
		index.setTimeFieldName(null);
		IndexWrapperIndriImpl wikiIndex = new IndexWrapperIndriImpl(config.get("wiki-index"));
		wikiIndex.setTimeFieldName(null);
		
		// load queries
		GQueries queries = new GQueriesJsonImpl();
		queries.read(config.get("queries"));
		
		// load stopper
		Stopper stopper = new Stopper(config.get("stoplist"));
		
		// out location
		String outDir = config.get("clarity-dir");
		
		Iterator<GQuery> queryIt = queries.iterator();
		while (queryIt.hasNext()) {
			GQuery query = queryIt.next();
			
			FeedbackRelevanceModel rm = new FeedbackRelevanceModel();
			rm.setDocCount(20);
			rm.setTermCount(20);
			rm.setStopper(stopper);
			rm.setIndex(index);
			rm.setOriginalQuery(query);
			rm.build();
			FeatureVector rmVec = rm.asGquery().getFeatureVector();
			rmVec.normalize();

			double origWeight = KLScorer.scoreKL(query.getFeatureVector(), index);
			
			rm = new FeedbackRelevanceModel();
			rm.setDocCount(20);
			rm.setTermCount(20);
			rm.setStopper(stopper);
			rm.setIndex(wikiIndex);
			rm.setOriginalQuery(query);
			rm.build();
			FeatureVector rmVecWiki = rm.asGquery().getFeatureVector();
			rmVec.normalize();
			
			double wikiWeight = KLScorer.scoreKL(rmVecWiki, wikiIndex);
			
			origWeight /= (origWeight + wikiWeight);
			
			FeatureVector combined = FeatureVector.interpolate(rmVec, rmVecWiki, origWeight);
			
			for (int i = 0; i <= 10; i++) {
				double qWeight = i / 10.0;
				FeatureVector rm3 = FeatureVector.interpolate(query.getFeatureVector(), combined, qWeight);
				GQuery rm3Q = new GQuery();
				rm3Q.setFeatureVector(rm3);
				rm3Q.setTitle(query.getTitle());

				System.err.println(rm3Q.toString());
				SearchHits results = index.runQuery(rm3Q, 1000);
				
				Writer outputWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outDir+"/"+qWeight, true)));
				FormattedOutputTrecEval output = FormattedOutputTrecEval.getInstance("entities", outputWriter);
				
				output.write(results, rm3Q.getTitle());
				
				output.close();
			}
		}
	}
	
}
