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
import baseball.FieldingStats;
import baseball.PitchingStats;
import db.MLBBattingStats;
import db.MLBFieldingStats;
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
					insertSQL = "INSERT IGNORE INTO MLB_PITCHING_STATS (MLB_PLAYER_ID, MLB_TEAM_ID, YEAR, INNINGS_PITCHED, WALKS, STRIKEOUTS, RUNS_ALLOWED, EARNED_RUNS_ALLOWED, HOME_RUNS_ALLOWED, STOLEN_BASES_ALLOWED, HIT_BATTERS, HITS_ALLOWED, HOLDS, SAVES, BLOWN_SAVES, GAMES_STARTED, BALKS, WILD_PITCHES, SAC_FLIES, BATTERS_FACED, WINS, LOSSES) VALUES (" +
						mps.getMlbPlayerId() + ", " + mps.getMlbTeamId() + ", " + mps.getYear() + ", " + ps.getInningsPitched() + ", " + ps.getWalks() + ", " + ps.getStrikeouts() + ", " + ps.getRunsAllowed() + ", " + ps.getEarnedRunsAllowed() +
						", " + ps.getHomeRunsAllowed() + ", " + ps.getStolenBasesAllowed() + ", " + ps.getHitBatters() + ", " + ps.getHitsAllowed() + ", " + ps.getHolds() + ", " + ps.getSaves() + ", " + ps.getBlownSaves() +
						", " + ps.getGamesStarted() + ", " + ps.getBalks() + ", " + ps.getWildPitches() + ", " + ps.getSacrificeFlies() + ", " + ps.getBattersFaced() + ", " + ps.getWins() + ", " + ps.getLosses() + ");";
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
	/*
	public static void createBatchStatsDataFromPlayersMaps(HashMap<Integer, MLBPlayer> battersMap, HashMap<Integer, MLBPlayer> pitchersMap, HashMap<Integer, MLBPlayer> fieldersMap) {
		HashMap<Integer, MLBBattingStats> mlbBattingStatsMap = new HashMap<Integer, MLBBattingStats>();
		HashMap<Integer, MLBPitchingStats> mlbPitchingStatsMap = new HashMap<Integer, MLBPitchingStats>();
		HashMap<Integer, MLBFieldingStats> mlbFieldingStatsMap = new HashMap<Integer, MLBFieldingStats>();
		for (Map.Entry<Integer, MLBPlayer> entry : battersMap.entrySet()) {
			mlbBattingStatsMap.put(entry.getKey(), entry.getValue().getMlbBattingStats());
		}
		for (Map.Entry<Integer, MLBPlayer> entry : pitchersMap.entrySet()) {
			mlbPitchingStatsMap.put(entry.getKey(), entry.getValue().getMlbPitchingStats());
		}
		for (Map.Entry<Integer, MLBPlayer> entry : fieldersMap.entrySet()) {
			mlbFieldingStatsMap.put(entry.getKey(), entry.getValue().getMlbFieldingStats());
		}
		createBatchDataFromMap(mlbBattingStatsMap);
		createBatchDataFromMap(mlbPitchingStatsMap);
		//createBatchDataFromMap(mlbFieldingStatsMap);
	}*/
	
	public static void createBatchDataFromMap(HashMap<?, ?> mlbDataMap) {
		try {
			conn.setAutoCommit(false);
			Statement stmt = conn.createStatement();
			int mlbDataCount = 0;
			for (Map.Entry<?, ?> entry : mlbDataMap.entrySet()) {
				String insertSQL = "";
				if (entry.getValue() instanceof MLBTeam) {
					MLBTeam t = (MLBTeam)entry.getValue();
					insertSQL = "INSERT IGNORE INTO MLB_TEAM (TEAM_ID, MLB_FRANCHISE_ID, FULL_NAME, SHORT_NAME, LEAGUE, FIRST_YEAR_PLAYED, LAST_YEAR_PLAYED) VALUES (" + 
						t.getTeamId() + " ," + t.getMlbFranchiseId() + ", '" + t.getFullTeamName().replace("'", "") + "', '" + t.getShortTeamName() + 
							"', '" + t.getLeague() + "'," + t.getFirstYearPlayed() + "," + t.getLastYearPlayed() + ");";
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
				else if (entry.getValue() instanceof MLBFieldingStats) {
					MLBFieldingStats mfs = (MLBFieldingStats)entry.getValue();
					FieldingStats fs = mfs.getFieldingStats();
					insertSQL = "INSERT IGNORE INTO MLB_FIELDING_STATS (MLB_PLAYER_ID, MLB_TEAM_ID, YEAR, POSITION, ASSISTS, PUT_OUTS, ERRORS) VALUES (" +
						mfs.getMlbPlayerId() + ", " + mfs.getMlbTeamId() + ", " + mfs.getYear() + ", '" + mfs.getPosition() + "', " + fs.getAssists() + ", " + fs.getPutOuts() + ", " + fs.getErrors() + ");";
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
				sql = "SELECT P.*, PS.* from MLB_PITCHING_STATS PS, MLB_PLAYER P WHERE PS.MLB_PLAYER_ID = P.MLB_PLAYER_ID AND YEAR = " + year + " AND MLB_TEAM_ID = " + mlbTeamId;
			}
			else {
				sql = "SELECT P.*, BS.* from MLB_BATTING_STATS BS, MLB_PLAYER P WHERE BS.MLB_PLAYER_ID = P.MLB_PLAYER_ID AND YEAR = " + year + " AND MLB_TEAM_ID = " + mlbTeamId;
			}
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				if (table.equals("MLB_FRANCHISE")) {
					dataMap.put(rs.getString("SHORT_NAME"), rs.getInt("MLB_FRANCHISE_ID"));
				}
				else if (table.equals("MLB_TEAM")) {
					MLBTeam team = new MLBTeam(rs.getInt("TEAM_ID"), rs.getInt("MLB_FRANCHISE_ID"), rs.getString("FULL_NAME"), rs.getString("SHORT_NAME"), rs.getString("LEAGUE"), rs.getInt("FIRST_YEAR_PLAYED"), rs.getInt("LAST_YEAR_PLAYED"));
					dataMap.put(team.getTeamId(), team);
				}
				else if (table.equals("MLB_PLAYER")) {
					MLBPlayer p = new MLBPlayer(rs.getInt("MLB_PLAYER_ID"), rs.getString("FULL_NAME"),  rs.getString("PRIMARY_POSITION"), rs.getString("ARM_THROWS"), rs.getString("BATS"), rs.getInt("JERSEY_NUMBER"));
					if (pitchers != null && !pitchers.booleanValue()) {
						p.setMlbBattingStats(new MLBBattingStats(rs.getInt("MLB_PLAYER_ID"), rs.getInt("MLB_TEAM_ID"),  year, new BattingStats(rs.getInt("AT_BATS"), rs.getInt("HITS"), rs.getInt("DOUBLES"), rs.getInt("TRIPLES"), 
							rs.getInt("HOME_RUNS"), rs.getInt("WALKS"), rs.getInt("STRIKEOUTS"), rs.getInt("HIT_BY_PITCH"), rs.getInt("RUNS"), rs.getInt("RBIS"), rs.getInt("STOLEN_BASES"), rs.getInt("PLATE_APPEARANCES"), rs.getInt("CAUGHT_STEALING"))));
					}
					else {
						p.setMlbPitchingStats(new MLBPitchingStats(rs.getInt("MLB_PLAYER_ID"), rs.getInt("MLB_TEAM_ID"),  year, new PitchingStats(rs.getDouble("INNINGS_PITCHED"), rs.getInt("EARNED_RUNS_ALLOWED"), rs.getInt("RUNS_ALLOWED"), 
							rs.getInt("WALKS"), rs.getInt("STRIKEOUTS"),  rs.getInt("HOME_RUNS_ALLOWED"), rs.getInt("STOLEN_BASES_ALLOWED"), rs.getInt("HIT_BATTERS"), rs.getInt("HITS_ALLOWED"), rs.getInt("HOLDS"), rs.getInt("SAVES"), rs.getInt("BLOWN_SAVES"), 
							rs.getInt("GAMES_STARTED"), rs.getInt("BALKS"), rs.getInt("WILD_PITCHES"), rs.getInt("SAC_FLIES"), rs.getInt("BATTERS_FACED"), rs.getInt("WINS"), rs.getInt("LOSSES"))));
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
	
	public static ArrayList<MLBTeam> getAllTeamsList() {
		ArrayList<MLBTeam> allTeams = new ArrayList<MLBTeam>();
		try {
			Statement stmt = conn.createStatement();
			String sql = "SELECT * FROM MLB_TEAM";
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				MLBTeam team = new MLBTeam(rs.getInt("TEAM_ID"), rs.getInt("MLB_FRANCHISE_ID"), rs.getString("FULL_NAME"), rs.getString("SHORT_NAME"), 
					rs.getString("LEAGUE"), rs.getInt("FIRST_YEAR_PLAYED"), rs.getInt("LAST_YEAR_PLAYED"));
				allTeams.add(team);
			}
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		return allTeams;
	}
	
	public static HashMap<Integer, Object> getPlayerStatsMapForMultipleTeams(Integer year, boolean pitchers) {
		HashMap<Integer, Object> dataMap = new HashMap<Integer, Object>();
		try {
			Statement stmt = conn.createStatement();
			String sql = "SELECT MLB_PLAYER_ID, ";
			if (pitchers) {
				sql += "SUM(INNINGS_PITCHED) AS SUM_IP, SUM(WALKS) AS SUM_BB, SUM(STRIKEOUTS) AS SUM_K, SUM(RUNS_ALLOWED) AS SUM_RA, SUM(EARNED_RUNS_ALLOWED) AS SUM_ERA, SUM(HOME_RUNS_ALLOWED) AS SUM_HRA, ";
				sql += "SUM(STOLEN_BASES_ALLOWED) AS SUM_SBA, SUM(HIT_BATTERS) AS SUM_HB, SUM(HITS_ALLOWED) AS SUM_HA, SUM(HOLDS) AS SUM_H, SUM(SAVES) AS SUM_S, SUM(BLOWN_SAVES) AS SUM_BS, ";
				sql += "SUM(GAMES_STARTED) AS SUM_GS, SUM(BALKS) AS SUM_B, SUM(WILD_PITCHES) AS SUM_WP, SUM(SAC_FLIES) AS SUM_SF, SUM(BATTERS_FACED) AS SUM_BF, SUM(WINS) AS SUM_W, SUM(LOSSES) AS SUM_L ";
				sql += "FROM MLB_PITCHING_STATS WHERE YEAR = " + year + " GROUP BY MLB_PLAYER_ID HAVING COUNT(*) > 1";
			}
			else {
				sql += "SUM(AT_BATS) as SUM_AB, SUM(HITS) as SUM_H, SUM(DOUBLES) as SUM_D, SUM(TRIPLES) as SUM_T, SUM(HOME_RUNS) as SUM_HR, SUM(WALKS) as SUM_BB, SUM(STRIKEOUTS) as SUM_K, ";
				sql += "SUM(HIT_BY_PITCH) as SUM_HBP, SUM(RUNS) as SUM_R, SUM(RBIS) as SUM_RBI, SUM(STOLEN_BASES) as SUM_SB, SUM(PLATE_APPEARANCES) as SUM_PA, SUM(CAUGHT_STEALING) as SUM_CS ";
				sql += "FROM MLB_BATTING_STATS WHERE YEAR = " + year + " GROUP BY MLB_PLAYER_ID HAVING COUNT(*) > 1";
			}
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				if (!pitchers) {
					BattingStats b = new BattingStats(rs.getInt("SUM_AB"), rs.getInt("SUM_H"), rs.getInt("SUM_D"), rs.getInt("SUM_T"), 
						rs.getInt("SUM_HR"), rs.getInt("SUM_BB"), rs.getInt("SUM_K"), rs.getInt("SUM_HBP"), rs.getInt("SUM_R"), rs.getInt("SUM_RBI"), rs.getInt("SUM_SB"), rs.getInt("SUM_PA"), rs.getInt("SUM_CS"));
					dataMap.put(rs.getInt("MLB_PLAYER_ID"), b);
				}
				else {
					// Adjust SUM_IP (only works if IP <= to <IP>.5)
					PitchingStats p = new PitchingStats(rs.getDouble("SUM_IP"), rs.getInt("SUM_ERA"), rs.getInt("SUM_RA"), rs.getInt("SUM_BB"), rs.getInt("SUM_K"), rs.getInt("SUM_HRA"), rs.getInt("SUM_SBA"),
						rs.getInt("SUM_HB"), rs.getInt("SUM_HA"), rs.getInt("SUM_H"), rs.getInt("SUM_S"), rs.getInt("SUM_BS"), rs.getInt("SUM_GS"), rs.getInt("SUM_B"), rs.getInt("SUM_WP"), rs.getInt("SUM_SF"), 
						rs.getInt("SUM_BF"), rs.getInt("SUM_W"), rs.getInt("SUM_L"));
					p.setInningsPitchedFromOuts(p.getOuts()); // Convert IP > <IP>.2
					dataMap.put(rs.getInt("MLB_PLAYER_ID"), p);
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
		HashMap<Integer, Object> battersForMultipleTeams = getPlayerStatsMapForMultipleTeams(year, false);
		HashMap<Object, Object> objectMap = getDataMap("MLB_PLAYER", mlbTeamId, year, false);
		for (Map.Entry<Object, Object> entry : objectMap.entrySet()) {
			// Check if player played on multiple teams, if so use their cumulative stats for all teams
			if (battersForMultipleTeams.get((Integer)entry.getKey()) != null) {
				MLBPlayer mlbPlayer = (MLBPlayer)entry.getValue();
				MLBBattingStats mlbBs = ((MLBPlayer)entry.getValue()).getMlbBattingStats();
				mlbBs.setBattingStats((BattingStats)battersForMultipleTeams.get((Integer)entry.getKey()));
				battersMap.put((Integer)entry.getKey(), mlbPlayer);
			}
			else {
				battersMap.put((Integer)entry.getKey(), (MLBPlayer)entry.getValue());
			}
		}
		return battersMap;
	}
	
	public static HashMap<Integer, MLBPlayer> getPitchersMapByTeamAndYear(Integer mlbTeamId, Integer year) {
		HashMap<Integer, MLBPlayer> pitchersMap = new HashMap<Integer, MLBPlayer>();
		HashMap<Integer, Object> pitchersForMultipleTeams = getPlayerStatsMapForMultipleTeams(year, true);
		HashMap<Object, Object> objectMap = getDataMap("MLB_PLAYER", mlbTeamId, year, true);
		for (Map.Entry<Object, Object> entry : objectMap.entrySet()) {
			// Check if player played on multiple teams, if so use their cumulative stats for all teams
			if (pitchersForMultipleTeams.get((Integer)entry.getKey()) != null) {
				MLBPlayer mlbPlayer = (MLBPlayer)entry.getValue();
				MLBPitchingStats mlbBs = ((MLBPlayer)entry.getValue()).getMlbPitchingStats();
				mlbBs.setPitchingStats((PitchingStats)pitchersForMultipleTeams.get((Integer)entry.getKey()));
				pitchersMap.put((Integer)entry.getKey(), mlbPlayer);
			}
			else {
				pitchersMap.put((Integer)entry.getKey(), (MLBPlayer)entry.getValue());
			}
		}
		return pitchersMap;
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
