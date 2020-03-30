package baseball;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

public class BaseballSimulator {
	
	public static final int OUTS_PER_INNING = 3;
	public static final int INNINGS_PER_GAME = 9;
	
	static Game game = new Game();
	static int inning = 1;
	static int top = 0;
	static int currentBasesSituation = 0;
	static int[] runnersOnBase = {0, 0, 0};
	static int[] battingOrder = {1, 1};
	static HashMap<Integer, BattingStats> playerStatsMap = new HashMap<Integer, BattingStats>();
	static int year = 2019;
	
	static Map<Integer, String> baseSituations  = new HashMap<Integer, String>() {
		private static final long serialVersionUID = 1L;
	{
	    put(0, "BASES EMPTY");
	    put(1, "MAN ON FIRST");
	    put(2, "MAN ON SECOND");
	    put(3, "MAN ON FIRST AND SECOND");
	    put(4, "MAN ON THIRD");
	    put(5, "MAN ON FIRST AND THIRD");
	    put(6, "MAN ON SECOND AND THIRD");
	    put(7, "BASES LOADED");
	}};
	
	static Map<Integer, String> positions  = new HashMap<Integer, String>() {
		private static final long serialVersionUID = 1L;
	{
	    put(1, "PITCHER");
	    put(2, "CATCHER");
	    put(3, "FIRST BASE");
	    put(4, "SECOND BASE");
	    put(5, "THIRD BASE");
	    put(6, "SHORTSTOP");
	    put(7, "LEFT FIELD");
	    put(8, "CENTER FIELD");
	    put(9, "RIGHT FIELD");
	}};
	
	static Map<Integer, String> outTypes  = new HashMap<Integer, String>() {
		private static final long serialVersionUID = 1L;
	{
	    put(1, "STRUCK OUT");
	    put(2, "GROUNDED OUT");
	    put(3, "FLEW OUT");
	    put(4, "LINED OUT");
	    put(5, "POPPED OUT");
	}};

	public static void main(String[] args) {
		if (args!= null && args.length > 0) {
			year = Integer.parseInt(args[0]);
		}
		System.out.println("START GAME FOR " + year);
		game.setLineup(setLineup());
		String[] teamNames = {"Houston Astros", "New York Yankees"};
		game.setTeamNames(teamNames);
		getPlayerStats();
		while (inning <= INNINGS_PER_GAME || game.getScore(inning)[0] == game.getScore(inning)[1]) {
			for (top = 0; top < 2; top++) {
				System.out.println((top == 0 ? "\n***TOP " : "***BOTTOM ") + " INN: " + inning + " ***");
				int outs  = 0;
				boolean gameTiedStartOfAB;
				currentBasesSituation = 0;
				Arrays.fill(runnersOnBase, 0);
				while (outs < OUTS_PER_INNING) {
					System.out.println(game.getLineup()[top][battingOrder[top] - 1].getName() + " UP OUTS: " + outs + " " 
						+ baseSituations.get(currentBasesSituation) + " " + runnersOnBase[0] + " " + runnersOnBase[1] + " " + runnersOnBase[2]);
					BattingStats currentBatterGameStats = game.getLineup()[top][battingOrder[top] - 1].getGameStats();
					int rando = getRandomNumberInRange(1, 1000);
					
					gameTiedStartOfAB = game.getScore(inning)[1] == game.getScore(inning)[0] ? true : false;
					if (rando <= 680) {
						outs += getOutResult(currentBatterGameStats, outs);
						currentBatterGameStats.incrementAtBats();
					}
					else {
						if (rando > 680 && rando <= 775) {
							if (rando >= 760) {
								System.out.println("HIT BY PITCH");
							}
							else {
								System.out.println("WALKED");
								currentBatterGameStats.incrementWalks();
							}
							setBasesSituationWalk();
						}
						else {
							int noOutResult = getNotOutResult(currentBatterGameStats);
							setRunsScored(noOutResult);
							setCurrentBasesSituation(noOutResult);
							game.incrementHits(top);
							currentBatterGameStats.incrementHits();
							currentBatterGameStats.incrementAtBats();
						}
						game.getLineup()[top][battingOrder[top] - 1].setGameStats(currentBatterGameStats);
						if (inning >= 9 && game.getScore(inning)[1] > game.getScore(inning)[0] && gameTiedStartOfAB) {
							game.setWalkOff(true);
							System.out.println("WALKOFF ");
							break;
						}
					}
					battingOrder[top] = battingOrder[top] == 9 ? 1 : battingOrder[top] + 1;
				} // outs
				// Did game end after top of inning?
				if (inning >= 9 && game.getScore(inning)[1] > game.getScore(inning)[0] && top == 0) {
					System.out.println("GAME OVER AFTER " + (inning - 1) + " 1/2");
					break;
				}
			}
			inning++;
		}
		// Were there extra innings?
		if (inning > (INNINGS_PER_GAME + 1)) {
			System.out.println("EXTRA INNINGS: " + (inning - 1));
		}
		// Output Box Score
		 outputBoxScore();
	}
	
