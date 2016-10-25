package edu.gslis.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;

import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.output.FormattedOutputTrecEval;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.queries.GQuery;
import edu.gslis.queries.expansion.FeedbackRelevanceModel;
import edu.gslis.readers.RelevanceModelReader;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;
import edu.gslis.utils.config.Configuration;
import edu.gslis.utils.config.SimpleConfiguration;

public class RunRMRetrieval {

	public static void main(String[] args) {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		IndexWrapperIndriImpl index = new IndexWrapperIndriImpl(config.get("index"));
		index.setTimeFieldName(null);
		Stopper stopper = new Stopper(config.get("stoplist"));
		GQueriesJsonImpl queries = new GQueriesJsonImpl();
		queries.read(config.get("queries"));
		
		RelevanceModelReader rmReader = new RelevanceModelReader();
		String rmDocsDir = null;
		if (config.get("rm-docs-dir") != null) {
			rmDocsDir = config.get("rm-docs-dir");
		}
		String rmDocsDir2 = null;
		if (config.get("rm-docs-dir2") != null) {
			rmDocsDir2 = config.get("rm-docs-dir2");
		}
		
		int fbDocs = 20;
		if (config.get("fb-docs") != null) {
			fbDocs = Integer.parseInt(config.get("fb-docs"));
		}
		
		int fbTerms = 20;
		if (config.get("fb-terms") != null) {
			fbTerms = Integer.parseInt(config.get("fb-terms"));
		}

		Double origQueryWeight = null;
		if (config.get("original-query-weight") != null) {
			origQueryWeight = Double.parseDouble(config.get("original-query-weight"));
		}
		
		double rmCombine = 0.5;
		if (config.get("rm-combination-weight") != null) {
			rmCombine = Double.parseDouble(config.get("rm-combination-weight"));
		} else if (args.length > 1) {
			rmCombine = Double.parseDouble(args[1]);
		}
		
		Writer outputWriter = new BufferedWriter(new OutputStreamWriter(System.out));
		FormattedOutputTrecEval output = FormattedOutputTrecEval.getInstance("rm1", outputWriter);

		Iterator<GQuery> queryIt = queries.iterator();
		int i = 1;
		while (queryIt.hasNext()) {
			GQuery query = queryIt.next();
			query.applyStopper(stopper);
			query.getFeatureVector().normalize();
			
			System.err.println("Working on query "+query.getTitle()+". ("+i+++"/"+queries.numQueries()+")");

			if (query.getFeatureVector().getLength() == 0) {
				continue;
			}

			FeatureVector rmVec;
			if (rmDocsDir != null) {
				rmReader.readFile(new File(rmDocsDir+"/"+query.getTitle()));
				rmVec = rmReader.getVector();
				if (rmDocsDir2 != null) {
					int rmVecLength = rmVec.getFeatureCount();
					rmReader.readFile(new File(rmDocsDir2+"/"+query.getTitle()));
					rmVec = FeatureVector.interpolate(rmVec, rmReader.getVector(), rmCombine);
					rmVec.clip(rmVecLength);
				}
			} else {
				FeedbackRelevanceModel rm = new FeedbackRelevanceModel();
				rm.setIndex(index);
				rm.setDocCount(fbDocs);
				rm.setTermCount(fbTerms);
				rm.setStopper(stopper);
				rm.setOriginalQuery(query);
				rm.build();
				rmVec = rm.asGquery().getFeatureVector();
				rmVec.normalize();
			}
			
			if (origQueryWeight != null) {
				rmVec = FeatureVector.interpolate(query.getFeatureVector(), rmVec, origQueryWeight);
			}
			
			GQuery newQuery = new GQuery();
			newQuery.setFeatureVector(rmVec);
			newQuery.setTitle(query.getTitle());
			newQuery.setText(query.getText());

			SearchHits results = index.runQuery(newQuery, 1000);
			output.write(results, newQuery.getTitle());
		}
		output.close();
	}

}
