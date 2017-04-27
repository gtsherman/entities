package edu.gslis.main.precompute;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import edu.gslis.indexes.IndexWrapper;
import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.output.FormattedOutputTrecEval;
import edu.gslis.queries.GQueriesIndriImpl;
import edu.gslis.queries.GQuery;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;
import edu.gslis.utils.config.Configuration;
import edu.gslis.utils.config.SimpleConfiguration;

public class CreateDocumentClusters {

	public static void main(String[] args) {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		IndexWrapperIndriImpl index = new IndexWrapperIndriImpl(config.get("index"));
		IndexWrapperIndriImpl wikiIndex = new IndexWrapperIndriImpl(config.get("wiki-index"));
		
		Stopper stopper = new Stopper(config.get("stoplist"));
		
		GQueriesIndriImpl queries = new GQueriesIndriImpl();
		queries.read(config.get("queries"));	
		
		int documentTerms = config.get("document-terms") == null ? 20 : Integer.parseInt(config.get("document-terms"));

		Writer outputWriter = new BufferedWriter(new OutputStreamWriter(System.out));
		
		List<GQuery> queryList = new ArrayList<GQuery>();
		queries.forEach(query -> queryList.add(query));
		
		Set<String> docnos = queryList
				.stream()
				.parallel()
				.map(query -> index.runQuery(query, 100))
				.flatMap(results -> results.hits().stream().map(hit -> hit.getDocno()))
				.collect(Collectors.toSet());
		
		docnos.stream()
			.parallel()
			.forEach(docno -> foo(docno, index, wikiIndex, stopper, documentTerms, outputWriter));

	}

	public static void foo(String docno, IndexWrapper index,
			IndexWrapper wikiIndex, Stopper stopper,
			int documentTerms, Writer outputWriter) {
		FeatureVector doc = index.getDocVector(docno, stopper);
		doc.clip(documentTerms);
		
		GQuery pseudoQuery = new GQuery();
		pseudoQuery.setFeatureVector(doc);
		pseudoQuery.setTitle(docno);
		
		SearchHits expansionDocs = wikiIndex.runQuery(pseudoQuery, 100);

		FormattedOutputTrecEval output = FormattedOutputTrecEval.getInstance("expansionDocs", outputWriter);
		output.write(expansionDocs, pseudoQuery.getTitle());
	}

}