	private static int getNotOutResult(BattingStats currentBatterGameStats) {
		int notOutResult = 1;
		int notOutRando = getRandomNumberInRange(1, 1000);
		if (notOutRando > 1 && notOutRando <= 25) {
			System.out.println("REACHED BY ERROR");
			notOutResult = 1;
			game.incrementErrors(top == 0 ? 1 : 0);
		}
		else if (notOutRando > 25 && notOutRando <= 160) {
			System.out.println("HOME RUN");
			notOutResult = 4;
			currentBatterGameStats.incrementHomeRuns();
		}
		else if (notOutRando > 170 && notOutRando <= 200) {
			System.out.println("TRIPLE");
			notOutResult = 3;
			currentBatterGameStats.incrementTriples();
		}
		else if (notOutRando > 210 && notOutRando < 500) {
			System.out.println("DOUBLE");
			notOutResult = 2;
			currentBatterGameStats.incrementDoubles();
		}
		else {
			System.out.println("SINGLE");
		}
		
		return notOutResult;
	}
	
	private static int getOutResult(BattingStats currentBatterGameStats, int outs) {
		int outsRecorded = 1;
		int notOutRando = getRandomNumberInRange(1, 100);
		if (notOutRando > 1 && notOutRando <= 20) {
			System.out.println(outTypes.get(1)); // STRUCK OUT
			currentBatterGameStats.incremenStrikeOuts();
		}
		else if (notOutRando > 20 && notOutRando <= 50) {
			System.out.println(outTypes.get(2) + " TO " + positions.get(getRandomNumberInRange(1, 6))); //GROUNDED OUT
			if (doublePlay(true, outs)) {
				outsRecorded++;
			}
		}
		else if (notOutRando > 50 && notOutRando <= 80) {
			System.out.println(outTypes.get(3) + " TO " + positions.get(getRandomNumberInRange(7, 9))); // FLEW OUT
		}
		else if (notOutRando > 80 && notOutRando < 90) {
			System.out.println(outTypes.get(4) + " TO " + positions.get(getRandomNumberInRange(1, 9, 2))); // LINED OUT
		}
		else {
			System.out.println(outTypes.get(5) + " TO " + positions.get(getRandomNumberInRange(1, 6))); // POPPED OUT
		}
		return outsRecorded;
	}
	
	private static void setRunsScored(int event) {
		int runsScored = 0;
		if ((currentBasesSituation&4) == 4) { // man on third scored
   			runsScored++;
   		}
		if (event > 1 && (currentBasesSituation&2) == 2) { // man on second scored
			runsScored++;
		}
		if (event > 2 && (currentBasesSituation&1) == 1) { // man on first scored
			runsScored++;
		}
		if (event == 4) { // batter scored (home run)
			runsScored++; 
		}
		game.setBoxScore(top, inning, runsScored);
		if (runsScored > 0) {
			System.out.println(runsScored + " RUNS SCORED - VIS: " + game.getScore(inning)[0]  + " HOME: " + game.getScore(inning)[1]);
		}
	}
	
