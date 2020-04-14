package baseball;


import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import dao.DAO;
import db.MLBBattingStats;
import db.MLBPitchingStats;
import db.MLBPlayer;

public class BaseballSimulator {
	
	public static final int OUTS_PER_INNING = 3;
	public static final int INNINGS_PER_GAME = 9;
	
	static boolean auto = true;
	static int autoBefore = 1000;
	static GameResults gameResults = new GameResults();
	static GameState gameState = new GameState();
	static Roster[] rosters  = new Roster[2];
	static HashMap<Integer, BattingStats> battingStatsMap = new HashMap<Integer, BattingStats>();
	static HashMap<Integer, PitchingStats> pitchingStatsMap = new HashMap<Integer, PitchingStats>();
	static DecimalFormat df = new DecimalFormat(".000");
	static List<String> randoLog = new ArrayList<String>();
	static HashMap<?, ?> franchisesMap;
	
	static final int BASES_EMPTY = 0;
	static final int MAN_ON_FIRST = 1;
	static final int MAN_ON_SECOND = 2;
	static final int MAN_ON_FIRST_AND_SECOND = 3;
	static final int MAN_ON_THIRD = 4;
	static final int MAN_ON_FIRST_AND_THIRD = 5;
	static final int MAN_ON_SECOND_AND_THIRD = 6;
	static final int BASES_LOADED = 7;
	
	static final int STRUCK_OUT = 0;
	static final int GROUNDED_OUT = 1;
	static final int FLEW_OUT = 2;
	static final int FLEW_OUT_DEEP = 3;
	static final int LINED_OUT = 4;
	static final int POPPED_OUT = 5;
	
	static final String[] nationalLeagueTeams = {"ARI","CHC","LAD","WSH","NYM","PIT","SD","SF","STL","PHI","ATL","MIA","MIL","COL","CIN"};
	
	static Map<Integer, String> baseSituations  = new HashMap<Integer, String>() {
		private static final long serialVersionUID = 1L;
	{
	    put(BASES_EMPTY, "BASES EMPTY");
	    put(MAN_ON_FIRST, "MAN ON FIRST");
	    put(MAN_ON_SECOND, "MAN ON SECOND");
	    put(MAN_ON_FIRST_AND_SECOND, "MAN ON FIRST AND SECOND");
	    put(MAN_ON_THIRD, "MAN ON THIRD");
	    put(MAN_ON_FIRST_AND_THIRD, "MAN ON FIRST AND THIRD");
	    put(MAN_ON_SECOND_AND_THIRD, "MAN ON SECOND AND THIRD");
	    put(BASES_LOADED, "BASES LOADED");
	}};
	
	static Map<Integer, String> positions  = new HashMap<Integer, String>() {
		private static final long serialVersionUID = 1L;
	{
	    put(1, "P");
	    put(2, "C");
	    put(3, "1B");
	    put(4, "2B");
	    put(5, "3B");
	    put(6, "SS");
	    put(7, "LF");
	    put(8, "CF");
	    put(9, "RF");
	    put(10, "DH");
	}};
	
	static Map<Integer, String> outTypes  = new HashMap<Integer, String>() {
		private static final long serialVersionUID = 1L;
	{
	    put(STRUCK_OUT, "STRUCK OUT");
	    put(GROUNDED_OUT, "GROUNDED OUT");
	    put(FLEW_OUT, "FLEW OUT");
	    put(FLEW_OUT_DEEP, "FLEW OUT DEEP");
	    put(LINED_OUT, "LINED OUT");
	    put(POPPED_OUT, "POPPED OUT");
	}};

