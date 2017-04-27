package edu.gslis.main.analysis;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

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

public class ComputePseudoQueryTrueQueryKL {

	public static void main(String[] args) {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		IndexWrapper index = new IndexWrapperIndriImpl(config.get(Configuration.INDEX_PATH));
		GQueries queries = GQueriesFactory.getGQueries(config.get(Configuration.QUERIES_PATH));
		Stopper stopper = new Stopper(config.get(Configuration.STOPLIST_PATH));
		
		for (GQuery query : queries) {
			query.applyStopper(stopper);
			
			SearchHits results = index.runQuery(query, 10);
			for (SearchHit result : results) {
				GQuery pseudoQuery = new GQuery();
				pseudoQuery.setFeatureVector(result.getFeatureVector());
				pseudoQuery.applyStopper(stopper);
				pseudoQuery.getFeatureVector().clip(20);
				
				double num = 0.0;
				double denomX = 0.0;
				double denomY = 0.0;
				
				Set<String> vocab = new HashSet<String>();
				vocab.addAll(query.getFeatureVector().getFeatures());
				vocab.addAll(pseudoQuery.getFeatureVector().getFeatures());

				Iterator<String> termIt = vocab.iterator();
				String term;
				while (termIt.hasNext()) {
					term = termIt.next();
					
					double docWeight = query.getFeatureVector().getFeatureWeight(term);
					double otherdocWeight = pseudoQuery.getFeatureVector().getFeatureWeight(term); 
					
					num += docWeight * otherdocWeight;
					denomX += Math.pow(docWeight, 2);
					denomY += Math.pow(otherdocWeight, 2);
				}
				double denom = Math.sqrt(denomX)*Math.sqrt(denomY);
				if (denom == 0) denom = 1;  // should be unnecessary...
				double cosine = num/denom;

				System.out.println(StringUtils.join(new String[] {query.getTitle(), result.getDocno(), Double.toString(cosine)}, "\t"));
			}
		}

	}

}
