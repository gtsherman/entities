package edu.gslis.main;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.gslis.docscoring.support.CollectionStats;
import edu.gslis.docscoring.support.IndexBackedCollectionStats;
import edu.gslis.entities.docscoring.ScorerDirichlet;
import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.output.FormattedOutputTrecEval;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.queries.GQuery;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.utils.Configuration;
import edu.gslis.utils.SimpleConfiguration;
import edu.gslis.utils.Stopper;

public class RunBaselineRetrieval {
	
	static final Logger logger = LoggerFactory.getLogger(RunBaselineRetrieval.class);
	
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
		
		int numDocs = 1000;
		if (config.get("num-docs") != null) {
			numDocs = Integer.parseInt(config.get("num-docs"));
		}

		ScorerDirichlet scorer = new ScorerDirichlet();
		scorer.setCollectionStats(cs);
		scorer.setParameter(scorer.PARAMETER_NAME, 2500);

		Writer outputWriter = new BufferedWriter(new OutputStreamWriter(System.out));
		FormattedOutputTrecEval output = FormattedOutputTrecEval.getInstance("categories", outputWriter);
		
		Iterator<GQuery> queryIt = queries.iterator();
		int i = 0;
		while (queryIt.hasNext()) {
			GQuery query = queryIt.next();

			i++;
			logger.info("Working on query "+query.getTitle()+". ("+i+"/"+queries.numQueries()+")");

			if (stopper != null)
				query.applyStopper(stopper);
			scorer.setQuery(query);
			
			SearchHits hits = index.runQuery(query, numDocs);
			/*Iterator<SearchHit> hitIt = hits.iterator();
			while (hitIt.hasNext()) {
				SearchHit hit = hitIt.next();
				FeatureVector dv = index.getDocVector(hit.getDocID(), null);
				hit.setFeatureVector(dv);
				hit.setLength(dv.getLength());
				hit.setScore(scorer.score(hit));
			}
			hits.rank();*/
			output.write(hits, query.getTitle());
		}
	}

}
