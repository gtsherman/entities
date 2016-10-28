package edu.gslis.main.precompute;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import edu.gslis.docscoring.support.CollectionStats;
import edu.gslis.docscoring.support.IndexBackedCollectionStats;
import edu.gslis.entities.docscoring.ScorerDirichletEntityInterpolated;
import edu.gslis.entities.docscoring.support.EntityExpectedProbability;
import edu.gslis.entities.docscoring.support.EntityProbability;
import edu.gslis.entities.docscoring.support.EntityPseudoDocumentProbability;
import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.queries.GQueries;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.queries.GQuery;
import edu.gslis.readers.DocumentEntityReader;
import edu.gslis.readers.QueryDocsReader;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.utils.Stopper;
import edu.gslis.utils.config.Configuration;
import edu.gslis.utils.config.SimpleConfiguration;

public class PrecomputeEntityProbabilities {
	
	public static void main(String[] args) {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		IndexWrapperIndriImpl index = new IndexWrapperIndriImpl(config.get("index"));
		IndexWrapperIndriImpl wikiIndex = null;
		if (config.get("wiki-index") != null)
			wikiIndex = new IndexWrapperIndriImpl(config.get("wiki-index"));
		
		Stopper stopper = null;
		if (config.get("stoplist") != null)
			stopper = new Stopper(config.get("stoplist"));
		
		GQueriesJsonImpl queries = new GQueriesJsonImpl();
		queries.read(config.get("queries"));
		
		GQueriesJsonImpl rmQueries = new GQueriesJsonImpl();
		rmQueries.read(config.get("rm-queries"));
		
		DocumentEntityReader de = new DocumentEntityReader();
		int numEntities = Integer.parseInt(args[1]);
		de.setLimit(numEntities);
		de.readFileAbsolute(config.get("document-entities-file"));
		
		QueryDocsReader qdocs = new QueryDocsReader();
		String baseDocs = config.get("base-docs");
		if (baseDocs != null) {
			qdocs.readFileAbsolute(baseDocs);
		}
		
		
		EntityProbability cp = new EntityExpectedProbability(de, wikiIndex, stopper);

		CollectionStats cs = new IndexBackedCollectionStats();
		cs.setStatSource(config.get("index"));
		
		if (cp instanceof EntityPseudoDocumentProbability) {
			((EntityPseudoDocumentProbability) cp).setCollectionStats(cs);
		}
		if (cp instanceof EntityExpectedProbability) {
			((EntityExpectedProbability) cp).setCollectionStats(cs);
		}
		
		int numDocs = 1000;
		if (config.get("num-docs") != null) {
			numDocs = Integer.parseInt(config.get("num-docs"));
		}
		
		String dataDir = config.get("entity-probability-data-dir");
		
		Iterator<GQuery> queryIt = queries.iterator();
		int i = 0;
		while (queryIt.hasNext()) {
			GQuery query = queryIt.next();
			query.applyStopper(stopper);

			i++;
			System.err.println("Working on query "+query.getTitle()+". ("+i+"/"+queries.numQueries()+")");
			
			File queryDir = new File(dataDir+"."+numEntities+"/"+query.getTitle());
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
				compute(doc, index, rmQueries, query, cp, dataDir, numEntities);
			}
		}
	}
	
	public static void compute(SearchHit doc, IndexWrapperIndriImpl index, GQueries queries, GQuery query, EntityProbability cp, String dataDir, int numEntities) {
		doc.setDocID(index.getDocId(doc.getDocno()));
		
		Map<String, Double> termProbs = ScorerDirichletEntityInterpolated.getTermProbs(doc, queries.getNamedQuery(query.getTitle()), cp);
		try {
			FileWriter out = new FileWriter(dataDir+"."+numEntities+"/"+query.getTitle()+"/"+doc.getDocno());
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
