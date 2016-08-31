package edu.gslis.queryrunning;

import java.util.Iterator;

import edu.gslis.evaluation.SearchHitsBatch;
import edu.gslis.patches.IndexWrapperIndriImpl;
import edu.gslis.queries.GQuery;
import edu.gslis.readers.RelevanceModelReader;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;

public class RMRunner extends GenericRunner {

	public static final String ORIG_QUERY_WEIGHT = "original";
	public static final String TARGET_RM_WEIGHT = "target";
	
	private IndexWrapperIndriImpl index;
	private RelevanceModelReader rmReader;
	private Stopper stopper;
	
	public RMRunner(IndexWrapperIndriImpl index2, RelevanceModelReader rmReader, Stopper stopper) {
		this.index = index2;
		this.rmReader = rmReader;
		this.stopper = stopper;
	}

	public void run(int numResults) {
		batchResults = new SearchHitsBatch();
		Iterator<GQuery> queryIt = queries.iterator();
		while (queryIt.hasNext()) {
			GQuery query = queryIt.next();
			query.applyStopper(stopper);

			rmReader.readFileRelative(query.getTitle());
			FeatureVector targetRM = rmReader.getVector();
			
			FeatureVector rm3 = FeatureVector.interpolate(query.getFeatureVector(), targetRM, params.get(ORIG_QUERY_WEIGHT));
			
			GQuery newQuery = new GQuery();
			newQuery.setTitle(query.getTitle());
			newQuery.setFeatureVector(rm3);
			
			SearchHits results = index.runQuery(newQuery, numResults);
			batchResults.setSearchHits(query.getTitle(), results);
		}
	}

}
