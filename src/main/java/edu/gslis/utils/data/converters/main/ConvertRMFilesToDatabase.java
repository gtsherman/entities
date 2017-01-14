package edu.gslis.utils.data.converters.main;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;

import edu.gslis.utils.config.Configuration;
import edu.gslis.utils.config.SimpleConfiguration;
import edu.gslis.utils.data.converters.FileToDatabaseConverter;
import edu.gslis.utils.data.interpreters.RelevanceModelDataInterpreter;

public class ConvertRMFilesToDatabase {

	public static void main(String[] args) {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		String db = config.get("database");
		String whichCollection = config.get("expansion-collection");
		String rmsDir = config.get("expansion-rms-dir") + File.separator + whichCollection;
		
		Connection con = null;
		try {
			con = DriverManager.getConnection("jdbc:sqlite://" + db);
		} catch (SQLException e) {
			System.err.println("Error connecting to database " + db);
			e.printStackTrace(System.err);
			System.exit(-1);
		}
		
		FileToDatabaseConverter converter = new FileToDatabaseConverter(con);
		File rmRoot = new File(rmsDir);
		String table = "expansion_rms_" + whichCollection;
		for (File rm : rmRoot.listFiles()) {
			System.err.println("Converting " + rm.getName() + " to table " + table);
			converter.convert(rm,
					table,
					Arrays.asList(RelevanceModelDataInterpreter.SCORE_FIELD,
							RelevanceModelDataInterpreter.TERM_FIELD,
							"ORIGINAL_DOCUMENT"),
					rm.getName());
		}
	}

}
