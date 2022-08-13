package baseball;

import java.io.BufferedReader;
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

public class DBImport {
	
	static int currentYear = Calendar.getInstance().get(Calendar.YEAR);

	public static void main(String[] args) {
		Integer year = null;
		boolean allYears = false;
		ArrayList<MLBTeam> allTeamsList;
		ArrayList<MLBTeam> teamsForYearList;
		
		if (args.length < 2) {
			System.out.println("INVALID ARGS");
			return;
		}
		DAO.setConnection();
		allTeamsList = DAO.getAllTeamsList();
		String fn = args[0];
		if (!args[1].equalsIgnoreCase("ALL")) {
			year = Integer.parseInt(args[1]);
		}
		else {
			allYears = true;
		}
		if (fn.equals("TEAM")) {
			importFranchisesAndTeams(year, allYears);
		}
		else if (fn.equals("PLAYER")) {
			if (!args[2].equalsIgnoreCase("ALL")) {
				teamsForYearList = getTeamsByYear(year, args[2], allTeamsList);
			}
			else {
				teamsForYearList = getTeamsByYear(year, null, allTeamsList);
			}
			HashMap<Integer, MLBPlayer> hittersMap = importMlbPlayers(year, true, teamsForYearList); // import batters
			HashMap<Integer, MLBPlayer> filteredHittersMap = new HashMap<Integer, MLBPlayer>();
			HashMap<Integer, MLBPlayer> pitchersMap = importMlbPlayers(year, false, teamsForYearList); // import pitchers
			HashMap<Integer, MLBPlayer> filteredPitchersMap = new HashMap<Integer, MLBPlayer>();
			//HashMap<Integer, MLBFieldingStats> fieldingStatsMap = new HashMap<Integer, MLBFieldingStats>();
			ArrayList<Object> battingStatsList = importBattingStats(hittersMap, year, filteredHittersMap);
			ArrayList<Object> pitchingStatsList = importPitchingStats(pitchersMap, year, filteredPitchersMap);
			//importFieldingStats(hittersMap, year, fieldingStatsMap);  // hitters fielding
			//importFieldingStats(pitchersMap, year, fieldingStatsMap); // pitchers fielding
			DAO.createBatchDataFromMap(filteredHittersMap);
			DAO.createBatchDataFromMap(filteredPitchersMap);
			DAO.createBatchDataFromList(battingStatsList);
			DAO.createBatchDataFromList(pitchingStatsList);
			//DAO.createBatchDataFromMap(fieldingStatsMap);
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
				System.out.println("Import players from " + t.getTeamId());
				String getPlayersAPI = "http://lookup-service-prod.mlb.com/json/named.roster_team_alltime.bam?start_season=%27" + year + "%27" + 
				"&end_season=%27" + year + "%27" + "&team_id=%27" + t.getTeamId() + "%27";
				URL obj = new URL(getPlayersAPI);
				HttpURLConnection con = (HttpURLConnection)obj.openConnection();
				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream())); 
				try {
					JSONObject players = new JSONObject(in.readLine());
					JSONObject searchAll = new JSONObject(players.getString("roster_team_alltime"));
					JSONObject queryResults = new JSONObject(searchAll.getString("queryResults"));
					if (queryResults.getString("totalSize").equals("0")) {  // Skip if no players
						continue;
					}
					JSONArray allPlayers = new JSONArray(queryResults.getString("row"));
					for (int i = 0; i < allPlayers.length(); i++) {
						JSONObject playerJson = allPlayers.getJSONObject(i);
						if ((hitters && playerJson.getString("primary_position").equals("P")) ||
						   (!hitters && !playerJson.getString("primary_position").equals("P"))) { // Skip pitchers or hitters
								continue;
						}
						Integer playerId = Integer.parseInt(playerJson.getString("player_id"));
						Integer jerseyNumber = playerJson.getString("jersey_number").length() > 0 ? Integer.parseInt(playerJson.getString("jersey_number")) : null;
						MLBPlayer p = new MLBPlayer(playerId, playerJson.getString("name_last_first"), playerJson.getString("primary_position"), 
								playerJson.getString("throws"), playerJson.getString("bats"), jerseyNumber);
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
		for (Map.Entry<Integer, MLBPlayer> entry : allPlayersMap.entrySet()) {
			System.out.println(entry.getValue().getMlbPlayerId() + " " + entry.getValue().getFirstLastName() + " " + entry.getValue().getPrimaryPosition() +
			    " " + entry.getValue().getArmThrows() + " " + entry.getValue().getBats() + " " + entry.getValue().getJerseyNumber());
		}
		return allPlayersMap;
	}
	
	public static ArrayList<Object> importBattingStats(HashMap<Integer, MLBPlayer> hittersMap, int year, HashMap<Integer, MLBPlayer> filteredHittersMap) {
		ArrayList<Object> battingStatsList = new ArrayList<Object>();
		int index = 1;
		try {   
			for (Map.Entry<Integer, MLBPlayer> entry : hittersMap.entrySet()) {
				String getBattingStatsAPI = "http://lookup-service-prod.mlb.com/json/named.sport_hitting_tm.bam?league_list_id=%27mlb%27&game_type=%27R%27&season=%27" + 
					year + "%27" + "&player_id=%27" + entry.getValue().getMlbPlayerId() + "%27";
				URL obj = new URL(getBattingStatsAPI);
				HttpURLConnection con = (HttpURLConnection)obj.openConnection();
				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream())); 
				try {
					JSONObject playerStats = new JSONObject(in.readLine());
					JSONObject sportHittingTm = new JSONObject(playerStats.getString("sport_hitting_tm"));
					JSONObject queryResults = new JSONObject(sportHittingTm.getString("queryResults"));
					int numberOfResults = Integer.parseInt(queryResults.getString("totalSize"));
					if (numberOfResults == 0) {
						continue;
					}
					JSONObject battingStatsJson = null;
					if (numberOfResults > 1) {
						JSONArray multipleTeamStats = new JSONArray(queryResults.getString("row"));
						for (int i = 0; i < multipleTeamStats.length(); i++) {
							battingStatsJson = multipleTeamStats.getJSONObject(i);
							if (Integer.parseInt(battingStatsJson.getString("tpa")) > 100) { // only import if p app > 100
								System.out.print(index + " ");
								if (index % 50 == 0) {
									System.out.println();
								}
								MLBBattingStats mbs = createMLBBattingStats(entry.getValue().getMlbPlayerId(), battingStatsJson, year);
								battingStatsList.add(mbs);
								filteredHittersMap.put(entry.getValue().getMlbPlayerId(), entry.getValue());
								index++;
							}
						}
					}
					else {
						battingStatsJson = new JSONObject(queryResults.getString("row"));
						if (Integer.parseInt(battingStatsJson.getString("tpa")) > 100) { // only import if p app > 100
							System.out.print(index + " ");
							if (index % 50 == 0) {
								System.out.println();
							}
							MLBBattingStats mbs = createMLBBattingStats(entry.getValue().getMlbPlayerId(), battingStatsJson, year);
							battingStatsList.add(mbs);
							filteredHittersMap.put(entry.getValue().getMlbPlayerId(), entry.getValue());
							index++;
						}
					}
				}
				catch (JSONException e) {
					System.out.println(getBattingStatsAPI);
					e.printStackTrace();
				}
			}
		}
		catch (MalformedURLException e) { 	
			e.printStackTrace();
		}
		catch (IOException e) { 
			e.printStackTrace();
		}
		System.out.println("\nBatter stats import complete");
		return battingStatsList;
	}
	
	public static ArrayList<Object> importPitchingStats(HashMap<Integer, MLBPlayer> pitchersMap, int year, HashMap<Integer, MLBPlayer> filteredPitchersMap) {
		ArrayList<Object> pitchingStatsList = new ArrayList<Object>();
		int index = 1;
		try {   
			for (Map.Entry<Integer, MLBPlayer> entry : pitchersMap.entrySet()) {
				String getPitchingStatsAPI = "http://lookup-service-prod.mlb.com/json/named.sport_pitching_tm.bam?league_list_id=%27mlb%27&game_type=%27R%27&season=%27" + 
					year + "%27" + "&player_id=%27" + entry.getValue().getMlbPlayerId() + "%27";
				URL obj = new URL(getPitchingStatsAPI);
				HttpURLConnection con = (HttpURLConnection)obj.openConnection();
				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream())); 
				try {
					JSONObject playerStats = new JSONObject(in.readLine());
					JSONObject sportHittingTm = new JSONObject(playerStats.getString("sport_pitching_tm"));
					JSONObject queryResults = new JSONObject(sportHittingTm.getString("queryResults"));
					int numberOfResults = Integer.parseInt(queryResults.getString("totalSize"));
					if (numberOfResults == 0) {
						continue;
					}
					JSONObject pitchingStatsJson = null;
					if (numberOfResults > 1) {
						JSONArray multipleTeamStats = new JSONArray(queryResults.getString("row"));
						for (int i = 0; i < multipleTeamStats.length(); i++) {
							pitchingStatsJson = multipleTeamStats.getJSONObject(i);
							if (Integer.parseInt(pitchingStatsJson.getString("g")) > 20) { // only import if games > 20
								System.out.print(index + " ");
								if (index % 50 == 0) {
									System.out.println();
								}
								MLBPitchingStats mps = createMLBPitchingStats(entry.getValue().getMlbPlayerId(), pitchingStatsJson, year);
								pitchingStatsList.add(mps);
								filteredPitchersMap.put(entry.getValue().getMlbPlayerId(), entry.getValue());
								index++;
							}
						}
					}
					else {
						pitchingStatsJson = new JSONObject(queryResults.getString("row"));
						if (Integer.parseInt(pitchingStatsJson.getString("g")) > 20) { // only import if games > 20
							System.out.print(index + " ");
							if (index % 50 == 0) {
								System.out.println();
							}
							MLBPitchingStats mps = createMLBPitchingStats(entry.getValue().getMlbPlayerId(), pitchingStatsJson, year);
							pitchingStatsList.add(mps);
							filteredPitchersMap.put(entry.getValue().getMlbPlayerId(), entry.getValue());
							index++;
						}
					}
				}
				catch (JSONException e) {
					System.out.println(getPitchingStatsAPI);
					e.printStackTrace();
				}
			}
		}
		catch (MalformedURLException e) { 	
			e.printStackTrace();
		}
		catch (IOException e) { 
			e.printStackTrace();
		}
		System.out.println("\nPitcher stats import complete");
		return pitchingStatsList;
	}
	
	/*
	public static void importFieldingStats(HashMap<Integer, MLBPlayer> playersMap, int year, HashMap<Integer, MLBFieldingStats> fieldingStatsMap) {
		int index = 1;
		try {   
			for (Map.Entry<Integer, MLBPlayer> entry : playersMap.entrySet()) {
				String getFieldingStatsAPI = "http://lookup-service-prod.mlb.com/json/named.sport_fielding_tm.bam?league_list_id=%27mlb%27&game_type=%27R%27&season=%27" + 
					year + "%27" + "&player_id=%27" + entry.getValue().getMlbPlayerId() + "%27";
				URL obj = new URL(getFieldingStatsAPI);
				HttpURLConnection con = (HttpURLConnection)obj.openConnection();
				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream())); 
				try {
					JSONObject playerStats = new JSONObject(in.readLine());
					JSONObject sportHittingTm = new JSONObject(playerStats.getString("sport_fielding_tm"));
					JSONObject queryResults = new JSONObject(sportHittingTm.getString("queryResults"));
					int numberOfResults = Integer.parseInt(queryResults.getString("totalSize"));
					if (numberOfResults == 0) {
						continue;
					}
					JSONObject fieldingStatsJson = null;
					if (numberOfResults > 1) {
						JSONArray multipleTeamStats = new JSONArray(queryResults.getString("row"));
						for (int i = 0; i < multipleTeamStats.length(); i++) {
							fieldingStatsJson = multipleTeamStats.getJSONObject(i);
							if (Integer.parseInt(fieldingStatsJson.getString("g")) > 20) { // only import if games > 20
								System.out.print(index + " ");
								if (index % 50 == 0) {
									System.out.println();
								}
								MLBFieldingStats mbs = createMLBFieldingStats(entry.getValue().getMlbPlayerId(), fieldingStatsJson, year);
								fieldingStatsMap.put(entry.getValue().getMlbPlayerId(), mbs);
								index++;
							}
						}
					}
					else {
						fieldingStatsJson = new JSONObject(queryResults.getString("row"));
						if (Integer.parseInt(fieldingStatsJson.getString("g")) > 20) { // only import if games > 20
							System.out.print(index + " ");
							if (index % 50 == 0) {
								System.out.println();
							}
							MLBFieldingStats mbs = createMLBFieldingStats(entry.getValue().getMlbPlayerId(), fieldingStatsJson, year);
							fieldingStatsMap.put(entry.getValue().getMlbPlayerId(), mbs);
							index++;
						}
					}
				}
				catch (JSONException e) {
					System.out.println(getFieldingStatsAPI);
					e.printStackTrace();
				}
			}
		}
		catch (MalformedURLException e) { 	
			e.printStackTrace();
		}
		catch (IOException e) { 
			e.printStackTrace();
		}
		for (Map.Entry<Integer, MLBFieldingStats> entry : fieldingStatsMap.entrySet()) {
			MLBFieldingStats mbs = entry.getValue();
			System.out.println(mbs.getMlbPlayerId() + " " + mbs.getMlbTeamId() + " " +  mbs.getYear() + " " +
				mbs.getFieldingStats().getAssists() + " " + mbs.getFieldingStats().getPutOuts() + " " + mbs.getFieldingStats().getErrors());
		}
		System.out.println("\nFielder import complete");
	}*/
	
	static MLBBattingStats createMLBBattingStats(Integer mlbPlayerId, JSONObject battingStatsJson, Integer year) {
		MLBBattingStats mbs = null;
		try {
			int cs = battingStatsJson.getString("cs").length() == 0 ? 0 : Integer.parseInt(battingStatsJson.getString("cs"));
			int hbp = battingStatsJson.getString("hbp").length() == 0 ? 0 : Integer.parseInt(battingStatsJson.getString("hbp"));
			int so = battingStatsJson.getString("so").length() == 0 ? 0 : Integer.parseInt(battingStatsJson.getString("so"));
			mbs = new MLBBattingStats(mlbPlayerId, Integer.parseInt(battingStatsJson.getString("team_id")), year,
				new BattingStats(Integer.parseInt(battingStatsJson.getString("ab")), Integer.parseInt(battingStatsJson.getString("h")), Integer.parseInt(battingStatsJson.getString("d")), 
					Integer.parseInt(battingStatsJson.getString("t")), Integer.parseInt(battingStatsJson.getString("hr")), Integer.parseInt(battingStatsJson.getString("bb")), 
					so, hbp, Integer.parseInt(battingStatsJson.getString("r")), Integer.parseInt(battingStatsJson.getString("rbi")), Integer.parseInt(battingStatsJson.getString("sb")), 
					Integer.parseInt(battingStatsJson.getString("tpa")), cs));
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
		return mbs;
	}
	
	static MLBPitchingStats createMLBPitchingStats(Integer mlbPlayerId, JSONObject pitchingStatsJson, Integer year) {
		MLBPitchingStats mps = null;
		try {
			int sb = pitchingStatsJson.getString("sb").length() == 0 ? 0 : Integer.parseInt(pitchingStatsJson.getString("sb"));
			int hld = pitchingStatsJson.getString("hld").length() == 0 ? 0 : Integer.parseInt(pitchingStatsJson.getString("hld"));
			int sf = pitchingStatsJson.getString("sf").length() == 0 ? 0 : Integer.parseInt(pitchingStatsJson.getString("sf"));
			int sv = pitchingStatsJson.getString("sv").length() == 0 ? 0 : Integer.parseInt(pitchingStatsJson.getString("sv"));
			int svo = pitchingStatsJson.getString("svo").length() == 0 ? 0 : Integer.parseInt(pitchingStatsJson.getString("svo"));
			mps = new MLBPitchingStats(mlbPlayerId, Integer.parseInt(pitchingStatsJson.getString("team_id")), year,
				new PitchingStats(Double.parseDouble(pitchingStatsJson.getString("ip")), Integer.parseInt(pitchingStatsJson.getString("er")), Integer.parseInt(pitchingStatsJson.getString("r")), 
					Integer.parseInt(pitchingStatsJson.getString("bb")), Integer.parseInt(pitchingStatsJson.getString("so")), Integer.parseInt(pitchingStatsJson.getString("hr")), 
					sb, Integer.parseInt(pitchingStatsJson.getString("hb")), Integer.parseInt(pitchingStatsJson.getString("h")), hld, sv, (svo == 0 ? 0 : svo - sv),
					Integer.parseInt(pitchingStatsJson.getString("gs")), Integer.parseInt(pitchingStatsJson.getString("bk")), Integer.parseInt(pitchingStatsJson.getString("wp")),
					sf, Integer.parseInt(pitchingStatsJson.getString("tbf")), Integer.parseInt(pitchingStatsJson.getString("w")), Integer.parseInt(pitchingStatsJson.getString("l"))));
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
		return mps;
	}
	
	static MLBFieldingStats createMLBFieldingStats(Integer mlbPlayerId, JSONObject fieldingStatsJson, Integer year) {
		MLBFieldingStats mbs = null;
		try {
			mbs = new MLBFieldingStats(mlbPlayerId, Integer.parseInt(fieldingStatsJson.getString("team_id")), year, fieldingStatsJson.getString("position_txt"),
				new FieldingStats(Integer.parseInt(fieldingStatsJson.getString("a")), Integer.parseInt(fieldingStatsJson.getString("po")), Integer.parseInt(fieldingStatsJson.getString("e"))));
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
}