	private static void setCurrentBasesSituation(int event) {
		int basesSituation = (currentBasesSituation << event) + (int)Math.pow(2, (event - 1));
		if (basesSituation > 7) {
			basesSituation = basesSituation % 8;
		}
		currentBasesSituation = basesSituation;
		
		for (int x = 0; x < event; x++) {
			for (int y = 2; y >= 0; y--) {
				if (y > 0) {
					runnersOnBase[y] = runnersOnBase[y-1];
				}
				else if (y == 0 && x == 0) {
					runnersOnBase[y] = game.getLineup()[top][battingOrder[top] - 1].getId();
				}
				else {
					runnersOnBase[0] = 0;
				}
				/*if (player on third id != 0) {
					player on third.incrmentRuns;
					runsScored++;
				}*/
			}
			/*game.setBoxScore(top, inning, runsScored);
			if (runsScored > 0) {
				System.out.println(runsScored + " RUNS SCORED - VIS: " + game.getScore(inning)[0]  + " HOME: " + game.getScore(inning)[1]);
			}*/
		}
	}
	
	private static void setBasesSituationWalk() {
		int basesSituation = 7; // Bases loaded is default for loaded, 2+3, 1+3 or 1+2
		if (baseSituations.get(currentBasesSituation).equals("BASES EMPTY")) {
			basesSituation = 1; // 1
		}
		else if (baseSituations.get(currentBasesSituation).equals("MAN ON FIRST") || baseSituations.get(currentBasesSituation).equals("MAN ON SECOND")) {
			basesSituation = 3; // 1+2
		}
		else if (baseSituations.get(currentBasesSituation).equals("MAN ON THIRD")) {
			basesSituation = 5; // 1+3
		}
		else if (baseSituations.get(currentBasesSituation).equals("BASES LOADED")) {
			game.setBoxScore(top, inning, 1); // run scores
			System.out.println("RUN SCORES - VIS: " + game.getScore(inning)[0]  + " HOME: " + game.getScore(inning)[1]);
		}
		currentBasesSituation = basesSituation;
		
		// No need for checking empty, 2, 3, or 23
		if ((runnersOnBase[0] != 0 && runnersOnBase[1] != 0 && runnersOnBase[2] != 0) || // 123
			(runnersOnBase[0] != 0 && runnersOnBase[1] != 0 && runnersOnBase[2] == 0)) { // 12
			// if 123 runner 3 scores
			runnersOnBase[2] = runnersOnBase[1]; // runner 2->3
		}
		if (runnersOnBase[0] != 0) { // Runner on first
			runnersOnBase[1] = runnersOnBase[0]; // runner 1->2
		}
		runnersOnBase[0] = game.getLineup()[top][battingOrder[top] - 1].getId();
		
	}
	
	private static void setBasesSituationDoublePlay() {
		int basesSituation = 0; // Covers bases loaded and 1 and 13
		boolean runScores = baseSituations.get(currentBasesSituation).equals("BASES LOADED") || baseSituations.get(currentBasesSituation).equals("MAN ON FIRST AND THIRD");
		if (baseSituations.get(currentBasesSituation).equals("MAN ON FIRST AND SECOND") || baseSituations.get(currentBasesSituation).equals("BASES LOADED")) {
			basesSituation = 4;
		}
		if (runScores) {
			game.setBoxScore(top, inning, 1); // run scores
			System.out.println("RUN SCORES - VIS: " + game.getScore(inning)[0]  + " HOME: " + game.getScore(inning)[1]);
		}
		currentBasesSituation = basesSituation;
	}
	
	private static boolean doublePlay(boolean ground, int outs) {
		boolean dp = false;
		int dpRando = getRandomNumberInRange(1, 4);
		
		//TBD factor in speed of runner
		// Ground ball, less than 2 outs, man on first
		if (ground && outs != 2 && dpRando > 1 && (currentBasesSituation&1) == 1) {
			dp = true;
			setBasesSituationDoublePlay();
			System.out.println("DOUBLE PLAY");
		}
		return dp;
	}
	
