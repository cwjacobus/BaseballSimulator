package baseball;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import dao.DAO;
import db.MLBBattingStats;
import db.MLBFieldingStats;
import db.MLBFranchise;
import db.MLBPitchingStats;
import db.MLBPlayer;
import db.MLBTeam;
import db.MLBWorldSeries;

public class DBImport {
	
	static int currentYear = Calendar.getInstance().get(Calendar.YEAR);

	public static void main(String[] args) {
		// Example PLAYER 2022 2022 ALL
		int startYear = 0;
		int endYear = -1;
		boolean allYears = false;
		ArrayList<MLBTeam> allTeamsList;
		ArrayList<MLBTeam> teamsForYearList;
		boolean fieldingOnly = false;
		
		if (args.length < 3 && !args[0].equalsIgnoreCase("WORLDSERIES")) {
			System.out.println("INVALID ARGS");
			return;
		}
		DAO.setConnection();
		allTeamsList = DAO.getAllTeamsList();
		String fn = args[0];
		
		if (!args[0].equalsIgnoreCase("WORLDSERIES") && !args[1].equalsIgnoreCase("ALL")) {
			try {
				startYear = Integer.parseInt(args[1]);
			}
			catch (NumberFormatException e) {
				System.out.println("INVALID YEAR: " + args[1]);
				return;
			}
		}
		else {
			allYears = true;
		}
		if (fn.equals("TEAM")) {
			importFranchisesAndTeams(startYear, allYears);
		}
		if (fn.equals("WORLDSERIES")) {
			importWorldSeries();
		}
		else if (fn.equals("PLAYER")) {
			if (args.length < 4) {
				System.out.println("INVALID ARGS");
				return;
			}
			if (!args[2].equalsIgnoreCase("ALL")) {
				try {
					endYear = Integer.parseInt(args[2]);
				}
				catch (NumberFormatException e) {
					System.out.println("INVALID YEAR:" + args[2]);
					return;
				}
			}
			if (args.length >= 5) {
				fieldingOnly = args[4].equalsIgnoreCase("F");
			}
			
			for (int year = startYear; year <= endYear; year++) {
				System.out.println("Import players for: " + year);
				if (!args[3].equalsIgnoreCase("ALL")) {
					teamsForYearList = getTeamsByYear(year, args[3], allTeamsList);
				}
				else {
					teamsForYearList = getTeamsByYear(year, null, allTeamsList);
				}
				System.out.println("Import all batters for " + year);
				HashMap<Integer, MLBPlayer> hittersMap = importMlbPlayers(year, true, teamsForYearList); // import hitters
				HashMap<Integer, MLBPlayer> qualifiedHittersMap = new HashMap<Integer, MLBPlayer>();
				HashMap<Integer, MLBPlayer> newHittersMap = new HashMap<Integer, MLBPlayer>();
				System.out.println("Import all batting stats for " + year);
				ArrayList<Object> battingStatsList = importBattingStats(teamsForYearList, year, hittersMap, qualifiedHittersMap, newHittersMap);
				if (!fieldingOnly) {
					System.out.println("Import all pitchers for " + year);
					HashMap<Integer, MLBPlayer> pitchersMap = importMlbPlayers(year, false, teamsForYearList); // import pitchers
					HashMap<Integer, MLBPlayer> qualifiedPitchersMap = new HashMap<Integer, MLBPlayer>();
					HashMap<Integer, MLBPlayer> newPitchersMap = new HashMap<Integer, MLBPlayer>();
					System.out.println("Import all pitching stats for " + year);
					ArrayList<Object> pitchingStatsList = importPitchingStats(teamsForYearList, year, pitchersMap, qualifiedPitchersMap, newPitchersMap);
					//DAO.createBatchDataFromMap(newHittersMap);
					//DAO.createBatchDataFromMap(newPitchersMap);
					//DAO.createBatchDataFromList(battingStatsList);
					//DAO.createBatchDataFromList(pitchingStatsList);
				}
				System.out.println("Import all fielding stats for " + year);
				ArrayList<Object> fieldingStatsList = importFieldingStats(teamsForYearList, qualifiedHittersMap, year);  // hitters fielding
				
				//DAO.createBatchDataFromList(fieldingStatsList);
			}
		}
		else {
			System.out.println("INVALID FUNCTION");
		}
	}
	
