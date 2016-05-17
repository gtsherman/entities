package edu.gslis.main;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.gslis.docscoring.support.CollectionStats;
import edu.gslis.docscoring.support.IndexBackedCollectionStats;
import edu.gslis.patches.IndexWrapperIndriImpl;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.queries.GQuery;
import edu.gslis.queries.expansion.FeedbackRelevanceModel;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Configuration;
import edu.gslis.utils.SimpleConfiguration;
import edu.gslis.utils.Stopper;

public class CreateSecondOrderRMs {

	static final Logger logger = LoggerFactory.getLogger(CreateDocumentClusters.class);

	public static void main(String[] args) throws IOException {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		IndexWrapperIndriImpl index = new IndexWrapperIndriImpl(config.get("index"));
		IndexWrapperIndriImpl wikiIndex = new IndexWrapperIndriImpl(config.get("wiki-index"));
		CollectionStats cs = new IndexBackedCollectionStats();
		cs.setStatSource(config.get("index"));
		Stopper stopper = new Stopper(config.get("stoplist"));
		GQueriesJsonImpl queries = new GQueriesJsonImpl();
		queries.read(config.get("queries"));
		String outDir = config.get("out-dir");
		
		int fbDocs = 20;
		if (config.get("fb-docs") != null) {
			fbDocs = Integer.parseInt(config.get("fb-docs"));
		}
		
		int fbTerms = 20;
		if (config.get("fb-terms") != null) {
			fbTerms = Integer.parseInt(config.get("fb-terms"));
		}
		
		Iterator<GQuery> queryIt = queries.iterator();
		int i = 0;
		while (queryIt.hasNext()) {
			GQuery query = queryIt.next();
			query.applyStopper(stopper);
			
			logger.info("Working on query "+query.getTitle()+". ("+i+++"/"+queries.numQueries()+")");

			// RM built on target index
			FeedbackRelevanceModel rm = new FeedbackRelevanceModel();
			rm.setIndex(index);
			rm.setDocCount(fbDocs);
			rm.setTermCount(fbTerms);
			rm.setStopper(stopper);
			rm.setOriginalQuery(query);
			rm.build();
			FeatureVector rmVec = rm.asGquery().getFeatureVector();
			rmVec.normalize();
			
			FileWriter out = new FileWriter(outDir+"/"+query.getTitle()+"_targetRM");
			out.write(rmVec.toString(50));
			out.close();

			// Make it an RM3
			for (int j = 0; j <= 10; j++) {
				double origQueryWeight = j / 10.0;
				out = new FileWriter(outDir+"/"+query.getTitle()+"_"+origQueryWeight);
				
				FeatureVector rm3Vec = FeatureVector.interpolate(query.getFeatureVector(), rmVec, origQueryWeight);
				GQuery rmQuery = new GQuery();
				rmQuery.setFeatureVector(rm3Vec);
				rmQuery.setTitle(query.getTitle());

				// RM built on wiki index
				FeedbackRelevanceModel rm3 = new FeedbackRelevanceModel();
				rm3.setIndex(wikiIndex);
				rm3.setDocCount(fbDocs);
				rm3.setTermCount(fbTerms);
				rm3.setStopper(stopper);
				rm3.setOriginalQuery(rmQuery);
				rm3.build();
				FeatureVector rm3WikiVec = rm3.asGquery().getFeatureVector();
				rm3WikiVec.normalize();
				
				out.write(rm3WikiVec.toString(50));
				out.close();
			}
		}
	}

}