	public static void main(String[] args) {
		if (args == null || args.length < 4) {
			System.out.println("Invalid args - expecting <year><vis><year><home> - ex. 2019 HOU 2019 NYY [AUTOOFF|PLAY<#>]]");
			return;
		}
		DAO.setConnection();
		franchisesMap = DAO.getDataMap("MLB_FRANCHISE");
		gameResults.setVisYear(Integer.parseInt(args[0]));
		gameResults.setHomeYear(Integer.parseInt(args[2]));
		String[] teamNames = {args[1], args[3]};
		gameResults.setTeamNames(teamNames);
		if (args.length > 4 && args[4] != null) {
			if (args[4].equalsIgnoreCase("AUTOOFF")) {
				auto = false;
				autoBefore = 0;
			}
			else if (args[4].indexOf("PLAY") != -1) {
				autoBefore = Integer.parseInt(args[4].substring(args[4].length()-1));
			}
		}
		if (!setLineup()) {
			return;
		}
		System.out.println("Starting pitchers : " + gameResults.getTeamNames()[0] + ": " + gameState.getCurrentPitchers()[0].getFirstLastName() + " v " + gameResults.getTeamNames()[1] + ": " + gameState.getCurrentPitchers()[1].getFirstLastName());
		//getBattingStatsFromAPI();
		getBattingStatsFromDB();
		getPitchingStatsFromDB();
		while (gameState.getInning() <= INNINGS_PER_GAME || gameResults.getScore(gameState.getInning())[0] == gameResults.getScore(gameState.getInning())[1]) {
			int inning = gameState.getInning();
			Scanner myObj = null;
			for (int top = 0; top < 2; top++) {
				gameState.setTop(top);
				gameState.setOuts(0);
				System.out.println((top == 0 ? "\n***TOP " : "***BOTTOM ") + " INN: " + inning + " ***");
				if (top == 0) {
					System.out.println("SCORE - " + gameResults.getTeamNames()[0] + ": " + gameResults.getScore(gameState.getInning())[0]  + " " + gameResults.getTeamNames()[1] + ": " + gameResults.getScore(gameState.getInning())[1]);
				}
				//int outs  = 0;
				boolean gameTiedStartOfAB;
				Arrays.fill(gameState.getRunnersOnBase(), 0);
				while (gameState.getOuts() < OUTS_PER_INNING) {
					System.out.println(gameResults.getLineup()[top][gameState.getBattingOrder()[top] - 1].getName() + " UP OUTS: " + gameState.getOuts() + " " 
						+ baseSituations.get(getCurrentBasesSituation()) + " " + gameState.getRunnersOnBase()[0] + " " + gameState.getRunnersOnBase()[1] + " " + gameState.getRunnersOnBase()[2]);
					
					//if (!auto) {
					if (autoBefore <= inning) {
						myObj = new Scanner(System.in);
						System.out.print("PITCH: ");
					    String command = myObj.nextLine();
					    if (!processCommand(command)) {
					    	continue;
					    }
					}
					BattingStats currentBatterGameStats = gameResults.getLineup()[top][gameState.getBattingOrder()[top] - 1].getBattingStats();
					BattingStats currentBatterSeasonStats = battingStatsMap.get(gameResults.getLineup()[top][gameState.getBattingOrder()[top] - 1].getId()) != null ? 
						battingStatsMap.get(gameResults.getLineup()[top][gameState.getBattingOrder()[top] - 1].getId()) : new BattingStats();
					PitchingStats currentPitcherGameStats = gameState.getCurrentPitchers()[top==0?1:0].getPitchingStats();
					PitchingStats currentPitcherSeasonStats = pitchingStatsMap.get(gameState.getCurrentPitchers()[top==0?1:0].getId()) != null ? 
						pitchingStatsMap.get(gameState.getCurrentPitchers()[top==0?1:0].getId()) : new PitchingStats();
					//if (auto) {  // Steal 2?
					if (autoBefore > inning) { // Steal 2?
						if (isRunnerStealing(2)) {
							gameState.setOuts(gameState.getOuts() + stealBase(2));
						}
					}
					if (gameState.getOuts() == OUTS_PER_INNING) {
						break;
					}
					int rando = getRandomNumberInRange(1, 1000);
					
					gameTiedStartOfAB = gameResults.getScore(inning)[1] == gameResults.getScore(inning)[0] ? true : false;
					long onBaseEndPoint = 1000 - Math.round(currentBatterSeasonStats.getOnBasePercentage()*1000);
					if (rando <= onBaseEndPoint) { // OUT
						int outResult = getOutResult(currentBatterGameStats, currentBatterSeasonStats, currentPitcherGameStats, currentPitcherSeasonStats, gameState.getOuts());
						gameState.setOuts(gameState.getOuts() + outResult);
						currentBatterGameStats.incrementAtBats();
					}
					else {
						long bbEndPoint = 1000 - Math.round((currentBatterSeasonStats.getWalkRate()*1000) + ((currentPitcherSeasonStats.getWalkRate()*1000) - 250));
						bbEndPoint = bbEndPoint > 850 ? 850 : bbEndPoint; // set max bbEndPoint
						if (rando > onBaseEndPoint && rando <= bbEndPoint) {
							if (rando >= (bbEndPoint - 20)) {  // Hardcoded HBP rate
								System.out.println("HIT BY PITCH");
							}
							else {
								System.out.println("WALKED");
								currentBatterGameStats.incrementWalks();
							}
							updateBasesSituationNoRunnersAdvance();
						}
						else { // HIT
							int noOutResult = getNotOutResult(currentBatterGameStats, currentBatterSeasonStats);
							if (noOutResult == 1 && (getRandomNumberInRange(0, 5) + currentBatterGameStats.getSpeedRating()) > 4) { // infield single ?
								if (gameState.getOuts() != 2) {  // less than 2 outs runners hold
									updateBasesSituationNoRunnersAdvance();
								}
								else {
									updateBasesSituationRunnersAdvance(noOutResult);
								}
								System.out.println("INFIELD SINGLE");
							}
							else {
								updateBasesSituationRunnersAdvance(noOutResult);
							}
							gameResults.incrementHits(top);
							currentBatterGameStats.incrementHits();
							currentBatterGameStats.incrementAtBats();
						}
						gameResults.getLineup()[top][gameState.getBattingOrder()[top] - 1].setBattingStats(currentBatterGameStats);
						if (inning >= 9 && gameResults.getScore(inning)[1] > gameResults.getScore(inning)[0] && gameTiedStartOfAB) {
							gameResults.setWalkOff(true);
							System.out.println("WALKOFF ");
							break;
						}
					}
					gameState.getBattingOrder()[top] = gameState.getBattingOrder()[top] == 9 ? 1 : gameState.getBattingOrder()[top] + 1;
				} // outs
				// Did game end after top of inning?
				if (inning >= 9 && gameResults.getScore(inning)[1] > gameResults.getScore(inning)[0] && top == 0) {
					System.out.println("GAME OVER AFTER " + (inning - 1) + " 1/2");
					break;
				}
			}
			gameState.incrementInning();
		}
		// Were there extra innings?
		if (gameState.getInning() > (INNINGS_PER_GAME + 1)) {
			System.out.println("EXTRA INNINGS: " + (gameState.getInning() - 1));
		}
		// Output Box Score
		 outputBoxScore();
		 
		 /*for (int i = 0; i < randoLog.size(); i++) {
			 System.out.println(randoLog.get(i));
		 }*/ 
	}
	
