package edu.gslis.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.output.FormattedOutputTrecEval;
import edu.gslis.queries.GQueriesJsonImpl;
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
		
		IndexWrapperIndriImpl wikiIndex = null;
		if (config.get("wiki-index") != null)
			wikiIndex = new IndexWrapperIndriImpl(config.get("wiki-index"));
		
		Stopper stopper = null;
		if (config.get("stoplist") != null)
			stopper = new Stopper(config.get("stoplist"));
		
		GQueriesJsonImpl queries = new GQueriesJsonImpl();
		queries.read(config.get("queries"));	
		
		int documentTerms = 20;
		if (config.get("document-terms") != null) {
			documentTerms = Integer.parseInt(config.get("document-terms"));
		}

		Writer outputWriter = new BufferedWriter(new OutputStreamWriter(System.out));
		
		Set<String> docs = new HashSet<String>();
		String documentsFile = config.get("documents-file");
		try {
			Scanner scanner = new Scanner(new File(documentsFile));
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				docs.add(line.trim());
			}
			scanner.close();
		} catch (Exception e) {
			System.err.println("Error reading file: "+documentsFile+". This file is required. Exiting...");
			System.exit(-1);
		} 
		
		Iterator<String> docIt = docs.iterator();
		while (docIt.hasNext()) {
			String doc = docIt.next();
			
			FeatureVector dv = index.getDocVector(doc, stopper);
			dv.clip(documentTerms);
			
			GQuery docAsQuery = new GQuery();
			docAsQuery.setFeatureVector(dv);
			docAsQuery.setTitle(doc);
			docAsQuery.applyStopper(stopper);  // shouldn't be necessary, but just in case
			
			SearchHits entities = wikiIndex.runQuery(docAsQuery, 100);

			FormattedOutputTrecEval output = FormattedOutputTrecEval.getInstance("entities", outputWriter);
			output.write(entities, docAsQuery.getTitle());
		}
	}

}
