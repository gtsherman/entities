package edu.gslis.main;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;

import edu.gslis.docscoring.support.CollectionStats;
import edu.gslis.docscoring.support.IndexBackedCollectionStats;
import edu.gslis.entities.docscoring.ScorerDirichlet;
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
		
		CollectionStats cs = new IndexBackedCollectionStats();
		cs.setStatSource(config.get("index"));
		
		double mu = 2500;
		if (config.get("mu") != null) {
			mu = Double.parseDouble(config.get("mu"));
		}

		ScorerDirichlet scorer = new ScorerDirichlet();
		scorer.setCollectionStats(cs);
		scorer.setParameter(scorer.PARAMETER_NAME, mu);

		Writer outputWriter = new BufferedWriter(new OutputStreamWriter(System.out));
		FormattedOutputTrecEval output = FormattedOutputTrecEval.getInstance("entities", outputWriter);
		
		Iterator<GQuery> queryIt = queries.iterator();
		while (queryIt.hasNext()) {
			GQuery query = queryIt.next();
			
			FeedbackRelevanceModel rm = new FeedbackRelevanceModel();
			rm.setIndex(index);
			rm.setDocCount(20);
			rm.setTermCount(20);
			rm.setStopper(stopper);
			rm.setOriginalQuery(query);
			
			rm.build();
			FeatureVector rmVec = rm.asGquery().getFeatureVector();
			rmVec.normalize();
			FeatureVector rm3 = FeatureVector.interpolate(rmVec, query.getFeatureVector(), 0.9);
			GQuery rmQuery = new GQuery();
			rmQuery.setFeatureVector(rm3);
			rmQuery.setTitle(query.getTitle());
			
			scorer.setQuery(rmQuery);
			
			SearchHits hits = index.runQuery(rmQuery, 1000);
			hits.rank();
			hits.crop(1000);
			output.write(hits, query.getTitle());
		}
		output.close();
	}

}