	private static int getRandomNumberInRange(int min, int max) {
		if (min >= max) {
			throw new IllegalArgumentException("max must be greater than min");
		}
		return (int)(Math.random() * ((max - min) + 1)) + min;
	}
	
	private static int getRandomNumberInRange(int min, int max, int excluding) {
		if (min >= max) {
			throw new IllegalArgumentException("max must be greater than min");
		}
		int rando = (int)(Math.random() * ((max - min) + 1)) + min;
		if (rando == excluding) {
			rando = getRandomNumberInRange(min, max, excluding);
		}
		return rando;
	}
	
	private static Player[][] setLineup() {
		Player[][] lineup = new Player[2][Game.NUM_OF_PLAYERS_IN_LINEUP];
		
		for (int t = 0 ; t < 2; t++) {
			for (int p = 0 ; p < Game.NUM_OF_PLAYERS_IN_LINEUP; p++) {
				lineup[t][p] = new Player();
			}
		}
		
		lineup[0][0].setName("Jose Altuve");
		lineup[0][1].setName("Michael Brantley");
		lineup[0][2].setName("Alex Bregman");
		lineup[0][3].setName("George Springer");
		lineup[0][4].setName("Yuli Gurriel");
		lineup[0][5].setName("Carlos Correa");
		lineup[0][6].setName("Josh Reddick");
		lineup[0][7].setName("Yordan Alvarez");
		lineup[0][8].setName("Robinson Chirinos");
		
		lineup[1][0].setName("Brett Gardner");
		lineup[1][1].setName("Didi Gregorius");
		lineup[1][2].setName("Giancarlo Stanton");
		lineup[1][3].setName("Aaron Judge");
		lineup[1][4].setName("Gleyber Torres");
		lineup[1][5].setName("Gary Sanchez");
		lineup[1][6].setName("Miguel Andujar");
		lineup[1][7].setName("Luke Voit");
		lineup[1][8].setName("Mike Tauchman");
		
		return lineup;
	}
	
