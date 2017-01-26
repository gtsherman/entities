package edu.gslis.utils.data.converters;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import edu.gslis.utils.data.sources.FileDataSource;

public class FileToDatabaseConverter {
	
	private Connection connection;

	public FileToDatabaseConverter(Connection connection) {
		this.connection = connection;
	}
	
	public void convert(File file, String table, List<String> fields, String... extraTuples) {
		FileDataSource ds = new FileDataSource(file);
		
		createTableIfNecessary(table, fields);
		
		for (String[] tuple : ds) {
			String dbQuery = "INSERT INTO " + table + " values('" +
					StringUtils.join(tuple, "', '");
			if (extraTuples.length > 0) {
					dbQuery += "', '" + StringUtils.join(extraTuples, "', '");
			}
			dbQuery += "');";

			Statement statement = null;
			try {
				connection.setAutoCommit(false);
				statement = connection.createStatement();
				statement.executeUpdate(dbQuery);
				statement.close();
			} catch (SQLException e) {
				System.err.println("Error inserting table " + table);
				System.err.println("Faulty query: " + dbQuery);
				e.printStackTrace(System.err);
			}
		}
		try {
			connection.setAutoCommit(true);
		} catch (SQLException e) {
			e.printStackTrace(System.err);
		}
	}
	
	public void createTableIfNecessary(String table, List<String> fields) {
		String dbQuery = "CREATE TABLE IF NOT EXISTS " + table + "(" +
				StringUtils.join(fields, " VARCHAR(250), ") + ");";

		Statement statement = null;
		try {
			statement = connection.createStatement();
			statement.executeUpdate(dbQuery);
			statement.close();
		} catch (SQLException e) {
			System.err.println("Error inserting table " + table);
			System.err.println("Faulty query: " + dbQuery);
			System.err.println(e.getStackTrace());
		}
	}

}
