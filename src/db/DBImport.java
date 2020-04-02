package db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DBImport {
	
	static int year = 2019;

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if (args!= null && args.length > 0) {
			year = Integer.parseInt(args[0]);
		}
		importTeams();
	}
	
	// Get player stats from API
	private static void importTeams() {
		try {    	
			String getTeamsAPI = "http://lookup-service-prod.mlb.com/json/named.team_all_season.bam?sport_code=%27mlb%27&&season=%27" + year +"%27";
		    URL obj = new URL(getTeamsAPI);
		    HttpURLConnection con = (HttpURLConnection)obj.openConnection();
		    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream())); 
		    try {
		    	JSONObject teams = new JSONObject(in.readLine());
		    	JSONObject searchAll = new JSONObject(teams.getString("team_all_season"));
		    	JSONObject queryResults = new JSONObject(searchAll.getString("queryResults"));
		    	JSONArray allTeams = new JSONArray(queryResults.getString("row"));
		    	for (int i = 0; i < allTeams.length(); i++) {
					JSONObject team = allTeams.getJSONObject(i);
					if (team.getString("mlb_org_id").length() > 0) {
						System.out.println(team.getString("name_display_full") + " " + team.getString("name_abbrev") + " " + team.getString("team_id"));
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

}