	private static int getNotOutResult(BattingStats playerGameStats, BattingStats playerSeasonStats) {
		long errorEndPoint = 25;
		long hrEndPoint = (playerSeasonStats != null && playerSeasonStats.getHits() != 0 ? 
			Math.round((((double)playerSeasonStats.getHomeRuns()/playerSeasonStats.getHits())*1000)) : 160) + errorEndPoint;
		hrEndPoint = playerSeasonStats.getHomeRuns() == 0 ? 8 + errorEndPoint : hrEndPoint;       // Give some chance if player has 0 hrs
		long triplesEndPoint = (playerSeasonStats != null && playerSeasonStats.getHits() != 0 ? 
			Math.round((((double)playerSeasonStats.getTriples()/playerSeasonStats.getHits())*1000)) : 18) + hrEndPoint;
		triplesEndPoint = playerSeasonStats.getTriples() == 0 ? 8 + hrEndPoint : triplesEndPoint; // Give some chance if player has 0 triples
		long doublesEndPoint = (playerSeasonStats != null && playerSeasonStats.getHits() != 0 ? 
			Math.round((((double)playerSeasonStats.getDoubles()/playerSeasonStats.getHits())*1000)) : 203) + triplesEndPoint;
		int notOutResult = 1;
		int notOutRando = getRandomNumberInRange(1, 1000);
		if (notOutRando > 1 && notOutRando <= errorEndPoint) {
			System.out.println("REACHED BY ERROR");
			notOutResult = 1;
			gameResults.incrementErrors(gameState.getTop() == 0 ? 1 : 0);
		}
		else if (notOutRando > errorEndPoint && notOutRando <= hrEndPoint) {
			System.out.println("HOME RUN");
			notOutResult = 4;
			playerGameStats.incrementHomeRuns();
		}
		else if (notOutRando > hrEndPoint && notOutRando <= triplesEndPoint) {
			System.out.println("TRIPLE");
			notOutResult = 3;
			playerGameStats.incrementTriples();
		}
		else if (notOutRando > triplesEndPoint && notOutRando < doublesEndPoint) {
			System.out.println("DOUBLE");
			notOutResult = 2;
			playerGameStats.incrementDoubles();
		}
		else {
			System.out.println("SINGLE");
		}
		
		return notOutResult;
	}
	
	private static int getOutResult(BattingStats batterGameStats, BattingStats batterSeasonStats, PitchingStats pitcherGameStats, PitchingStats pitcherSeasonStats, int outs) {
		int outsRecorded = 1;
		int notOutRando = getRandomNumberInRange(1, 100);
		long soEndPoint = (pitcherSeasonStats != null && batterSeasonStats != null? 
			Math.round((pitcherSeasonStats.getStrikeoutRate()*100) + ((batterSeasonStats.getStrikeoutRate()*100) - 34)) : 34);
		soEndPoint = soEndPoint > 75 ? 75 : soEndPoint; // set max soEndPoint
		long outIncrement = Math.round((double)((100 - soEndPoint)/5));
		if (notOutRando > 1 && notOutRando <= soEndPoint) {
			System.out.println(outTypes.get(STRUCK_OUT)); // STRUCK OUT
			batterGameStats.incremenStrikeOuts();
		}
		else if (notOutRando > soEndPoint && notOutRando <= soEndPoint + outIncrement) {
			System.out.println(outTypes.get(GROUNDED_OUT) + " TO " + positions.get(getRandomNumberInRange(1, 6))); //GROUNDED OUT
			if (doublePlay(true, outs)) {
				outsRecorded++;
			}
		}
		else if (notOutRando > soEndPoint + outIncrement && notOutRando <= soEndPoint + (outIncrement*2)) {
			System.out.println(outTypes.get(FLEW_OUT) +  " TO " + positions.get(getRandomNumberInRange(7, 9))); // FLEW OUT
			if (outs < 2 && gameState.getRunnersOnBase()[2] != 0) {
				if (updateBasesSituationSacFly(false) == 1) {
					outsRecorded++;
				}
			}
		}
		else if (notOutRando > soEndPoint + (outIncrement*2) && notOutRando <= soEndPoint + (outIncrement*3)) {
			System.out.println(outTypes.get(FLEW_OUT_DEEP) +  " TO " + positions.get(getRandomNumberInRange(7, 9))); // FLEW OUT DEEP
			if (outs < 2 && gameState.getRunnersOnBase()[2] != 0) {
				if (updateBasesSituationSacFly(true) == 1) {
					outsRecorded++;
				}
			}
		}
		else if (notOutRando > soEndPoint + (outIncrement*3) && notOutRando < soEndPoint + (outIncrement*4)) {
			System.out.println(outTypes.get(LINED_OUT) + " TO " + positions.get(getRandomNumberInRange(1, 9, 2))); // LINED OUT
		}
		else {
			System.out.println(outTypes.get(POPPED_OUT) + " TO " + positions.get(getRandomNumberInRange(1, 6))); // POPPED OUT
		}
		return outsRecorded;
	}
	
