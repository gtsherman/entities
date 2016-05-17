package edu.gslis.queryrunning;

import java.util.Iterator;

import edu.gslis.evaluation.SearchHitsBatch;
import edu.gslis.patches.IndexWrapperIndriImpl;
import edu.gslis.queries.GQuery;
import edu.gslis.readers.RelevanceModelReader;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;

public class SecondOrderRMRunner extends GenericRunner {

	public static final String ORIG_QUERY_WEIGHT = "original";
	public static final String TARGET_RM_WEIGHT = "target";
	public static final String EXTERNAL_RM_WEIGHT = "external";
	
	private IndexWrapperIndriImpl index;
	private RelevanceModelReader rmReader;
	private Stopper stopper;
	
	public SecondOrderRMRunner(IndexWrapperIndriImpl index2, RelevanceModelReader rmReader, Stopper stopper) {
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

			rmReader.readFileRelative(query.getTitle()+"_targetRM");
			FeatureVector targetRM = rmReader.getVector();
			
			rmReader.readFileRelative(query.getTitle()+"_0.5");
			FeatureVector wikiRM = rmReader.getVector();

			FeatureVector targetWiki = FeatureVector.interpolate(targetRM, wikiRM, params.get(TARGET_RM_WEIGHT));
			FeatureVector origTargetWiki = FeatureVector.interpolate(query.getFeatureVector(), targetWiki, params.get(ORIG_QUERY_WEIGHT));
			
			GQuery newQuery = new GQuery();
			newQuery.setTitle(query.getTitle());
			newQuery.setFeatureVector(origTargetWiki);
			
			SearchHits results = index.runQuery(newQuery, numResults);
			batchResults.setSearchHits(query.getTitle(), results);
		}
	}

}
