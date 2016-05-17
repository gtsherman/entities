package edu.gslis.main;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.gslis.patches.FormattedOutputTrecEval;
import edu.gslis.patches.IndexWrapperIndriImpl;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.queries.GQuery;
import edu.gslis.readers.RelevanceModelReader;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Configuration;
import edu.gslis.utils.SimpleConfiguration;
import edu.gslis.utils.Stopper;

public class RunSecondOrderRMSweep {

	static final Logger logger = LoggerFactory.getLogger(CreateDocumentClusters.class);

	public static void main(String[] args) throws IOException {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		IndexWrapperIndriImpl index = new IndexWrapperIndriImpl(config.get("index"));
		Stopper stopper = new Stopper(config.get("stoplist"));
		GQueriesJsonImpl queries = new GQueriesJsonImpl();
		queries.read(config.get("queries"));
		String outDir = config.get("out-dir");
		String sweepDir = config.get("sweep-dir");
		
		int numDocs = 1000;
		if (config.get("num-docs") != null) {
			numDocs = Integer.parseInt(config.get("num-docs"));
		}
		
		RelevanceModelReader rmReader = new RelevanceModelReader();
		
		FormattedOutputTrecEval output = new FormattedOutputTrecEval();
		output.setRunId("second-order-rm");

		Iterator<GQuery> queryIt = queries.iterator();
		int i = 0;
		while (queryIt.hasNext()) {
			GQuery query = queryIt.next();
			query.applyStopper(stopper);
			i++;
			logger.info("Working on query "+query.getTitle()+". ("+i+"/"+queries.numQueries()+")");
			FeatureVector queryVector = query.getFeatureVector();
			
			rmReader.readFileAbsolute(outDir+"/"+query.getTitle()+"_targetRM");
			FeatureVector targetRM = rmReader.getVector();
			
			rmReader.readFileAbsolute(outDir+"/"+query.getTitle()+"_0.5");
			FeatureVector wikiRM = rmReader.getVector();
			
			for (int origW = 0; origW <= 10; origW++) {
				double origWeight = origW / 10.0;
				for (int targetRMW = 0; targetRMW <= 10-origW; targetRMW++) {
					double targetRMWeight = targetRMW / 10.0;
					double wikiRMWeight = (10-(origW+targetRMW)) / 10.0;

					FeatureVector targetWiki = FeatureVector.interpolate(targetRM, wikiRM, targetRMWeight);
					FeatureVector origTargetWiki = FeatureVector.interpolate(queryVector, targetWiki, origWeight);
					
					GQuery newQuery = new GQuery();
					newQuery.setTitle(query.getTitle());
					newQuery.setFeatureVector(origTargetWiki);
					
					SearchHits results = index.runQuery(newQuery, numDocs);

					Writer outputWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(sweepDir+"/"+origWeight+"_"+targetRMWeight+"_"+wikiRMWeight, true)));
					output.setWriter(outputWriter);
					
					output.write(results, query.getTitle());
				}
			}
		}
	}

}