	private static void outputBoxScore() {
		for (int top = 0; top < 2; top++) {
			String team = (top == 0) ? "\nVis:  " : "Home: ";
			System.out.print(team + " ");
			for (int i = 1; i < inning; i++) {
				if (i == (inning - 1) && (game.getScore(i)[1] > game.getScore(i)[0] && top == 1) && !game.isWalkOff()) {
					System.out.print("X "); // Bottom of inning was not necessary
				}
				else {
					System.out.print(game.getBoxScore(top, i-1) + (game.getBoxScore(top == 0 ? 1: 0, i-1) < 10 ? " " : "  "));
				}
			}
			System.out.println(" " + game.getScore(inning)[top] + (game.getScore(inning)[top] < 10 ? " " : "") + " " + game.getHits()[top] + 
				(game.getHits()[top] < 10 ? " " : "") + " " +  game.getErrors()[top]);
		}
		for (int top = 0; top < 2; top++) {
			System.out.println();
			System.out.println(game.getTeamNames()[top]);
			System.out.println("HITTERS\t\t\t" + "AB  R   H   RBI BB  K    AVG  OBP  SLG");
			for (int p = 0; p < Game.NUM_OF_PLAYERS_IN_LINEUP; p++) {
				Player player = game.getLineup()[top][p];
				BattingStats gameStats = player.getGameStats();
				BattingStats playerSeasonStats = playerStatsMap.get(player.getId());
				System.out.print(player.getName());
				System.out.print("\t");
				if (player.getName().length() < 16) {
					System.out.print("\t");
				}
				System.out.print(gameStats.getAtBats() + (gameStats.getAtBats() > 9 ? "  " : "   "));
				System.out.print(gameStats.getRuns() + (gameStats.getRuns() > 9 ? "  " : "   "));
				System.out.print(gameStats.getHits() + (gameStats.getHits() > 9 ? "  " : "   "));
				System.out.print(gameStats.getRbis() + (gameStats.getRbis() > 9 ? "  " : "   "));
				System.out.print(gameStats.getWalks() + (gameStats.getWalks() > 9 ? "  " : "   "));
				System.out.print(gameStats.getStrikeOuts() + (gameStats.getStrikeOuts() > 9 ? "  " : "   "));
				DecimalFormat df = new DecimalFormat(".000");
				String roundAvgString = ".000";
				String roundOBPString = ".000";
				String roundSlgString = ".000";
				if (playerSeasonStats != null) {
					double avg = playerSeasonStats.getAtBats() > 0 ? (double)playerSeasonStats.getHits()/playerSeasonStats.getAtBats() : 0.0;
					roundAvgString = df.format((double) Math.round(avg * 1000) / 1000);
					double oBP = playerSeasonStats.getPlateAppearances() > 0 ? (double)(playerSeasonStats.getHits() + playerSeasonStats.getWalks() + playerSeasonStats.getHitByPitch())
						/ playerSeasonStats.getPlateAppearances() : 0.0;
					roundOBPString = df.format((double) Math.round(oBP * 1000) / 1000);
					double slg = playerSeasonStats.getAtBats() > 0 ? (double)(playerSeasonStats.getSingles() + (playerSeasonStats.getDoubles()*2) + (playerSeasonStats.getTriples()*3)+ (playerSeasonStats.getHomeRuns()*4)) 
						/ playerSeasonStats.getAtBats() : 0.0;
					roundSlgString = df.format((double) Math.round(slg * 1000) / 1000);
				}
				System.out.print((roundAvgString.charAt(0) != '1' ? roundAvgString : "1.00") + " ");
				System.out.print((roundOBPString.charAt(0) != '1' ? roundOBPString : "1.00") + " ");
				System.out.print((roundSlgString.charAt(0) != '1' ? roundSlgString : "1.00") + " ");
				System.out.println();
			}
		}
		System.out.println("BATTING");
		String[] doublesString = {"", ""};
		for (int top = 0; top < 2; top++) {
			for (int p = 0; p < Game.NUM_OF_PLAYERS_IN_LINEUP; p++) {
				int numD = game.getLineup()[top][p].getGameStats().getDoubles();
				if (numD > 0) {
					doublesString[top] += (game.getLineup()[top][p].getName() + (numD > 1 ? "(" + numD + ")" : "") + ", ");
				}
			}
		}
		if (doublesString[0].length() > 0 || doublesString[1].length() > 0)  {
			System.out.println("2B");
			for (int top = 0; top < 2; top++) {
				if (doublesString[top].length() == 0) {
					continue;
				}
				System.out.println(game.getTeamNames()[top]);
				System.out.println(doublesString[top].substring(0, doublesString[top].length()-2));
			}
		}
		String[] triplesString = {"", ""};
		for (int top = 0; top < 2; top++) {
			for (int p = 0; p < Game.NUM_OF_PLAYERS_IN_LINEUP; p++) {
				int numT = game.getLineup()[top][p].getGameStats().getTriples();
				if (numT > 0) {
					triplesString[top] += (game.getLineup()[top][p].getName() + (numT > 1 ? "(" + numT + ")" : "") + ", ");
				}
			}
		}
		if (triplesString[0].length() > 0 || triplesString[1].length() > 0)  {
			System.out.println("3B");
			for (int top = 0; top < 2; top++) {
				if (triplesString[top].length() == 0) {
					continue;
				}
				System.out.println(game.getTeamNames()[top]);
				System.out.println(triplesString[top].substring(0, triplesString[top].length()-2));
			}
		}
		String[] homeRunsString = {"", ""};
		for (int top = 0; top < 2; top++) {
			for (int p = 0; p < Game.NUM_OF_PLAYERS_IN_LINEUP; p++) {
				int numH = game.getLineup()[top][p].getGameStats().getHomeRuns();
				if (numH > 0) {
					homeRunsString[top] += (game.getLineup()[top][p].getName() + (numH > 1 ? "(" + numH + ")" : "") + ", ");
				}
			}
		}
		if (homeRunsString[0].length() > 0 || homeRunsString[1].length() > 0)  {
			System.out.println("HR");
			for (int top = 0; top < 2; top++) {
				if (homeRunsString[top].length() == 0) {
					continue;
				}
				System.out.println(game.getTeamNames()[top]);
				System.out.println(homeRunsString[top].substring(0, homeRunsString[top].length()-2));
			}
		}
	}
	