	private static void updateBasesSituationRunnersAdvance(int event) {
	  /*int basesSituation = (currentBasesSituation << event) + (int)Math.pow(2, (event - 1));
		if (basesSituation > 7) {
			basesSituation = basesSituation % 8;
		}
		currentBasesSituation = basesSituation;*/
		
		int runsScored = 0;
		for (int e = 0; e < event; e++) {
			for (int b = 3; b >= 0; b--) {
				if (b > 0) {
					if (b != 3) {
						gameState.getRunnersOnBase()[b] = gameState.getRunnersOnBase()[b-1];
					}
				}
				else if (b == 0 && e == 0) {
					gameState.getRunnersOnBase()[b] = gameResults.getLineup()[gameState.getTop()][gameState.getBattingOrder()[gameState.getTop()] - 1].getId();
				}
				else {
					gameState.getRunnersOnBase()[0] = 0;
				}
				if (b == 3 && gameState.getRunnersOnBase()[2] != 0) {     // If runner on 3rd run scores
					int bo = getBattingOrderForPlayer(gameState.getRunnersOnBase()[2]);
					gameResults.getLineup()[gameState.getTop()][bo - 1].getBattingStats().incrementRuns();
					runsScored++;
				}
			}
		}
		if (runsScored > 0) {
			gameResults.setBoxScore(gameState.getTop(), gameState.getInning(), runsScored);
			System.out.println(runsScored + " RUNS SCORED - VIS: " + gameResults.getScore(gameState.getInning())[0]  + " HOME: " + gameResults.getScore(gameState.getInning())[1]);
		}
	}
	
	// For walks, hit by pitch and some infield singles
	private static void updateBasesSituationNoRunnersAdvance() { 
		// No need for checking empty, 2, 3, or 23
		if (getCurrentBasesSituation() == BASES_LOADED || getCurrentBasesSituation() == MAN_ON_FIRST_AND_SECOND) { // 123 or 12
			// if 123 runner 3 scores
			if (getCurrentBasesSituation() == BASES_LOADED) {
				gameResults.incrementBoxScore(gameState.getTop(), gameState.getInning()); // run scores
				int bo = getBattingOrderForPlayer(gameState.getRunnersOnBase()[2]);
				gameResults.getLineup()[gameState.getTop()][bo - 1].getBattingStats().incrementRuns();
				System.out.println("1 RUN SCORED - VIS: " + gameResults.getScore(gameState.getInning())[0]  + " HOME: " + gameResults.getScore(gameState.getInning())[1]);
			}
			gameState.getRunnersOnBase()[2] = gameState.getRunnersOnBase()[1]; // runner 2->3
		}
		if (gameState.getRunnersOnBase()[0] != 0) { // Runner on first
			gameState.getRunnersOnBase()[1] = gameState.getRunnersOnBase()[0]; // runner 1->2
		}
		gameState.getRunnersOnBase()[0] = gameResults.getLineup()[gameState.getTop()][gameState.getBattingOrder()[gameState.getTop()] - 1].getId();
		
	}
	
	private static int updateBasesSituationSacFly(boolean deep) {
		int outAdvancing = 0;
		int bo = getBattingOrderForPlayer(gameState.getRunnersOnBase()[2]);
		BattingStats bs = battingStatsMap.get(gameResults.getLineup()[gameState.getTop()][bo - 1].getId()) != null ? battingStatsMap.get(gameResults.getLineup()[gameState.getTop()][bo - 1].getId()) : new BattingStats();
		int sacRando = getRandomNumberInRange(0, 5) + bs.getSpeedRating();
		if (deep || (!deep && bs.getSpeedRating() > 2)) {  // If going to try to score on fly ball
			System.out.println(gameResults.getLineup()[gameState.getTop()][bo - 1].getName() + " TAGGING UP ON A FLY BALL");
			if (deep ||(sacRando > 5)) { // safe
				gameResults.incrementBoxScore(gameState.getTop(), gameState.getInning()); // run scores
				gameResults.getLineup()[gameState.getTop()][bo - 1].getBattingStats().incrementRuns();
				System.out.println("SAC FLY - 1 RUN SCORED - VIS: " + gameResults.getScore(gameState.getInning())[0]  + " HOME: " + gameResults.getScore(gameState.getInning())[1]);
			}
			else { // out
				outAdvancing = 1;
				System.out.println("OUT AT THE PLATE");
			}
			gameState.getRunnersOnBase()[2] = 0;
		}
		return outAdvancing;
	}
	
	private static void updateBasesSituationDoublePlay() {
		boolean runScores = getCurrentBasesSituation() == BASES_LOADED || getCurrentBasesSituation() == MAN_ON_FIRST_AND_THIRD;
		if (getCurrentBasesSituation() == MAN_ON_FIRST_AND_SECOND || getCurrentBasesSituation() == BASES_LOADED) {
			gameState.getRunnersOnBase()[2] = gameState.getRunnersOnBase()[1]; // 2->3
		}
		gameState.getRunnersOnBase()[1] = 0;
		gameState.getRunnersOnBase()[0] = 0;
		if (runScores) {
			gameResults.incrementBoxScore(gameState.getTop(), gameState.getInning()); // run scores
			System.out.println("RUN SCORES - VIS: " + gameResults.getScore(gameState.getInning())[0]  + " HOME: " + gameResults.getScore(gameState.getInning())[1]);
			int bo = getBattingOrderForPlayer(gameState.getRunnersOnBase()[2]);
			gameResults.getLineup()[gameState.getTop()][bo - 1].getBattingStats().incrementRuns();
		}
	}
	
