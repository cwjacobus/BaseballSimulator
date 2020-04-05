package db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import baseball.BattingStats;
import dao.DAO;

public class DBImport {
	
	
	static int currentYear = Calendar.getInstance().get(Calendar.YEAR);

	public static void main(String[] args) {
		if (args.length == 0) {
			System.out.println("INVALID ARGS");
			return;
		}
		String fn = args[0];
		String year = args[1];
		Integer teamId = 0;
		if (fn.equals("PLAYER")) {
			teamId = Integer.parseInt(args[2]);
		}
		boolean allYears = fn.equals("PLAYER") && year.equals("ALL");
		if (fn.equals("TEAM")) {
			importFranchisesAndTeams(allYears ? currentYear: Integer.parseInt(year), allYears);
		}
		else if (fn.equals("PLAYER")) {
			HashMap<Integer, MLBPlayer> hittersMap = importMlbPlayers(Integer.parseInt(year), teamId, true);
			importBattingStats(hittersMap, Integer.parseInt(year));
		}
		else {
			System.out.println("INVALID FUNCTION");
		}
	}
	
	// Get player stats from API
	private static void importFranchisesAndTeams(int year, boolean allYears) {
		HashMap<String, MLBTeam> allTeamsMap = new HashMap<String, MLBTeam>();
		HashMap<Integer, MLBFranchise> allFranchisesMap = new HashMap<Integer, MLBFranchise>();
		int beginYear = allYears ? 1900 : year;
		DAO.setConnection();
		for (int y = beginYear; y <= year; y++) {
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
		
		DAO.createBatchMlbFranchises(allFranchisesMap);
		DAO.createBatchTeams(allTeamsMap);
	}
	
	private static HashMap<Integer, MLBPlayer> importMlbPlayers(int year, int teamId, boolean hitters) {
		HashMap<Integer, MLBPlayer> allPlayersMap = new HashMap<Integer, MLBPlayer>();
		HashMap<Integer, TeamPlayer> allTeamPlayersMap = new HashMap<Integer, TeamPlayer>();
		DAO.setConnection();
		try {    	
			String getPlayersAPI = "http://lookup-service-prod.mlb.com/json/named.roster_team_alltime.bam?start_season=%27" + year + "%27" + 
				"&end_season=%27" + year + "%27" + "&team_id=%27" + teamId + "%27";
			URL obj = new URL(getPlayersAPI);
			HttpURLConnection con = (HttpURLConnection)obj.openConnection();
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream())); 
			try {
				JSONObject players = new JSONObject(in.readLine());
				JSONObject searchAll = new JSONObject(players.getString("roster_team_alltime"));
				JSONObject queryResults = new JSONObject(searchAll.getString("queryResults"));
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
					allTeamPlayersMap.put(playerId, new TeamPlayer(teamId, playerId, year));
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
		
		for (Map.Entry<Integer, MLBPlayer> entry : allPlayersMap.entrySet()) {
			System.out.println(entry.getValue().getMlbPlayerId() + " " + entry.getValue().getFirstLastName() + " " + entry.getValue().getPrimaryPosition() +
			    " " + entry.getValue().getArmThrows() + " " + entry.getValue().getBats() + " " + entry.getValue().getJerseyNumber());
		}
		
		//DAO.createBatchMlbPlayers(allPlayersMap);
		//DAO.createBatchTeamPlayers(allTeamPlayersMap);
		return allPlayersMap;
	}
	
	private static void importBattingStats(HashMap<Integer, MLBPlayer> playersMap, int year) {
		HashMap<Integer, MLBBattingStats> battingStatsMap = new HashMap<Integer, MLBBattingStats>();
		DAO.setConnection();
		try {   
			for (Map.Entry<Integer, MLBPlayer> entry : playersMap.entrySet()) {
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
					else if (numberOfResults > 1) {
						// TBD handle multiple stats (player played with multiple teams)
						continue;
					}
					JSONObject battingStatsJson = new JSONObject(queryResults.getString("row"));
					if (Integer.parseInt(battingStatsJson.getString("ab")) > 50) { // only import if abs greater than 50
						MLBBattingStats bs = new MLBBattingStats(entry.getValue().getMlbPlayerId(), new BattingStats(Integer.parseInt(battingStatsJson.getString("ab")), 
							0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
						battingStatsMap.put(entry.getValue().getMlbPlayerId(), bs);
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
		
		for (Map.Entry<Integer, MLBBattingStats> entry : battingStatsMap.entrySet()) {
			System.out.println(entry.getValue().getMlbPlayerId() + " " + entry.getValue().getBattingStats().getAtBats());
		}
		//DAO.createBatchMlbBattingStats(allFranchisesMap);
	}

}
