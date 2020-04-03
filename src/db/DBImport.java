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

import dao.DAO;

public class DBImport {
	
	
	static int currentYear = Calendar.getInstance().get(Calendar.YEAR);

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		int defaultYear = 2019;
		boolean allYears = false;
		if (args!= null && args.length > 0) {
			if (args[0].equalsIgnoreCase("ALL")) {
				allYears = true;
			}
			else {
				defaultYear = Integer.parseInt(args[0]);
			}
		}
		if (!allYears) {
			HashMap<String, Team> teams = importTeams(defaultYear, false);
			
			DAO.createBatchTeams(teams);
		}
		else {
			HashMap<String, Team> allTeams = importTeams(currentYear, true);
			HashMap<String, Team> currentTeams = importTeams(currentYear, false);
			
			for (Map.Entry<String, Team> entry : allTeams.entrySet()) {
				Team t = entry.getValue();
				if (currentTeams.get(t.getTeamId() + ":" +t.getShortTeamName()) != null) {
					t.setActive(true);
					allTeams.put(t.getTeamId() + ":" +t.getShortTeamName(), t);
				}
			}
			
			DAO.createBatchTeams(allTeams);
		}
	}
	
	// Get player stats from API
	private static HashMap<String, Team> importTeams(int year, boolean allYears) {
		HashMap<String, Team> allTeamsMap = new HashMap<String, Team>();
		DAO.setConnection();
		int beginYear = allYears ? 1900 : year;
		for (int y = beginYear; y <= year; y++) {
			try {    	
				String getTeamsAPI = "http://lookup-service-prod.mlb.com/json/named.team_all_season.bam?sport_code=%27mlb%27&&season=%27" + y +"%27";
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
							if (allTeamsMap.get((teamJson.getString("team_id") + ":" + teamJson.getString("name_abbrev"))) == null) {
								Team t = new Team(Integer.parseInt(teamJson.getString("team_id")), teamJson.getString("name_display_full"), teamJson.getString("name_abbrev"), teamJson.getString("league"),
									Integer.parseInt(teamJson.getString("first_year_of_play")), false);
								allTeamsMap.put((teamJson.getString("team_id") + ":" + teamJson.getString("name_abbrev")), t);
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
		for (Map.Entry<String, Team> entry : allTeamsMap.entrySet()) {
			System.out.println(entry.getValue().getFullTeamName() + " " + entry.getValue().getTeamId() + " " + entry.getValue().getShortTeamName() +
			    " " + entry.getValue().getLeague() + " " + entry.getValue().getFirstYearPlayed());
		}
		/*
		for (Team t : allTeamsList) {
			System.out.println(t.getFullTeamName());
		}*/
		
		/*Map<Integer, Team> getTeamsMap = DAO.getTeamsMap();
		for (Map.Entry<Integer, Team> entry : getTeamsMap.entrySet()) {
		    System.out.println(entry.getValue().getFullTeamName() + " " + entry.getValue().getTeamId() + " " + entry.getValue().getShortTeamName() +
		    	" " + entry.getValue().getLeague() + " " + entry.getValue().getFirstYearPlayed());
		}*/
		return allTeamsMap;
	}

}