	// Get player stats from API
	private static void importFranchisesAndTeams(Integer year, boolean allYears) {
		HashMap<String, MLBTeam> allTeamsMap = new HashMap<String, MLBTeam>();
		HashMap<Integer, MLBFranchise> allFranchisesMap = new HashMap<Integer, MLBFranchise>();
		int beginYear = allYears ? 1900 : year;
		int endYear = allYears ? currentYear : year;
		DAO.setConnection();
		for (int y = beginYear; y <= endYear; y++) {
			try {    	
				String getTeamsAPI = "http://lookup-service-prod.mlb.com/json/named.team_all_season.bam?sport_code=%27mlb%27&season=%27" + y +"%27";
				URL obj = new URL(getTeamsAPI);
				HttpURLConnection con = (HttpURLConnection)obj.openConnection();
				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream())); 
				try {
					JSONObject teams = new JSONObject(in.readLine());
					JSONObject searchAll = new JSONObject(teams.getString("team_all_season"));
					JSONObject queryResults = new JSONObject(searchAll.getString("queryResults"));
					JSONArray allTeams = new JSONArray(queryResults.getString("row"));
					for (int i = 0; i < allTeams.length(); i++) {
						JSONObject teamJson = allTeams.getJSONObject(i);
						// Note: expansion teams appear a year or two before they start playing with no league assigned
						if (teamJson.getString("mlb_org_id").length() > 0 && teamJson.getString("league").length() > 0) { // Filter out non-franchise teams (like all star teams) 
							if (allFranchisesMap.get(Integer.parseInt(teamJson.getString("mlb_org_id"))) == null) {
								allFranchisesMap.put(Integer.parseInt(teamJson.getString("mlb_org_id")), new MLBFranchise(Integer.parseInt(teamJson.getString("mlb_org_id")),
									teamJson.getString("mlb_org"), teamJson.getString("mlb_org_abbrev"), Integer.parseInt(teamJson.getString("first_year_of_play"))));
							}
							if (allTeamsMap.get((teamJson.getString("name_display_full") + ":" + teamJson.getString("mlb_org_id") + ":" + teamJson.getString("league"))) == null) {
								MLBTeam t = new MLBTeam(Integer.parseInt(teamJson.getString("team_id")), Integer.parseInt(teamJson.getString("mlb_org_id")), 
									teamJson.getString("name_display_full"), teamJson.getString("name_abbrev"), teamJson.getString("league"), y, y);
								allTeamsMap.put((teamJson.getString("name_display_full") + ":" + teamJson.getString("mlb_org_id") + ":" + teamJson.getString("league")), t);
							}
							else {
								MLBTeam t = allTeamsMap.get((teamJson.getString("name_display_full") + ":" + teamJson.getString("mlb_org_id") + ":" + teamJson.getString("league")));
								t.setLastYearPlayed(y==currentYear?null:y); // Set a present team to null for last year played
								allTeamsMap.put((teamJson.getString("name_display_full") + ":" + teamJson.getString("mlb_org_id") + ":" + teamJson.getString("league")), t);
							}
						}
					}
				}
				catch (JSONException e) {
					e.printStackTrace();
				}	    	
			}
			catch (MalformedURLException e) { 	
				e.printStackTrace();
			}
			catch (IOException e) { 
				e.printStackTrace();
			}
		}
		
		// Special logic for LAA
		// 1961-195 and returned with same name, org and league in 2005 so need to be 2 separate teams
		MLBTeam oldLAA = allTeamsMap.get("Los Angeles Angels:108:AL");
		oldLAA.setLastYearPlayed(1965);
		allTeamsMap.put("Los Angeles Angels:108:AL", oldLAA);
		allTeamsMap.put("Los Angeles Angels of Anaheim:108:AL", new MLBTeam(108, 108, "Los Angeles Angels of Anaheim", "LAA", "AL", 2005, null));
		allTeamsMap = sortHashMapByName(allTeamsMap);
		
		System.out.println("FRANCHISES");
		for (Map.Entry<Integer, MLBFranchise> entry : allFranchisesMap.entrySet()) {
			System.out.println(entry.getValue().getFullTeamName() + " " + entry.getValue().getMlbFranchiseId() + " " + entry.getValue().getShortTeamName() + " " + entry.getValue().getFirstYearPlayed());
		}
		System.out.println("TEAMS");
		for (Map.Entry<String, MLBTeam> entry : allTeamsMap.entrySet()) {
			System.out.print(entry.getValue().getFullTeamName() + " " + entry.getValue().getTeamId() + " " + entry.getValue().getShortTeamName() +
			    " " + entry.getValue().getLeague() + " " + entry.getValue().getFirstYearPlayed() + " to " + entry.getValue().getLastYearPlayed());
			if (entry.getValue().getLeague() == null || entry.getValue().getLeague().length() == 0) {
				System.out.print(" NO LEAGUE!");
			}
			System.out.println();
		}
		
		DAO.createBatchDataFromMap(allFranchisesMap);
		DAO.createBatchDataFromMap(allTeamsMap);
	}
	
	public static HashMap<Integer, MLBPlayer> importMlbPlayers(Integer year, boolean hitters, ArrayList<MLBTeam> teamsList) {
		HashMap<Integer, MLBPlayer> allPlayersMap = new HashMap<Integer, MLBPlayer>();
		for (MLBTeam t : teamsList) {
			try { 
				System.out.println("Import players from " + t.getFullTeamName());
				String getPlayersAPI = "https://statsapi.mlb.com/api/v1/teams/" + t.getTeamId() + "/roster?season=" + year;
				URL obj = new URL(getPlayersAPI);
				HttpURLConnection con = (HttpURLConnection)obj.openConnection();
				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream())); 
				try {
					JSONObject players = new JSONObject(in.readLine());
					JSONArray roster = new JSONArray(players.getString("roster"));
					for (int i = 0; i < roster.length(); i++) {
						JSONObject player = roster.getJSONObject(i);
						JSONObject position = new JSONObject(player.getString("position"));
						JSONObject person = new JSONObject(player.getString("person"));
						if ((hitters && position.getString("abbreviation").equals("P")) ||
						   (!hitters && !position.getString("abbreviation").equals("P"))) { // Skip pitchers or hitters
								continue;
						}
						Integer playerId = Integer.parseInt(person.getString("id"));
						Integer jerseyNumber = null;
						if (player.getString("jerseyNumber").length() > 0) {
							try {
								jerseyNumber = Integer.parseInt(player.getString("jerseyNumber"));
							}
							catch (NumberFormatException e) {
							}
						}
						MLBPlayer p = new MLBPlayer(playerId, person.getString("fullName"), position.getString("abbreviation"), null, null, jerseyNumber);
						allPlayersMap.put(playerId, p);
					}
				}
				catch (JSONException e) {
					e.printStackTrace();
				}	    	
			}
			catch (MalformedURLException e) { 	
				e.printStackTrace();
			}
			catch (IOException e) { 
				e.printStackTrace();
			}
		}
		/*for (Map.Entry<Integer, MLBPlayer> entry : allPlayersMap.entrySet()) {
			System.out.println(entry.getValue().getMlbPlayerId() + " " + entry.getValue().getFirstLastName() + " " + entry.getValue().getPrimaryPosition() +
			    " " + entry.getValue().getArmThrows() + " " + entry.getValue().getBats() + " " + entry.getValue().getJerseyNumber());
		}*/
		return allPlayersMap;
	}
	
	public static MLBPlayer importMlbPlayer(Integer mlbPlayerId) {
		MLBPlayer mlbPlayer = null;
		try { 
			String getPlayerAPI = "https://statsapi.mlb.com/api/v1/people/" + mlbPlayerId;
			URL obj = new URL(getPlayerAPI);
			HttpURLConnection con = (HttpURLConnection)obj.openConnection();
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream())); 
			try {
				JSONObject player = new JSONObject(in.readLine());
				JSONArray people = new JSONArray(player.getString("people"));
				for (int i = 0; i < people.length(); i++) {
					JSONObject person = people.getJSONObject(i);
					JSONObject primaryPosition = new JSONObject(person.getString("primaryPosition"));
					Integer playerId = Integer.parseInt(person.getString("id"));
					Integer primaryNumber = null;
					JSONObject batSide = new JSONObject(person.getString("batSide"));
					String bats = batSide.getString("code");
					JSONObject pitchHand = new JSONObject(person.getString("pitchHand"));
					String pitchHandString = pitchHand.getString("code");
					String fullName = person.getString("lastFirstName");
					String primaryPositionAbbreviation = primaryPosition.getString("abbreviation");
					if (person.getString("primaryNumber").length() > 0) {
						try {
							primaryNumber = Integer.parseInt(person.getString("primaryNumber"));
						}
						catch (NumberFormatException e) {
						}
						catch (JSONException e) {
							System.out.println("NO JERSEY NUMBER FOUND FOR " + fullName);
						}
					}
					mlbPlayer = new MLBPlayer(playerId, fullName, primaryPositionAbbreviation, pitchHandString, bats, primaryNumber);
				}
			}
			catch (JSONException e) {
				e.printStackTrace();
			}	    	
		}
		catch (MalformedURLException e) { 	
			e.printStackTrace();
		}
		catch (IOException e) { 
			e.printStackTrace();
		}
		System.out.println("***NEW MLB PLAYER***: " + mlbPlayer.getMlbPlayerId() + " " + mlbPlayer.getFirstLastName() + " " + mlbPlayer.getPrimaryPosition() +
			 " " + mlbPlayer.getArmThrows() + " " + mlbPlayer.getBats() + " " + mlbPlayer.getJerseyNumber());
		return mlbPlayer;
	}
	
	public static ArrayList<Object> importBattingStats(ArrayList<MLBTeam> teamsList, int year, HashMap<Integer, MLBPlayer> hittersMap,
		HashMap<Integer, MLBPlayer> qualifiedHittersMap, HashMap<Integer, MLBPlayer> newHittersMap) {
		ArrayList<Object> battingStatsList = new ArrayList<Object>();
		int qualifyingPlateAppearances = 100;
		if (year == 2020) { // pandemic year
			qualifyingPlateAppearances = 38;
		}
		else if (year == 1994) { // strike year
			qualifyingPlateAppearances = 71;
		}
		int count = 0;
		for (MLBTeam team : teamsList) {
			String getBattingStatsAPI = "https://statsapi.mlb.com/api/v1/stats?season=" + year + "&group=hitting&stats=season&gameType=R" + 
				"&playerPool=ALL&limit=1000&teamId=" + team.getTeamId(); // Limit must be set or else returns 50
			try {
				System.out.println("\n" + team.getFullTeamName() + " qualified batters");
				URL obj = new URL(getBattingStatsAPI);
				HttpURLConnection con = (HttpURLConnection)obj.openConnection();
				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream())); 
				JSONObject playerStats = new JSONObject(in.readLine());
				JSONArray stats = new JSONArray(playerStats.getString("stats"));
				JSONArray splits = new JSONArray(stats.getJSONObject(0).getString("splits"));
				List<Integer> allMLBPlayersIdList = DAO.getAllMlbPlayerIdsList();
				for (int i = 0; i < splits.length(); i++) {
					JSONObject battingStatsJSON = splits.getJSONObject(i);
					JSONObject player = new JSONObject(battingStatsJSON.getString("player"));
					JSONObject stat = new JSONObject(battingStatsJSON.getString("stat"));
					JSONObject position = new JSONObject(battingStatsJSON.getString("position"));
					Integer playerId = Integer.parseInt(player.getString("id"));
					// Only include stats if they played enough games at the position, are not a pitcher and have enough plate appearances
					if (stat.getInt("plateAppearances") > qualifyingPlateAppearances && !position.getString("abbreviation").equals("P")) {
						count++;
						MLBBattingStats mbs = createMLBBattingStats(playerId, battingStatsJSON, year);
						battingStatsList.add(mbs);
						System.out.println(player.getString("fullName") + " had " + stat.getInt("plateAppearances") + " plate appearances at " + 
							position.getString("abbreviation"));
						qualifiedHittersMap.put(playerId, hittersMap.get(playerId));
						if (!allMLBPlayersIdList.contains(playerId)) {  // New player
							MLBPlayer newPlayer = importMlbPlayer(playerId);
							newHittersMap.put(playerId, newPlayer);
						}
					}
				}
			} // try
			catch (JSONException e) {
				System.out.println(getBattingStatsAPI);
				e.printStackTrace();
			}
			catch (IOException e) { 
				System.out.println("\nBatting stats not found");
			}
		} // for loop
		System.out.println("\nBatting stats import complete. " + count + " total batting records\n");
		return battingStatsList;
	}
	
	public static ArrayList<Object> importPitchingStats(ArrayList<MLBTeam> teamsList, int year, HashMap<Integer, MLBPlayer> pitchersMap,
			HashMap<Integer, MLBPlayer> qualifiedPitchersMap, HashMap<Integer, MLBPlayer> newPitchersMap) {
		ArrayList<Object> pitchingStatsList = new ArrayList<>();
		double qualifyingInningsPitched = 40.0;
		if (year == 2020) { // pandemic year
			qualifyingInningsPitched = 16.0;
		}
		else if (year == 1994) { // strike year
			qualifyingInningsPitched = 28.0;
		} 
		int count = 0;
		for (MLBTeam team : teamsList) {
			String getPitchingStatsAPI = "https://statsapi.mlb.com/api/v1/stats?season=" + year + "&group=pitching&stats=season&gameType=R" + 
				"&playerPool=ALL&limit=1000&teamId=" + team.getTeamId(); // Limit must be set or else returns 50
			try {
				System.out.println("\n" + team.getFullTeamName() + " qualified pitchers");
				URL obj = new URL(getPitchingStatsAPI);
				HttpURLConnection con = (HttpURLConnection)obj.openConnection();
				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream())); 
				JSONObject playerStats = new JSONObject(in.readLine());
				JSONArray stats = new JSONArray(playerStats.getString("stats"));
				JSONArray splits = new JSONArray(stats.getJSONObject(0).getString("splits"));
				List<Integer> allMLBPlayersIdList = DAO.getAllMlbPlayerIdsList();
				for (int i = 0; i < splits.length(); i++) {
					JSONObject pitchingStatsJSON = splits.getJSONObject(i);
					JSONObject player = new JSONObject(pitchingStatsJSON.getString("player"));
					JSONObject stat = new JSONObject(pitchingStatsJSON.getString("stat"));
					JSONObject position = new JSONObject(pitchingStatsJSON.getString("position"));
					Integer playerId = Integer.parseInt(player.getString("id"));
					// Only include stats if they played enough games at the position, are a pitcher and have enough plate appearances
					if (stat.getInt("inningsPitched") > qualifyingInningsPitched && position.getString("abbreviation").equals("P")) {
						count++;
						MLBPitchingStats mbs = createMLBPitchingStats(playerId, pitchingStatsJSON, year);
						pitchingStatsList.add(mbs);
						System.out.println(player.getString("fullName") + " pitched " + stat.getString("inningsPitched") + " innings");
						qualifiedPitchersMap.put(playerId, pitchersMap.get(playerId));
						if (!allMLBPlayersIdList.contains(playerId)) {  // New player
							MLBPlayer newPlayer = importMlbPlayer(playerId);
							newPitchersMap.put(playerId, newPlayer);
						}
					}
				}
			} // try
			catch (JSONException e) {
				System.out.println(getPitchingStatsAPI);
				e.printStackTrace();
			}
			catch (IOException e) { 
				System.out.println("\nPitching stats not found");
			}
		} // for loop
		System.out.println("\nPitching stats import complete. " + count + " total pitching records\n");
		return pitchingStatsList;
	}
	
	public static ArrayList<Object> importFieldingStats(ArrayList<MLBTeam> teamsList, HashMap<Integer, MLBPlayer> qualifiedBatters, int year) {
		ArrayList<Object> fieldingStatsList = new ArrayList<>();
		int qualifyingGames = 20;
		if (year == 2020) { // pandemic year
			qualifyingGames = 8;
			
		}
		else if (year == 1994) { // strike year
			qualifyingGames = 15;
		}
		int count = 0;
		for (MLBTeam team : teamsList) {
			String getFieldingStatsAPI = "https://statsapi.mlb.com/api/v1/stats?season=" + year + "&group=fielding&stats=season&gameType=R" + 
				"&playerPool=ALL&limit=1000&teamId=" + team.getTeamId(); // Limit must be set or else returns 50
			try {
				System.out.println("\n" + team.getFullTeamName());
				URL obj = new URL(getFieldingStatsAPI);
				HttpURLConnection con = (HttpURLConnection)obj.openConnection();
				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream())); 
				JSONObject playerFieldingStats = new JSONObject(in.readLine());
				JSONArray stats = new JSONArray(playerFieldingStats.getString("stats"));
				JSONArray splits = new JSONArray(stats.getJSONObject(0).getString("splits"));
				for (int i = 0; i < splits.length(); i++) {
					JSONObject fieldingStatsJSON = splits.getJSONObject(i);
					JSONObject player = new JSONObject(fieldingStatsJSON.getString("player"));
					JSONObject stat = new JSONObject(fieldingStatsJSON.getString("stat"));
					JSONObject position = new JSONObject(fieldingStatsJSON.getString("position"));
					Integer playerId = Integer.parseInt(player.getString("id"));
					// Only include fielding stats if they played enough games at the position, are not a pitcher or DH and have enough plate appearances
					if (stat.getInt("gamesPlayed") > qualifyingGames && !position.getString("abbreviation").equals("P") && 
						!position.getString("abbreviation").equals("DH") && qualifiedBatters.get(playerId) != null) {
							count++;
							MLBFieldingStats mfs = createMLBFieldingStats(playerId, fieldingStatsJSON, year);
							fieldingStatsList.add(mfs);
							System.out.println(player.getString("fullName") + " played " + stat.getInt("gamesPlayed") + " games at " + position.getString("abbreviation"));
					}
				}
			}
			catch (JSONException e) {
				System.out.println(getFieldingStatsAPI);
				e.printStackTrace();
			}
			catch (IOException e) { 
				System.out.println("\nFielding stats not found");
			}
		}
		/*
		System.out.println();
		for (Object o : fieldingStatsList) {
			MLBFieldingStats mfs = (MLBFieldingStats)o;
			System.out.println(mfs.getMlbPlayerId() + " " + mfs.getMlbTeamId() + " " +  mfs.getYear() + " " + mfs.getPosition() + " " + mfs.getGames() + " " +
				mfs.getFieldingStats().getAssists() + " " + mfs.getFieldingStats().getPutOuts() + " " + mfs.getFieldingStats().getErrors());
		}*/
		System.out.println("\nFielder stats import complete. " + count + " total fielding records\n");
		return fieldingStatsList;
	}
	
	static MLBBattingStats createMLBBattingStats(Integer mlbPlayerId, JSONObject battingStatsJson, Integer year) {
		MLBBattingStats mbs = null;
		try {
			JSONObject team = new JSONObject(battingStatsJson.getString("team"));
			JSONObject stat = new JSONObject(battingStatsJson.getString("stat"));
			int cs = stat.getString("caughtStealing").length() == 0 ? 0 : Integer.parseInt(stat.getString("caughtStealing"));
			int hbp = stat.getString("hitByPitch").length() == 0 ? 0 : Integer.parseInt(stat.getString("hitByPitch"));
			int so = stat.getString("strikeOuts").length() == 0 ? 0 : Integer.parseInt(stat.getString("strikeOuts"));
			mbs = new MLBBattingStats(mlbPlayerId, Integer.parseInt(team.getString("id")), year,
				new BattingStats(Integer.parseInt(stat.getString("atBats")), Integer.parseInt(stat.getString("hits")), Integer.parseInt(stat.getString("doubles")), 
					Integer.parseInt(stat.getString("triples")), Integer.parseInt(stat.getString("homeRuns")), Integer.parseInt(stat.getString("baseOnBalls")), 
					so, hbp, Integer.parseInt(stat.getString("runs")), Integer.parseInt(stat.getString("rbi")), Integer.parseInt(stat.getString("stolenBases")), 
					Integer.parseInt(stat.getString("plateAppearances")), cs));
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
		return mbs;
	}
	
	static MLBPitchingStats createMLBPitchingStats(Integer mlbPlayerId, JSONObject pitchingStatsJson, Integer year) {
		MLBPitchingStats mps = null;
		try {
			JSONObject team = new JSONObject(pitchingStatsJson.getString("team"));
			JSONObject stat = new JSONObject(pitchingStatsJson.getString("stat"));
			int sb = stat.getString("stolenBases").length() == 0 ? 0 : Integer.parseInt(stat.getString("stolenBases"));
			int hld = stat.getString("holds").length() == 0 ? 0 : Integer.parseInt(stat.getString("holds"));
			int sf = stat.getString("sacFlies").length() == 0 ? 0 : Integer.parseInt(stat.getString("sacFlies"));
			int sv = stat.getString("saves").length() == 0 ? 0 : Integer.parseInt(stat.getString("saves"));
			int bs = stat.getString("blownSaves").length() == 0 ? 0 : Integer.parseInt(stat.getString("blownSaves"));
			mps = new MLBPitchingStats(mlbPlayerId, Integer.parseInt(team.getString("id")), year,
				new PitchingStats(Double.parseDouble(stat.getString("inningsPitched")), Integer.parseInt(stat.getString("earnedRuns")), Integer.parseInt(stat.getString("runs")), 
					Integer.parseInt(stat.getString("baseOnBalls")), Integer.parseInt(stat.getString("strikeOuts")), Integer.parseInt(stat.getString("homeRuns")), 
					sb, Integer.parseInt(stat.getString("hitByPitch")), Integer.parseInt(stat.getString("hits")), hld, sv, bs,
					Integer.parseInt(stat.getString("gamesStarted")), Integer.parseInt(stat.getString("balks")), Integer.parseInt(stat.getString("wildPitches")),
					sf, Integer.parseInt(stat.getString("battersFaced")), Integer.parseInt(stat.getString("wins")), Integer.parseInt(stat.getString("losses"))));
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
		return mps;
	}
	
	static MLBFieldingStats createMLBFieldingStats(Integer mlbPlayerId, JSONObject fieldingStatsJSON, Integer year) {
		MLBFieldingStats mbs = null;
		try {
			JSONObject team = new JSONObject(fieldingStatsJSON.getString("team"));
			JSONObject stat = new JSONObject(fieldingStatsJSON.getString("stat"));
			JSONObject position = new JSONObject(fieldingStatsJSON.getString("position"));
			mbs = new MLBFieldingStats(mlbPlayerId, Integer.parseInt(team.getString("id")), year, position.getString("abbreviation"), Integer.parseInt(stat.getString("gamesPlayed")),
				new FieldingStats(Integer.parseInt(stat.getString("assists")), Integer.parseInt(stat.getString("putOuts")), Integer.parseInt(stat.getString("errors"))));
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
		return mbs;
	}
	
	private static HashMap<String, MLBTeam> sortHashMapByName(HashMap<String, MLBTeam> mlbTeams) { 
        // Create a list from elements of HashMap 
        List<Map.Entry<String, MLBTeam>> list = new LinkedList<Map.Entry<String, MLBTeam>>(mlbTeams.entrySet()); 
        Collections.sort(list, new Comparator<Map.Entry<String, MLBTeam>>() { 
            public int compare(Map.Entry<String, MLBTeam> o1,  
                               Map.Entry<String, MLBTeam> o2) { 
            	return (o1.getValue().getFullTeamName().compareTo(o2.getValue().getFullTeamName())); 
            } 
        });  
        // put data from sorted list to hashmap  
        HashMap<String, MLBTeam> temp = new LinkedHashMap<String, MLBTeam>(); 
        for (Map.Entry<String, MLBTeam> aa : list) { 
            temp.put(aa.getKey(), aa.getValue()); 
        } 
        return temp; 
    }
	
	private static ArrayList<MLBTeam> getTeamsByYear(Integer year, String shortName, ArrayList<MLBTeam> allTeams) {
		ArrayList<MLBTeam> teamsByYear = new ArrayList<MLBTeam>();
		for (MLBTeam team : allTeams) {
			// Null last year means active in current year
			int lastYear = (team.getLastYearPlayed() == null || team.getLastYearPlayed() == 0) ? Calendar.getInstance().get(Calendar.YEAR) : team.getLastYearPlayed();
			if (team.getFirstYearPlayed() <= year && lastYear >= year) {
				if (shortName == null || (shortName != null && team.getShortTeamName().equalsIgnoreCase(shortName))) {
					teamsByYear.add(team);
				}
			}
		}
		return teamsByYear;
	}
	
	private static void importWorldSeries() {
		System.out.println("Import World Series");
		
		ArrayList<Object> importedWs = new ArrayList<Object>();
		String[] lineArray = {"", ""};
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader("C:\\Users\\cjaco\\Documents\\Sports\\BBSim\\WorldSeries_AllResults.txt"));
			String line = reader.readLine();
			while (line != null) {
				lineArray = line.split(":");
				if (lineArray[1].contains("NONE")) { // Skip years with no WS
					line = reader.readLine();
					continue;
				}
				int year = Integer.parseInt(lineArray[0].trim());
				String team1String = "";
				team1String = lineArray[1].substring(0, lineArray[1].indexOf('(')).trim();
				
				String team2String = lineArray[4].substring(0, lineArray[4].indexOf('(')).trim();
				MLBWorldSeries ws = new MLBWorldSeries(year, team1String, team2String, team1String);
				System.out.println(ws);
				importedWs.add(ws);
				line = reader.readLine();
			}
			DAO.createBatchDataFromList(importedWs);
		}
		catch (IOException e) {
			System.out.println("Import file not found.  Import failed!");
			closeFileReader(reader);
		}
		catch (NumberFormatException e) {
			System.out.println("Invalid year: " + lineArray[0]);
			closeFileReader(reader);
		}
		finally {
			closeFileReader(reader);
		}
	}
	
	private static void closeFileReader(BufferedReader reader) {
		try {
			if (reader != null) {
				reader.close();
			}
		}
		catch (IOException e) {	
		}
	}
}
