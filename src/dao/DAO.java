package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import db.MLBFranchise;
import db.MLBTeam;

public class DAO {
	
	public static Connection conn; 
	
	public static void createBatchTeams(HashMap<String, MLBTeam> teamsMap) {
		try {
			conn.setAutoCommit(false);
			Statement stmt = conn.createStatement();
			int teamsCount = 0;
			for (Map.Entry<String, MLBTeam> entry : teamsMap.entrySet()) {
			    MLBTeam t = entry.getValue();
				String insertSQL = "INSERT INTO MLB_TEAM (TEAM_ID, MLB_FRANCHISE_ID, FULL_NAME, SHORT_NAME, LEAGUE) VALUES (" + 
					t.getTeamId() + " ," + t.getMlbFranchiseId() + ", '" + t.getFullTeamName().replace("'", "") + "', '" + t.getShortTeamName() + "', '" + t.getLeague() + "');";
				stmt.addBatch(insertSQL);
				teamsCount++;
				// Every 500 lines, insert the records
				if (teamsCount % 250 == 0) {
					System.out.println("Insert teams " + (teamsCount - 250) + " : " + teamsCount);
					stmt.executeBatch();
					conn.commit();
					stmt.close();
					stmt = conn.createStatement();
				}
			}
			// Insert the remaining records
			System.out.println("Insert remaining teams " + (teamsCount - (teamsCount % 250)) + " : " + teamsCount);
			stmt.executeBatch();
			conn.commit();
			conn.setAutoCommit(true); // set auto commit back to true for next inserts
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static void createBatchMlbFranchises(HashMap<Integer, MLBFranchise> mlbFranchisesMap) {
		try {
			conn.setAutoCommit(false);
			Statement stmt = conn.createStatement();
			int mlbFranchisesCount = 0;
			for (Map.Entry<Integer, MLBFranchise> entry : mlbFranchisesMap.entrySet()) {
				MLBFranchise mlf = entry.getValue();
			    String insertSQL = "INSERT INTO MLB_FRANCHISE (MLB_FRANCHISE_ID, FULL_NAME, SHORT_NAME, FIRST_YEAR_PLAYED) VALUES (" + 
					mlf.getMlbFranchiseId() + ", '" + mlf.getFullTeamName().replace("'", "") + "', '" + mlf.getShortTeamName() + "', " + mlf.getFirstYearPlayed() + ");";
				stmt.addBatch(insertSQL);
				mlbFranchisesCount++;
				// Every 500 lines, insert the records
				if (mlbFranchisesCount % 250 == 0) {
					System.out.println("Insert mlb franchises " + (mlbFranchisesCount - 250) + " : " + mlbFranchisesCount);
					stmt.executeBatch();
					conn.commit();
					stmt.close();
					stmt = conn.createStatement();
				}
			}
			// Insert the remaining records
			System.out.println("Insert remaining mlb franchises " + (mlbFranchisesCount - (mlbFranchisesCount % 250)) + " : " + mlbFranchisesCount);
			stmt.executeBatch();
			conn.commit();
			conn.setAutoCommit(true); // set auto commit back to true for next inserts
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static Map<Integer, MLBTeam> getTeamsMap() {
		Map<Integer, MLBTeam> teamsMap = new HashMap<Integer, MLBTeam>();
		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM MLB_TEAM ORDER BY FULL_NAME");
			MLBTeam team;
			while (rs.next()) {
				team = new MLBTeam(rs.getInt("TEAM_ID"), rs.getInt("MLB_FRANCHISE_ID"), rs.getString("FULL_NAME"), rs.getString("SHORT_NAME"), rs.getString("LEAGUE"));
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
