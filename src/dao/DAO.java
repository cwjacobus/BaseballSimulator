package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import db.Team;

public class DAO {
	
	public static Connection conn; 
	
	public static void createBatchTeams(HashMap<String, Team> teamsMap) {
		try {
			conn.setAutoCommit(false);
			Statement stmt = conn.createStatement();
			int teamsCount = 0;
			for (Map.Entry<String, Team> entry : teamsMap.entrySet()) {
			    Team t = entry.getValue();
				String insertSQL = "INSERT IGNORE INTO MLB_TEAM (TEAM_ID, FULL_NAME, SHORT_NAME, LEAGUE, FIRST_YEAR_PLAYED, ACTIVE) VALUES (" + 
					t.getTeamId() + ", '" + t.getFullTeamName().replace("'", "") + "', '" + t.getShortTeamName() + "', '" + 
						t.getLeague() + "', " + t.getFirstYearPlayed() + ", " + t.isActive() + ");";
				stmt.addBatch(insertSQL);
				teamsCount++;
				// Every 500 lines, insert the records
				if (teamsCount % 250 == 0) {
					System.out.println("Insert picks " + (teamsCount - 250) + " : " + teamsCount);
					stmt.executeBatch();
					conn.commit();
					stmt.close();
					stmt = conn.createStatement();
				}
			}
			// Insert the remaining records
			System.out.println("Insert remaining picks " + (teamsCount - (teamsCount % 250)) + " : " + teamsCount);
			stmt.executeBatch();
			conn.commit();
			conn.setAutoCommit(true); // set auto commit back to true for next inserts
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static Map<Integer, Team> getTeamsMap() {
		Map<Integer, Team> teamsMap = new HashMap<Integer, Team>();
		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM MLB_TEAM ORDER BY FULL_NAME");
			Team team;
			while (rs.next()) {
				team = new Team(rs.getInt("TEAM_ID"), rs.getString("FULL_NAME"), rs.getString("SHORT_NAME"), rs.getString("LEAGUE"), rs.getInt("FIRST_YEAR_PLAYED"), rs.getBoolean("ACTIVE"));
				teamsMap.put(team.getTeamId(), team);
			}
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		return teamsMap;
	}
	
	public static void setConnection() {
		try {
            Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
        } 
		catch (Exception ex) {
        }
		try {
			String connString = "jdbc:mysql://localhost/mlb";
			connString += "?user=root&password=PASSWORD&useSSL=false&allowPublicKeyRetrieval=true";
			conn = DriverManager.getConnection(connString);
		}
		catch (SQLException ex) {
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
		}
	}
}
