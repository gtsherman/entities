package edu.gslis.main.precompute;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.gslis.docscoring.support.CollectionStats;
import edu.gslis.docscoring.support.IndexBackedCollectionStats;
import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.queries.GQuery;
import edu.gslis.readers.QueryDocsReader;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.utils.Stopper;
import edu.gslis.utils.config.Configuration;
import edu.gslis.utils.config.SimpleConfiguration;

public class PrecomputeDocumentProbabilities {
	
	public static void main(String[] args) {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		IndexWrapperIndriImpl index = new IndexWrapperIndriImpl(config.get("index"));
		
		Stopper stopper = null;
		if (config.get("stoplist") != null)
			stopper = new Stopper(config.get("stoplist"));
		
		GQueriesJsonImpl queries = new GQueriesJsonImpl();
		queries.read(config.get("rm-queries"));
		
		CollectionStats cs = new IndexBackedCollectionStats();
		cs.setStatSource(config.get("index"));
		
		QueryDocsReader qdocs = new QueryDocsReader();
		String baseDocs = config.get("base-docs");
		if (baseDocs != null) {
			qdocs.readFileAbsolute(baseDocs);
		}

		int numDocs = 1000;
		if (config.get("num-docs") != null) {
			numDocs = Integer.parseInt(config.get("num-docs"));
		}
		
		String outDir = config.get("document-probability-data-dir");
		
		Iterator<GQuery> queryIt = queries.iterator();
		int i = 0;
		while (queryIt.hasNext()) {
			GQuery query = queryIt.next();
			query.applyStopper(stopper);

			i++;
			System.err.println("Working on query "+query.getTitle()+". ("+i+"/"+queries.numQueries()+")");
			
			File queryDir = new File(outDir+"/"+query.getTitle());
			if (!queryDir.exists())
				queryDir.mkdirs();
			
			SearchHits initialHits;
			if (baseDocs == null) {
				initialHits = index.runQuery(query, numDocs);
			} else {
				initialHits = qdocs.getDocsForQuery(query);
			}

			if (initialHits == null) {
				System.err.println("No documents for "+query.getTitle());
				continue;
			}

			Iterator<SearchHit> hitIt = initialHits.iterator();
			while (hitIt.hasNext()) {
				SearchHit doc = hitIt.next();
				compute(doc, index, query, cs, outDir);
			}
		}
	}
	
	public static void compute(SearchHit doc, IndexWrapperIndriImpl index, GQuery query, CollectionStats cs, String outDir) {
		doc.setDocID(index.getDocId(doc.getDocno()));
		doc.setFeatureVector(index.getDocVector(doc.getDocID(), null));
		doc.setLength(index.getDocLength(doc.getDocID()));
		
		Map<String, Double> termProbs = new HashMap<String, Double>();
		Iterator<String> qit = query.getFeatureVector().iterator();
		while (qit.hasNext()) {
			String term = qit.next();
			double collectionScore = (1.0 + cs.termCount(term)) / cs.getTokCount();
			double mu = 2500;
			double qlscore = (doc.getFeatureVector().getFeatureWeight(term) + mu*collectionScore) / (doc.getLength() + mu);
			termProbs.put(term, qlscore);
		}
		
		try {
			FileWriter out = new FileWriter(outDir+"/"+query.getTitle()+"/"+doc.getDocno());
			for (String term : termProbs.keySet()) {
				out.write(term+"\t"+termProbs.get(term)+"\n");
			}
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}	
	}

}