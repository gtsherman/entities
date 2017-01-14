package edu.gslis.utils.data.converters.main;

import java.io.File;
import java.sql.Connection;
import java.util.Arrays;

import edu.gslis.utils.config.Configuration;
import edu.gslis.utils.config.SimpleConfiguration;
import edu.gslis.utils.data.converters.FileToDatabaseConverter;
import edu.gslis.utils.data.interpreters.RelevanceModelDataInterpreter;
import edu.gslis.utils.data.sources.DatabaseDataSource;

public class ConvertQueryProbabilityFilesToDatabase {

	public static void main(String[] args) {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		String db = config.get("database");
		String whichCollection = config.get("expansion-collection");
		String dir = config.get("entity-probability-data-dir");
		
		String table = "expansion_probabilities_" + whichCollection;

		Connection con = DatabaseDataSource.getConnection(db);
		FileToDatabaseConverter converter = new FileToDatabaseConverter(con);

		File root = new File(dir);
		for (File query : root.listFiles()) {
			for (File doc : query.listFiles()) {
				System.err.println("Converting " + doc.getAbsolutePath());
				converter.convert(doc,
						table,
						Arrays.asList(RelevanceModelDataInterpreter.TERM_FIELD,
								RelevanceModelDataInterpreter.SCORE_FIELD,
								"QUERY",
								"DOCUMENT"),
						query.getName(),
						doc.getName());
			}
		}
	}

}