	private static void getPlayerStats() {
	  //String getStatsAPI = "http://lookup-service-prod.mlb.com/json/named.sport_hitting_tm.bam?league_list_id='mlb'&game_type='R'&season='2019'&player_id='592450'";	
      //String searchAPI = "http://lookup-service-prod.mlb.com/json/named.search_player_all.bam?sport_code='mlb'&active_sw='Y'&name_part='Aaron Judge'";
        try {
        	
        	BattingStats battingStats;
        	for (int top = 0; top < 2; top++) {
    			for (int p = 0; p < Game.NUM_OF_PLAYERS_IN_LINEUP; p++) {
    				String searchAPI = "http://lookup-service-prod.mlb.com/json/named.search_player_all.bam?sport_code=%27mlb%27&active_sw=%27Y%27&name_part=";
    				String getStatsAPI = "http://lookup-service-prod.mlb.com/json/named.sport_hitting_tm.bam?league_list_id=%27mlb%27&game_type=%27R%27&season=%27" + year + "%27&player_id=";
    				battingStats = new BattingStats();
    				String playerName = game.getLineup()[top][p].getName();
    				System.out.print(playerName + " ");
    				searchAPI += ("%27" + playerName.replace(" ", "%20") + "%27");
    				URL obj = new URL(searchAPI);
    				HttpURLConnection con = (HttpURLConnection)obj.openConnection();
    				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream())); 
    				JSONObject row = null;
    				try {
    					JSONObject player = new JSONObject(in.readLine());
    					JSONObject searchAll = new JSONObject(player.getString("search_player_all"));
    					JSONObject queryResults = new JSONObject(searchAll.getString("queryResults"));
    					row = new JSONObject(queryResults.getString("row"));
    					String playerId = row.getString("player_id");
        				game.getLineup()[top][p].setId(Integer.parseInt(playerId));
        				System.out.println(playerId + " ");
        				getStatsAPI += ("%27" + playerId + "%27");
    				}
    				catch (JSONException e) {
    			        System.out.println("PLAYER NOT FOUND!");
    			        continue;
    				}
    				in.close();
    				obj = new URL(getStatsAPI);
		        	con = (HttpURLConnection)obj.openConnection();
		        	in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		        	try {	
		        		JSONObject playerStats = new JSONObject(in.readLine());
		        		JSONObject sportHittingTm = new JSONObject(playerStats.getString("sport_hitting_tm"));
		        		JSONObject queryResults = new JSONObject(sportHittingTm.getString("queryResults"));
		        		row = new JSONObject(queryResults.getString("row"));
		        		battingStats.setHomeRuns(Integer.parseInt(row.getString("hr")));
		        		battingStats.setDoubles(Integer.parseInt(row.getString("d")));
		        		battingStats.setTriples(Integer.parseInt(row.getString("t")));
		        		battingStats.setAtBats(Integer.parseInt(row.getString("ab")));
		        		battingStats.setPlateAppearances(Integer.parseInt(row.getString("tpa")));
		        		battingStats.setHits(Integer.parseInt(row.getString("h")));
		        		battingStats.setHitByPitch(Integer.parseInt(row.getString("hbp")));
		        		battingStats.setWalks(Integer.parseInt(row.getString("bb")));
			        	playerStatsMap.put(game.getLineup()[top][p].getId(), battingStats);
		        	}
    				catch (JSONException e) {
    			        System.out.println("STATS NOT FOUND!");
    			        continue;
    				}
		        	in.close();	
    			}
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
