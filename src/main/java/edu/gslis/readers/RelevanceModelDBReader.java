package edu.gslis.readers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RelevanceModelDBReader extends Reader {
	
	private Connection con;
	private String db;
	
	public RelevanceModelDBReader(Connection con, String db) {
		this.con = con;
		this.db = db;
	}
	
	
	public ResultSet read(String doc, int length) {
		return null;
	}

	@Override
	public ResultSet read(String query) {
		try {
			ResultSet results = getConnection().createStatement().executeQuery(query);
			return results;
		} catch (SQLException e) {
			System.err.println(e.getStackTrace());
			return null;
		}
	}

}