	private static boolean doublePlay(boolean ground, int outs) {
		boolean dp = false;
		if (gameState.getRunnersOnBase()[0] == 0 || outs == 2 || !ground) { // Must a ground out, less than 2 outs and runner on 1st
			return dp;
		}
		int bo = getBattingOrderForPlayer(gameState.getRunnersOnBase()[0]);
		BattingStats bs = battingStatsMap.get(gameResults.getLineup()[gameState.getTop()][bo - 1].getId()) != null ? battingStatsMap.get(gameResults.getLineup()[gameState.getTop()][bo - 1].getId()) : new BattingStats();
		int dpRando = getRandomNumberInRange(0, 5) + bs.getSpeedRating();
		// Ground ball, less than 2 outs, man on first
		if (dpRando < 5) {
			dp = true;
			updateBasesSituationDoublePlay();
			System.out.println("DOUBLE PLAY");
		}
		return dp;
	}
	
	private static boolean isRunnerStealing(int baseToSteal) {
		boolean runnerIsStealing = false;
		int runnerStealingIndex = baseToSteal - 2;
		int nextBaseIndex = baseToSteal == 4 ? 2 : (runnerStealingIndex + 1);
		if (gameState.getRunnersOnBase()[runnerStealingIndex] == 0 || gameState.getRunnersOnBase()[nextBaseIndex] != 0) {
			return false;
		}
		int bo = getBattingOrderForPlayer(gameState.getRunnersOnBase()[runnerStealingIndex]);
		BattingStats bs = battingStatsMap.get(gameResults.getLineup()[gameState.getTop()][bo - 1].getId());
		int stealRando = getRandomNumberInRange(0, 5) + bs.getSpeedRating();
		if (stealRando > 5) {
			runnerIsStealing = true;
		}
		return runnerIsStealing;
	}
	
	private static int stealBase(int baseToSteal) {
		int outStealing = 0;
		int runnerStealingIndex = baseToSteal - 2;  
		int nextBaseIndex = baseToSteal == 4 ? 2 : (runnerStealingIndex + 1);
		int bo = getBattingOrderForPlayer(gameState.getRunnersOnBase()[runnerStealingIndex]);
		BattingStats bs = battingStatsMap.get(gameResults.getLineup()[gameState.getTop()][bo - 1].getId());
		double stealPctg = ((bs.getStolenBases() + bs.getCaughtStealing()) != 0) ? 
			(double)bs.getStolenBases()/(bs.getStolenBases() + bs.getCaughtStealing()) : 0.2;  // Give a chance if no SB
		System.out.print(gameResults.getLineup()[gameState.getTop()][bo - 1].getName() + 
			" ATTEMPTING TO STEAL " + baseToSteal + " - SR: " + bs.getSpeedRating() + " SP: " + df.format(stealPctg));
		if (getRandomNumberInRange(1, 10) < Math.round(stealPctg*10)) { // safe
			System.out.println("- SAFE!");
			gameState.getRunnersOnBase()[nextBaseIndex] = gameState.getRunnersOnBase()[runnerStealingIndex];
		}
		else { // out
			System.out.println("- OUT!");
			outStealing = 1;
		}
		gameState.getRunnersOnBase()[runnerStealingIndex] = 0;
		System.out.println(baseSituations.get(getCurrentBasesSituation()) + " " + gameState.getRunnersOnBase()[0] + 
			" " + gameState.getRunnersOnBase()[1] + " " + gameState.getRunnersOnBase()[2]);
		return outStealing;
	}
	
	private static int getRandomNumberInRange(int min, int max) {
		if (min >= max) {
			throw new IllegalArgumentException("max must be greater than min");
		}
		int rando = (int)(Math.random()*((max - min) + 1)) + min;
		randoLog.add(rando + " " + min + " to " + max);
		return rando;
	}
	
	private static int getRandomNumberInRange(int min, int max, int excluding) {
		if (min >= max) {
			throw new IllegalArgumentException("max must be greater than min");
		}
		int rando = (int)(Math.random()*((max - min) + 1)) + min;
		if (rando == excluding) {
			rando = getRandomNumberInRange(min, max, excluding);
		}
		randoLog.add(rando + " " + min + " to " + max + " ex: " + excluding);
		return rando;
	}
	
