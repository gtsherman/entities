package edu.gslis.main.precompute;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.util.Iterator;

import edu.gslis.docscoring.support.CollectionStats;
import edu.gslis.docscoring.support.IndexBackedCollectionStats;
import edu.gslis.entities.docscoring.ExpansionDocsDocScorer;
import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.queries.GQuery;
import edu.gslis.related_docs.DocumentClusterDataInterpreter;
import edu.gslis.related_docs.DocumentClusterDataSource;
import edu.gslis.related_docs.RelatedDocs;
import edu.gslis.scoring.DocScorer;
import edu.gslis.searchhits.IndexBackedSearchHit;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.searchhits.SearchHitsBatch;
import edu.gslis.utils.Stopper;
import edu.gslis.utils.config.Configuration;
import edu.gslis.utils.config.SimpleConfiguration;
import edu.gslis.utils.data.interpreters.SearchResultsDataInterpreter;
import edu.gslis.utils.data.sources.DatabaseDataSource;

public class PrecomputeClusterTermProbabilities {
	
	public static void main(String[] args) {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		IndexWrapperIndriImpl index = new IndexWrapperIndriImpl(config.get("index"));
		IndexWrapperIndriImpl wikiIndex = null;
		if (config.get("wiki-index") != null)
			wikiIndex = new IndexWrapperIndriImpl(config.get("wiki-index"));
		
		Connection dbCon = DatabaseDataSource.getConnection(config.get("database"));

		DatabaseDataSource data = new DatabaseDataSource(dbCon, SearchResultsDataInterpreter.DATA_NAME);
		data.read();
		SearchResultsDataInterpreter dataInterpreter = new SearchResultsDataInterpreter(index);
		SearchHitsBatch initialHitsBatch = dataInterpreter.build(data);
		
		Stopper stopper = null;
		if (config.get("stoplist") != null)
			stopper = new Stopper(config.get("stoplist"));
		
		GQueriesJsonImpl queries = new GQueriesJsonImpl();
		queries.read(config.get("queries"));
		
		int numEntities = Integer.parseInt(args[1]);
		DocumentClusterDataInterpreter clustersReader = new DocumentClusterDataInterpreter();
		RelatedDocs clusters = clustersReader.build(
				new DocumentClusterDataSource(new File(config.get("document-entities-file")), numEntities));
		
		CollectionStats cs = new IndexBackedCollectionStats();
		cs.setStatSource(config.get("index"));
		
		String dataDir = config.get("entity-probability-data-dir");
		
		Iterator<GQuery> queryIt = queries.iterator();
		while (queryIt.hasNext()) {
			GQuery query = queryIt.next();
			query.applyStopper(stopper);

			System.err.println("Calculating query " + query.getTitle());
			
			String outDir = dataDir + "." + numEntities + File.separator + query.getTitle();
			File queryDir = new File(outDir);
			if (!queryDir.exists())
				queryDir.mkdirs();
			
			SearchHits initialHits = initialHitsBatch.getSearchHits(query);
			if (initialHits == null) {
				System.err.println("No documents for "+query.getTitle());
				continue;
			}

			Iterator<SearchHit> hitIt = initialHits.iterator();
			while (hitIt.hasNext()) {
				SearchHit doc = new IndexBackedSearchHit(index, hitIt.next());
				
				DocScorer expansionBatchScorer = new ExpansionDocsDocScorer(wikiIndex, clusters);
				try {
					FileWriter out = new FileWriter(outDir + File.separator + doc.getDocno());
					Iterator<String> qtermIt = query.getFeatureVector().iterator();
					while (qtermIt.hasNext()) {
						String term = qtermIt.next();
						out.write(term + "\t" + expansionBatchScorer.scoreTerm(term, doc) + "\n");
					}
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(-1);
				}
			}
		}
	}
}
