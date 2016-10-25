package edu.gslis.main;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.queries.GQuery;
import edu.gslis.queries.expansion.FeedbackRelevanceModel;
import edu.gslis.readers.DocumentEntityReader;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;
import edu.gslis.utils.config.Configuration;
import edu.gslis.utils.config.SimpleConfiguration;

public class PrecomputeExpansionRMs {
	
	static final Logger logger = LoggerFactory.getLogger(PrecomputeExpansionRMs.class);

	public static void main(String[] args) throws IOException {
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
		
		int numEntities = 10;
		if (config.get("num-entities") != null) {
			numEntities = Integer.parseInt(config.get("num-entities"));
		}
		
		DocumentEntityReader deSelf = new DocumentEntityReader();
		deSelf.setLimit(numEntities);
		deSelf.readFileAbsolute(config.get("document-entities-file-self"));

		DocumentEntityReader deWiki = new DocumentEntityReader();
		deWiki.setLimit(numEntities);
		deWiki.readFileAbsolute(config.get("document-entities-file-wiki"));
		
		String outDir = config.get("expansion-rms-dir");
		
		Iterator<GQuery> queryIt = queries.iterator();
		while (queryIt.hasNext()) {
			GQuery query = queryIt.next();
			query.applyStopper(stopper);

			compute(query, index, wikiIndex, stopper, deSelf, deWiki, outDir);
		}
	}
	
	public static void compute(GQuery query,
			IndexWrapperIndriImpl index,
			IndexWrapperIndriImpl wikiIndex,
			Stopper stopper,
			DocumentEntityReader deSelf,
			DocumentEntityReader deWiki,
			String outDir) throws IOException {

		SearchHits initialHits = index.runQuery(query, 100);
		
		Iterator<SearchHit> hitIt = initialHits.iterator();
		while (hitIt.hasNext()) {
			SearchHit hit = hitIt.next();
			
			compute(hit, index, wikiIndex, stopper, deSelf, deWiki, outDir);
		}
	}
	
	public static void compute(SearchHit hit,
			IndexWrapperIndriImpl index,
			IndexWrapperIndriImpl wikiIndex,
			Stopper stopper,
			DocumentEntityReader deSelf,
			DocumentEntityReader deWiki,
			String outDir) throws IOException {

		File hitDir = new File(outDir+File.separator+hit.getDocno());
		if (!hitDir.exists()) {
			hitDir.mkdirs();
		} else {
			System.err.println("Seen "+hit.getDocno()+". Skipping.");
			return; // we've already expanded this doc
		}
		
		GQuery hitQuery = new GQuery();
		hitQuery.setTitle(hit.getDocno());
		hitQuery.setText(hit.getDocno());

		// Build an RM on the expansion documents
		FeedbackRelevanceModel rm = new FeedbackRelevanceModel();
		rm.setDocCount(20);
		rm.setTermCount(Integer.MAX_VALUE);
		rm.setIndex(index);
		rm.setRes(deSelf.getEntitiesAsSearchHits(hit.getDocno(), index));
		rm.setStopper(stopper);
		rm.setOriginalQuery(hitQuery);
		rm.build();
		FeatureVector rmVec = rm.asFeatureVector();
		rmVec.normalize();
		
		File selfOut = new File(outDir+File.separator+hit.getDocno()+File.separator+"self");
		FileUtils.write(selfOut, rmVec.toString());
		
		// Build an RM on the expansion documents
		rm = new FeedbackRelevanceModel();
		rm.setDocCount(20);
		rm.setTermCount(Integer.MAX_VALUE);
		rm.setIndex(wikiIndex);
		rm.setRes(deWiki.getEntitiesAsSearchHits(hit.getDocno(), wikiIndex));
		rm.setStopper(stopper);
		rm.setOriginalQuery(hitQuery);
		rm.build();
		rmVec = rm.asFeatureVector();
		rmVec.normalize();
		
		File wikiOut = new File(outDir+File.separator+hit.getDocno()+File.separator+"wiki");
		FileUtils.write(wikiOut, rmVec.toString());
	}

}
