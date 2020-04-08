package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
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
	
	public static void createBatchDataFromList(ArrayList<Object> mlbDataList) {
		try {
			conn.setAutoCommit(false);
			Statement stmt = conn.createStatement();
			int mlbDataCount = 0;
			for (Object mlbData : mlbDataList) {
				String insertSQL = "";
				if (mlbData instanceof MLBBattingStats) {
					MLBBattingStats mbs = (MLBBattingStats)mlbData;
					BattingStats bs = mbs.getBattingStats();
					insertSQL = "INSERT IGNORE INTO MLB_BATTING_STATS (MLB_PLAYER_ID, MLB_TEAM_ID, YEAR, AT_BATS, HITS, DOUBLES, TRIPLES, HOME_RUNS, WALKS, STRIKEOUTS, HIT_BY_PITCH, RUNS, RBIS, STOLEN_BASES, PLATE_APPEARANCES, CAUGHT_STEALING) VALUES (" +
						mbs.getMlbPlayerId() + ", " + mbs.getMlbTeamId() + ", " + mbs.getYear() + ", " + bs.getAtBats() + ", " + bs.getHits() + ", " + bs.getDoubles() + ", " + bs.getTriples() + ", " + bs.getHomeRuns() +
						", " + bs.getWalks() + ", " + bs.getStrikeOuts() + ", " + bs.getHitByPitch() + ", " + bs.getRuns() + ", " + bs.getRbis() + ", " + bs.getStolenBases() +
						", " + bs.getPlateAppearances() + ", " + bs.getCaughtStealing() +");";
				}
				stmt.addBatch(insertSQL);
				mlbDataCount++;
				// Every 500 lines, insert the records
				if (mlbDataCount % 250 == 0) {
					System.out.println("Insert " + (mlbDataCount - 250) + " : " + mlbDataCount);
					stmt.executeBatch();
					conn.commit();
					stmt.close();
					stmt = conn.createStatement();
				}
			}
			// Insert the remaining records
			System.out.println("Insert remaining " + (mlbDataCount - (mlbDataCount % 250)) + " : " + mlbDataCount);
			stmt.executeBatch();
			conn.commit();
			conn.setAutoCommit(true); // set auto commit back to true for next inserts
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static void createBatchDataFromMap(HashMap<?, ?> mlbDataMap) {
		try {
			conn.setAutoCommit(false);
			Statement stmt = conn.createStatement();
			int mlbDataCount = 0;
			for (Map.Entry<?, ?> entry : mlbDataMap.entrySet()) {
				String insertSQL = "";
				if (entry.getValue() instanceof MLBTeam) {
					MLBTeam t = (MLBTeam)entry.getValue();
					insertSQL = "INSERT IGNORE INTO MLB_TEAM (TEAM_ID, MLB_FRANCHISE_ID, FULL_NAME, SHORT_NAME, LEAGUE) VALUES (" + 
						t.getTeamId() + " ," + t.getMlbFranchiseId() + ", '" + t.getFullTeamName().replace("'", "") + "', '" + t.getShortTeamName() + "', '" + t.getLeague() + "');";
				}
				else if (entry.getValue() instanceof MLBFranchise) {
					MLBFranchise mlf = (MLBFranchise)entry.getValue();
					insertSQL = "INSERT IGNORE INTO MLB_FRANCHISE (MLB_FRANCHISE_ID, FULL_NAME, SHORT_NAME, FIRST_YEAR_PLAYED) VALUES (" + 
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
					insertSQL = "INSERT IGNORE INTO MLB_BATTING_STATS (MLB_PLAYER_ID, MLB_TEAM_ID, YEAR, AT_BATS, HITS, DOUBLES, TRIPLES, HOME_RUNS, WALKS, STRIKEOUTS, HIT_BY_PITCH, RUNS, RBIS, STOLEN_BASES, PLATE_APPEARANCES, CAUGHT_STEALING) VALUES (" +
						mbs.getMlbPlayerId() + ", " + mbs.getMlbTeamId() + ", " + mbs.getYear() + ", " + bs.getAtBats() + ", " + bs.getHits() + ", " + bs.getDoubles() + ", " + bs.getTriples() + ", " + bs.getHomeRuns() +
						", " + bs.getWalks() + ", " + bs.getStrikeOuts() + ", " + bs.getHitByPitch() + ", " + bs.getRuns() + ", " + bs.getRbis() + ", " + bs.getStolenBases() +
						", " + bs.getPlateAppearances() + ", " + bs.getCaughtStealing() +");";
				}
				stmt.addBatch(insertSQL);
				mlbDataCount++;
				// Every 500 lines, insert the records
				if (mlbDataCount % 250 == 0) {
					System.out.println("Insert " + (mlbDataCount - 250) + " : " + mlbDataCount);
					stmt.executeBatch();
					conn.commit();
					stmt.close();
					stmt = conn.createStatement();
				}
			}
			// Insert the remaining records
			System.out.println("Insert remaining " + (mlbDataCount - (mlbDataCount % 250)) + " : " + mlbDataCount);
			stmt.executeBatch();
			conn.commit();
			conn.setAutoCommit(true); // set auto commit back to true for next inserts
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static HashMap<Object, Object> getDataMap(String table) {
		return getDataMap(table, null, null);
	}
	
	public static HashMap<Object, Object> getDataMap(String table, Integer mlbTeamId, Integer year) {
		HashMap<Object, Object> dataMap = new HashMap<Object, Object>();
		try {
			Statement stmt = conn.createStatement();
			String sql = "SELECT * FROM " + table + ((mlbTeamId != null && year != null) ? " WHERE MLB_TEAM_ID = " +  mlbTeamId + " AND YEAR = " + year : "");
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				if (table.equals("MLB_FRANCHISE")) {
					dataMap.put(rs.getString("SHORT_NAME"), rs.getInt("MLB_FRANCHISE_ID"));
				}
				else if (table.equals("MLB_TEAM")) {
					MLBTeam team = new MLBTeam(rs.getInt("TEAM_ID"), rs.getInt("MLB_FRANCHISE_ID"), rs.getString("FULL_NAME"), rs.getString("SHORT_NAME"), rs.getString("LEAGUE"));
					dataMap.put(team.getTeamId(), team);
				}
				else if (table.equals("MLB_BATTING_STATS")) {
					MLBBattingStats bs = new MLBBattingStats(rs.getInt("MLB_PLAYER_ID"), rs.getInt("MLB_TEAM_ID"),  rs.getInt("YEAR"), new BattingStats(rs.getInt("AT_BATS"), rs.getInt("HITS"), rs.getInt("DOUBLES"), rs.getInt("TRIPLES"), 
						rs.getInt("HOME_RUNS"), rs.getInt("WALKS"), rs.getInt("STRIKEOUTS"), rs.getInt("HIT_BY_PITCH"), rs.getInt("RUNS"), rs.getInt("RBIS"), rs.getInt("STOLEN_BASES"), rs.getInt("PLATE_APPEARANCES"), rs.getInt("CAUGHT_STEALING")));
					dataMap.put(bs.getMlbPlayerId(), bs);
				}
			}
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		return dataMap;
	}
	
	public static MLBPlayer getMlbPlayerWithMostPlateAppearances(Integer teamId, Integer year, String position) {
		return getMlbPlayerWithMostPlateAppearances(teamId, year, position, null);
	}
	
	public static MLBPlayer getMlbPlayerWithMostPlateAppearances(Integer teamId, Integer year, ArrayList<Integer> excludingList) {
		return getMlbPlayerWithMostPlateAppearances(teamId, year, null, excludingList);
	}
	
	public static MLBPlayer getMlbPlayerWithMostPlateAppearances(Integer teamId, Integer year, String position, ArrayList<Integer> excludingList) {
		MLBPlayer player = new MLBPlayer();
		try {
			Statement stmt = conn.createStatement();
			String excludingSql = excludingList != null && excludingList.size() > 0 ? " and bs.mlb_player_id not in (" : "";
			if (excludingList != null && excludingList.size() > 0) {
				for (Integer id : excludingList) {
					excludingSql += id + ",";
				}
				excludingSql = excludingSql.substring(0, excludingSql.length() - 1) + ")";
			}
			// Add hits to sum for uniqueness
			String sql = "SELECT * FROM mlb_batting_stats bs, mlb_player p where bs.mlb_player_id=p.mlb_player_id and bs.mlb_team_id =  " + teamId;
			if (position != null) {
				sql += " and p.primary_position = '" + position + "'";
			}
			sql += " and bs.year = " + year + " and (bs.plate_appearances + bs.hits) = (select max(plate_appearances + hits) from mlb_batting_stats bs, mlb_player p where bs.mlb_player_id=p.mlb_player_id and  bs.mlb_team_id = " +teamId;
			if (position != null) {
				sql += " and p.primary_position = '" + position + "'";
			}
			sql += " and bs.year = " + year + excludingSql + ")";
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				player.setFullName(rs.getString("FULL_NAME"));
				player.setMlbPlayerId(rs.getInt("MLB_PLAYER_ID"));
			}
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		return player;
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
