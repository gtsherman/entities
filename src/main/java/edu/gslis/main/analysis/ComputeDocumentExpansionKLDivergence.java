package edu.gslis.main.analysis;

import java.io.File;
import java.util.Iterator;
import java.util.Map;

import edu.gslis.indexes.IndexWrapper;
import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.related_docs.DocumentClusterDataInterpreter;
import edu.gslis.related_docs.DocumentClusterDataSource;
import edu.gslis.related_docs.RelatedDocs;
import edu.gslis.searchhits.IndexBackedSearchHit;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.searchhits.SearchHitsBatch;
import edu.gslis.similarity.KLScorer;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.config.Configuration;
import edu.gslis.utils.config.SimpleConfiguration;
import edu.gslis.utils.data.factory.DataSourceFactory;
import edu.gslis.utils.data.interpreters.SearchResultsDataInterpreter;
import edu.gslis.utils.data.sources.DataSource;

public class ComputeDocumentExpansionKLDivergence {
	
	public static final String DELIMITER = "\t";

	public static void main(String[] args) {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		System.err.println("Loading indexes");
		IndexWrapper index = new IndexWrapperIndriImpl(config.get("index"));
		IndexWrapper wikiIndex = new IndexWrapperIndriImpl(config.get("wiki-index"));
		
		System.err.println("Loading initial results");
		DataSource data = DataSourceFactory.getDataSource(config.get("intial-hits"),
				config.get("database"), SearchResultsDataInterpreter.DATA_NAME);
		SearchResultsDataInterpreter dataInterpreter = new SearchResultsDataInterpreter(index);
		SearchHitsBatch initialHitsBatch = dataInterpreter.build(data);
		
		System.err.println("Loading clusters");
		DocumentClusterDataInterpreter clusterReader = new DocumentClusterDataInterpreter();
		RelatedDocs clusters = clusterReader.build(
				new DocumentClusterDataSource(new File(config.get("document-entities-file-wiki"))));
		
		System.err.println("Iterating queries");
		System.out.println("Query" + DELIMITER + "Document" + DELIMITER + "KL");
		Iterator<String> queryit = initialHitsBatch.queryIterator();
		while (queryit.hasNext()) {
			String query = queryit.next();

			System.err.println("Query: " + query);

			SearchHits queryResults = initialHitsBatch.getSearchHits(query);
			Iterator<SearchHit> hitit = queryResults.iterator();
			while (hitit.hasNext()) {
				SearchHit doc = new IndexBackedSearchHit(index, hitit.next());
				FeatureVector docVector = doc.getFeatureVector();
				docVector.clip(20);
				docVector.normalize();
				
				FeatureVector relatedDocsVector = new FeatureVector(null);
				Map<String, Double> relatedDocs = clusters.getDocsRelatedTo(doc);
				if (relatedDocs == null) {
					continue;
				}
				for (String relDoc : relatedDocs.keySet()) {
					SearchHit relHit = new IndexBackedSearchHit(wikiIndex);
					relHit.setDocno(relDoc);

					Iterator<String> termit = relHit.getFeatureVector().iterator();
					while (termit.hasNext()) {
						String term = termit.next();
						relatedDocsVector.addTerm(term, relHit.getFeatureVector().getFeatureWeight(term) * relatedDocs.get(relDoc));
					}
				}
				relatedDocsVector.clip(20);
				relatedDocsVector.normalize();
				
				System.out.println(query + DELIMITER + doc.getDocno() + DELIMITER + KLScorer.scoreKL(docVector, relatedDocsVector));
			}
		}
	}

}
