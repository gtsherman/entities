package edu.gslis.main.precompute;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.util.Iterator;

import edu.gslis.docscoring.support.CollectionStats;
import edu.gslis.docscoring.support.IndexBackedCollectionStats;
import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.queries.GQuery;
import edu.gslis.scoring.DirichletDocScorer;
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

public class PrecomputeDocumentProbabilities {
	
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
		
		Connection dbCon = DatabaseDataSource.getConnection(config.get("database"));

		DatabaseDataSource data = new DatabaseDataSource(dbCon, SearchResultsDataInterpreter.DATA_NAME);
		data.read();
		SearchResultsDataInterpreter dataInterpreter = new SearchResultsDataInterpreter(index);
		SearchHitsBatch initialHitsBatch = dataInterpreter.build(data);
		
		String outDir = config.get("document-probability-data-dir");
		
		Iterator<GQuery> queryIt = queries.iterator();
		while (queryIt.hasNext()) {
			GQuery query = queryIt.next();
			query.applyStopper(stopper);

			System.err.println("Query "+query.getTitle());
			
			File queryDir = new File(outDir + File.separator + query.getTitle());
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
				
				DocScorer docScorer = new DirichletDocScorer(cs);
				try {
					FileWriter out = new FileWriter(outDir+"/"+query.getTitle()+"/"+doc.getDocno());
					Iterator<String> qit = query.getFeatureVector().iterator();
					while (qit.hasNext()) {
						String term = qit.next();
						out.write(term + "\t" + docScorer.scoreTerm(term, doc) + "\n");
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
