package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import baseball.BattingStats;
import db.MLBBattingStats;
import db.MLBFranchise;
import db.MLBPlayer;
import db.MLBTeam;
import db.TeamPlayer;

public class DAO {
	
	public static Connection conn; 
	
	public static void createBatchDataFromMap(HashMap<?, ?> mlbDataMap) {
		try {
			conn.setAutoCommit(false);
			Statement stmt = conn.createStatement();
			int mlbFranchisesCount = 0;
			for (Map.Entry<?, ?> entry : mlbDataMap.entrySet()) {
				String insertSQL = "";
				if (entry.getValue() instanceof MLBTeam) {
					MLBTeam t = (MLBTeam)entry.getValue();
					insertSQL = "INSERT IGNORE INTO MLB_TEAM (TEAM_ID, MLB_FRANCHISE_ID, FULL_NAME, SHORT_NAME, LEAGUE) VALUES (" + 
						t.getTeamId() + " ," + t.getMlbFranchiseId() + ", '" + t.getFullTeamName().replace("'", "") + "', '" + t.getShortTeamName() + "', '" + t.getLeague() + "');";
				}
				else if (entry.getValue() instanceof MLBFranchise) {
					MLBFranchise mlf = (MLBFranchise)entry.getValue();
					insertSQL = "INSERT INTO MLB_FRANCHISE (MLB_FRANCHISE_ID, FULL_NAME, SHORT_NAME, FIRST_YEAR_PLAYED) VALUES (" + 
							mlf.getMlbFranchiseId() + ", '" + mlf.getFullTeamName().replace("'", "") + "', '" + mlf.getShortTeamName() + "', " + mlf.getFirstYearPlayed() + ");";
				}
				else if (entry.getValue() instanceof MLBPlayer) {
					MLBPlayer mlbPlayer = (MLBPlayer)entry.getValue();
					insertSQL = "INSERT IGNORE INTO MLB_PLAYER (MLB_PLAYER_ID, FULL_NAME, PRIMARY_POSITION, ARM_THROWS, BATS, JERSEY_NUMBER) VALUES (" + 
						mlbPlayer.getMlbPlayerId() + ", '" + mlbPlayer.getFullName().replace("'", "") + "', '" + mlbPlayer.getPrimaryPosition() +
						"', '" + mlbPlayer.getArmThrows() +"', '" + mlbPlayer.getBats() + "', " + mlbPlayer.getJerseyNumber() + ");";;
				}
				else if (entry.getValue() instanceof TeamPlayer) {
					TeamPlayer teamPlayer = (TeamPlayer)entry.getValue();
					insertSQL = "INSERT IGNORE INTO TEAM_PLAYER (TEAM_PLAYER_ID, MLB_TEAM_ID, MLB_PLAYER_ID, YEAR) VALUES (" + 
						teamPlayer.getTeamPlayerId() + ", " + teamPlayer.getMlbTeamId() + ", " + teamPlayer.getMlbPlayerId() +
						", " + teamPlayer.getYear() + ");";
				}
				else if (entry.getValue() instanceof MLBBattingStats) {
					MLBBattingStats mbs = (MLBBattingStats)entry.getValue();
					BattingStats bs = mbs.getBattingStats();
					insertSQL = "INSERT INTO MLB_BATTING_STATS (MLB_PLAYER_ID, MLB_TEAM_ID, YEAR, AT_BATS, HITS, DOUBLES, TRIPLES, HOME_RUNS, WALKS, STRIKEOUTS, HIT_BY_PITCH, RUNS, RBIS, STOLEN_BASES, PLATE_APPEARANCES, CAUGHT_STEALING) VALUES (" +
						mbs.getMlbPlayerId() + ", " + mbs.getMlbTeamId() + ", " + mbs.getYear() + ", " + bs.getAtBats() + ", " + bs.getHits() + ", " + bs.getDoubles() + ", " + bs.getTriples() + ", " + bs.getHomeRuns() +
						", " + bs.getWalks() + ", " + bs.getStrikeOuts() + ", " + bs.getHitByPitch() + ", " + bs.getRuns() + ", " + bs.getRbis() + ", " + bs.getStolenBases() +
						", " + bs.getPlateAppearances() + ", " + bs.getCaughtStealing() +");";
				}
				stmt.addBatch(insertSQL);
				mlbFranchisesCount++;
				// Every 500 lines, insert the records
				if (mlbFranchisesCount % 250 == 0) {
					System.out.println("Insert " + (mlbFranchisesCount - 250) + " : " + mlbFranchisesCount);
					stmt.executeBatch();
					conn.commit();
					stmt.close();
					stmt = conn.createStatement();
				}
			}
			// Insert the remaining records
			System.out.println("Insert remaining " + (mlbFranchisesCount - (mlbFranchisesCount % 250)) + " : " + mlbFranchisesCount);
			stmt.executeBatch();
			conn.commit();
			conn.setAutoCommit(true); // set auto commit back to true for next inserts
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static HashMap<Integer, MLBTeam> getTeamsMap() {
		HashMap<Integer, MLBTeam> teamsMap = new HashMap<Integer, MLBTeam>();
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
	
	public static HashMap<Integer, TeamPlayer> getTeamPlayersMap() {
		HashMap<Integer, TeamPlayer> teamPlayersMap = new HashMap<Integer, TeamPlayer>();
		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM TEAM_PLAYER");
			TeamPlayer teamPlayer;
			while (rs.next()) {
				teamPlayer = new TeamPlayer(rs.getInt("TEAM_PLAYER_ID"), rs.getInt("MLB_TEAM_ID"), rs.getInt("MLB_PLAYER_ID"), rs.getInt("YEAR"));
				teamPlayersMap.put(teamPlayer.getTeamPlayerId(), teamPlayer);
			}
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		return teamPlayersMap;
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
