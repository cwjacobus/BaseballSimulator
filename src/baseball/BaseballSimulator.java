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
	public static final int NUM_OF_PLAYERS_IN_LINEUP = 9;
	
	static boolean simulationMode = true;
	static int autoBeforeInning = 1000;
	static GameState gameState = new GameState();
	static BoxScore[] boxScores = new BoxScore[2];
	static Roster[] rosters  = new Roster[2];
	static HashMap<Integer, BattingStats> battingStatsMap = new HashMap<Integer, BattingStats>();
	static HashMap<Integer, PitchingStats> pitchingStatsMap = new HashMap<Integer, PitchingStats>();
	static DecimalFormat df = new DecimalFormat(".000");
	static List<String> randoLog = new ArrayList<String>();
	static HashMap<?, ?> franchisesMap;
	
	static final int STRUCK_OUT = 0;
	static final int GROUNDED_OUT = 1;
	static final int FLEW_OUT = 2;
	static final int FLEW_OUT_DEEP = 3;
	static final int LINED_OUT = 4;
	static final int POPPED_OUT = 5;
	
	static final String[] nationalLeagueTeams = {"ARI","CHC","LAD","WSH","NYM","PIT","SD","SF","STL","PHI","ATL","MIA","MIL","COL","CIN"};
	
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
			System.out.println("Invalid args - expecting <visYear><vis><homeYear><home><MODE> - ex. 2019 HOU 2019 NYY [SIM|GAME|AUTO<#>]]");
			return;
		}
		DAO.setConnection();
		franchisesMap = DAO.getDataMap("MLB_FRANCHISE");
		String[] teamNames = {args[1], args[3]};
		for (int t = 0; t < 2; t++) {
			boxScores[t] = new BoxScore();
			rosters[t] = new Roster();
			if (franchisesMap.get(teamNames[t]) == null) {
				System.out.println("Invalid team name: " + teamNames[t]);
				return;
			}
			boxScores[t].setTeamName(teamNames[t]);
		}
		boxScores[0].setYear(Integer.parseInt(args[0]));
		boxScores[1].setYear(Integer.parseInt(args[2]));
		if (args.length > 4 && args[4] != null) {
			if (args[4].equalsIgnoreCase("GAME")) {
				autoBeforeInning = 0;
				simulationMode = false;
			}
			if (args[4].equalsIgnoreCase("SIM")) {
				autoBeforeInning = 1000;
				simulationMode = true;
			}
			else if (args[4].indexOf("AUTO") != -1) {
				try {
					autoBeforeInning = Integer.parseInt(args[4].substring(args[4].length()-1));
				}
				catch (Exception e) {
					autoBeforeInning = 1000;
				}
				simulationMode = true;
			}
		}
		if (!setLineup()) {
			return;
		}
		System.out.println("Starting pitchers : " + boxScores[0].getTeamName() + ": " + gameState.getCurrentPitchers()[0].getFirstLastName() + " v " + boxScores[1].getTeamName() + ": " + gameState.getCurrentPitchers()[1].getFirstLastName());
		//getBattingStatsFromAPI();
		getBattingStatsFromDB();
		getPitchingStatsFromDB();
		while (gameState.getInning() <= INNINGS_PER_GAME || boxScores[0].getScore(gameState.getInning()) == boxScores[1].getScore(gameState.getInning())) {
			int inning = gameState.getInning();
			Scanner myObj = null;
			for (int top = 0; top < 2; top++) {
				BoxScore boxScore = boxScores[top];
				ArrayList<ArrayList<Player>> batters = boxScore.getBatters();
				gameState.setTop(top);
				gameState.setOuts(0);
				System.out.println((top == 0 ? "\n***TOP " : "***BOTTOM ") + " INN: " + inning + " ***");
				if (top == 0) {
					System.out.println("SCORE - " + boxScores[0].getTeamName() + ": " + boxScores[0].getScore(gameState.getInning())  + " " + 
						boxScores[1].getTeamName() + ": " + boxScores[1].getScore(gameState.getInning()));
				}
				//int outs  = 0;
				boolean gameTiedStartOfAB;
				Arrays.fill(gameState.getRunnersOnBase(), 0);
				while (gameState.getOuts() < OUTS_PER_INNING) {
					Player currentBatter = batters.get(gameState.getBattingOrder()[top] - 1).get(batters.get(gameState.getBattingOrder()[top] - 1).size() - 1);
					System.out.println(currentBatter.getName() + " UP OUTS: " + gameState.getOuts() + " "  + gameState.getBaseSituations().get(gameState.getCurrentBasesSituation()) + 
						" (" + getPlayerNameFromId(gameState.getRunnersOnBase()[0]) + ":" + getPlayerNameFromId(gameState.getRunnersOnBase()[1]) + ":" + getPlayerNameFromId(gameState.getRunnersOnBase()[2]) + ")");
					
					BattingStats currentBatterGameStats = currentBatter.getBattingStats();
					BattingStats currentBatterSeasonStats = battingStatsMap.get(currentBatter.getId());
					PitchingStats currentPitcherGameStats = boxScores[top==0?1:0].getPitchers().get(gameState.getCurrentPitchers()[top==0?1:0].getId()).getPitchingStats();
					PitchingStats currentPitcherSeasonStats = pitchingStatsMap.get(gameState.getCurrentPitchers()[top==0?1:0].getId()) != null ? 
						pitchingStatsMap.get(gameState.getCurrentPitchers()[top==0?1:0].getId()) : new PitchingStats();
						
					if (!simulationMode || autoBeforeInning <= inning) {
						myObj = new Scanner(System.in);
						System.out.print("PITCH: ");
					    String command = myObj.nextLine();
					    if (!processCommand(command, currentPitcherGameStats, currentBatter)) {
					    	continue;
					    }
					}
					if (simulationMode && autoBeforeInning > inning) { // Steal 2?
						if (isRunnerStealing(2)) {
							int sbOuts = stealBase(2);
							if (sbOuts > 0) {
								currentPitcherGameStats.incrementInningsPitched(1);
								gameState.setOuts(gameState.getOuts() + sbOuts);
								// Check if 3 outs decrement batting order so same batter bats next inning
							}
						}
					}
					if (gameState.getOuts() == OUTS_PER_INNING) {
						break;
					}
					int rando = getRandomNumberInRange(1, 1000);
					
					if (processOtherAction(currentBatter)) {
						gameState.getBattingOrder()[top] = gameState.getBattingOrder()[top] == 9 ? 1 : gameState.getBattingOrder()[top] + 1;
						continue;
					}
					
					gameTiedStartOfAB = boxScores[1].getScore(inning) == boxScores[0].getScore(inning);
					long onBaseEndPoint = 1000 - Math.round(currentBatterSeasonStats.getOnBasePercentage()*1000);
					if (rando <= onBaseEndPoint) { // OUT
						int outResult = getOutResult(currentBatter, currentBatterSeasonStats, currentPitcherGameStats, currentPitcherSeasonStats);
						gameState.setOuts(gameState.getOuts() + outResult);
						currentBatterGameStats.incrementAtBats();
						currentPitcherGameStats.incrementInningsPitched(outResult);
					}
					else {
						long bbEndPoint = 1000 - (Math.round(((currentBatterSeasonStats.getWalkRate() + currentPitcherSeasonStats.getWalkRate())/2)*1000));
						if (rando > onBaseEndPoint && rando <= bbEndPoint) {
							if (rando >= (bbEndPoint - 20)) {  // Hardcoded HBP rate
								System.out.println("HIT BY PITCH");
							}
							else {
								System.out.println("WALKED");
								currentBatterGameStats.incrementWalks();
								currentPitcherGameStats.incrementWalks();
							}
							updateBasesSituationNoRunnersAdvance(currentBatter);
						}
						else { // HIT
							int noOutResult = getNotOutResult(currentBatterGameStats, currentBatterSeasonStats, currentPitcherGameStats);
							if (noOutResult == 1 && (getRandomNumberInRange(0, 5) + currentBatterGameStats.getSpeedRating()) > 4) { // infield single ?
								if (gameState.getOuts() != 2) {  // less than 2 outs runners hold
									updateBasesSituationNoRunnersAdvance(currentBatter);
								}
								else {
									updateBasesSituationRunnersAdvance(noOutResult, currentBatter);
								}
								System.out.println("INFIELD SINGLE");
							}
							else {
								updateBasesSituationRunnersAdvance(noOutResult, currentBatter);
							}
							boxScore.incrementHits();
							currentBatterGameStats.incrementHits();
							currentBatterGameStats.incrementAtBats();
							currentPitcherGameStats.incrementHitsAllowed();
						}
						currentBatter.setBattingStats(currentBatterGameStats);
						if (inning >= 9 && boxScores[1].getScore(inning) > boxScores[0].getScore(inning) && gameTiedStartOfAB) {
							boxScore.setWalkOff(true);
							System.out.println("WALKOFF ");
							break;
						}
					}
					gameState.getBattingOrder()[top] = gameState.getBattingOrder()[top] == 9 ? 1 : gameState.getBattingOrder()[top] + 1;
					gameState.setHitAndRun(false);  // clear hit and run, if on
				} // outs
				// Did game end after top of inning?
				if (inning >= 9 && boxScores[1].getScore(inning) > boxScores[0].getScore(inning) && top == 0) {
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
	
	private static int getNotOutResult(BattingStats batterGameStats, BattingStats batterSeasonStats, PitchingStats pitcherGameStats) {
		long errorEndPoint = 25;
		long hrEndPoint = (batterSeasonStats != null && batterSeasonStats.getHits() != 0 ? 
			Math.round((((double)batterSeasonStats.getHomeRuns()/batterSeasonStats.getHits())*1000)) : 160) + errorEndPoint;
		hrEndPoint = batterSeasonStats.getHomeRuns() == 0 ? 8 + errorEndPoint : hrEndPoint;       // Give some chance if batter has 0 hrs
		long triplesEndPoint = (batterSeasonStats != null && batterSeasonStats.getHits() != 0 ? 
			Math.round((((double)batterSeasonStats.getTriples()/batterSeasonStats.getHits())*1000)) : 18) + hrEndPoint;
		triplesEndPoint = batterSeasonStats.getTriples() == 0 ? 8 + hrEndPoint : triplesEndPoint; // Give some chance if batter has 0 triples
		long doublesEndPoint = (batterSeasonStats != null && batterSeasonStats.getHits() != 0 ? 
			Math.round((((double)batterSeasonStats.getDoubles()/batterSeasonStats.getHits())*1000)) : 203) + triplesEndPoint;
		int notOutResult = 1;
		int notOutRando = getRandomNumberInRange(1, 1000);
		if (notOutRando > 1 && notOutRando <= errorEndPoint) {
			System.out.println("REACHED BY ERROR");
			notOutResult = 1;
			boxScores[gameState.getTop() == 0 ? 1 : 0].incrementErrors();
		}
		else if (notOutRando > errorEndPoint && notOutRando <= hrEndPoint) {
			System.out.println("HOME RUN");
			notOutResult = 4;
			batterGameStats.incrementHomeRuns();
			pitcherGameStats.incrementHomeRunsAllowed();
		}
		else if (notOutRando > hrEndPoint && notOutRando <= triplesEndPoint) {
			System.out.println("TRIPLE");
			notOutResult = 3;
			batterGameStats.incrementTriples();
		}
		else if (notOutRando > triplesEndPoint && notOutRando < doublesEndPoint) {
			System.out.println("DOUBLE");
			notOutResult = 2;
			batterGameStats.incrementDoubles();
		}
		else {
			System.out.println("SINGLE");
		}
		
		return notOutResult;
	}
	
	private static int getOutResult(Player currentBatter, BattingStats batterSeasonStats, PitchingStats pitcherGameStats, PitchingStats pitcherSeasonStats) {
		int outsRecorded = 1;
		int notOutRando = getRandomNumberInRange(1, 100);
		BoxScore boxScore = boxScores[gameState.getTop()];
		long soEndPoint = Math.round(((pitcherSeasonStats.getStrikeoutRate()+batterSeasonStats.getStrikeoutRate())/2)*100);
		long outIncrement = Math.round((double)((100 - soEndPoint)/5));
		if (notOutRando > 1 && notOutRando <= soEndPoint) { // STRUCK OUT
			System.out.println(outTypes.get(STRUCK_OUT)); 
			currentBatter.getBattingStats().incremenStrikeOuts();
			pitcherGameStats.incrementStrikeouts();
		}
		else if (notOutRando > soEndPoint && notOutRando <= soEndPoint + outIncrement) {
			String groundBallRecipientPosition = positions.get(getRandomNumberInRange(1, 6));
			System.out.println(outTypes.get(GROUNDED_OUT) + " TO " + groundBallRecipientPosition); //GROUNDED OUT
			if (doublePlay(true)) {
				outsRecorded++;
			}
			else {
				fieldersChoice(groundBallRecipientPosition, currentBatter);
			}
		}
		else if (notOutRando > soEndPoint + outIncrement && notOutRando <= soEndPoint + (outIncrement*2)) {
			System.out.println(outTypes.get(FLEW_OUT) +  " TO " + positions.get(getRandomNumberInRange(7, 9))); // FLEW OUT
			int bo3 = getBattingOrderForPlayer(gameState.getRunnersOnBase()[2]);
			if (bo3 != 0) {  // Only tag up if there is a runner on 3rd
				Player runnerOnThird = boxScore.getBatters().get(bo3 - 1).get(boxScore.getBatters().get(bo3 - 1).size() - 1);
				BattingStats bs3 = battingStatsMap.get(runnerOnThird.getId());
				if (gameState.getOuts() < 2 && bs3.getSpeedRating() > 2) {
					if (updateBasesSituationSacFly(runnerOnThird, false) == 1) {
						outsRecorded++;
					}
				}
			}
		}
		else if (notOutRando > soEndPoint + (outIncrement*2) && notOutRando <= soEndPoint + (outIncrement*3)) {
			System.out.println(outTypes.get(FLEW_OUT_DEEP) +  " TO " + positions.get(getRandomNumberInRange(7, 9))); // FLEW OUT DEEP
			int bo3 = getBattingOrderForPlayer(gameState.getRunnersOnBase()[2]);
			if (bo3 != 0) {  // Only tag up if there is a runner on 3rd
				Player runnerOnThird = boxScore.getBatters().get(bo3 - 1).get(boxScore.getBatters().get(bo3 - 1).size() - 1);
				if (gameState.getOuts() < 2) { // Everyone tags with less than 2 outs, no dependency on runners speed
					if (updateBasesSituationSacFly(runnerOnThird, true) == 1) {
						outsRecorded++;
					}
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
	
	private static boolean processOtherAction (Player currentBatter) {
		boolean actionProcessed = false;
		if (gameState.isBuntAttempt()) {
			System.out.println(currentBatter.getFirstLastName() + " attempted a bunt");
			updateBasesSituationSacBunt(currentBatter);
			gameState.setBuntAttempt(false);
			actionProcessed = true;
		}
		else if (gameState.isIntentionalWalk()) {
			System.out.println(currentBatter.getFirstLastName() + " was intentionally walked");
			updateBasesSituationNoRunnersAdvance(currentBatter);
			gameState.setIntentionalWalk(false);
			actionProcessed = true;
		}
		return actionProcessed;
	}
	
	private static void updateBasesSituationRunnersAdvance(int event, Player currentBatter) {
	  /*int basesSituation = (currentBasesSituation << event) + (int)Math.pow(2, (event - 1));
		if (basesSituation > 7) {
			basesSituation = basesSituation % 8;
		}
		currentBasesSituation = basesSituation;*/
		BoxScore boxScore = boxScores[gameState.getTop()];
		ArrayList<ArrayList<Player>> batters = boxScore.getBatters();
		int runsScored = 0;
		for (int e = 0; e < event; e++) {
			for (int b = 3; b >= 0; b--) {
				if (b > 0) {
					if (b != 3) {
						gameState.getRunnersOnBase()[b] = gameState.getRunnersOnBase()[b-1];
					}
				}
				else if (b == 0 && e == 0) {
					gameState.getRunnersOnBase()[b] = currentBatter.getId();
				}
				else {
					gameState.getRunnersOnBase()[0] = 0;
				}
				if (b == 3 && gameState.isBaseOccupied(3)) {     // If runner on 3rd run scores
					int bo = getBattingOrderForPlayer(gameState.getRunnersOnBase()[2]);
					Player runnerOnThird = batters.get(bo - 1).get(batters.get(bo - 1).size() - 1);
					runnerOnThird.getBattingStats().incrementRuns();
					runsScored++;
				}
			}
		}
		if (runsScored > 0) {
			boxScore.setRunsScored(gameState.getInning(), runsScored);
			System.out.println(runsScored + " RUNS SCORED - VIS: " + boxScores[0].getScore(gameState.getInning())  + " HOME: " + boxScores[1].getScore(gameState.getInning()));
		}
	}
	
	// For walks, hit by pitch and some infield singles
	private static void updateBasesSituationNoRunnersAdvance(Player currentBatter) { 
		BoxScore boxScore = boxScores[gameState.getTop()];
		// No need for checking empty, 2, 3, or 23
		if (gameState.getCurrentBasesSituation() == GameState.BASES_LOADED || gameState.getCurrentBasesSituation() == GameState.MAN_ON_FIRST_AND_SECOND) { // 123 or 12
			// if 123 runner 3 scores
			if (gameState.getCurrentBasesSituation() == GameState.BASES_LOADED) {
				boxScore.incrementRunsScored(gameState.getInning()); // run scores
				int bo = getBattingOrderForPlayer(gameState.getRunnersOnBase()[2]);
				boxScore.getBatters().get(bo - 1).get(boxScore.getBatters().get(bo - 1).size() - 1).getBattingStats().incrementRuns();
				System.out.println("1 RUN SCORED - VIS: " + boxScores[0].getScore(gameState.getInning())  + " HOME: " + boxScores[1].getScore(gameState.getInning()));
			}
			gameState.getRunnersOnBase()[2] = gameState.getRunnersOnBase()[1]; // runner 2->3
		}
		if (gameState.isBaseOccupied(1)) { // Runner on first
			gameState.getRunnersOnBase()[1] = gameState.getRunnersOnBase()[0]; // runner 1->2
		}
		gameState.getRunnersOnBase()[0] = currentBatter.getId();
	}
	
	private static void updateBasesSituationSacBunt(Player currentBatter) { 
		int buntRando = getRandomNumberInRange(0, 100);
		if (buntRando < 81) { // 80 %
			System.out.println("SUCCESSFUL BUNT!");
			if (gameState.isBaseOccupied(2)) { // Runner on second or first and second
				gameState.getRunnersOnBase()[2] = gameState.getRunnersOnBase()[1]; // runner 2->3
				gameState.getRunnersOnBase()[1] = 0;
			}
			else if (gameState.isBaseOccupied(1)) { // Runner on first or first and third
				gameState.getRunnersOnBase()[1] = gameState.getRunnersOnBase()[0]; // runner 1->2
				gameState.getRunnersOnBase()[0] = 0;
			}
		}
		else if (buntRando < 94) { // 13 %
			System.out.println("UNSUCCESSFUL BUNT!"); 
			fieldersChoice("P", currentBatter);
			
		}
		else { // 7%
			System.out.println("UNSUCCESSFUL BUNT! DOUBLE PLAY?");
			// TBD
		}
		gameState.incrementOuts();
		//currentPitcherGameStats.incrementInningsPitched(1);
		// incrementAB?
	}
	
	private static int updateBasesSituationSacFly(Player runnerOnThird, boolean deep) {
		int outAdvancing = 0;
		BoxScore boxScore = boxScores[gameState.getTop()];
		BattingStats bs = battingStatsMap.get(runnerOnThird.getId());
		int sacRando = getRandomNumberInRange(0, 5) + bs.getSpeedRating();
		sacRando += deep ? 5 : 0;  // Tagging on deep FB should be almost a sure thing
		System.out.println(runnerOnThird.getFirstLastName() + " TAGGING UP ON A FLY BALL");
		if (sacRando > 5) { // safe
			boxScore.incrementRunsScored(gameState.getInning()); // run scores
			runnerOnThird.getBattingStats().incrementRuns();
			System.out.println("SAC FLY - 1 RUN SCORED - VIS: " + boxScores[0].getScore(gameState.getInning())  + " HOME: " + boxScores[1].getScore(gameState.getInning()));
		}
		else { // out
			outAdvancing = 1;
			System.out.println("OUT AT THE PLATE");
		}
		gameState.getRunnersOnBase()[2] = 0;
		return outAdvancing;
	}
	
	private static void updateBasesSituationDoublePlay() {
		BoxScore boxScore = boxScores[gameState.getTop()];
		boolean runScores = gameState.getCurrentBasesSituation() == GameState.BASES_LOADED || gameState.getCurrentBasesSituation() == GameState.MAN_ON_FIRST_AND_THIRD;
		if (gameState.getCurrentBasesSituation() == GameState.MAN_ON_FIRST_AND_SECOND || gameState.getCurrentBasesSituation() == GameState.BASES_LOADED) {
			gameState.getRunnersOnBase()[2] = gameState.getRunnersOnBase()[1]; // 2->3
		}
		gameState.getRunnersOnBase()[1] = 0;
		gameState.getRunnersOnBase()[0] = 0;
		if (runScores) {
			boxScore.incrementRunsScored(gameState.getInning()); // run scores
			System.out.println("RUN SCORES - VIS: " + boxScores[0].getScore(gameState.getInning())  + " HOME: " + boxScores[1].getScore(gameState.getInning()));
			int bo = getBattingOrderForPlayer(gameState.getRunnersOnBase()[2]);
			Player runnerOnThird = boxScore.getBatters().get(bo - 1).get(boxScore.getBatters().get(bo - 1).size() - 1);
			runnerOnThird.getBattingStats().incrementRuns();
		}
	}
	private static void updateBasesSituationFieldersChoice(Player currentBatter) {
		if (gameState.getCurrentBasesSituation() == GameState.BASES_LOADED) {
			gameState.getRunnersOnBase()[2] = gameState.getRunnersOnBase()[1];
			gameState.getRunnersOnBase()[1] = gameState.getRunnersOnBase()[0];
		}
		else if (gameState.isBaseOccupied(2)) {
			gameState.getRunnersOnBase()[2] = gameState.getRunnersOnBase()[1];
			gameState.getRunnersOnBase()[1] = 0;
		}
		else {
			gameState.getRunnersOnBase()[1] = 0;
		}
		gameState.getRunnersOnBase()[0] = currentBatter.getId();
	}
	
	private static boolean doublePlay(boolean ground) {
		boolean dp = false;
		BoxScore boxScore = boxScores[gameState.getTop()];
		if (!gameState.isBaseOccupied(1) || gameState.getOuts() == 2 || !ground) { // Must a ground out, less than 2 outs and runner on 1st
			return false;
		}
		int bo = getBattingOrderForPlayer(gameState.getRunnersOnBase()[0]);
		Player runnerOnFirst = boxScore.getBatters().get(bo - 1).get(boxScore.getBatters().get(bo - 1).size() - 1);
		BattingStats bs = battingStatsMap.get(runnerOnFirst.getId());
		int dpRando = getRandomNumberInRange(0, 5) + bs.getSpeedRating();
		// Ground ball, less than 2 outs, man on first
		if (dpRando < 5) {
			dp = true;
			updateBasesSituationDoublePlay();
			System.out.println("DOUBLE PLAY");
		}
		return dp;
	}
	
	private static void fieldersChoice(String groundBallRecipientPosition, Player currentBatter) {
		if (gameState.getCurrentBasesSituation() == GameState.BASES_EMPTY || gameState.getCurrentBasesSituation() == GameState.MAN_ON_THIRD || 
			gameState.getCurrentBasesSituation() == GameState.MAN_ON_SECOND_AND_THIRD || gameState.getOuts() == 2 || gameState.isHitAndRun()) {
				return;
		}
		else {
			System.out.println("FIELDER'S CHOICE");
			updateBasesSituationFieldersChoice(currentBatter);
		}
	}
	
	private static boolean isRunnerStealing(int baseToSteal) {
		boolean runnerIsStealing = false;
		BoxScore boxScore = boxScores[gameState.getTop()];
		int runnerStealingIndex = baseToSteal - 2;
		int nextBaseIndex = baseToSteal == 4 ? 2 : (runnerStealingIndex + 1);
		if (!gameState.isBaseOccupied(runnerStealingIndex+1) || gameState.isBaseOccupied(nextBaseIndex+1)) {
			return false;
		}
		int bo = getBattingOrderForPlayer(gameState.getRunnersOnBase()[runnerStealingIndex]);
		Player runnerStealing = boxScore.getBatters().get(bo - 1).get(boxScore.getBatters().get(bo - 1).size() - 1);
		BattingStats bs = battingStatsMap.get(runnerStealing.getId());
		int stealRando = getRandomNumberInRange(0, 5) + bs.getSpeedRating();
		if (stealRando > 5) {
			runnerIsStealing = true;
		}
		return runnerIsStealing;
	}
	
	private static int stealBase(int baseToSteal) {
		BoxScore boxScore = boxScores[gameState.getTop()];
		int outStealing = 0;
		int runnerStealingIndex = baseToSteal - 2;  
		int nextBaseIndex = baseToSteal == 4 ? 2 : (runnerStealingIndex + 1);
		int bo = getBattingOrderForPlayer(gameState.getRunnersOnBase()[runnerStealingIndex]);
		Player runnerStealing = boxScore.getBatters().get(bo - 1).get(boxScore.getBatters().get(bo - 1).size() - 1);
		BattingStats bs = battingStatsMap.get(runnerStealing.getId());
		double stealPctg = ((bs.getStolenBases() + bs.getCaughtStealing()) != 0) ? 
			(double)bs.getStolenBases()/(bs.getStolenBases() + bs.getCaughtStealing()) : 0.2;  // Give a chance if no SB
		System.out.print(runnerStealing.getFirstLastName() + " ATTEMPTING TO STEAL " + baseToSteal + " - SR: " + bs.getSpeedRating() + " SP: " + df.format(stealPctg));
		if (getRandomNumberInRange(1, 10) < Math.round(stealPctg*10)) { // safe
			System.out.println("- SAFE!");
			gameState.getRunnersOnBase()[nextBaseIndex] = gameState.getRunnersOnBase()[runnerStealingIndex];
		}
		else { // out
			System.out.println("- OUT!");
			outStealing = 1;
		}
		gameState.getRunnersOnBase()[runnerStealingIndex] = 0;
		System.out.println(gameState.getBaseSituations().get(gameState.getCurrentBasesSituation()) + " " + gameState.getRunnersOnBase()[0] + 
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
		ArrayList<ArrayList<Player>> batters;
		BoxScore boxScore;
		MLBPlayer mlbPlayer;
		int pitcherDHLineupPosition = 1;
		for (int t = 0 ; t < 2; t++) {
			boxScore = boxScores[t];
			batters = boxScore.getBatters();
			HashMap<Object, Object> pitchersMap = DAO.getPitchersMapByTeamAndYear((Integer)franchisesMap.get(boxScore.getTeamName()), boxScore.getYear());
			HashMap<Object, Object> battersMap = DAO.getBattersMapByTeamAndYear((Integer)franchisesMap.get(boxScore.getTeamName()), boxScore.getYear());
			rosters[t].setPitchers(pitchersMap);
			rosters[t].setBatters(battersMap);
			// Get random starter 1-5
			MLBPlayer startingPitcher = DAO.getStartingPitcherByIndex((Integer)franchisesMap.get(boxScore.getTeamName()), boxScore.getYear(), getRandomNumberInRange(1, 5));  
			gameState.getCurrentPitchers()[t] = new Player(startingPitcher.getFullName(), startingPitcher.getMlbPlayerId(), "P");
			boxScore.getPitchers().put(startingPitcher.getMlbPlayerId(), new Player(startingPitcher.getFullName(), startingPitcher.getMlbPlayerId(), "P"));
			ArrayList<Integer> randomLineup = getRandomLineupByPosition();
			ArrayList<Integer> outfielderIdList = new ArrayList<>();
			ArrayList<Integer> battersPlayerIdList = new ArrayList<>();
			for (int p = 0 ; p < NUM_OF_PLAYERS_IN_LINEUP; p++) {
				Integer position = randomLineup.get(p);
				if (!positions.get(position).equals("P")) {
					mlbPlayer = DAO.getMlbPlayerWithMostPlateAppearances((Integer)franchisesMap.get(boxScore.getTeamName()), boxScore.getYear(), positions.get(position));
					if (mlbPlayer != null && mlbPlayer.getMlbPlayerId() != null) {
						batters.get(p).add(new Player(mlbPlayer.getFullName(), mlbPlayer.getMlbPlayerId(), positions.get(position)));
						battersPlayerIdList.add(mlbPlayer.getMlbPlayerId());
					}
					else {
						// No specific OF positions before 1987
						if (positions.get(position).equals("LF") || positions.get(position).equals("CF") || positions.get(position).equals("RF")) {
							mlbPlayer = DAO.getMlbPlayerWithMostPlateAppearances((Integer)franchisesMap.get(boxScore.getTeamName()), boxScore.getYear(), "OF", outfielderIdList);
							if (mlbPlayer != null && mlbPlayer.getMlbPlayerId() != null) {
								batters.get(p).add(new Player(mlbPlayer.getFullName(), mlbPlayer.getMlbPlayerId(), positions.get(position)));
								outfielderIdList.add(mlbPlayer.getMlbPlayerId());
								battersPlayerIdList.add(mlbPlayer.getMlbPlayerId());
							}
							else {
								System.out.println("No players at: OF for " + boxScore.getTeamName());
								return false;
							}
						}
						else {
							System.out.println("No players at: " + positions.get(position) + " for " + boxScore.getTeamName());
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
			if (Arrays.asList(nationalLeagueTeams).contains(boxScores[1].getTeamName()) || boxScores[1].getYear() < 1973) {
				batters.get(pitcherDHLineupPosition).add(new Player(gameState.getCurrentPitchers()[t].getName(), gameState.getCurrentPitchers()[t].getId(), "P"));
				battingStatsMap.put(gameState.getCurrentPitchers()[t].getId(), new BattingStats(75, 10, 2, 0, 1, 2, 44, 0, 4, 3, 0, 100, 0)); // Default pitcher batting stats
			}
			else { // DH
				mlbPlayer = DAO.getMlbPlayerWithMostPlateAppearances((Integer)franchisesMap.get(boxScore.getTeamName()), boxScore.getYear(), battersPlayerIdList);
				batters.get(pitcherDHLineupPosition).add(new Player(mlbPlayer.getFullName(), mlbPlayer.getMlbPlayerId(), "DH"));
			}
		}
		return true;
	}
	
	private static void outputBoxScore() {
		BoxScore boxScore;
		for (int top = 0; top < 2; top++) {
			boxScore = boxScores[top];
			String team = (top == 0) ? "\n" + boxScore.getTeamName() : boxScore.getTeamName();
			team += team.length() < 3 ? " " : "";
			System.out.print(team + " ");
			for (int i = 1; i < gameState.getInning(); i++) {
				if (i == (gameState.getInning() - 1) && (boxScores[1].getScore(i) > boxScores[0].getScore(i) && top == 1) && !boxScore.isWalkOff()) {
					System.out.print("X "); // Bottom of inning was not necessary
				}
				else {
					System.out.print(boxScore.getRunsScored(i) + (boxScores[top == 0 ? 1: 0].getRunsScored(i) < 10 ? " " : "  "));
				}
			}
			System.out.println(" " + boxScore.getScore(gameState.getInning()) + (boxScore.getScore(gameState.getInning()) < 10 ? " " : "") + " " + boxScore.getHits() + 
				(boxScore.getHits() < 10 ? " " : "") + " " +  boxScore.getErrors());
		}
		for (int top = 0; top < 2; top++) {
			ArrayList<ArrayList<Player>> batters = boxScores[top].getBatters();
			System.out.println();
			System.out.print(boxScores[top].getTeamName() + " ");
			System.out.println("Hitting\t\t\t" + "AB  R   H  RBI  BB  K    AVG  OBP  SLG");
			for (ArrayList<Player> playerList : batters) {
				for (Player batter : playerList) {
					BattingStats gameStats = batter.getBattingStats();
					BattingStats playerSeasonStats = battingStatsMap.get(batter.getId());
					String playerOutput = batter.getName() + " " + batter.getPosition();
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
		}
		System.out.println("BATTING");
		String[] doublesString = {"", ""};
		String[] triplesString = {"", ""};
		String[] homeRunsString = {"", ""};
		for (int top = 0; top < 2; top++) {
			ArrayList<ArrayList<Player>> batters = boxScores[top].getBatters();
			for (ArrayList<Player> playerList : batters) {
				for (Player batter : playerList) {
					int numD = batter.getBattingStats().getDoubles();
					int numT = batter.getBattingStats().getTriples();
					int numH = batter.getBattingStats().getHomeRuns();
					if (numD > 0) {
						doublesString[top] += (batter.getFirstLastName() + (numD > 1 ? "(" + numD + ")" : "") + ", ");
					}
					if (numT > 0) {
						triplesString[top] += (batter.getFirstLastName() + (numT > 1 ? "(" + numT + ")" : "") + ", ");
					}
					if (numH > 0) {
						homeRunsString[top] += (batter.getFirstLastName() + (numH > 1 ? "(" + numH + ")" : "") + ", ");
					}
				}
			}
		}
		if (doublesString[0].length() > 0 || doublesString[1].length() > 0)  {
			System.out.println("2B");
			for (int top = 0; top < 2; top++) {
				if (doublesString[top].length() == 0) {
					continue;
				}
				System.out.println(boxScores[top].getTeamName());
				System.out.println(doublesString[top].substring(0, doublesString[top].length()-2));
			}
		}
		if (triplesString[0].length() > 0 || triplesString[1].length() > 0)  {
			System.out.println("3B");
			for (int top = 0; top < 2; top++) {
				if (triplesString[top].length() == 0) {
					continue;
				}
				System.out.println(boxScores[top].getTeamName());
				System.out.println(triplesString[top].substring(0, triplesString[top].length()-2));
			}
		}
		if (homeRunsString[0].length() > 0 || homeRunsString[1].length() > 0)  {
			System.out.println("HR");
			for (int top = 0; top < 2; top++) {
				if (homeRunsString[top].length() == 0) {
					continue;
				}
				System.out.println(boxScores[top].getTeamName());
				System.out.println(homeRunsString[top].substring(0, homeRunsString[top].length()-2));
			}
		}
		System.out.println();
		for (int top = 0; top < 2; top++) {
			System.out.println();
			System.out.print(boxScores[top].getTeamName() + " ");
			System.out.println("Pitching\t\t\t" + "IP    H   R   ER  BB  K   HR   ERA");
			HashMap<Integer, Player> pitchers = boxScores[top].getPitchers();
			for (Map.Entry<Integer, Player> entry : pitchers.entrySet()) {
				PitchingStats ps = entry.getValue().getPitchingStats();
				System.out.print(entry.getValue().getFirstLastName());
				System.out.print("\t\t");
				if (entry.getValue().getFirstLastName().length() < 16) {
					System.out.print("\t");
				}
				System.out.print(ps.getInningsPitched() + (ps.getInningsPitched() > 9.2 ? "  " : "   "));
				System.out.print(ps.getHitsAllowed() + (ps.getHitsAllowed() > 9 ? "  " : "   "));
				System.out.print(ps.getRunsAllowed() + (ps.getRunsAllowed() > 9 ? "  " : "   "));
				System.out.print(ps.getEarnedRunsAllowed() + (ps.getEarnedRunsAllowed() > 9 ? "  " : "   "));
				System.out.print(ps.getWalks() + (ps.getWalks() > 9 ? "  " : "   "));
				System.out.print(ps.getStrikeouts() + (ps.getStrikeouts() > 9 ? "  " : "   "));
				System.out.print(ps.getHomeRunsAllowed() + (ps.getHomeRunsAllowed() > 9 ? "  " : "   "));
				System.out.println(".000");
			}
		}
	}
	
	static int getBattingOrderForPlayer(int id) {
		// Returns 0 if not found
		int order = 1;
		ArrayList<ArrayList<Player>> batters = boxScores[gameState.getTop()].getBatters();
		for (ArrayList<Player> playerList : batters) {
			for (Player p : playerList) {
				if (p.getId() == id) {
					return order;
				}
			}
			order++;
		}
		return 0;
	}
	static Player getPlayerFromId(int id) {
		if (id == 0) {
			return null;
		}
		int bo = getBattingOrderForPlayer(id);
		return boxScores[gameState.getTop()].getBatters().get(bo - 1).get(boxScores[gameState.getTop()].getBatters().get(bo - 1).size() - 1);
	}
	
	static String getPlayerNameFromId(int id) {
		Player player = getPlayerFromId(id);
		return player != null ? player.getFirstLastName() : "<>";
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
	private static boolean processCommand(String command, PitchingStats currentPitcherGameStats, Player currentBatter) {
		if (command == null || command.length() == 0) { // NOOP (if CR entered)
			return true;
		}
		command = command.toUpperCase();
		int top = gameState.getTop();
		HashMap<Integer, Player> gamePitchers = boxScores[top==0?1:0].getPitchers();
		HashMap<Object, Object> rosterPitchers = rosters[top==0?1:0].getPitchers();
		ArrayList<ArrayList<Player>> gameBatters = boxScores[top].getBatters();
		HashMap<Object, Object> rosterBatters = rosters[top].getBatters();
		if (command.indexOf("STEAL") != -1) {
			int baseToSteal = 0;
			try {
				baseToSteal = Integer.parseInt(command.substring(command.length()-1));
			}
			catch (Exception e) {
			}
			if ((baseToSteal < 2 || baseToSteal > 4 ) ||
			   ((baseToSteal == 2 || baseToSteal == 3) && (!gameState.isBaseOccupied(baseToSteal-1) || gameState.isBaseOccupied(baseToSteal))) ||
			    (baseToSteal == 4 && !gameState.isBaseOccupied(baseToSteal-1))) {
					System.out.print("INVALID BASE TO STEAL!\n");
			}
			else {
				int sbOuts = stealBase(baseToSteal);
				if (sbOuts > 0) {
					currentPitcherGameStats.incrementInningsPitched(1);
					gameState.setOuts(gameState.getOuts() + sbOuts);
				}
			}
			return false;
		}
		else if (command.indexOf("AUTO") != -1) {
			try {
				autoBeforeInning = Integer.parseInt(command.substring(command.length()-1));
			}
			catch (Exception e) {
			}
			simulationMode = true;
			return true;
		}
		else if (command.indexOf("SUBP") != -1) {
			String[] commandArray = command.split(" ");
			if (commandArray.length < 2) {
				System.out.print("INVALID COMMAND!\n");
				return false;
			}
			int newPitcherId = Integer.parseInt(commandArray[1]);
			MLBPlayer newPitcher = (MLBPlayer)rosterPitchers.get(newPitcherId);
			if (newPitcher == null) {
				System.out.print("No pitcher found for " + newPitcherId + "!\n");
				return false;
			}
			if (gamePitchers.get(newPitcherId) != null) {
				System.out.print("Pitcher has already pitched in this game " + newPitcherId + "!\n");
				return false;
			}
			Player newPitcherPlayer = new Player(newPitcher.getFullName(), newPitcher.getMlbPlayerId(), "P");
			boxScores[top==0?1:0].getPitchers().put(newPitcher.getMlbPlayerId(), newPitcherPlayer);
			gameState.setCurrentPitcher(newPitcherPlayer, top==0?1:0);
			System.out.println("Pitcher changed to: " + newPitcher.getFirstLastName() + "\n");
			return false;
		}
		else if (command.indexOf("SUBB") != -1) {
			String[] commandArray = command.split(" ");
			if (commandArray.length < 2) {
				System.out.print("INVALID COMMAND!\n");
				return false;
			}
			 
			int newBatterId = Integer.parseInt(commandArray[1]);
			MLBPlayer newBatter = (MLBPlayer)rosterBatters.get(newBatterId);
			if (newBatter == null) {
				System.out.print("No batter found for " + newBatterId + "!\n");
				return false;
			}
			if (getBattingOrderForPlayer(newBatterId) != 0) {
				System.out.print("Batter has already hit in this game " + newBatterId + "!\n");
				return false;
			}
			Player newBatterPlayer = new Player(newBatter.getFullName(), newBatter.getMlbPlayerId(), currentBatter.getPosition());
			int bo = gameState.getBattingOrder()[gameState.getTop()];
			gameBatters.get(bo - 1).add(newBatterPlayer);
			System.out.println("Batter changed to: " + newBatter.getFirstLastName() + "\n");
			return false;
		}
		else {
			switch (command) {
				case "SIM":
					simulationMode = true;
					autoBeforeInning = 1000;
					return true;
				case "BATTERS":
					System.out.println("\nEligible Pinch hitters:");
					for (Map.Entry<Object, Object> entry : rosterBatters.entrySet()) {
						MLBPlayer batter = (MLBPlayer)entry.getValue();
						if (getBattingOrderForPlayer(batter.getMlbPlayerId()) == 0) {
							System.out.println(batter.getFirstLastName() + " " + batter.getMlbPlayerId());
						}
					}
					System.out.println();
					return false;
				case "PITCHERS":
					System.out.println("\nEligible Pitchers:");
					for (Map.Entry<Object, Object> entry : rosterPitchers.entrySet()) {
						MLBPlayer pitcher = (MLBPlayer)entry.getValue();
						if (gamePitchers.get(pitcher.getMlbPlayerId()) == null) {
							System.out.println(pitcher.getFirstLastName() + " " + pitcher.getMlbPlayerId());
						}
					}
					System.out.println();
					return false;
				case "INTBB":
					gameState.setIntentionalWalk(true);
					return true;
				case "SACBUNT":
					if (gameState.getCurrentBasesSituation() == GameState.BASES_LOADED || gameState.getCurrentBasesSituation() == GameState.MAN_ON_SECOND_AND_THIRD || 
							gameState.getCurrentBasesSituation() == GameState.MAN_ON_THIRD || gameState.getCurrentBasesSituation() == GameState.BASES_EMPTY ||
							gameState.getOuts() >= 2) {
						System.out.println("CAN NOT SACIFICE BUNT IN THIS SITUATION!");
						return false;
					}
					else {
						gameState.setBuntAttempt(true);
					}
					return true;
				case "HITRUN":
					if (gameState.getCurrentBasesSituation() == GameState.BASES_EMPTY) {
						System.out.println("CAN NOT HIT AND RUN WITH BASES EMPTY!");
					}
					else {
						System.out.println("HIT AND RUN");
						gameState.setHitAndRun(true);
					}
					return false;
				case "?":
					System.out.print("COMMANDS - SIM, AUTO<inning#>, STEAL<#>, PITCHERS,  SUBP <id#>, BATTERS, SUBB <id#>, INTBB, SACBUNT, HITRUN, \n\n");
					return false;
				default:
					System.out.println("UNKNOWN COMMAND!");
					return false;
			}
		}
		//return true;
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
			HashMap<Object, Object> visitorBattingStats = DAO.getDataMap("MLB_BATTING_STATS", (Integer)franchisesMap.get(boxScores[0].getTeamName()), boxScores[0].getYear());
			HashMap<Object, Object> homeBattingStats = DAO.getDataMap("MLB_BATTING_STATS", (Integer)franchisesMap.get(boxScores[1].getTeamName()), boxScores[1].getYear());
			for (Map.Entry<Object, Object> entry : visitorBattingStats.entrySet()) {
				MLBBattingStats battingStats = (MLBBattingStats)entry.getValue();
				battingStatsMap.put(battingStats.getMlbPlayerId(), battingStats.getBattingStats());
			}
			for (Map.Entry<Object, Object> entry : homeBattingStats.entrySet()) {
				MLBBattingStats battingStats = (MLBBattingStats)entry.getValue();
				battingStatsMap.put(battingStats.getMlbPlayerId(), battingStats.getBattingStats());
			}
		}
		
		// Get pitching stats from DB
		private static void getPitchingStatsFromDB() {
			HashMap<Object, Object> visitorPitchingStats = DAO.getDataMap("MLB_PITCHING_STATS", (Integer)franchisesMap.get(boxScores[0].getTeamName()), boxScores[0].getYear());
			HashMap<Object, Object> homePitchingStats = DAO.getDataMap("MLB_PITCHING_STATS", (Integer)franchisesMap.get(boxScores[1].getTeamName()), boxScores[1].getYear());
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
