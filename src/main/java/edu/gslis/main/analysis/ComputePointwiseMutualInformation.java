package edu.gslis.main.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import edu.gslis.indexes.IndexWrapper;
import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.queries.GQueries;
import edu.gslis.queries.GQueriesFactory;
import edu.gslis.queries.GQuery;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.utils.Stopper;
import edu.gslis.utils.config.Configuration;
import edu.gslis.utils.config.SimpleConfiguration;
import lemurproject.indri.QueryEnvironment;

public class ComputePointwiseMutualInformation {
	
	public static void main(String[] args) throws Exception {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		IndexWrapper index = new IndexWrapperIndriImpl(config.get(Configuration.INDEX_PATH));
		IndexWrapper wikiIndex = new IndexWrapperIndriImpl(config.get("wiki-index"));
		QueryEnvironment indriWikiIndex = (QueryEnvironment) wikiIndex.getActualIndex();
		GQueries queries = GQueriesFactory.getGQueries(config.get(Configuration.QUERIES_PATH));
		Stopper stopper = new Stopper(config.get(Configuration.STOPLIST_PATH));
		
		Map<String, Double> seenTerms = new HashMap<String, Double>();
		Map<String, Map<String, Double>> pmis = new HashMap<String, Map<String, Double>>();
		for (GQuery query : queries) {
			query.applyStopper(stopper);
			
			SearchHits results = index.runQuery(query, 10);
			for (SearchHit result : results) {
				GQuery pseudoQuery = new GQuery();
				pseudoQuery.setFeatureVector(result.getFeatureVector());
				pseudoQuery.applyStopper(stopper);
				pseudoQuery.getFeatureVector().clip(20);
				
				List<String> terms = new ArrayList<String>(pseudoQuery.getFeatureVector().getFeatures());
				for (int i = 0; i < terms.size(); i++) {
					String term1temp = terms.get(i);
					for (int j = i + 1; j < terms.size(); j++) {
						String term2temp = terms.get(j);
						
						String[] pair = new String[] { term1temp, term2temp };
						Arrays.sort(pair);
						
						String term1 = pair[0];
						String term2 = pair[1];
						
						if (!pmis.containsKey(term1)) {
							pmis.put(term1, new HashMap<String, Double>());
						}

						if (!pmis.get(term1).containsKey(term2)) {
							double coocur = indriWikiIndex.expressionCount("#uw20( " + term1 + " " + term2 + " )");
							pmis.get(term1).put(term2, coocur + 1);
						}

						if (!seenTerms.containsKey(term1)) {
							double t1 = indriWikiIndex.expressionCount(term1);
							seenTerms.put(term1, t1 + 1);
						}

						if (!seenTerms.containsKey(term2)) {
							double t2 = indriWikiIndex.expressionCount(term2);
							seenTerms.put(term2, t2 + 1);
						}
						
						double numDocs = wikiIndex.docCount();
						double jointProb = pmis.get(term1).get(term2) / numDocs;
						double t1Prob = seenTerms.get(term1) / numDocs;
						double t2Prob = seenTerms.get(term2) / numDocs;
						double pmi = Math.log(jointProb / (t1Prob * t2Prob));
						
						System.out.println(StringUtils.join(new String[] {
								query.getTitle(), result.getDocno(), term1, term2, Double.toString(pmi) }, "\t"));
					}
				}
			}
		}
	}

}
