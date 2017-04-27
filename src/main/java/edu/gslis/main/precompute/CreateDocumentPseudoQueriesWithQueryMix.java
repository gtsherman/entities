package edu.gslis.main.precompute;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.queries.GQueries;
import edu.gslis.queries.GQueriesFactory;
import edu.gslis.queries.GQuery;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;
import edu.gslis.utils.config.Configuration;
import edu.gslis.utils.config.SimpleConfiguration;

public class CreateDocumentPseudoQueriesWithQueryMix {
	
	public static void main(String[] args) throws IOException {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		IndexWrapperIndriImpl index = new IndexWrapperIndriImpl(config.get("index"));
		
		Stopper stopper = null;
		if (config.get("stoplist") != null)
			stopper = new Stopper(config.get("stoplist"));
		
		GQueries queries = GQueriesFactory.getGQueries(config.get("queries"));
		
		int documentTerms = 20;
		if (config.get("document-terms") != null) {
			documentTerms = Integer.parseInt(config.get("document-terms"));
		}

		for (GQuery query : queries) {
			query.applyStopper(stopper);
			query.getFeatureVector().normalize();
			
			FileWriter out = new FileWriter(query.getTitle(), true);
			
			SearchHits results = index.runQuery(query, 100);
		
			out.write("<parameters>\n");
			Iterator<SearchHit> docIt = results.iterator();
			while (docIt.hasNext()) {
				SearchHit doc = docIt.next();
				
				// Convert to query to facilitate applying stopper
				FeatureVector pseudoVector = FeatureVector.interpolate(doc.getFeatureVector(),
						query.getFeatureVector(), 0.5);
				GQuery pseudoQuery = new GQuery();
				pseudoQuery.setFeatureVector(pseudoVector);
				pseudoQuery.applyStopper(stopper);

				// Get the stopped vector and clip to desired length
				FeatureVector dv = pseudoQuery.getFeatureVector();
				dv.clip(documentTerms);
				dv.normalize();
				
				// Initial query output
				out.write("<query>\n");
				out.write("<number>" + doc.getDocno() + "</number>\n");
				out.write("<text>\n");
				
				// Add each term's weight
				String indriQuery = "#weight( ";
				Iterator<String> termit = dv.iterator();
				while (termit.hasNext()) {
					String term = termit.next();
					indriQuery += dv.getFeatureWeight(term) + " " + term + " ";
				}
				indriQuery += ")\n";
				out.write(indriQuery);
				
				// Finish the query output
				out.write("</text>\n");
				out.write("</query>\n");
			}
			out.write("</parameters>");
			out.close();
		}
	}

}
