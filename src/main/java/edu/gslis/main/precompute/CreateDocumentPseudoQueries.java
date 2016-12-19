package edu.gslis.main.precompute;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.Sets;

import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.queries.GQuery;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;
import edu.gslis.utils.config.Configuration;
import edu.gslis.utils.config.SimpleConfiguration;

public class CreateDocumentPseudoQueries {
	
	public static void main(String[] args) {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		IndexWrapperIndriImpl index = new IndexWrapperIndriImpl(config.get("index"));
		
		Stopper stopper = null;
		if (config.get("stoplist") != null)
			stopper = new Stopper(config.get("stoplist"));
		
		GQueriesJsonImpl queries = new GQueriesJsonImpl();
		queries.read(config.get("queries"));	
		
		int documentTerms = 20;
		if (config.get("document-terms") != null) {
			documentTerms = Integer.parseInt(config.get("document-terms"));
		}

		// Read initial hits and create a set of unique documents (no sense repeating a document we've done before)
		/*SearchHitsBatch initialHits = (new SearchResultsReader(new File(config.get("initial-hits")), index)).getBatchResults(); 
		Set<SearchHit> docs = new HashSet<SearchHit>();
		Iterator<SearchHits> hitsIt = initialHits.searchHitIterator();
		while (hitsIt.hasNext()) {
			Set<SearchHit> hitSet = Sets.newHashSet(hitsIt.next().hits());
			docs.addAll(hitSet);
		}*/
		Set<SearchHit> docs = new HashSet<SearchHit>();
		for (GQuery query : queries) {
			SearchHits results = index.runQuery(query, 1000);
			docs.addAll(Sets.newHashSet(results.iterator()));
		}
		
		System.out.println("<parameters>");
		Iterator<SearchHit> docIt = docs.iterator();
		while (docIt.hasNext()) {
			SearchHit doc = docIt.next();
			
			// Convert to query to facilitate applying stopper
			GQuery query = new GQuery();
			query.setFeatureVector(doc.getFeatureVector());
			query.applyStopper(stopper);

			// Get the stopped vector and clip to desired length
			FeatureVector dv = query.getFeatureVector();
			dv.clip(documentTerms);
			
			// Initial query output
			System.out.println("<query>");
			System.out.println("<number>" + doc.getDocno() + "</number>");
			System.out.println("<text>");
			
			// Add each term's weight
			String indriQuery = "#weight( ";
			Iterator<String> termit = dv.iterator();
			while (termit.hasNext()) {
				String term = termit.next();
				indriQuery += dv.getFeatureWeight(term) + " " + term + " ";
			}
			indriQuery += ")";
			System.out.println(indriQuery);
			
			// Finish the query output
			System.out.println("</text>");
			System.out.println("</query>");
		}
		System.out.println("</parameters>");
	}

}
