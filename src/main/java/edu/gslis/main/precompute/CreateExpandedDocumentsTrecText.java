package edu.gslis.main.precompute;

import java.io.File;
import java.sql.Connection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.gslis.indexes.IndexWrapper;
import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.related_docs.DocumentClusterDataInterpreter;
import edu.gslis.related_docs.DocumentClusterDataSource;
import edu.gslis.related_docs.RelatedDocs;
import edu.gslis.searchhits.IndexBackedSearchHit;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.searchhits.SearchHitsBatch;
import edu.gslis.utils.config.Configuration;
import edu.gslis.utils.config.SimpleConfiguration;
import edu.gslis.utils.data.interpreters.SearchResultsDataInterpreter;
import edu.gslis.utils.data.sources.DatabaseDataSource;

public class CreateExpandedDocumentsTrecText {

	public static void main(String[] args) {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		IndexWrapper index = new IndexWrapperIndriImpl(config.get("index"));
		IndexWrapper wikiIndex = new IndexWrapperIndriImpl(config.get("wiki-index"));
		
		DocumentClusterDataInterpreter interpreter = new DocumentClusterDataInterpreter();
		DocumentClusterDataSource clusterDataSelf = new DocumentClusterDataSource(new File(config.get("document-entities-file-self")));
		DocumentClusterDataSource clusterDataWiki = new DocumentClusterDataSource(new File(config.get("document-entities-file-wiki")));
		RelatedDocs clustersSelf = interpreter.build(clusterDataSelf);
		RelatedDocs clustersWiki = interpreter.build(clusterDataWiki);
		
		Connection dbCon = DatabaseDataSource.getConnection(config.get("database"));
		
		DatabaseDataSource data = new DatabaseDataSource(dbCon, SearchResultsDataInterpreter.DATA_NAME);
		data.read();
		SearchResultsDataInterpreter dataInterpreter = new SearchResultsDataInterpreter(index);
		SearchHitsBatch initialHitsBatch = dataInterpreter.build(data);
		
		Set<String> seenDocs = new HashSet<String>();
		Iterator<SearchHits> searchHitsIt = initialHitsBatch.searchHitIterator();
		while (searchHitsIt.hasNext()) {
			SearchHits hits = searchHitsIt.next();
			for (SearchHit hit : hits) {
				if (seenDocs.contains(hit.getDocno())) {
					continue;
				}
				seenDocs.add(hit.getDocno());
				
				Set<String> relatedDocsSelf;
				try {
					relatedDocsSelf = clustersSelf.getDocsRelatedTo(hit).keySet();
				} catch (NullPointerException e) {
					relatedDocsSelf = new HashSet<String>();
				}
				Set<String> relatedDocsWiki;
				try {
					relatedDocsWiki = clustersWiki.getDocsRelatedTo(hit).keySet();
				} catch (NullPointerException e) {
					relatedDocsWiki = new HashSet<String>();
				}
				
				System.out.println("<DOC>");
				System.out.println("<DOCNO> " + hit.getDocno() + " </DOCNO>");
				System.out.println("<TEXT>");
				System.out.println("<original>");
				System.out.println(index.getDocText(hit.getDocID()));
				System.out.println("</original>");
				for (String relatedDoc : relatedDocsSelf) {
					System.out.println("<self>");
					SearchHit relatedHit = new IndexBackedSearchHit(index);
					relatedHit.setDocno(relatedDoc);
					System.out.println(index.getDocText(relatedHit.getDocID()));
					System.out.println("</self>");
				}
				for (String relatedDoc : relatedDocsWiki) {
					System.out.println("<wiki>");
					SearchHit relatedHit = new IndexBackedSearchHit(wikiIndex);
					relatedHit.setDocno(relatedDoc);
					System.out.println(wikiIndex.getDocText(relatedHit.getDocID()));
					System.out.println("</wiki>");
				}
				System.out.println("</TEXT>");
				System.out.println("</DOC>");
			}
		}
	}

}
