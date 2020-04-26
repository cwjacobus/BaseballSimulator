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
import baseball.PitchingStats;
import db.MLBBattingStats;
import db.MLBFranchise;
import db.MLBPitchingStats;
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
				else if (mlbData instanceof MLBPitchingStats) {
					MLBPitchingStats mps = (MLBPitchingStats)mlbData;
					PitchingStats ps = mps.getPitchingStats();
					insertSQL = "INSERT IGNORE INTO MLB_PITCHING_STATS (MLB_PLAYER_ID, MLB_TEAM_ID, YEAR, INNINGS_PITCHED, WALKS, STRIKEOUTS, RUNS_ALLOWED, EARNED_RUNS_ALLOWED, HOME_RUNS_ALLOWED, STOLEN_BASES_ALLOWED, HIT_BATTERS, HITS_ALLOWED, HOLDS, SAVES, GAMES_STARTED, BALKS, WILD_PITCHES, SAC_FLIES, BATTERS_FACED) VALUES (" +
						mps.getMlbPlayerId() + ", " + mps.getMlbTeamId() + ", " + mps.getYear() + ", " + ps.getInningsPitched() + ", " + ps.getWalks() + ", " + ps.getStrikeouts() + ", " + ps.getRunsAllowed() + ", " + ps.getEarnedRunsAllowed() +
						", " + ps.getHomeRunsAllowed() + ", " + ps.getStolenBasesAllowed() + ", " + ps.getHitBatters() + ", " + ps.getHitsAllowed() + ", " + ps.getHolds() + ", " + ps.getSaves() +
						", " + ps.getGamesStarted() + ", " + ps.getBalks() + ", " + ps.getWildPitches() + ", " + ps.getSacrificeFlies() + ", " + ps.getBattersFaced() + ");";
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
						", " + bs.getPlateAppearances() + ", " + bs.getCaughtStealing() + ");";
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
		return getDataMap(table, null, null, null);
	}
	
	public static HashMap<Object, Object> getDataMap(String table, Integer mlbTeamId, Integer year) {
		return getDataMap(table, mlbTeamId, year, null);
	}
	
	public static HashMap<Object, Object> getDataMap(String table, Integer mlbTeamId, Integer year, Boolean pitchers) {
		HashMap<Object, Object> dataMap = new HashMap<Object, Object>();
		try {
			Statement stmt = conn.createStatement();
			String sql = "";
			if (pitchers == null) {
				 sql = "SELECT * FROM " + table + ((mlbTeamId != null && year != null) ? " WHERE MLB_TEAM_ID = " +  mlbTeamId + " AND YEAR = " + year : "");
			}
			else if (pitchers.booleanValue()) {
				sql = "SELECT P.*, PS.* from MLB_PITCHING_STATS PS, MLB_PLAYER P WHERE PS.MLB_PLAYER_ID = P.MLB_PLAYER_ID AND YEAR =" + year + " AND MLB_TEAM_ID = " + mlbTeamId;
			}
			else {
				sql = "SELECT P.*, BS.* from MLB_BATTING_STATS BS, MLB_PLAYER P WHERE BS.MLB_PLAYER_ID = P.MLB_PLAYER_ID AND YEAR =" + year + " AND MLB_TEAM_ID = " + mlbTeamId;
			}
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				if (table.equals("MLB_FRANCHISE")) {
					dataMap.put(rs.getString("SHORT_NAME"), rs.getInt("MLB_FRANCHISE_ID"));
				}
				else if (table.equals("MLB_TEAM")) {
					MLBTeam team = new MLBTeam(rs.getInt("TEAM_ID"), rs.getInt("MLB_FRANCHISE_ID"), rs.getString("FULL_NAME"), rs.getString("SHORT_NAME"), rs.getString("LEAGUE"));
					dataMap.put(team.getTeamId(), team);
				}
				/*else if (table.equals("MLB_BATTING_STATS")) {
					MLBBattingStats bs = new MLBBattingStats(rs.getInt("MLB_PLAYER_ID"), rs.getInt("MLB_TEAM_ID"),  rs.getInt("YEAR"), new BattingStats(rs.getInt("AT_BATS"), rs.getInt("HITS"), rs.getInt("DOUBLES"), rs.getInt("TRIPLES"), 
						rs.getInt("HOME_RUNS"), rs.getInt("WALKS"), rs.getInt("STRIKEOUTS"), rs.getInt("HIT_BY_PITCH"), rs.getInt("RUNS"), rs.getInt("RBIS"), rs.getInt("STOLEN_BASES"), rs.getInt("PLATE_APPEARANCES"), rs.getInt("CAUGHT_STEALING")));
					dataMap.put(bs.getMlbPlayerId(), bs);
				}
				else if (table.equals("MLB_PITCHING_STATS")) {
					MLBPitchingStats ps = new MLBPitchingStats(rs.getInt("MLB_PLAYER_ID"), rs.getInt("MLB_TEAM_ID"),  rs.getInt("YEAR"), new PitchingStats(rs.getDouble("INNINGS_PITCHED"), rs.getInt("RUNS_ALLOWED"), rs.getInt("EARNED_RUNS_ALLOWED"), 
						rs.getInt("WALKS"), rs.getInt("STRIKEOUTS"),  rs.getInt("HOME_RUNS_ALLOWED"), rs.getInt("STOLEN_BASES_ALLOWED"), rs.getInt("HIT_BATTERS"), rs.getInt("HITS_ALLOWED"), rs.getInt("HOLDS"), rs.getInt("SAVES"), rs.getInt("GAMES_STARTED"), 
						rs.getInt("BALKS"), rs.getInt("WILD_PITCHES"), rs.getInt("SAC_FLIES"), rs.getInt("BATTERS_FACED")));
					dataMap.put(ps.getMlbPlayerId(), ps);
				}*/
				else if (table.equals("MLB_PLAYER")) {
					MLBPlayer p = new MLBPlayer(rs.getInt("MLB_PLAYER_ID"), rs.getString("FULL_NAME"),  rs.getString("PRIMARY_POSITION"), rs.getString("ARM_THROWS"), rs.getString("BATS"), rs.getInt("JERSEY_NUMBER"));
					if (pitchers != null && !pitchers.booleanValue()) {
						p.setMlbBattingStats(new MLBBattingStats(rs.getInt("MLB_PLAYER_ID"), rs.getInt("MLB_TEAM_ID"),  rs.getInt("YEAR"), new BattingStats(rs.getInt("AT_BATS"), rs.getInt("HITS"), rs.getInt("DOUBLES"), rs.getInt("TRIPLES"), 
							rs.getInt("HOME_RUNS"), rs.getInt("WALKS"), rs.getInt("STRIKEOUTS"), rs.getInt("HIT_BY_PITCH"), rs.getInt("RUNS"), rs.getInt("RBIS"), rs.getInt("STOLEN_BASES"), rs.getInt("PLATE_APPEARANCES"), rs.getInt("CAUGHT_STEALING"))));
					}
					else {
						p.setMlbPitchingStats(new MLBPitchingStats(rs.getInt("MLB_PLAYER_ID"), rs.getInt("MLB_TEAM_ID"),  rs.getInt("YEAR"), new PitchingStats(rs.getDouble("INNINGS_PITCHED"), rs.getInt("EARNED_RUNS_ALLOWED"), rs.getInt("RUNS_ALLOWED"), 
							rs.getInt("WALKS"), rs.getInt("STRIKEOUTS"),  rs.getInt("HOME_RUNS_ALLOWED"), rs.getInt("STOLEN_BASES_ALLOWED"), rs.getInt("HIT_BATTERS"), rs.getInt("HITS_ALLOWED"), rs.getInt("HOLDS"), rs.getInt("SAVES"), rs.getInt("GAMES_STARTED"), 
							rs.getInt("BALKS"), rs.getInt("WILD_PITCHES"), rs.getInt("SAC_FLIES"), rs.getInt("BATTERS_FACED"))));
					}
					dataMap.put(p.getMlbPlayerId(), p);
				}
			}
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		return dataMap; 
	}
	
	public static HashMap<Integer, MLBPlayer> getBattersMapByTeamAndYear(Integer mlbTeamId, Integer year) {
		HashMap<Integer, MLBPlayer> battersMap = new HashMap<Integer, MLBPlayer>();
		HashMap<Object, Object> objectMap = getDataMap("MLB_PLAYER", mlbTeamId, year, false);
		for (Map.Entry<Object, Object> entry : objectMap.entrySet()) {
			battersMap.put((Integer)entry.getKey(), (MLBPlayer)entry.getValue());
		}
		return battersMap;
	}
	
	public static HashMap<Integer, MLBPlayer> getPitchersMapByTeamAndYear(Integer mlbTeamId, Integer year) {
		HashMap<Integer, MLBPlayer> pitchersMap = new HashMap<Integer, MLBPlayer>();
		HashMap<Object, Object> objectMap = getDataMap("MLB_PLAYER", mlbTeamId, year, true);
		for (Map.Entry<Object, Object> entry : objectMap.entrySet()) {
			pitchersMap.put((Integer)entry.getKey(), (MLBPlayer)entry.getValue());
		}
		return pitchersMap;
	}
	
	public static MLBPlayer getReliefPitcher(Integer mlbTeamId, Integer year, HashMap<Integer, MLBPlayer> excludingPitchers, boolean closer) {
		// For getting a random starting pitcher
		ArrayList<MLBPlayer> pitcherList = new ArrayList<MLBPlayer>();
		try {
			String excludingSql = excludingPitchers != null && excludingPitchers.size() > 0 ? " AND BS.MLB_PLAYER_ID NOT IN (" : "";
			if (excludingPitchers != null && excludingPitchers.size() > 0) {
				for (Map.Entry<Integer, MLBPlayer> entry : excludingPitchers.entrySet()) {
					excludingSql += entry.getValue().getMlbPlayerId() + ",";
				}
				excludingSql = excludingSql.substring(0, excludingSql.length() - 1) + ")";
			}
			String orderBy = closer ? "SAVES" : "HOLDS";
			Statement stmt = conn.createStatement();
			String sql = "SELECT P.* from MLB_PITCHING_STATS BS, MLB_PLAYER P WHERE BS.MLB_PLAYER_ID = P.MLB_PLAYER_ID AND YEAR = " + year + " AND MLB_TEAM_ID = " + 
				mlbTeamId + excludingSql + " ORDER BY " + orderBy + " DESC";
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				MLBPlayer p = new MLBPlayer(rs.getInt("MLB_PLAYER_ID"), rs.getString("FULL_NAME"),  rs.getString("PRIMARY_POSITION"), rs.getString("ARM_THROWS"), rs.getString("BATS"), rs.getInt("JERSEY_NUMBER"));
				pitcherList.add(p);
			}
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		if (pitcherList.size() > 0) {
			return pitcherList.get(0);
		}
		else {
			return null;
		}
	}
	
	public static MLBPlayer getStartingPitcherByIndex(Integer mlbTeamId, Integer year, int index) {
		// For getting a random starting pitcher
		ArrayList<MLBPlayer> pitcherList = new ArrayList<MLBPlayer>();
		try {
			Statement stmt = conn.createStatement();
			String sql = "SELECT P.* from MLB_PITCHING_STATS BS, MLB_PLAYER P WHERE BS.MLB_PLAYER_ID = P.MLB_PLAYER_ID AND GAMES_STARTED > 3 AND YEAR = " + year + 
				" AND MLB_TEAM_ID = " + mlbTeamId + " ORDER BY INNINGS_PITCHED DESC;";
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				MLBPlayer p = new MLBPlayer(rs.getInt("MLB_PLAYER_ID"), rs.getString("FULL_NAME"),  rs.getString("PRIMARY_POSITION"), rs.getString("ARM_THROWS"), rs.getString("BATS"), rs.getInt("JERSEY_NUMBER"));
				pitcherList.add(p);
			}
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		index = ((index - 1) >= pitcherList.size()) ? 1 : index;
		return pitcherList.get(index-1);
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
			String sql = "SELECT * FROM MLB_BATTING_STATS BS, MLB_PLAYER P WHERE BS.MLB_PLAYER_ID=P.MLB_PLAYER_ID AND BS.MLB_TEAM_ID =  " + teamId;
			if (position != null) {
				sql += " AND P.PRIMARY_POSITION = '" + position + "'";
			}
			sql += " and bs.year = " + year + " aND (BS.PLATE_APPEARANCES + BS.HITS) = (SELECT MAX(PLATE_APPEARANCES + HITS) FROM MLB_BATTING_STATS BS, MLB_PLAYER P WHERE BS.MLB_PLAYER_ID=P.MLB_PLAYER_ID AND BS.MLB_TEAM_ID = " +teamId;
			if (position != null) {
				sql += " AND P.PRIMARY_POSITION = '" + position + "'";
			}
			sql += " and bs.year = " + year + excludingSql + ")";
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				player.setFullName(rs.getString("FULL_NAME"));
				player.setMlbPlayerId(rs.getInt("MLB_PLAYER_ID"));
				player.setPrimaryPosition(rs.getString("PRIMARY_POSITION"));
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
