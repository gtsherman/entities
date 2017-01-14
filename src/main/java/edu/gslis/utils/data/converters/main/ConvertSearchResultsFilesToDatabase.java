package edu.gslis.utils.data.converters.main;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import edu.gslis.utils.config.Configuration;
import edu.gslis.utils.config.SimpleConfiguration;
import edu.gslis.utils.data.converters.FileToDatabaseConverter;
import edu.gslis.utils.data.interpreters.SearchResultsDataInterpreter;

public class ConvertSearchResultsFilesToDatabase {

	public static void main(String[] args) {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		String file = config.get("initial-hits");
		String db = config.get("database");
		
		Connection con = null;
		try {
			con = DriverManager.getConnection("jdbc:sqlite://" + db);
		} catch (SQLException e) {
			System.err.println("Error connecting to database " + db);
			e.printStackTrace(System.err);
			System.exit(-1);
		}
		
		FileToDatabaseConverter converter = new FileToDatabaseConverter(con);
		converter.convert(new File(file),
				SearchResultsDataInterpreter.DATA_NAME,
				SearchResultsDataInterpreter.ALL_FIELDS);
	}

}