	private static boolean setLineup() {
		Player[][] lineup = new Player[2][GameResults.NUM_OF_PLAYERS_IN_LINEUP];
		MLBPlayer mlbPlayer;
		int pitcherDHLineupPosition = 1;
		for (int t = 0 ; t < 2; t++) {
			// Create rosters of all players for that team and year
			rosters[t] = new Roster();
			HashMap<Object, Object> pitchersMap = DAO.getPitchersMapByTeamAndYear((Integer)franchisesMap.get(gameResults.getTeamNames()[t]), gameResults.getYears()[t]);
			HashMap<Object, Object> battersMap = DAO.getBattersMapByTeamAndYear((Integer)franchisesMap.get(gameResults.getTeamNames()[t]), gameResults.getYears()[t]);
			rosters[t].setPitchers(pitchersMap);
			rosters[t].setBatters(battersMap);
			// Get random starter 1-5
			MLBPlayer startingPitcher = DAO.getStartingPitcherByIndex((Integer)franchisesMap.get(gameResults.getTeamNames()[t]), gameResults.getYears()[t], getRandomNumberInRange(1, 5));  
			gameState.getCurrentPitchers()[t] = new Player(startingPitcher.getFullName(), startingPitcher.getMlbPlayerId(), "P");
			gameResults.addPitcher(new Player(startingPitcher.getFullName(), startingPitcher.getMlbPlayerId(), "P"), t);
			ArrayList<Integer> randomLineup = getRandomLineupByPosition();
			ArrayList<Integer> outfielderIdList = new ArrayList<>();
			ArrayList<Integer> lineupPlayerIdList = new ArrayList<>();
			for (int p = 0 ; p < GameResults.NUM_OF_PLAYERS_IN_LINEUP; p++) {
				Integer position = randomLineup.get(p);
				if (!positions.get(position).equals("P")) {
					mlbPlayer = DAO.getMlbPlayerWithMostPlateAppearances((Integer)franchisesMap.get(gameResults.getTeamNames()[t]), gameResults.getYears()[t], positions.get(position));
					if (mlbPlayer != null && mlbPlayer.getMlbPlayerId() != null) {
						lineup[t][p] = new Player(mlbPlayer.getFullName(), mlbPlayer.getMlbPlayerId(), positions.get(position));
						lineupPlayerIdList.add(mlbPlayer.getMlbPlayerId());
					}
					else {
						// No specific OF positions before 1987
						if (positions.get(position).equals("LF") || positions.get(position).equals("CF") || positions.get(position).equals("RF")) {
							mlbPlayer = DAO.getMlbPlayerWithMostPlateAppearances((Integer)franchisesMap.get(gameResults.getTeamNames()[t]), gameResults.getYears()[t], "OF", outfielderIdList);
							if (mlbPlayer != null && mlbPlayer.getMlbPlayerId() != null) {
								lineup[t][p] = new Player(mlbPlayer.getFullName(), mlbPlayer.getMlbPlayerId(), positions.get(position));
								outfielderIdList.add(mlbPlayer.getMlbPlayerId());
								lineupPlayerIdList.add(mlbPlayer.getMlbPlayerId());
							}
							else {
								System.out.println("No players at: OF for " + gameResults.getTeamNames()[t]);
								return false;
							}
						}
						else {
							System.out.println("No players at: " + positions.get(position) + " for " + gameResults.getTeamNames()[t]);
							return false;
						}
					}
				}
				else { 
					pitcherDHLineupPosition = p;
				}
			}
			// Set DH/P
			// Use P v DH
			if (Arrays.asList(nationalLeagueTeams).contains(gameResults.getTeamNames()[1]) && gameResults.getYears()[1] >= 1973) {
				lineup[t][pitcherDHLineupPosition] = new Player(gameState.getCurrentPitchers()[t].getName(), gameState.getCurrentPitchers()[t].getId(), "P");	
				battingStatsMap.put(gameState.getCurrentPitchers()[t].getId(), new BattingStats(75, 10, 2, 0, 1, 2, 44, 0, 4, 3, 0, 100, 0)); // Default pitcher batting stats
			}
			else { // DH
				mlbPlayer = DAO.getMlbPlayerWithMostPlateAppearances((Integer)franchisesMap.get(gameResults.getTeamNames()[t]), gameResults.getYears()[t], lineupPlayerIdList);
				lineup[t][pitcherDHLineupPosition] = new Player(mlbPlayer.getFullName(), mlbPlayer.getMlbPlayerId(), "DH");
			}
		}
		gameResults.setLineup(lineup);
		return true;
	}
	
