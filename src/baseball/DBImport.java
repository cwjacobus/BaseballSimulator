package baseball;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import dao.DAO;
import db.MLBBattingStats;
import db.MLBFranchise;
import db.MLBPlayer;
import db.MLBTeam;

public class DBImport {
	
	static int currentYear = Calendar.getInstance().get(Calendar.YEAR);

	public static void main(String[] args) {
		Integer year = null;
		Integer teamId = 0;
		boolean allTeams = false;
		boolean allYears = false;
		HashMap<?, ?> franchisesMap;
		
		if (args.length < 2) {
			System.out.println("INVALID ARGS");
			return;
		}
		DAO.setConnection();
		franchisesMap = DAO.getDataMap("MLB_FRANCHISE");
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
				teamId = (Integer)franchisesMap.get(args[2]);
				System.out.println(args[2] + " " + teamId);
			}
			else {
				allTeams = true;
			}
			HashMap<Integer, MLBPlayer> hittersMap = importMlbPlayers(year, allTeams, teamId, true, franchisesMap);
			HashMap<Integer, MLBPlayer> filteredHittersMap = new HashMap<Integer, MLBPlayer>();
			ArrayList<Object> battingStatsList = importBattingStats(hittersMap, year, filteredHittersMap);
			for (Map.Entry<Integer, MLBPlayer> entry : hittersMap.entrySet()) {
				System.out.println(entry.getValue().getMlbPlayerId() + " " + entry.getValue().getFirstLastName() + " " + entry.getValue().getPrimaryPosition() +
				    " " + entry.getValue().getArmThrows() + " " + entry.getValue().getBats() + " " + entry.getValue().getJerseyNumber());
			}
			DAO.createBatchDataFromMap(filteredHittersMap);
			DAO.createBatchDataFromList(battingStatsList);
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
						if (teamJson.getString("mlb_org_id").length() > 0) { // Filter out non-franchise teams
							if (allFranchisesMap.get(Integer.parseInt(teamJson.getString("mlb_org_id"))) == null) {
								allFranchisesMap.put(Integer.parseInt(teamJson.getString("mlb_org_id")), new MLBFranchise(Integer.parseInt(teamJson.getString("mlb_org_id")),
										teamJson.getString("mlb_org"), teamJson.getString("mlb_org_abbrev"), Integer.parseInt(teamJson.getString("first_year_of_play"))));
							}
							if (allTeamsMap.get((teamJson.getString("name_display_full") + ":" + teamJson.getString("mlb_org_id"))) == null) {
								MLBTeam t = new MLBTeam(Integer.parseInt(teamJson.getString("team_id")), Integer.parseInt(teamJson.getString("mlb_org_id")), 
								teamJson.getString("name_display_full"), teamJson.getString("name_abbrev"), teamJson.getString("league"));
								allTeamsMap.put((teamJson.getString("name_display_full") + ":" + teamJson.getString("mlb_org_id")), t);
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
		System.out.println("FRANCHISES");
		for (Map.Entry<Integer, MLBFranchise> entry : allFranchisesMap.entrySet()) {
			System.out.println(entry.getValue().getFullTeamName() + " " + entry.getValue().getMlbFranchiseId() + " " + entry.getValue().getShortTeamName() + " " + entry.getValue().getFirstYearPlayed());
		}
		System.out.println("TEAMS");
		for (Map.Entry<String, MLBTeam> entry : allTeamsMap.entrySet()) {
			System.out.println(entry.getValue().getFullTeamName() + " " + entry.getValue().getTeamId() + " " + entry.getValue().getShortTeamName() +
			    " " + entry.getValue().getLeague());
		}
		
		DAO.createBatchDataFromMap(allFranchisesMap);
		DAO.createBatchDataFromMap(allTeamsMap);
	}
	
	private static HashMap<Integer, MLBPlayer> importMlbPlayers(Integer year, boolean allTeams, Integer teamId, boolean hitters, HashMap<?, ?> franchisesMap) {
		HashMap<Integer, MLBPlayer> allPlayersMap = new HashMap<Integer, MLBPlayer>();
		for (Map.Entry<?, ?> entry : franchisesMap.entrySet()) {
			teamId = allTeams ? (Integer)entry.getValue() : teamId;
			try { 
				System.out.println("Import players from " + teamId);
				String getPlayersAPI = "http://lookup-service-prod.mlb.com/json/named.roster_team_alltime.bam?start_season=%27" + year + "%27" + 
				"&end_season=%27" + year + "%27" + "&team_id=%27" + teamId + "%27";
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
						if (playerJson.getString("primary_position").equals("P")) { // Skip pitchers
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
			if (!allTeams) {
				break;
			}
		}
		for (Map.Entry<Integer, MLBPlayer> entry : allPlayersMap.entrySet()) {
			System.out.println(entry.getValue().getMlbPlayerId() + " " + entry.getValue().getFirstLastName() + " " + entry.getValue().getPrimaryPosition() +
			    " " + entry.getValue().getArmThrows() + " " + entry.getValue().getBats() + " " + entry.getValue().getJerseyNumber());
		}
		return allPlayersMap;
	}
	
	private static ArrayList<Object> importBattingStats(HashMap<Integer, MLBPlayer> hiitersMap, int year, HashMap<Integer, MLBPlayer> filteredHittersMap) {
		ArrayList<Object> battingStatsList = new ArrayList<Object>();
		DAO.setConnection();
		try {   
			for (Map.Entry<Integer, MLBPlayer> entry : hiitersMap.entrySet()) {
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
								MLBBattingStats mbs = createMLBBattingStats(entry.getValue().getMlbPlayerId(), battingStatsJson, year);
								battingStatsList.add(mbs);
								filteredHittersMap.put(entry.getValue().getMlbPlayerId(), entry.getValue());
							}
						}
					}
					else {
						battingStatsJson = new JSONObject(queryResults.getString("row"));
						if (Integer.parseInt(battingStatsJson.getString("tpa")) > 100) { // only import if p app > 100
							MLBBattingStats mbs = createMLBBattingStats(entry.getValue().getMlbPlayerId(), battingStatsJson, year);
							battingStatsList.add(mbs);
							filteredHittersMap.put(entry.getValue().getMlbPlayerId(), entry.getValue());
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
		for (Object o : battingStatsList) {
			MLBBattingStats mbs = (MLBBattingStats) o;
			System.out.println(mbs.getMlbPlayerId() + " " + mbs.getMlbTeamId() + " " +  mbs.getYear() + " " +
				mbs.getBattingStats().getAtBats() + " " + mbs.getBattingStats().getPlateAppearances());
		}
		return battingStatsList;
	}
	
	static MLBBattingStats createMLBBattingStats(Integer mlbPlayerId, JSONObject battingStatsJson, Integer year) {
		MLBBattingStats mbs = null;
		try {
			mbs = new MLBBattingStats(mlbPlayerId, Integer.parseInt(battingStatsJson.getString("team_id")), year,
				new BattingStats(Integer.parseInt(battingStatsJson.getString("ab")), Integer.parseInt(battingStatsJson.getString("h")), Integer.parseInt(battingStatsJson.getString("d")), 
					Integer.parseInt(battingStatsJson.getString("t")), Integer.parseInt(battingStatsJson.getString("hr")), Integer.parseInt(battingStatsJson.getString("bb")), 
					Integer.parseInt(battingStatsJson.getString("so")), Integer.parseInt(battingStatsJson.getString("hbp")), Integer.parseInt(battingStatsJson.getString("r")), 
					Integer.parseInt(battingStatsJson.getString("rbi")), Integer.parseInt(battingStatsJson.getString("sb")), Integer.parseInt(battingStatsJson.getString("tpa")), 
					Integer.parseInt(battingStatsJson.getString("cs"))));
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
		return mbs;
	}

}
