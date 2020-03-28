package baseball;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class BaseballSimulator {
	
	public static final int OUTS_PER_INNING = 3;
	public static final int INNINGS_PER_GAME = 9;
	
	static Game game = new Game();
	static int inning = 1;
	static int top = 0;
	static int currentBasesSituation = 0;
	static int[] battingOrder = {1, 1};
	
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
		System.out.println("START GAME");
		game.setLineup(setLineup());
		String[] teamNames = {"Houston Astros", "New York Yankees"};
		game.setTeamNames(teamNames);
		while (inning <= INNINGS_PER_GAME || game.getScore(inning)[0] == game.getScore(inning)[1]) {
			for (int top = 0; top < 2; top++) {
				System.out.println((top == 0 ? "\n***TOP " : "***BOTTOM ") + " INN: " + inning + " ***");
				int outs  = 0;
				boolean gameTiedStartOfAB;
				currentBasesSituation = 0;
				while (outs < OUTS_PER_INNING) {
					System.out.println(game.getLineup()[top][battingOrder[top] - 1].getName() + " UP OUTS: " + outs + " " + baseSituations.get(currentBasesSituation));
					Stats currentBatterGameStats = game.getLineup()[top][battingOrder[top] - 1].getGameStats();
					int rando = getRandomNumberInRange(1, 1000);
					
					gameTiedStartOfAB = game.getScore(inning)[1] == game.getScore(inning)[0] ? true : false;
					if (rando <= 680) {
						getOutResult(currentBatterGameStats);
						outs++;
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
							if (baseSituations.get(currentBasesSituation).equalsIgnoreCase("BASES LOADED")) {
								game.setBoxScore(top, inning, 1);
							}
							currentBasesSituation = getBasesSituationWalk(currentBasesSituation);
						}
						else {
							int noOutResult = getNotOutResult(currentBatterGameStats);
							getRunsScored(noOutResult, currentBasesSituation, top);
							currentBasesSituation = getBasesSituation(noOutResult, currentBasesSituation);
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
		 
		 getPlayerStats();
	}
	
	private static int getNotOutResult(Stats currentBatterGameStats) {
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
	
	private static boolean getOutResult(Stats currentBatterGameStats) {
		boolean doublePlay = false;
		int notOutRando = getRandomNumberInRange(1, 100);
		if (notOutRando > 1 && notOutRando <= 20) {
			System.out.println(outTypes.get(1)); // STRUCK OUT
			currentBatterGameStats.incremenStrikeOuts();
		}
		else if (notOutRando > 20 && notOutRando <= 50) {
			System.out.println(outTypes.get(2) + " TO " + positions.get(getRandomNumberInRange(1, 6))); //GROUNDED OUT
			doublePlay = doublePlay(true);
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
		return doublePlay;
	}
	
	private static void getRunsScored(int event, int situation, int top) {
		int runsScored = 0;
		if ((situation&4) == 4) {
   			runsScored++;
   		}
		if (event > 1 && (situation&2) == 2) {
			runsScored++;
		}
		if (event > 2 && (situation&1) == 1) {
			runsScored++;
		}
		if (event == 4) {
			runsScored++;
		}
		game.setBoxScore(top, inning, runsScored);
		if (runsScored > 0) {
			System.out.println(runsScored + " RUNS SCORED - VIS: " + game.getScore(inning)[0]  + " HOME: " + game.getScore(inning)[1]);
		}
	}
	
	private static int getBasesSituation(int event, int prevSituation) {
		int basesSituation = (prevSituation << event) + (int)Math.pow(2, (event - 1));
		if (basesSituation > 7) {
			basesSituation = basesSituation % 8;
		}
		return basesSituation;
	}
	
	private static int getBasesSituationWalk(int prevSituation) {
		int basesSituation = 7; // Bases loaded is default for loaded, 2+3, 1+3 or 1+2
		if (baseSituations.get(prevSituation).equals("Bases empty")) {
			basesSituation = 1; // 1
		}
		else if (baseSituations.get(prevSituation).equals("Man on First") || baseSituations.get(prevSituation).equals("Man on Second")) {
			basesSituation = 3; // 1+2
		}
		else if (baseSituations.get(prevSituation).equals("Man on Third")) {
			basesSituation = 5; // 1+3
		}
		return basesSituation;
	}
	
	private static boolean doublePlay(boolean ground) {
		boolean dp = false;
		/*
		int dpRando = getRandomNumberInRange(1, 100);
		
		if (ground && dpRando > 50) {
			dp = true;
		} */
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
		lineup[0][2].setName("Alex Bregmann");
		lineup[0][3].setName("George Springer");
		lineup[0][4].setName("Yuli Gurriel");
		lineup[0][5].setName("Carlos Correa");
		lineup[0][6].setName("Josh Reddick");
		lineup[0][7].setName("Yordan Alvarez");
		lineup[0][8].setName("Martin Maldonado");
		
		lineup[1][0].setName("Brett Gardner");
		lineup[1][1].setName("Didi Gregorious");
		lineup[1][2].setName("Giancarlo Stanton");
		lineup[1][3].setName("Aaron Judge");
		lineup[1][4].setName("Gleyber Torres");
		lineup[1][5].setName("Gary Sanchez");
		lineup[1][6].setName("Miguel Andujar");
		lineup[1][7].setName("Luke Voigt");
		lineup[1][8].setName("Mike Tauchmann");
		
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
			System.out.println("HITTERS\t\t\t" + "AB  R   H   RBI BB  K   AVG  OBP  SLG");
			for (int p = 0; p < Game.NUM_OF_PLAYERS_IN_LINEUP; p++) {
				Player player = game.getLineup()[top][p];
				Stats stats = player.getGameStats();
				System.out.print(player.getName());
				System.out.print("\t");
				if (player.getName().length() < 16) {
					System.out.print("\t");
				}
				System.out.print(stats.getAtBats() + (stats.getAtBats() > 9 ? "  " : "   "));
				System.out.print(stats.getRuns() + (stats.getRuns() > 9 ? "  " : "   "));
				System.out.print(stats.getHits() + (stats.getHits() > 9 ? "  " : "   "));
				System.out.print(stats.getRbis() + (stats.getRbis() > 9 ? "  " : "   "));
				System.out.print(stats.getWalks() + (stats.getWalks() > 9 ? "  " : "   "));
				System.out.print(stats.getStrikeOuts() + (stats.getStrikeOuts() > 9 ? "  " : "   "));
				double avg = 0.0;
				System.out.print(avg + " ");
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
		String getStatsAPI = "http://lookup-service-prod.mlb.com/json/named.sport_hitting_tm.bam?league_list_id=%27mlb%27&game_type=%27R%27&season=%272019%27&player_id=";
      //String searchAPI = "http://lookup-service-prod.mlb.com/json/named.search_player_all.bam?sport_code='mlb'&active_sw='Y'&name_part='Aaron Judge'";
        String searchAPI = "http://lookup-service-prod.mlb.com/json/named.search_player_all.bam?sport_code=%27mlb%27&active_sw=%27Y%27&name_part=";
        try {
        	String playerName = "Aaron Judge";
        	searchAPI += ("%27" + playerName.replace(" ", "%20") + "%27");
        	URL obj = new URL(searchAPI);
        	HttpURLConnection con = (HttpURLConnection)obj.openConnection();
        	int responseCode = con.getResponseCode();
        	//System.out.println("Response Code : " + responseCode);
        	BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream())); 
        	JSONObject player = new JSONObject(in.readLine());
        	JSONObject searchAll = new JSONObject(player.getString("search_player_all"));
        	JSONObject queryResults = new JSONObject(searchAll.getString("queryResults"));
        	JSONObject row = new JSONObject(queryResults.getString("row"));
        	in.close();
        	String playerId = row.getString("player_id");
        	System.out.println(playerId);
        	getStatsAPI += ("%27" + playerId + "%27");
        	obj = new URL(getStatsAPI);
        	con = (HttpURLConnection)obj.openConnection();
        	in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        	JSONObject playerStats = new JSONObject(in.readLine());
        	JSONObject sportHittingTm = new JSONObject(playerStats.getString("sport_hitting_tm"));
        	queryResults = new JSONObject(sportHittingTm.getString("queryResults"));
        	row = new JSONObject(queryResults.getString("row"));
        	in.close();
        	System.out.println(row.getString("hr"));
        }
        catch (MalformedURLException e) { 	
        	e.printStackTrace();
        }
        catch (IOException e) { 
        	e.printStackTrace();
        }
        catch (JSONException e) {
        	e.printStackTrace();
        }     		
	}

}
