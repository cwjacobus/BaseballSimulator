package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
				else if (mlbData instanceof MLBFieldingStats) {
					MLBFieldingStats mfs = (MLBFieldingStats)mlbData;
					FieldingStats fs = mfs.getFieldingStats();
					insertSQL = "INSERT IGNORE INTO MLB_FIELDING_STATS (MLB_PLAYER_ID, MLB_TEAM_ID, YEAR, POSITION, ASSISTS, PUT_OUTS, ERRORS, GAMES) VALUES (" +
						mfs.getMlbPlayerId() + ", " + mfs.getMlbTeamId() + ", " + mfs.getYear() + ", '" + mfs.getPosition() + "', " + fs.getAssists() + ", " + fs.getPutOuts() + ", " + fs.getErrors() + ", " + mfs.getGames() + ");";
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
	
	public static LinkedHashMap<Object, Object> getDataMap(String table, Integer mlbTeamId, Integer year, boolean pitchers) {
		LinkedHashMap<Object, Object> dataMap = new LinkedHashMap<>();
		try {
			Statement stmt = conn.createStatement();
			String sql = "";
			if (pitchers) {
				sql = "SELECT P.*, PS.* from MLB_PITCHING_STATS PS, MLB_PLAYER P WHERE PS.MLB_PLAYER_ID = P.MLB_PLAYER_ID AND YEAR = " + year + 
					(mlbTeamId != null  ? " AND MLB_TEAM_ID = " + mlbTeamId : " ORDER BY MLB_TEAM_ID");
			}
			else {
				sql = "SELECT P.*, BS.* from MLB_BATTING_STATS BS, MLB_PLAYER P WHERE BS.MLB_PLAYER_ID = P.MLB_PLAYER_ID AND YEAR = " + year + 
					(mlbTeamId != null  ? " AND MLB_TEAM_ID = " + mlbTeamId : " ORDER BY MLB_TEAM_ID");
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
					if (!pitchers) {
						p.setMlbBattingStats(new MLBBattingStats(rs.getInt("MLB_PLAYER_ID"), rs.getInt("MLB_TEAM_ID"),  year, new BattingStats(rs.getInt("AT_BATS"), rs.getInt("HITS"), rs.getInt("DOUBLES"), rs.getInt("TRIPLES"), 
							rs.getInt("HOME_RUNS"), rs.getInt("WALKS"), rs.getInt("STRIKEOUTS"), rs.getInt("HIT_BY_PITCH"), rs.getInt("RUNS"), rs.getInt("RBIS"), rs.getInt("STOLEN_BASES"), rs.getInt("PLATE_APPEARANCES"), rs.getInt("CAUGHT_STEALING"))));
						dataMap.put(p.getMlbPlayerId() + ":" + p.getMlbBattingStats().getMlbTeamId(), p);
					}
					else {
						p.setMlbPitchingStats(new MLBPitchingStats(rs.getInt("MLB_PLAYER_ID"), rs.getInt("MLB_TEAM_ID"),  year, new PitchingStats(rs.getDouble("INNINGS_PITCHED"), rs.getInt("EARNED_RUNS_ALLOWED"), rs.getInt("RUNS_ALLOWED"), 
							rs.getInt("WALKS"), rs.getInt("STRIKEOUTS"),  rs.getInt("HOME_RUNS_ALLOWED"), rs.getInt("STOLEN_BASES_ALLOWED"), rs.getInt("HIT_BATTERS"), rs.getInt("HITS_ALLOWED"), rs.getInt("HOLDS"), rs.getInt("SAVES"), rs.getInt("BLOWN_SAVES"), 
							rs.getInt("GAMES_STARTED"), rs.getInt("BALKS"), rs.getInt("WILD_PITCHES"), rs.getInt("SAC_FLIES"), rs.getInt("BATTERS_FACED"), rs.getInt("WINS"), rs.getInt("LOSSES"))));
						dataMap.put(p.getMlbPlayerId() + ":" + p.getMlbPitchingStats().getMlbTeamId(), p);
					}
				}
			}
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		return dataMap; 
	}
	
	public static ArrayList<MLBTeam> getAllTeamsList() {
		ArrayList<MLBTeam> allTeams = new ArrayList<>();
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
	
	public static MLBPlayer getMLBBatterFromMLBPlayerIdAndYear(Integer mlbPlayerId, Integer year) {
		MLBPlayer player = null;
		// TBD doesn't need the sums
		try {
			Statement stmt = conn.createStatement();
			String sql = "SELECT MLB_TEAM_ID, FULL_NAME, PRIMARY_POSITION, ARM_THROWS, BATS, JERSEY_NUMBER, ";
			sql += "sum(AT_BATS) AS SUM_AB, SUM(HITS) as SUM_H, SUM(DOUBLES) as SUM_D, SUM(TRIPLES) as SUM_T, SUM(HOME_RUNS) as SUM_HR, ";
			sql += "SUM(WALKS) as SUM_BB, SUM(STRIKEOUTS) as SUM_K, SUM(HIT_BY_PITCH) as SUM_HBP, SUM(RUNS) as SUM_R, SUM(RBIS) as SUM_RBI, SUM(STOLEN_BASES) as SUM_SB, ";
			sql += "SUM(PLATE_APPEARANCES) as SUM_PA, SUM(CAUGHT_STEALING) as SUM_CS ";
			sql += "FROM MLB_PLAYER P, MLB_BATTING_STATS BS WHERE P.MLB_PLAYER_ID = BS.MLB_PLAYER_ID AND BS.MLB_PLAYER_ID  = " + mlbPlayerId + " AND YEAR = " + year;
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				player = new MLBPlayer(mlbPlayerId, rs.getString("FULL_NAME"),  rs.getString("PRIMARY_POSITION"), rs.getString("ARM_THROWS"), rs.getString("BATS"), rs.getInt("JERSEY_NUMBER"));
				player.setMlbBattingStats(new MLBBattingStats(mlbPlayerId, rs.getInt("MLB_TEAM_ID"),  year, new BattingStats(rs.getInt("SUM_AB"), rs.getInt("SUM_H"), rs.getInt("SUM_D"), rs.getInt("SUM_T"), 
					rs.getInt("SUM_HR"), rs.getInt("SUM_BB"), rs.getInt("SUM_K"), rs.getInt("SUM_HBP"), rs.getInt("SUM_R"), rs.getInt("SUM_RBI"), rs.getInt("SUM_SB"), rs.getInt("SUM_PA"), rs.getInt("SUM_CS"))));
			}	
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		return player;
	}
	
	public static MLBPlayer getMLBPitcherFromMLBPlayerIdAndYear(Integer mlbPlayerId, Integer year) {
		MLBPlayer player = null;
		try {
			Statement stmt = conn.createStatement();
			String sql = "SELECT MLB_TEAM_ID, FULL_NAME, PRIMARY_POSITION, ARM_THROWS, BATS, JERSEY_NUMBER, ";
			sql += "SUM(INNINGS_PITCHED) AS SUM_IP, SUM(WALKS) AS SUM_BB, SUM(STRIKEOUTS) AS SUM_K, SUM(RUNS_ALLOWED) AS SUM_RA, ";
			sql += "SUM(EARNED_RUNS_ALLOWED) AS SUM_ERA, SUM(HOME_RUNS_ALLOWED) AS SUM_HRA, SUM(STOLEN_BASES_ALLOWED) AS SUM_SBA, SUM(HIT_BATTERS) AS SUM_HB, ";
			sql += "SUM(STOLEN_BASES_ALLOWED) AS SUM_SBA, SUM(HIT_BATTERS) AS SUM_HB, SUM(HITS_ALLOWED) AS SUM_HA, SUM(HOLDS) AS SUM_H, SUM(SAVES) AS SUM_S, ";
			sql += "SUM(BLOWN_SAVES) AS SUM_BS, SUM(GAMES_STARTED) AS SUM_GS, SUM(BALKS) AS SUM_B, SUM(WILD_PITCHES) AS SUM_WP, SUM(SAC_FLIES) AS SUM_SF, ";
			sql += "SUM(BATTERS_FACED) AS SUM_BF, SUM(WINS) AS SUM_W, SUM(LOSSES) AS SUM_L ";
			sql += "FROM MLB_PLAYER P, MLB_PITCHING_STATS BS WHERE P.MLB_PLAYER_ID = BS.MLB_PLAYER_ID AND BS.MLB_PLAYER_ID = " + mlbPlayerId + " AND YEAR = " + year;
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				player = new MLBPlayer(mlbPlayerId, rs.getString("FULL_NAME"),  rs.getString("PRIMARY_POSITION"), rs.getString("ARM_THROWS"), rs.getString("BATS"), rs.getInt("JERSEY_NUMBER"));
				player.setMlbPitchingStats(new MLBPitchingStats(mlbPlayerId, rs.getInt("MLB_TEAM_ID"), year, new PitchingStats(rs.getDouble("SUM_IP"), rs.getInt("SUM_ERA"), rs.getInt("SUM_RA"), 
					rs.getInt("SUM_BB"), rs.getInt("SUM_K"), rs.getInt("SUM_HRA"), rs.getInt("SUM_SBA"), rs.getInt("SUM_HB"), rs.getInt("SUM_HA"), rs.getInt("SUM_H"), rs.getInt("SUM_S"), 
					rs.getInt("SUM_BS"), rs.getInt("SUM_GS"), rs.getInt("SUM_B"), rs.getInt("SUM_WP"), rs.getInt("SUM_SF"), rs.getInt("SUM_BF"), rs.getInt("SUM_W"), rs.getInt("SUM_L"))));
			}	
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		return player;
	}
	
	public static HashMap<Integer, Object> getPlayerStatsMapForMultipleTeams(Integer year, boolean pitchers) {
		HashMap<Integer, Object> dataMap = new HashMap<>();
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
		HashMap<Integer, MLBPlayer> battersMap = new HashMap<>();
		HashMap<Integer, Object> battersForMultipleTeams = getPlayerStatsMapForMultipleTeams(year, false);
		HashMap<Object, Object> objectMap = getDataMap("MLB_PLAYER", mlbTeamId, year, false);
		for (Map.Entry<Object, Object> entry : objectMap.entrySet()) {
			Integer mlbPlayerId = Integer.parseInt(((String)entry.getKey()).split(":")[0]);
			// Check if player played on multiple teams, if so use their cumulative stats for all teams
			if (battersForMultipleTeams.get(mlbPlayerId) != null) {
				MLBPlayer mlbPlayer = (MLBPlayer)entry.getValue();
				MLBBattingStats mlbBs = ((MLBPlayer)entry.getValue()).getMlbBattingStats();
				mlbBs.setBattingStats((BattingStats)battersForMultipleTeams.get(mlbPlayerId));
				battersMap.put(mlbPlayerId, mlbPlayer);
			}
			else {
				battersMap.put(mlbPlayerId, (MLBPlayer)entry.getValue());
			}
		}
		return battersMap;
	}
	
	public static LinkedHashMap<Integer, HashMap<Integer, MLBPlayer>> getBattersMapByYear(Integer year) {
		LinkedHashMap<Integer, HashMap<Integer, MLBPlayer>> allBattersMap = new LinkedHashMap<>();
		HashMap<Integer, MLBPlayer> battersMap = null;
		HashMap<Integer, Object> battersForMultipleTeams = getPlayerStatsMapForMultipleTeams(year, false);
		HashMap<Object, Object> objectMap = getDataMap("MLB_PLAYER", null, year, false);
		MLBPlayer mlbPlayer;
		Integer prevTeamId = 0;
		for (Map.Entry<Object, Object> entry : objectMap.entrySet()) {
			mlbPlayer = (MLBPlayer)entry.getValue();
			Integer mlbPlayerId = Integer.parseInt(((String)entry.getKey()).split(":")[0]);
			Integer teamId = Integer.parseInt(((String)entry.getKey()).split(":")[1]);
			if (teamId.intValue() != prevTeamId.intValue()) {
				if (battersMap != null) {
					allBattersMap.put(prevTeamId, battersMap);
				}
				battersMap = new HashMap<>();
			}
			// Check if player played on multiple teams, if so use their cumulative stats for all teams
			if (battersForMultipleTeams.get(mlbPlayerId) != null) {
				MLBBattingStats mlbBs = ((MLBPlayer)entry.getValue()).getMlbBattingStats();
				mlbBs.setBattingStats((BattingStats)battersForMultipleTeams.get(mlbPlayerId));
				battersMap.put(mlbPlayerId, mlbPlayer);
			}
			else {
				battersMap.put(mlbPlayerId, mlbPlayer);
			}
			prevTeamId = teamId;
		}
		allBattersMap.put(prevTeamId, battersMap);
		return allBattersMap;
	}
	
	public static HashMap<Integer, MLBPlayer> getPitchersMapByTeamAndYear(Integer mlbTeamId, Integer year) {
		HashMap<Integer, MLBPlayer> pitchersMap = new HashMap<>();
		HashMap<Integer, Object> pitchersForMultipleTeams = getPlayerStatsMapForMultipleTeams(year, true);
		HashMap<Object, Object> objectMap = getDataMap("MLB_PLAYER", mlbTeamId, year, true);
		for (Map.Entry<Object, Object> entry : objectMap.entrySet()) {
			Integer mlbPlayerId = Integer.parseInt(((String)entry.getKey()).split(":")[0]);
			// Check if player played on multiple teams, if so use their cumulative stats for all teams
			if (pitchersForMultipleTeams.get(mlbPlayerId) != null) {
				MLBPlayer mlbPlayer = (MLBPlayer)entry.getValue();
				MLBPitchingStats mlbBs = ((MLBPlayer)entry.getValue()).getMlbPitchingStats();
				mlbBs.setPitchingStats((PitchingStats)pitchersForMultipleTeams.get(mlbPlayerId));
				pitchersMap.put(mlbPlayerId, mlbPlayer);
			}
			else {
				pitchersMap.put(mlbPlayerId, (MLBPlayer)entry.getValue());
			}
		}
		return pitchersMap;
	}
	
	public static LinkedHashMap<Integer, HashMap<Integer, MLBPlayer>> getPitchersMapByYear(Integer year) {
		LinkedHashMap<Integer, HashMap<Integer, MLBPlayer>> allPitchersMap = new LinkedHashMap<>();
		HashMap<Integer, MLBPlayer> pitchersMap = null;
		HashMap<Integer, Object> pitchersForMultipleTeams = getPlayerStatsMapForMultipleTeams(year, true);
		HashMap<Object, Object> objectMap = getDataMap("MLB_PLAYER", null, year, true);
		MLBPlayer mlbPlayer;
		Integer prevTeamId = 0;
		for (Map.Entry<Object, Object> entry : objectMap.entrySet()) {
			mlbPlayer = (MLBPlayer)entry.getValue();
			Integer mlbPlayerId = Integer.parseInt(((String)entry.getKey()).split(":")[0]);
			Integer teamId = Integer.parseInt(((String)entry.getKey()).split(":")[1]);
			if (teamId.intValue() != prevTeamId.intValue()) {
				if (pitchersMap != null) {
					allPitchersMap.put(prevTeamId, pitchersMap);
				}
				pitchersMap = new HashMap<>();
			}
			// Check if player played on multiple teams, if so use their cumulative stats for all teams
			if (pitchersForMultipleTeams.get(mlbPlayerId) != null) {
				MLBPitchingStats mlbBs = ((MLBPlayer)entry.getValue()).getMlbPitchingStats();
				mlbBs.setPitchingStats((PitchingStats)pitchersForMultipleTeams.get(mlbPlayerId));
				pitchersMap.put(mlbPlayerId, mlbPlayer);
			}
			else {
				pitchersMap.put(mlbPlayerId, mlbPlayer);
			}
			prevTeamId = teamId;
		}
		allPitchersMap.put(prevTeamId, pitchersMap);
		return allPitchersMap;
	}
	
	public static HashMap<Integer, Integer> getBattersOnMultipleTeamsByPrimaryTeam(int year) {
		HashMap<Integer, Integer> battersMap = new HashMap<>();
		try {
			Statement stmt = conn.createStatement();
			String sql = "SELECT MLB_PLAYER_ID, MLB_TEAM_ID, PLATE_APPEARANCES FROM MLB_BATTING_STATS WHERE MLB_PLAYER_ID IN (SELECT MLB_PLAYER_ID FROM " + 
				"MLB_BATTING_STATS WHERE YEAR = " + year + " GROUP BY MLB_PLAYER_ID HAVING COUNT(*) > 1) AND YEAR = " + year +
				" ORDER BY MLB_PLAYER_ID, PLATE_APPEARANCES DESC";
			ResultSet rs = stmt.executeQuery(sql);
			Integer prevMlbPlayerId = null;
			while (rs.next()) {
				Integer mlbPlayerId = rs.getInt("MLB_PLAYER_ID");
				if (prevMlbPlayerId == null || mlbPlayerId.intValue() != prevMlbPlayerId.intValue())
					battersMap.put(mlbPlayerId, rs.getInt("MLB_TEAM_ID"));
				prevMlbPlayerId = mlbPlayerId;
			}
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		return battersMap;
	}
	
	public static HashMap<Integer, Integer> getPitchersOnMultipleTeamsByPrimaryTeam(int year) {
		HashMap<Integer, Integer> pitchersMap = new HashMap<>();
		try {
			Statement stmt = conn.createStatement();
			String sql = "SELECT MLB_PLAYER_ID, MLB_TEAM_ID, BATTERS_FACED FROM MLB_PITCHING_STATS WHERE MLB_PLAYER_ID IN (SELECT MLB_PLAYER_ID FROM " + 
				"MLB_PITCHING_STATS WHERE YEAR = " + year + " GROUP BY MLB_PLAYER_ID HAVING COUNT(*) > 1) AND YEAR = " + year +
				" ORDER BY MLB_PLAYER_ID, BATTERS_FACED DESC";
			ResultSet rs = stmt.executeQuery(sql);
			Integer prevMlbPlayerId = null;
			while (rs.next()) {
				Integer mlbPlayerId = rs.getInt("MLB_PLAYER_ID");
				if (prevMlbPlayerId == null || mlbPlayerId.intValue() != prevMlbPlayerId.intValue())
					pitchersMap.put(mlbPlayerId, rs.getInt("MLB_TEAM_ID"));
				prevMlbPlayerId = mlbPlayerId;
			}
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		return pitchersMap;
	}
	
	public static HashMap<Integer, ArrayList<MLBFieldingStats>> getFieldingStatsMapByTeamAndYear(Integer mlbTeamId, Integer year) {
		HashMap<Integer, ArrayList<MLBFieldingStats>> fieldingStatsMap = new HashMap<>();
		ArrayList<MLBFieldingStats> playerFieldingStats = new ArrayList<>();
		Integer prevPlayerId = null;
		Integer playerId = null;
		MLBFieldingStats playerFieldingStatsByPosition = null;
		try {
			Statement stmt = conn.createStatement();
			String sql = "SELECT * FROM MLB_FIELDING_STATS WHERE MLB_TEAM_ID = " +  mlbTeamId + " AND YEAR = " + year + " ORDER BY MLB_PLAYER_ID, GAMES DESC";
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				playerId = rs.getInt("MLB_PLAYER_ID");
				if (prevPlayerId != null && playerId.intValue() != prevPlayerId.intValue()) {
					fieldingStatsMap.put(prevPlayerId, playerFieldingStats);
					playerFieldingStats = new ArrayList<>();
				}
				playerFieldingStatsByPosition = new MLBFieldingStats(playerId, rs.getInt("MLB_TEAM_ID"),  year, rs.getString("POSITION"), 
					rs.getInt("GAMES"), new FieldingStats(rs.getInt("ASSISTS"), rs.getInt("PUT_OUTS"), rs.getInt("ERRORS")));
				playerFieldingStats.add(playerFieldingStatsByPosition);
				prevPlayerId = playerId;
			}
			fieldingStatsMap.put(playerId, playerFieldingStats);
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		return fieldingStatsMap;
	}
	
	public static HashMap<Integer, HashMap<Integer, ArrayList<MLBFieldingStats>>> getFieldingStatsMapByYear(Integer year) {
		HashMap<Integer, HashMap<Integer, ArrayList<MLBFieldingStats>>> fieldingStatsMap = new HashMap<>();
		HashMap<Integer, ArrayList<MLBFieldingStats>> teamFieldingStats = new HashMap<>();
		ArrayList<MLBFieldingStats> playerFieldingStats = new ArrayList<>();
		Integer prevPlayerId = null;
		Integer playerId = null;
		Integer prevTeamId = null;
		Integer teamId = null;
		MLBFieldingStats playerFieldingStatsByPosition = null;
		try {
			Statement stmt = conn.createStatement();
			String sql = "SELECT * FROM MLB_FIELDING_STATS WHERE YEAR = " + year + " ORDER BY MLB_TEAM_ID, MLB_PLAYER_ID, GAMES DESC";
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				playerId = rs.getInt("MLB_PLAYER_ID");
				teamId = rs.getInt("MLB_TEAM_ID");
				if (prevPlayerId != null && playerId.intValue() != prevPlayerId.intValue()) {
					teamFieldingStats.put(prevPlayerId, playerFieldingStats);
					playerFieldingStats = new ArrayList<>();
				}
				if (prevTeamId != null && teamId.intValue() != prevTeamId.intValue()) {
					fieldingStatsMap.put(prevTeamId, teamFieldingStats);
					teamFieldingStats = new HashMap<>();
				}
				playerFieldingStatsByPosition = new MLBFieldingStats(playerId, rs.getInt("MLB_TEAM_ID"),  year, rs.getString("POSITION"), 
					rs.getInt("GAMES"), new FieldingStats(rs.getInt("ASSISTS"), rs.getInt("PUT_OUTS"), rs.getInt("ERRORS")));
				playerFieldingStats.add(playerFieldingStatsByPosition);
				teamFieldingStats.put(playerId, playerFieldingStats);
				prevPlayerId = playerId;
				prevTeamId = teamId;
			}
			fieldingStatsMap.put(teamId, teamFieldingStats);
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		return fieldingStatsMap;
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