	private static void outputBoxScore() {
		for (int top = 0; top < 2; top++) {
			String team = (top == 0) ? "\n" + gameResults.getTeamNames()[top] : gameResults.getTeamNames()[top];
			team += team.length() < 3 ? " " : "";
			System.out.print(team + " ");
			for (int i = 1; i < gameState.getInning(); i++) {
				if (i == (gameState.getInning() - 1) && (gameResults.getScore(i)[1] > gameResults.getScore(i)[0] && top == 1) && !gameResults.isWalkOff()) {
					System.out.print("X "); // Bottom of inning was not necessary
				}
				else {
					System.out.print(gameResults.getBoxScore(top, i-1) + (gameResults.getBoxScore(top == 0 ? 1: 0, i-1) < 10 ? " " : "  "));
				}
			}
			System.out.println(" " + gameResults.getScore(gameState.getInning())[top] + (gameResults.getScore(gameState.getInning())[top] < 10 ? " " : "") + " " + gameResults.getHits()[top] + 
				(gameResults.getHits()[top] < 10 ? " " : "") + " " +  gameResults.getErrors()[top]);
		}
		for (int top = 0; top < 2; top++) {
			System.out.println();
			System.out.println(gameResults.getTeamNames()[top]);
			System.out.println("HITTERS\t\t\t\t" + "AB  R   H   RBI BB  K    AVG  OBP  SLG");
			for (int p = 0; p < GameResults.NUM_OF_PLAYERS_IN_LINEUP; p++) {
				Player player = gameResults.getLineup()[top][p];
				BattingStats gameStats = player.getBattingStats();
				BattingStats playerSeasonStats = battingStatsMap.get(player.getId()) != null ? battingStatsMap.get(player.getId()) : new BattingStats();
				String playerOutput = player.getName() + " " + player.getPosition();
				System.out.print(playerOutput);
				System.out.print("\t\t");
				if (playerOutput.length() < 16) {
					System.out.print("\t");
				}
				System.out.print(gameStats.getAtBats() + (gameStats.getAtBats() > 9 ? "  " : "   "));
				System.out.print(gameStats.getRuns() + (gameStats.getRuns() > 9 ? "  " : "   "));
				System.out.print(gameStats.getHits() + (gameStats.getHits() > 9 ? "  " : "   "));
				System.out.print(gameStats.getRbis() + (gameStats.getRbis() > 9 ? "  " : "   "));
				System.out.print(gameStats.getWalks() + (gameStats.getWalks() > 9 ? "  " : "   "));
				System.out.print(gameStats.getStrikeOuts() + (gameStats.getStrikeOuts() > 9 ? "  " : "   "));
				String roundAvgString = ".000";
				String roundOBPString = ".000";
				String roundSlgString = ".000";
				if (playerSeasonStats != null) {
					roundAvgString = df.format((double) Math.round(playerSeasonStats.getBattingAverage()*1000)/1000);
					roundOBPString = df.format((double) Math.round(playerSeasonStats.getOnBasePercentage()*1000)/1000);
					roundSlgString = df.format((double) Math.round(playerSeasonStats.getSluggingPercentage()*1000)/1000);
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
			for (int p = 0; p < GameResults.NUM_OF_PLAYERS_IN_LINEUP; p++) {
				int numD = gameResults.getLineup()[top][p].getBattingStats().getDoubles();
				if (numD > 0) {
					doublesString[top] += (gameResults.getLineup()[top][p].getFirstLastName() + (numD > 1 ? "(" + numD + ")" : "") + ", ");
				}
			}
		}
		if (doublesString[0].length() > 0 || doublesString[1].length() > 0)  {
			System.out.println("2B");
			for (int top = 0; top < 2; top++) {
				if (doublesString[top].length() == 0) {
					continue;
				}
				System.out.println(gameResults.getTeamNames()[top]);
				System.out.println(doublesString[top].substring(0, doublesString[top].length()-2));
			}
		}
		String[] triplesString = {"", ""};
		for (int top = 0; top < 2; top++) {
			for (int p = 0; p < GameResults.NUM_OF_PLAYERS_IN_LINEUP; p++) {
				int numT = gameResults.getLineup()[top][p].getBattingStats().getTriples();
				if (numT > 0) {
					triplesString[top] += (gameResults.getLineup()[top][p].getFirstLastName() + (numT > 1 ? "(" + numT + ")" : "") + ", ");
				}
			}
		}
		if (triplesString[0].length() > 0 || triplesString[1].length() > 0)  {
			System.out.println("3B");
			for (int top = 0; top < 2; top++) {
				if (triplesString[top].length() == 0) {
					continue;
				}
				System.out.println(gameResults.getTeamNames()[top]);
				System.out.println(triplesString[top].substring(0, triplesString[top].length()-2));
			}
		}
		String[] homeRunsString = {"", ""};
		for (int top = 0; top < 2; top++) {
			for (int p = 0; p < GameResults.NUM_OF_PLAYERS_IN_LINEUP; p++) {
				int numH = gameResults.getLineup()[top][p].getBattingStats().getHomeRuns();
				if (numH > 0) {
					homeRunsString[top] += (gameResults.getLineup()[top][p].getFirstLastName() + (numH > 1 ? "(" + numH + ")" : "") + ", ");
				}
			}
		}
		if (homeRunsString[0].length() > 0 || homeRunsString[1].length() > 0)  {
			System.out.println("HR");
			for (int top = 0; top < 2; top++) {
				if (homeRunsString[top].length() == 0) {
					continue;
				}
				System.out.println(gameResults.getTeamNames()[top]);
				System.out.println(homeRunsString[top].substring(0, homeRunsString[top].length()-2));
			}
		}
		System.out.println("\n" + "PITCHING");
		for (int top = 0; top < 2; top++) {
			System.out.println(gameResults.getTeamNames()[top]);
			ArrayList<Player> pitchers = gameResults.getPitchers().get(top);
			for (Player p : pitchers) {
				System.out.println(p.getFirstLastName());
			}
		}
	}
	
	static int getBattingOrderForPlayer(int id) {
		int order = 1;
		for (Player p : gameResults.getLineup(gameState.getTop())) {
			if (p.getId() == id) {
				return order;
			}
			order++;
		}
		return order;
	}
	
	static int getCurrentBasesSituation() {
		int baseSituation = BASES_EMPTY;
		if ((gameState.getRunnersOnBase()[0] != 0 && gameState.getRunnersOnBase()[1] == 0 && gameState.getRunnersOnBase()[2] == 0)) {
			baseSituation = MAN_ON_FIRST;
		}
		else if ((gameState.getRunnersOnBase()[0] == 0 && gameState.getRunnersOnBase()[1] != 0 && gameState.getRunnersOnBase()[2] == 0)) {
			baseSituation = MAN_ON_SECOND;
		}
		else if ((gameState.getRunnersOnBase()[0] == 0 && gameState.getRunnersOnBase()[1] == 0 && gameState.getRunnersOnBase()[2] != 0)) {
			baseSituation = MAN_ON_THIRD;
		}
		else if ((gameState.getRunnersOnBase()[0] != 0 && gameState.getRunnersOnBase()[1] != 0 && gameState.getRunnersOnBase()[2] == 0)) {
			baseSituation = MAN_ON_FIRST_AND_SECOND;
		}
		else if ((gameState.getRunnersOnBase()[0] == 0 && gameState.getRunnersOnBase()[1] != 0 && gameState.getRunnersOnBase()[2] != 0)) {
			baseSituation = MAN_ON_SECOND_AND_THIRD;
		}
		else if ((gameState.getRunnersOnBase()[0] != 0 && gameState.getRunnersOnBase()[1] == 0 && gameState.getRunnersOnBase()[2] != 0)) {
			baseSituation = MAN_ON_FIRST_AND_THIRD;
		}
		else if ((gameState.getRunnersOnBase()[0] != 0 && gameState.getRunnersOnBase()[1] != 0 && gameState.getRunnersOnBase()[2] != 0)) {
			baseSituation = BASES_LOADED;
		}
		return baseSituation;
	}
	
	private static ArrayList<Integer> getRandomLineupByPosition() {
		ArrayList<Integer> randomLineup = new ArrayList<Integer>();
		for (int i = 1; i < 10; i++) {
			randomLineup.add(new Integer(i));
        }
        Collections.shuffle(randomLineup);
        return randomLineup;
	}
	
	// For play mode
	private static boolean processCommand(String command) {
		if (command == null || command.length() == 0) {
			return true;
		}
		if (command.indexOf("STEAL") != -1) {
			int baseToSteal = 0;
			try {
				baseToSteal = Integer.parseInt(command.substring(command.length()-1));
			}
			catch (Exception e) {
			}
			if ((baseToSteal < 2 || baseToSteal > 4 ) ||
			   ((baseToSteal == 2 || baseToSteal == 3) && (gameState.getRunnersOnBase()[baseToSteal-2] == 0 || gameState.getRunnersOnBase()[baseToSteal-1] != 0)) ||
			    (baseToSteal == 4 && gameState.getRunnersOnBase()[baseToSteal-2] == 0)) {
					System.out.print("INVALID BASE TO STEAL!\n");
			}
			else {
				gameState.setOuts(gameState.getOuts() + stealBase(baseToSteal));
			}
			return false;
		}
		else {
			switch (command) {
				case "AUTOON":
					auto = true;
					autoBefore = 1000;
					break;
				case "?":
					System.out.print("COMMANDS - AUTOON STEAL<base>\n");
					return false;
			}
		}
		return true;
	}
	
	// Get batting stats from API
	/*private static void getBattingStatsFromAPI() {
		  //String getStatsAPI = "http://lookup-service-prod.mlb.com/json/named.sport_hitting_tm.bam?league_list_id='mlb'&game_type='R'&season='2019'&player_id='592450'";	
	      //String searchAPI = "http://lookup-service-prod.mlb.com/json/named.search_player_all.bam?sport_code='mlb'&active_sw='Y'&name_part='Aaron Judge'";
	        try {
	        	
	        	BattingStats battingStats;
	        	for (int top = 0; top < 2; top++) {
	    			for (int p = 0; p < Game.NUM_OF_PLAYERS_IN_LINEUP; p++) {
	    				String searchAPI = "http://lookup-service-prod.mlb.com/json/named.search_player_all.bam?sport_code=%27mlb%27&active_sw=%27Y%27&name_part=";
	    				String getStatsAPI = "http://lookup-service-prod.mlb.com/json/named.sport_hitting_tm.bam?league_list_id=%27mlb%27&game_type=%27R%27&season=%27" + year + "%27&player_id=";
	    				battingStats = new BattingStats();
	    				String playerName = gameResults.getLineup()[top][p].getName();
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
	        				gameResults.getLineup()[top][p].setId(Integer.parseInt(playerId));
	        				System.out.print(playerId + " ");
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
			        		battingStats.setStolenBases(Integer.parseInt(row.getString("sb")));
			        		battingStats.setCaughtStealing(Integer.parseInt(row.getString("cs")));
			        		battingStatsMap.put(gameResults.getLineup()[top][p].getId(), battingStats);
				        	System.out.println(" SR: " + battingStats.getSpeedRating());
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
		}*/
	
		// Get batting stats from DB
		private static void getBattingStatsFromDB() {
			HashMap<Object, Object> visitorBattingStats = DAO.getDataMap("MLB_BATTING_STATS", (Integer)franchisesMap.get(gameResults.getTeamNames()[0]), gameResults.getYears()[0]);
			HashMap<Object, Object> homeBattingStats = DAO.getDataMap("MLB_BATTING_STATS", (Integer)franchisesMap.get(gameResults.getTeamNames()[1]), gameResults.getYears()[1]);
			for (Map.Entry<Object, Object> entry : visitorBattingStats.entrySet()) {
				MLBBattingStats battingStats = (MLBBattingStats)entry.getValue();
				battingStatsMap.put(battingStats.getMlbPlayerId(), battingStats.getBattingStats());
			}
			for (Map.Entry<Object, Object> entry : homeBattingStats.entrySet()) {
				MLBBattingStats battingStats = (MLBBattingStats)entry.getValue();
				battingStatsMap.put(battingStats.getMlbPlayerId(), battingStats.getBattingStats());
			}
		}
		
		// Get batting stats from DB
		private static void getPitchingStatsFromDB() {
			HashMap<Object, Object> visitorPitchingStats = DAO.getDataMap("MLB_PITCHING_STATS", (Integer)franchisesMap.get(gameResults.getTeamNames()[0]), gameResults.getYears()[0]);
			HashMap<Object, Object> homePitchingStats = DAO.getDataMap("MLB_PITCHING_STATS", (Integer)franchisesMap.get(gameResults.getTeamNames()[1]), gameResults.getYears()[1]);
			for (Map.Entry<Object, Object> entry : visitorPitchingStats.entrySet()) {
				MLBPitchingStats pitchingStats = (MLBPitchingStats)entry.getValue();
				pitchingStatsMap.put(pitchingStats.getMlbPlayerId(), pitchingStats.getPitchingStats());
			}
			for (Map.Entry<Object, Object> entry : homePitchingStats.entrySet()) {
				MLBPitchingStats pitchingStats = (MLBPitchingStats)entry.getValue();
				pitchingStatsMap.put(pitchingStats.getMlbPlayerId(), pitchingStats.getPitchingStats());
			}
		}

}
