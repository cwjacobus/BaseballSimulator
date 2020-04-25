package baseball;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

//import org.json.JSONObject;

import dao.DAO;
import db.MLBBattingStats;
import db.MLBPlayer;

public class BaseballSimulator {
	
	public static final int OUTS_PER_INNING = 3;
	public static final int INNINGS_PER_GAME = 9;
	public static final int NUM_OF_PLAYERS_IN_LINEUP = 9;
	
	static boolean simulationMode = true;
	static int autoBeforeInning = 1000;
	static GameState gameState;
	static BoxScore[] boxScores;
	static Roster[] rosters;
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
		int seriesLength = 1;
		if (args == null || args.length < 4) {
			System.out.println("Invalid args - expecting <visYear> <vis> <homeYear> <homeYear> <MODE>[SIM|GAME|AUTO<#>] SERIES(opt#) - ex. 2019 HOU 2019 NYY SIM 7");
			return;
		}
		DAO.setConnection();
		rosters  = new Roster[2];
		franchisesMap = DAO.getDataMap("MLB_FRANCHISE");
		int years[] = {Integer.parseInt(args[0]), Integer.parseInt(args[2])};
		String[] teamNames = {args[1], args[3]};
		for (int t = 0; t < 2; t++) {
			rosters[t] = new Roster();
			if (franchisesMap.get(teamNames[t]) == null) {
				System.out.println("Invalid team name: " + teamNames[t]);
				return;
			}
			rosters[t].setPitchers(DAO.getPitchersMapByTeamAndYear((Integer)franchisesMap.get(teamNames[t]), years[t]));
			rosters[t].setBatters(DAO.getBattersMapByTeamAndYear((Integer)franchisesMap.get(teamNames[t]), years[t]));
		}	
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
		if (args.length > 5 && args[5] != null) {
			seriesLength = Integer.parseInt(args[5]);
		}
		ArrayList<ArrayList<ArrayList<MLBPlayer>>> lineupBatters = setOptimalLineup(teamNames, years);
		if (lineupBatters == null) {
			return;
		}
		BoxScore[][] seriesBoxScores = new BoxScore[seriesLength][2];
		for (int s = 0; s < seriesLength; s++) {
			gameState = new GameState();
			boxScores = new BoxScore[2];
			for (int t = 0; t < 2; t++) {
				boxScores[t] = new BoxScore();
				boxScores[t].setYear(years[t]);
				boxScores[t].setTeamName(teamNames[t]);
				boxScores[t].setBatters(lineupBatters.get(t));
				clearPlayerGameData(boxScores[t]);
				MLBPlayer startingPitcher = DAO.getStartingPitcherByIndex((Integer)franchisesMap.get(boxScores[t].getTeamName()), boxScores[t].getYear(), getRandomNumberInRange(1, 5));  
				gameState.setCurrentPitcher(startingPitcher, t);
				boxScores[t].getPitchers().put(startingPitcher.getMlbPlayerId(), startingPitcher);
				if (Arrays.asList(nationalLeagueTeams).contains(teamNames[1]) || years[1] < 1973) {
					boxScores[t].getBatters().set(8, new ArrayList<MLBPlayer>());  // Clear out prior games pitcher spots
					boxScores[t].getBatters().get(8).add(startingPitcher); // Set pitcher as batting ninth, if no DH
				}
			}
			playBall(gameState, boxScores);
			seriesBoxScores[s] = boxScores;
		}
		
		if (seriesLength > 1) { // Series results and calculations
			HashMap<String, Integer> totalWins = new HashMap<String, Integer>();
			HashMap<String, Integer> totalRuns = new HashMap<String, Integer>();
			totalWins.put(boxScores[0].getTeamName(), 0);
			totalWins.put(boxScores[1].getTeamName(), 0);
			totalRuns.put(boxScores[0].getTeamName(), 0);
			totalRuns.put(boxScores[1].getTeamName(), 0);
			System.out.println("\n");
			for (BoxScore[] bsArray : seriesBoxScores) {
				int winner = bsArray[0].getFinalScore() > bsArray[1].getFinalScore() ? 0 : 1;
				System.out.println(bsArray[winner].getTeamName() + " " + bsArray[winner].getFinalScore() + "  " +  bsArray[winner==1?0:1].getTeamName() + " " + bsArray[winner==1?0:1].getFinalScore());
				totalWins.put(bsArray[winner].getTeamName(), totalWins.get(bsArray[winner].getTeamName()) + 1);
				totalRuns.put(bsArray[0].getTeamName(), totalRuns.get(bsArray[0].getTeamName()) + bsArray[0].getFinalScore());
				totalRuns.put(bsArray[1].getTeamName(), totalRuns.get(bsArray[1].getTeamName()) + bsArray[1].getFinalScore());
			}
			System.out.println("\nTotals:");
			int homeWinner = totalWins.get(boxScores[1].getTeamName()) > totalWins.get(boxScores[0].getTeamName()) ?1 : 0;
			System.out.println(boxScores[homeWinner].getTeamName() + ": " + totalWins.get(boxScores[homeWinner].getTeamName()) + "(" + 
				df.format((double)totalWins.get(boxScores[homeWinner].getTeamName())/seriesLength) +  ") " + boxScores[homeWinner==1?0:1].getTeamName() + ": " + totalWins.get(boxScores[homeWinner==1?0:1].getTeamName()) + 
				"(" + df.format((double)totalWins.get(boxScores[homeWinner==1?0:1].getTeamName())/seriesLength) + ")");
			System.out.println("Average Score:");
			System.out.println(boxScores[homeWinner].getTeamName() + ": " + df.format((double)totalRuns.get(boxScores[homeWinner].getTeamName())/seriesLength) + " " + boxScores[homeWinner==1?0:1].getTeamName() + 
				": " + df.format((double)totalRuns.get(boxScores[homeWinner==1?0:1].getTeamName())/seriesLength));
		}
	}
	
	private static void playBall (GameState gameState, BoxScore[] boxScores) {
		/*getBattingStatsFromAPI();
		if (!setRandomLineup()) {
			return;
		}*/
		System.out.println("\nStarting pitchers : " + boxScores[0].getTeamName() + ": " + gameState.getCurrentPitchers()[0].getFirstLastName() + " v " + boxScores[1].getTeamName() + 
			": " + gameState.getCurrentPitchers()[1].getFirstLastName());
		while (gameState.getInning() <= INNINGS_PER_GAME || boxScores[0].getScore(gameState.getInning()) == boxScores[1].getScore(gameState.getInning())) {
			int inning = gameState.getInning();
			Scanner myObj = null;
			for (int top = 0; top < 2; top++) {
				BoxScore boxScore = boxScores[top];
				Roster roster = rosters[top];
				ArrayList<ArrayList<MLBPlayer>> batters = boxScore.getBatters();
				gameState.setTop(top);
				gameState.setOuts(0);
				System.out.println((top == 0 ? "\n***TOP " : "***BOTTOM ") + " INN: " + inning + " ***");
				if (top == 0) {
					System.out.println("SCORE - " + boxScores[0].getTeamName() + ": " + boxScores[0].getScore(gameState.getInning())  + " " + 
						boxScores[1].getTeamName() + ": " + boxScores[1].getScore(gameState.getInning()));
				}
				boolean gameTiedStartOfAB;
				Arrays.fill(gameState.getBaseRunners(), new BaseRunner());
				while (gameState.getOuts() < OUTS_PER_INNING) {
					// TBD change pitcher logic
					MLBPlayer currentBatter = batters.get(gameState.getBattingOrder()[top] - 1).get(batters.get(gameState.getBattingOrder()[top] - 1).size() - 1);
					System.out.println(currentBatter.getFirstLastName() + " UP OUTS: " + gameState.getOuts() + " "  + gameState.getBaseSituations().get(gameState.getCurrentBasesSituation()) + 
						" (" + getBatterNameFromId(gameState.getBaseRunnerId(1)) + ":" + getBatterNameFromId(gameState.getBaseRunnerId(2)) + ":" + getBatterNameFromId(gameState.getBaseRunnerId(3)) + ")");
					MLBPlayer currentPitcher = gameState.getCurrentPitchers()[top==0?1:0];
					BattingStats currentBatterGameStats = currentBatter.getMlbBattingStats().getBattingStats();
					BattingStats currentBatterSeasonStats = getBattersSeasonBattingStats(roster, currentBatter.getMlbPlayerId());
					PitchingStats currentPitcherGameStats = currentPitcher.getMlbPitchingStats().getPitchingStats();
					PitchingStats currentPitcherSeasonStats = getPitchersSeasonPitchingStats(rosters[top==0?1:0], currentPitcher.getMlbPlayerId());
						
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
						gameState.incrementBattingOrder(top);
						continue;
					}
					gameTiedStartOfAB = boxScores[1].getScore(inning) == boxScores[0].getScore(inning);
					long onBaseEndPoint = 1000 - Math.round(((currentBatterSeasonStats.getOnBasePercentage() + currentPitcherSeasonStats.getOnBasePercentage())/2.0)*1000);
					onBaseEndPoint -= currentPitcherGameStats.getBattersFaced(); // pitcher fatigue
					if (currentPitcher.getArmThrows().equals(currentBatter.getBats())) {  // TBD: throws v bats
						onBaseEndPoint += 10;
					}
					else {
						onBaseEndPoint -= 10;
					}
					if (rando <= onBaseEndPoint) { // OUT
						int outResult = getOutResult(currentBatter, currentBatterSeasonStats, currentPitcherGameStats, currentPitcherSeasonStats);
						gameState.setOuts(gameState.getOuts() + outResult);
						currentBatterGameStats.incrementAtBats();
						currentPitcherGameStats.incrementInningsPitched(outResult);
					}
					else {
						long bbEndPoint = Math.round(((currentBatterSeasonStats.getWalkRate() + currentPitcherSeasonStats.getWalkRate())/2.0)*(1000 - onBaseEndPoint));
						if (rando < (bbEndPoint + onBaseEndPoint)) {
							System.out.println("WALKED");
							currentBatterGameStats.incrementWalks();
							currentPitcherGameStats.incrementWalks();
							updateBasesSituationNoRunnersAdvance(currentBatter);
						}
						else if (rando >= (bbEndPoint + onBaseEndPoint) && rando < ((bbEndPoint + onBaseEndPoint) + 10)) { // Hard coded HBP rate ~3%
							System.out.println("HIT BY PITCH");
							updateBasesSituationNoRunnersAdvance(currentBatter);
						}
						else { // HIT or ERROR
							int noOutResult = getNotOutResult(currentBatterGameStats, currentBatterSeasonStats, currentPitcherGameStats, currentPitcherSeasonStats);
							boolean reachedByError = false;
							if (noOutResult == 0) {
								reachedByError = true;
								noOutResult = 1;
							}
							if (noOutResult == 1 && (getRandomNumberInRange(0, 5) + currentBatterGameStats.getSpeedRating()) > 4) { // infield single/error?
								if (gameState.getOuts() != 2) {  // less than 2 outs runners hold
									updateBasesSituationNoRunnersAdvance(currentBatter);
								}
								else {
									updateBasesSituationRunnersAdvance(noOutResult, currentBatter);
								}
								System.out.println("STAYED IN INFIELD");
							}
							else {
								updateBasesSituationRunnersAdvance(noOutResult, currentBatter);
							}
							currentBatterGameStats.incrementAtBats();
							if (!reachedByError) {
								boxScore.incrementHits();
								currentBatterGameStats.incrementHits();
								currentPitcherGameStats.incrementHitsAllowed();
							}
						}
						currentBatter.getMlbBattingStats().setBattingStats(currentBatterGameStats);
						if (inning >= 9 && boxScores[1].getScore(inning) > boxScores[0].getScore(inning) && gameTiedStartOfAB) {
							boxScore.setWalkOff(true);
							System.out.println("WALKOFF ");
							break;
						}
					}
					currentPitcherGameStats.incrementBattersFaced();
					gameState.incrementBattingOrder(top);
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
		 
		 /*JSONObject bsJSON = new JSONObject(boxScores[0]);
		 System.out.println(bsJSON);
		 
		 /*for (int i = 0; i < randoLog.size(); i++) {
			 System.out.println(randoLog.get(i));
		 }*/
	}
	
	private static void clearPlayerGameData(BoxScore bs) {
		for (int i = 0 ; i <= NUM_OF_PLAYERS_IN_LINEUP - 1; i++) {
			ArrayList<MLBPlayer> posPlayers = bs.getBatters().get(i);
			for (MLBPlayer p : posPlayers) {
				p.setMlbBattingStats(new MLBBattingStats());
			}
		}
	}
	
	private static int getNotOutResult(BattingStats batterGameStats, BattingStats batterSeasonStats, PitchingStats pitcherGameStats, PitchingStats pitcherSeasonStats) {
		long errorEndPoint = 25;
		long hrEndPoint = Math.round(((double)(batterSeasonStats.getHomeRunRate() + pitcherSeasonStats.getHomeRunsAllowedRate())/2.0)*1000) + errorEndPoint;
		long triplesEndPoint = Math.round((((double)batterSeasonStats.getTriples()/batterSeasonStats.getHits())*1000)) + hrEndPoint;
		triplesEndPoint = batterSeasonStats.getTriples() == 0 ? 8 + hrEndPoint : triplesEndPoint; // Give some chance if batter has 0 triples
		long doublesEndPoint = Math.round((((double)batterSeasonStats.getDoubles()/batterSeasonStats.getHits())*1000)) + triplesEndPoint;
		int notOutResult = 1;
		int notOutRando = getRandomNumberInRange(1, 1000);
		if (notOutRando > 1 && notOutRando <= errorEndPoint) {
			System.out.println("REACHED BY ERROR");
			notOutResult = 0;
			boxScores[gameState.getTop()==0?1:0].incrementErrors();
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
	
	private static int getOutResult(MLBPlayer currentBatter, BattingStats batterSeasonStats, PitchingStats pitcherGameStats, PitchingStats pitcherSeasonStats) {
		int outsRecorded = 1;
		int notOutRando = getRandomNumberInRange(1, 100);
		long soEndPoint = Math.round(((pitcherSeasonStats.getStrikeoutRate()+batterSeasonStats.getStrikeoutRate())/2)*100);
		long outIncrement = Math.round((double)((100 - soEndPoint)/5));
		if (notOutRando > 1 && notOutRando <= soEndPoint) { // STRUCK OUT
			System.out.println(outTypes.get(STRUCK_OUT)); 
			currentBatter.getMlbBattingStats().getBattingStats().incremenStrikeOuts();
			pitcherGameStats.incrementStrikeouts();
		}
		else if (notOutRando > soEndPoint && notOutRando <= soEndPoint + outIncrement) {
			String groundBallRecipientPosition = positions.get(getRandomNumberInRange(1, 6));
			System.out.println(outTypes.get(GROUNDED_OUT) + " TO " + groundBallRecipientPosition); //GROUNDED OUT
			if (isDoublePlay(true)) {
				outsRecorded++; // 2nd out
			}
			else {
				fieldersChoice(groundBallRecipientPosition, currentBatter);
			}
		}
		else if (notOutRando > soEndPoint + outIncrement && notOutRando <= soEndPoint + (outIncrement*2)) {
			System.out.println(outTypes.get(FLEW_OUT) +  " TO " + positions.get(getRandomNumberInRange(7, 9))); // FLEW OUT
			if (gameState.isBaseOccupied(3)) {  // Only tag up if there is a runner on 3rd
				MLBPlayer runnerOnThird = getBatterFromId(gameState.getBaseRunnerId(3));
				BattingStats bs3 = getBattersSeasonBattingStats(rosters[gameState.getTop()], runnerOnThird.getMlbPlayerId());
				if (gameState.getOuts() < 2 && bs3.getSpeedRating() > 2) {
					if (updateBasesSituationSacFly(runnerOnThird, false) == 1) {
						outsRecorded++;
					}
				}
			}
		}
		else if (notOutRando > soEndPoint + (outIncrement*2) && notOutRando <= soEndPoint + (outIncrement*3)) {
			System.out.println(outTypes.get(FLEW_OUT_DEEP) +  " TO " + positions.get(getRandomNumberInRange(7, 9))); // FLEW OUT DEEP
			if (gameState.isBaseOccupied(3)) {  // Only tag up if there is a runner on 3rd
				MLBPlayer runnerOnThird = getBatterFromId(gameState.getBaseRunnerId(3));
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
	
	private static boolean processOtherAction (MLBPlayer currentBatter) {
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
	
	private static void updateBasesSituationRunnersAdvance(int event, MLBPlayer currentBatter) {
	  /*int basesSituation = (currentBasesSituation << event) + (int)Math.pow(2, (event - 1));
		if (basesSituation > 7) {
			basesSituation = basesSituation % 8;
		}*/
		MLBPlayer currentPitcher = gameState.getCurrentPitchers()[gameState.getTop()==0?1:0];
		for (int e = 0; e < event; e++) {
			for (int b = 3; b >= 0; b--) {
				int base = b + 1;
				if (b > 0) {
					if (base != 4) {
						gameState.setBaseRunner(base, gameState.getBaseRunner(base - 1));
					}
				}
				else if (b == 0 && e == 0) {
					gameState.setBaseRunner(base, new BaseRunner(currentBatter.getMlbPlayerId(), currentPitcher.getMlbPlayerId()));
				}
				else {
					gameState.setBaseRunner(base, new BaseRunner());
				}
				if (base == 4 && gameState.isBaseOccupied(3)) {     // If runner on 3rd run scores
					runScores();
				}
			}
		}
	}
	
	// For walks, hit by pitch and some infield singles
	private static void updateBasesSituationNoRunnersAdvance(MLBPlayer currentBatter) { 
		MLBPlayer currentPitcher = gameState.getCurrentPitchers()[gameState.getTop()==0?1:0];
		// No need for checking 0, 2, 3, or 23
		if (gameState.getCurrentBasesSituation() == GameState.BASES_LOADED || gameState.getCurrentBasesSituation() == GameState.MAN_ON_FIRST_AND_SECOND) { // 123 or 12
			// if 123 runner 3 scores
			if (gameState.getCurrentBasesSituation() == GameState.BASES_LOADED) {
				runScores();
			}
			gameState.setBaseRunner(3, gameState.getBaseRunner(2));
		}
		if (gameState.isBaseOccupied(1)) { // Runner on first
			gameState.setBaseRunner(2, gameState.getBaseRunner(1));
		}
		gameState.setBaseRunner(1, new BaseRunner(currentBatter.getMlbPlayerId(), currentPitcher.getMlbPlayerId()));
	}
	
	private static void updateBasesSituationSacBunt(MLBPlayer currentBatter) { 
		PitchingStats currentPitcherGameStats = boxScores[gameState.getTop()==0?1:0].getPitchers().get(gameState.getCurrentPitchers()[gameState.getTop()==0?1:0].getMlbPlayerId()).getMlbPitchingStats().getPitchingStats();
		int buntRando = getRandomNumberInRange(0, 100);
		if (buntRando < 11) { // 80 %
			System.out.println("SUCCESSFUL BUNT!");
			if (gameState.isBaseOccupied(2)) { // Runner on 2 or 12
				gameState.setBaseRunner(3, gameState.getBaseRunner(2));
				gameState.setBaseRunner(2, new BaseRunner());
			}
			else if (gameState.isBaseOccupied(1)) { // Runner on 1 or 13
				gameState.setBaseRunner(2, gameState.getBaseRunner(1));
				gameState.setBaseRunner(1, new BaseRunner());
			}
		}
		else if (buntRando < 14) { // 13 %
			System.out.println("UNSUCCESSFUL BUNT!"); 
			fieldersChoice("P", currentBatter);
			currentBatter.getMlbBattingStats().getBattingStats().incrementAtBats();
		}
		else { // 7%
			System.out.print("UNSUCCESSFUL BUNT!");
			if (gameState.isBaseOccupied(1)) {
				System.out.println(" DOUBLE PLAY!");
				updateBasesSituationDoublePlay();
				gameState.incrementOuts();
				currentPitcherGameStats.incrementInningsPitched(1);
			}
			else {
				fieldersChoice("P", currentBatter);
			}
			currentBatter.getMlbBattingStats().getBattingStats().incrementAtBats();
		}
		gameState.incrementOuts();
		currentPitcherGameStats.incrementInningsPitched(1);
	}
	
	private static int updateBasesSituationSacFly(MLBPlayer runnerOnThird, boolean deep) {
		int outAdvancing = 0;
		BattingStats bs = getBattersSeasonBattingStats(rosters[gameState.getTop()], runnerOnThird.getMlbPlayerId());
		int sacRando = getRandomNumberInRange(0, 5) + bs.getSpeedRating();
		sacRando += deep ? 5 : 0;  // Tagging on deep FB should be almost a sure thing
		System.out.println(runnerOnThird.getFirstLastName() + " TAGGING UP ON A FLY BALL");
		if (sacRando > 5) { // safe
			runScores();
		}
		else { // out
			outAdvancing = 1;
			System.out.println("OUT AT THE PLATE");
		}
		gameState.setBaseRunner(3, new BaseRunner());
		return outAdvancing;
	}
	
	private static void updateBasesSituationDoublePlay() {
		if ((gameState.getCurrentBasesSituation() == GameState.BASES_LOADED || gameState.getCurrentBasesSituation() == GameState.MAN_ON_FIRST_AND_THIRD) && gameState.getOuts() == 0) {
			runScores();
		}
		if (gameState.getCurrentBasesSituation() == GameState.MAN_ON_FIRST_AND_THIRD) {
			gameState.setBaseRunner(3, new BaseRunner());
		}
		else if (gameState.getCurrentBasesSituation() == GameState.MAN_ON_FIRST_AND_SECOND || gameState.getCurrentBasesSituation() == GameState.BASES_LOADED) {
			gameState.setBaseRunner(3, gameState.getBaseRunner(2));
		}
		gameState.setBaseRunner(2, new BaseRunner());
		gameState.setBaseRunner(1, new BaseRunner());
		
	}
	private static void updateBasesSituationFieldersChoice(MLBPlayer currentBatter) {
		MLBPlayer currentPitcher = gameState.getCurrentPitchers()[gameState.getTop()==0?1:0];
		if (gameState.getCurrentBasesSituation() == GameState.BASES_LOADED) {
			gameState.setBaseRunner(3, gameState.getBaseRunner(2));
			gameState.setBaseRunner(2, gameState.getBaseRunner(1));
			if (gameState.isHitAndRun()) {
				runScores();
				gameState.setBaseRunner(1, new BaseRunner());
			}
			else {
				System.out.println(getBatterNameFromId(gameState.getBaseRunnerId(3)) + " OUT AT THE PLATE!");
				gameState.setBaseRunner(1, new BaseRunner(currentBatter.getMlbPlayerId(), currentPitcher.getMlbPlayerId()));
			}
		}
		else if (gameState.getCurrentBasesSituation() == GameState.MAN_ON_SECOND_AND_THIRD && gameState.isHitAndRun()) {
			runScores();
			gameState.setBaseRunner(3, gameState.getBaseRunner(2));
		}
		else if (gameState.getCurrentBasesSituation() == GameState.MAN_ON_THIRD && gameState.isHitAndRun()) {
			runScores();
			gameState.setBaseRunner(3, new BaseRunner());
		}
		else if (gameState.getCurrentBasesSituation() == GameState.MAN_ON_SECOND && gameState.isHitAndRun()) {
			gameState.setBaseRunner(3, gameState.getBaseRunner(2));
		}
		else if (gameState.getCurrentBasesSituation() == GameState.MAN_ON_FIRST_AND_THIRD) {
			if (gameState.isHitAndRun()) {
				
			}
			else {
				System.out.println(getBatterNameFromId(gameState.getBaseRunnerId(1)) + " OUT AT SECOND!");
				gameState.setBaseRunner(1, new BaseRunner(currentBatter.getMlbPlayerId(), currentPitcher.getMlbPlayerId()));
			}
		}
		else if (gameState.getCurrentBasesSituation() == GameState.MAN_ON_FIRST_AND_SECOND) {
			if (gameState.isHitAndRun()) {
				
			}
			else {
				System.out.println(getBatterNameFromId(gameState.getBaseRunnerId(2)) + " OUT AT THIRD!");
				gameState.setBaseRunner(2, gameState.getBaseRunner(1));
				gameState.setBaseRunner(1, new BaseRunner(currentBatter.getMlbPlayerId(), currentPitcher.getMlbPlayerId()));
			}
		}
		else if (gameState.getCurrentBasesSituation() == GameState.MAN_ON_FIRST) {
			if (gameState.isHitAndRun()) {
				
			}
			else {
				System.out.println(getBatterNameFromId(gameState.getBaseRunnerId(1)) + " OUT AT SECOND!");
				gameState.setBaseRunner(1, new BaseRunner(currentBatter.getMlbPlayerId(), currentPitcher.getMlbPlayerId()));
			}
		}
	}
	
	private static boolean isDoublePlay(boolean ground) {
		boolean dp = false;
		if (!gameState.isBaseOccupied(1) || gameState.getOuts() == 2 || !ground) { // Must a ground out, less than 2 outs and runner on 1st
			return false;
		}
		BattingStats bs = getBatterFromId(gameState.getBaseRunnerId(1)).getMlbBattingStats().getBattingStats();
		int dpRando = getRandomNumberInRange(0, 5) + bs.getSpeedRating();
		// Ground ball, less than 2 outs, man on first
		if (dpRando < 5) {
			dp = true;
			System.out.println("DOUBLE PLAY");
			updateBasesSituationDoublePlay();
		}
		return dp;
	}
	
	private static void fieldersChoice(String groundBallRecipientPosition, MLBPlayer currentBatter) {
		if (gameState.getCurrentBasesSituation() == GameState.BASES_EMPTY || gameState.getOuts() == 2 || 
		   (!gameState.isHitAndRun() && (gameState.getCurrentBasesSituation() == GameState.MAN_ON_THIRD || 
		   gameState.getCurrentBasesSituation() == GameState.MAN_ON_SECOND_AND_THIRD || gameState.getCurrentBasesSituation() == GameState.MAN_ON_SECOND))) {
				return; // No FC
		}
		else {
			System.out.println("FIELDER'S CHOICE");
			updateBasesSituationFieldersChoice(currentBatter);
		}
	}
	
	private static void runScores() {
		BoxScore boxScore = boxScores[gameState.getTop()];
		PitchingStats pitcherGameStats = boxScores[gameState.getTop()==0?1:0].getPitchers().get(gameState.getCurrentPitchers()[gameState.getTop()==0?1:0].getMlbPlayerId()).getMlbPitchingStats().getPitchingStats();
		MLBPlayer runner = getBatterFromId(gameState.getBaseRunnerId(3));
		runner.getMlbBattingStats().getBattingStats().incrementRuns();
		pitcherGameStats.incrementRunsAllowed();
		pitcherGameStats.incrementEarnedRunsAllowed();
		boxScore.setRunsScored(gameState.getInning(), 1); // run scores
		System.out.println("RUN SCORES - " + boxScores[0].getTeamName() + ": " + boxScores[0].getScore(gameState.getInning())  + " " + 
			boxScores[1].getTeamName() + ": " + boxScores[1].getScore(gameState.getInning()));
	}
	
	private static boolean isRunnerStealing(int baseToSteal) {
		boolean runnerIsStealing = false;
		int fromBase = baseToSteal - 1;
		if (!gameState.isBaseOccupied(fromBase) || (baseToSteal != 4 && gameState.isBaseOccupied(baseToSteal))) {
			return false;
		}
		MLBPlayer runnerStealingPlayer = getBatterFromId(gameState.getBaseRunnerId(fromBase));
		BattingStats bs = getBattersSeasonBattingStats(rosters[gameState.getTop()], runnerStealingPlayer.getMlbPlayerId());
		int stealRando = getRandomNumberInRange(0, 5) + bs.getSpeedRating();
		if ((baseToSteal < 4 && stealRando > 5) || (baseToSteal == 4 && stealRando > 9)) { // Rarely try to steal home
			runnerIsStealing = true;
		}
		return runnerIsStealing;
	}
	
	private static int stealBase(int baseToSteal) {
		int outStealing = 0;
		int fromBase = baseToSteal - 1;  
		MLBPlayer runnerStealingPlayer = getBatterFromId(gameState.getBaseRunnerId(fromBase));
		BattingStats bs = getBattersSeasonBattingStats(rosters[gameState.getTop()], runnerStealingPlayer.getMlbPlayerId());
		double stealPctg = ((bs.getStolenBases() + bs.getCaughtStealing()) != 0) ? 
			(double)bs.getStolenBases()/(bs.getStolenBases() + bs.getCaughtStealing()) : 0.2;  // Give a chance if no SB
		System.out.print(runnerStealingPlayer.getFirstLastName() + " ATTEMPTING TO STEAL " + baseToSteal + " - SR: " + bs.getSpeedRating() + " SP: " + df.format(stealPctg));
		int safeOutStealRando = getRandomNumberInRange(1, 10);
		if ((baseToSteal < 4 && safeOutStealRando < Math.round(stealPctg*10)) || (baseToSteal < 4 && safeOutStealRando == 10)) { // safe/out - rarely stealing home is safe
			System.out.println("- SAFE!");
			if (baseToSteal == 4) {
				runScores();
			}
			else {
				gameState.setBaseRunner(baseToSteal, gameState.getBaseRunner(fromBase));
			}
		}
		else { // out
			System.out.println("- OUT!");
			outStealing = 1;
		}
		gameState.setBaseRunner(fromBase, new BaseRunner());
		System.out.println(gameState.getBaseSituations().get(gameState.getCurrentBasesSituation()) + " (" + getBatterNameFromId(gameState.getBaseRunnerId(1)) + 
			":" + getBatterNameFromId(gameState.getBaseRunnerId(2)) + ":" + getBatterNameFromId(gameState.getBaseRunnerId(3)) + ")");
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
	
	public static HashMap<Integer, MLBPlayer> sortHashMapByValue(HashMap<Integer, MLBPlayer> hm, String type) 
    { 
        // Create a list from elements of HashMap 
        List<Map.Entry<Integer, MLBPlayer>> list = new LinkedList<Map.Entry<Integer, MLBPlayer>>(hm.entrySet()); 
        Collections.sort(list, new Comparator<Map.Entry<Integer, MLBPlayer>>() { 
            public int compare(Map.Entry<Integer, MLBPlayer> o1,  
                               Map.Entry<Integer, MLBPlayer> o2) { 
            	if (type.equals("SB")) {
            		return (o1.getValue().getMlbBattingStats().getBattingStats().getStolenBases() >= o2.getValue().getMlbBattingStats().getBattingStats().getStolenBases() ? -1 : 1); 
            	}
            	else if (type.equals("H")){
            		if (o1.getValue().getMlbBattingStats().getBattingStats().getHits() == o2.getValue().getMlbBattingStats().getBattingStats().getHits()) {
            			return 0;
            		}
            		return (o1.getValue().getMlbBattingStats().getBattingStats().getHits() > o2.getValue().getMlbBattingStats().getBattingStats().getHits() ? -1 : 1);
            	}
            	else if (type.equals("HR")){
            		if (o1.getValue().getMlbBattingStats().getBattingStats().getHomeRuns() == o2.getValue().getMlbBattingStats().getBattingStats().getHomeRuns()) {
            			return 0;
            		}
            		return (o1.getValue().getMlbBattingStats().getBattingStats().getHomeRuns() > o2.getValue().getMlbBattingStats().getBattingStats().getHomeRuns() ? -1 : 1);
            	}
            	else if (type.equals("RBI")){
            		if (o1.getValue().getMlbBattingStats().getBattingStats().getRbis() == o2.getValue().getMlbBattingStats().getBattingStats().getRbis()) {
            			return 0;
            		}
            		return (o1.getValue().getMlbBattingStats().getBattingStats().getRbis() > o2.getValue().getMlbBattingStats().getBattingStats().getRbis() ? -1 : 1);
            	}
            	else {
            		return 0;
            	}
            } 
        });  
        // put data from sorted list to hashmap  
        HashMap<Integer, MLBPlayer> temp = new LinkedHashMap<Integer, MLBPlayer>(); 
        for (Map.Entry<Integer, MLBPlayer> aa : list) { 
            temp.put(aa.getKey(), aa.getValue()); 
        } 
        return temp; 
    }
	
	// Lineup is home/vis->batting order (1-9) -> List of Players
	private static ArrayList<ArrayList<ArrayList<MLBPlayer>>> setOptimalLineup(String[] teams, int[] years) {
			ArrayList<ArrayList<ArrayList<MLBPlayer>>> batters = new ArrayList<ArrayList<ArrayList<MLBPlayer>>>();
		ArrayList<Integer> playersInLineupList = new ArrayList<Integer>();
		HashMap<Integer, MLBPlayer> battingStatsSortedByStatMap;
		List<Map.Entry<Integer, MLBPlayer>> list;
		ArrayList<String> positionsUsed;
		boolean useDH = !Arrays.asList(nationalLeagueTeams).contains(teams[1]) && years[1] >= 1973;
		String statType = "";
		int ofCount;
		MLBPlayer player;
		for (int t = 0; t < 2; t++) {
			positionsUsed = new ArrayList<String>();
			ofCount = 0;
			batters.add(new ArrayList<ArrayList<MLBPlayer>>());
			// Get random starter 1-5
			for (int i = 1 ; i <= NUM_OF_PLAYERS_IN_LINEUP - 1; i++) {  // 1 - 8
				batters.get(t).add(new ArrayList<MLBPlayer>());
				if (i == 1) { 
					statType = "SB";
				}
				else if (i == 3) {
					statType = "RBI";
				}
				else if (i == 4) {
					statType = "HR";
				}
				else {
					statType = "H";
				}
				battingStatsSortedByStatMap = sortHashMapByValue(rosters[t].getBatters(), statType);
				list = new LinkedList<Map.Entry<Integer, MLBPlayer>>(battingStatsSortedByStatMap.entrySet());
				int index = 0;
				while (true) {
					if (index == rosters[t].getBatters().size()) {
						System.out.println("Not enough players at every position!");
						return null;
					}
					player = rosters[t].getBatters().get(list.get(index).getKey());
					if (player.getPrimaryPosition().equals("DH") || (years[t] > 2010 && player.getPrimaryPosition().equals("OF"))) {
						index++;
						continue;
					}
					boolean positionNeeded = (!positionsUsed.contains(player.getPrimaryPosition()) && !player.getPrimaryPosition().equals("OF")) || 
						(player.getPrimaryPosition().equals("OF") && ofCount < 3);
					if (player != null && !playersInLineupList.contains(list.get(index).getKey()) && positionNeeded) {  // Not already in lineup, save P for end
						/*System.out.println(player.getFirstLastName() + " " + player.getPrimaryPosition() + " SB: " + player.getMlbBattingStats().getBattingStats().getStolenBases() + " H: " +
							player.getMlbBattingStats().getBattingStats().getHits() + " HR: " + player.getMlbBattingStats().getBattingStats().getHomeRuns() + " RBI: " + 
							player.getMlbBattingStats().getBattingStats().getRbis());*/
						playersInLineupList.add(list.get(index).getKey());
						batters.get(t).get(i-1).add(new MLBPlayer(player.getMlbPlayerId(), player.getFullName(), player.getPrimaryPosition(), player.getArmThrows(), player.getBats(), player.getJerseyNumber()));
						positionsUsed.add(player.getPrimaryPosition());
						if (player.getPrimaryPosition().equals("OF")) {
							ofCount++;
						}
						break;
					}
					index++;
				}
			}
			batters.get(t).add(new ArrayList<MLBPlayer>()); // For DH/P in 9
			if (useDH) { // DH always bats ninth 
				player = DAO.getMlbPlayerWithMostPlateAppearances((Integer)franchisesMap.get(teams[t]), years[t], playersInLineupList);
				player.setPrimaryPosition("DH");
				/*System.out.println(player.getFirstLastName() + " " + player.getPrimaryPosition() + " SB: " + playerBattingStats.getStolenBases() + " H: " + playerBattingStats.getHits() + " HR: " + 
					playerBattingStats.getHomeRuns() + " RBI: " + playerBattingStats.getRbis());*/
				batters.get(t).get(8).add(new MLBPlayer(player.getMlbPlayerId(), player.getFullName(), player.getPrimaryPosition(), player.getArmThrows(), player.getBats(), player.getJerseyNumber()));
			}
			System.out.println();
		}
		return batters;
	}
	
	/*private static boolean setRandomLineup() {
		ArrayList<ArrayList<MLBPlayer>> batters;
		BoxScore boxScore;
		MLBPlayer mlbPlayer;
		int pitcherDHLineupPosition = 1;
		for (int t = 0 ; t < 2; t++) {
			boxScore = boxScores[t];
			batters = boxScore.getBatters();
			// Get random starter 1-5
			MLBPlayer startingPitcher = DAO.getStartingPitcherByIndex((Integer)franchisesMap.get(boxScore.getTeamName()), boxScore.getYear(), getRandomNumberInRange(1, 5));  
			gameState.getCurrentPitchers()[t] = new MLBPlayer(startingPitcher.getFullName(), startingPitcher.getMlbPlayerId(), "P");
			boxScore.getPitchers().put(startingPitcher.getMlbPlayerId(), new MLBPlayer(startingPitcher.getFullName(), startingPitcher.getMlbPlayerId(), "P"));
			ArrayList<Integer> randomLineup = getRandomLineupByPosition();
			ArrayList<Integer> outfielderIdList = new ArrayList<>();
			ArrayList<Integer> battersPlayerIdList = new ArrayList<>();
			for (int p = 0 ; p < NUM_OF_PLAYERS_IN_LINEUP; p++) {
				Integer position = randomLineup.get(p);
				if (!positions.get(position).equals("P")) {
					mlbPlayer = DAO.getMlbPlayerWithMostPlateAppearances((Integer)franchisesMap.get(boxScore.getTeamName()), boxScore.getYear(), positions.get(position));
					if (mlbPlayer != null && mlbPlayer.getMlbPlayerId() != null) {
						batters.get(p).add(new MLBPlayer(mlbPlayer.getFullName(), mlbPlayer.getMlbPlayerId(), positions.get(position)));
						battersPlayerIdList.add(mlbPlayer.getMlbPlayerId());
					}
					else {
						// No specific OF positions before 1987
						if (positions.get(position).equals("LF") || positions.get(position).equals("CF") || positions.get(position).equals("RF")) {
							mlbPlayer = DAO.getMlbPlayerWithMostPlateAppearances((Integer)franchisesMap.get(boxScore.getTeamName()), boxScore.getYear(), "OF", outfielderIdList);
							if (mlbPlayer != null && mlbPlayer.getMlbPlayerId() != null) {
								batters.get(p).add(new MLBPlayer(mlbPlayer.getFullName(), mlbPlayer.getMlbPlayerId(), positions.get(position)));
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
				batters.get(pitcherDHLineupPosition).add(new MLBPlayer(gameState.getCurrentPitchers()[t].getName(), gameState.getCurrentPitchers()[t].getMlbPlayerId(), "P"));
			}
			else { // DH
				mlbPlayer = DAO.getMlbPlayerWithMostPlateAppearances((Integer)franchisesMap.get(boxScore.getTeamName()), boxScore.getYear(), battersPlayerIdList);
				batters.get(pitcherDHLineupPosition).add(new MLBPlayer(mlbPlayer.getFullName(), mlbPlayer.getMlbPlayerId(), "DH"));
			}
		}
		return true;
	}*/
	
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
			System.out.println(" " + boxScore.getFinalScore() + (boxScore.getFinalScore() < 10 ? " " : "") + " " + boxScore.getHits() + 
				(boxScore.getHits() < 10 ? " " : "") + " " +  boxScore.getErrors());
		}
		for (int top = 0; top < 2; top++) {
			ArrayList<ArrayList<MLBPlayer>> batters = boxScores[top].getBatters();
			System.out.println();
			System.out.print(boxScores[top].getTeamName() + " ");
			System.out.println("Hitting\t\t\t" + "AB  R   H  RBI  BB  K    AVG  OBP  SLG");
			for (ArrayList<MLBPlayer> playerList : batters) {
				for (MLBPlayer batter : playerList) {
					BattingStats gameStats = batter.getMlbBattingStats().getBattingStats();
					BattingStats playerSeasonStats = getBattersSeasonBattingStats(rosters[top], batter.getMlbPlayerId());
					String playerOutput = batter.getFullName() + " " + batter.getPrimaryPosition();
					System.out.print(playerOutput);
					for (int tab = 32; tab >= 8; tab-=8) {
						if (playerOutput.length() < tab) {
							System.out.print("\t");
						}
					}
					System.out.print(gameStats.getAtBats() + (gameStats.getAtBats() > 9 ? "  " : "   "));
					System.out.print(gameStats.getRuns() + (gameStats.getRuns() > 9 ? "  " : "   "));
					System.out.print(gameStats.getHits() + (gameStats.getHits() > 9 ? "  " : "   "));
					System.out.print(gameStats.getRbis() + (gameStats.getRbis() > 9 ? "  " : "   "));
					System.out.print(gameStats.getWalks() + (gameStats.getWalks() > 9 ? "  " : "   "));
					System.out.print(gameStats.getStrikeOuts() + (gameStats.getStrikeOuts() > 9 ? "  " : "   "));
					System.out.print(playerSeasonStats.getBattingAverage() == 1.0 ? "1.00" : df.format(playerSeasonStats.getBattingAverage()) + " ");
					System.out.print(playerSeasonStats.getOnBasePercentage() == 1.0 ? "1.00" : df.format(playerSeasonStats.getOnBasePercentage()) + " ");
					System.out.print(playerSeasonStats.getSluggingPercentage() == 1.0 ? "1.00" : df.format(playerSeasonStats.getSluggingPercentage()) + " ");
					System.out.println();
				}
			}
		}
		System.out.println("BATTING");
		String[] doublesString = {"", ""};
		String[] triplesString = {"", ""};
		String[] homeRunsString = {"", ""};
		for (int top = 0; top < 2; top++) {
			ArrayList<ArrayList<MLBPlayer>> batters = boxScores[top].getBatters();
			for (ArrayList<MLBPlayer> playerList : batters) {
				for (MLBPlayer batter : playerList) {
					int numD = batter.getMlbBattingStats().getBattingStats().getDoubles();
					int numT = batter.getMlbBattingStats().getBattingStats().getTriples();
					int numH = batter.getMlbBattingStats().getBattingStats().getHomeRuns();
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
		DecimalFormat eraDf = new DecimalFormat(".00");
		for (int top = 0; top < 2; top++) {
			System.out.println();
			System.out.print(boxScores[top].getTeamName() + " ");
			System.out.println("Pitching\t\t\t" + "IP    H   R   ER  BB  K   HR  ERA");
			HashMap<Integer, MLBPlayer> pitchers = boxScores[top].getPitchers();
			for (Map.Entry<Integer, MLBPlayer> entry : pitchers.entrySet()) {
				PitchingStats ps = entry.getValue().getMlbPitchingStats().getPitchingStats();
				PitchingStats pitcherSeasonStats = getPitchersSeasonPitchingStats(rosters[top], entry.getValue().getMlbPlayerId());
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
				System.out.println(eraDf.format(pitcherSeasonStats.getEarnedRunAverage()));
			}
		}
	}
	
	static int getBattingOrderForPlayer(int id) {
		// Returns 0 if not found
		int order = 1;
		ArrayList<ArrayList<MLBPlayer>> batters = boxScores[gameState.getTop()].getBatters();
		for (ArrayList<MLBPlayer> playerList : batters) {
			for (MLBPlayer p : playerList) {
				if (p.getMlbPlayerId() == id) {
					return order;
				}
			}
			order++;
		}
		return 0;
	}
	static MLBPlayer getBatterFromId(int id) {
		if (id == 0) {
			return null;
		}
		int bo = getBattingOrderForPlayer(id);
		return boxScores[gameState.getTop()].getBatters().get(bo - 1).get(boxScores[gameState.getTop()].getBatters().get(bo - 1).size() - 1);
	}
	
	static String getBatterNameFromId(int id) {
		MLBPlayer player = getBatterFromId(id);
		return player != null ? player.getFirstLastName() : "<>";
	}
	
	static BattingStats getBattersSeasonBattingStats(Roster roster, int id) {
		if (roster.getBatters().get(id) == null) {
			return new BattingStats(75, 10, 2, 0, 1, 2, 44, 0, 4, 3, 0, 100, 0); // Default pitcher batting stats
		}
		return roster.getBatters().get(id).getMlbBattingStats().getBattingStats();
	}
	
	static PitchingStats getPitchersSeasonPitchingStats(Roster roster, int id) {
		if (roster.getPitchers().get(id) == null) {
			return new PitchingStats();
		}
		return roster.getPitchers().get(id).getMlbPitchingStats().getPitchingStats();
	}
	
	/*
	private static ArrayList<Integer> getRandomLineupByPosition() {
		ArrayList<Integer> randomLineup = new ArrayList<Integer>();
		for (int i = 1; i < 10; i++) {
			randomLineup.add(new Integer(i));
        }
        Collections.shuffle(randomLineup);
        return randomLineup;
	} */
	
	// For play mode
	private static boolean processCommand(String command, PitchingStats currentPitcherGameStats, MLBPlayer currentBatter) {
		if (command == null || command.length() == 0) { // NOOP (if CR entered)
			return true;
		}
		command = command.toUpperCase();
		int top = gameState.getTop();
		HashMap<Integer, MLBPlayer> gamePitchers = boxScores[top==0?1:0].getPitchers();
		HashMap<Integer, MLBPlayer> rosterPitchers = rosters[top==0?1:0].getPitchers();
		ArrayList<ArrayList<MLBPlayer>> gameBatters = boxScores[top].getBatters();
		HashMap<Integer, MLBPlayer> rosterBatters = rosters[top].getBatters();
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
			MLBPlayer newPitcherFromRoster = (MLBPlayer)rosterPitchers.get(newPitcherId);
			if (newPitcherFromRoster == null) {
				System.out.print("No pitcher found for " + newPitcherId + "!\n");
				return false;
			}
			if (gamePitchers.get(newPitcherId) != null) {
				System.out.print("Pitcher has already pitched in this game " + newPitcherId + "!\n");
				return false;
			}
			MLBPlayer newPitcher = new MLBPlayer(newPitcherFromRoster.getMlbPlayerId(), newPitcherFromRoster.getFullName(), newPitcherFromRoster.getPrimaryPosition(), 
				newPitcherFromRoster.getArmThrows(), newPitcherFromRoster.getBats(), newPitcherFromRoster.getJerseyNumber());
			boxScores[top==0?1:0].getPitchers().put(newPitcher.getMlbPlayerId(), newPitcher);
			gameState.setCurrentPitcher(newPitcher, top==0?1:0);
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
			MLBPlayer newBatterFromRoster = (MLBPlayer)rosterBatters.get(newBatterId);
			if (newBatterFromRoster == null) {
				System.out.print("No batter found for " + newBatterId + "!\n");
				return false;
			}
			if (getBattingOrderForPlayer(newBatterId) != 0) {
				System.out.print("Batter has already hit in this game " + newBatterId + "!\n");
				return false;
			}
			MLBPlayer newBatter = new MLBPlayer(newBatterFromRoster.getMlbPlayerId(), newBatterFromRoster.getFullName(), currentBatter.getPrimaryPosition(), newBatterFromRoster.getArmThrows(), 
				newBatterFromRoster.getBats(), newBatterFromRoster.getJerseyNumber());
			int bo = gameState.getBattingOrder()[gameState.getTop()];
			gameBatters.get(bo - 1).add(newBatter);
			System.out.println("Batter changed to: " + newBatterFromRoster.getFirstLastName() + "\n");
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
					for (Map.Entry<Integer, MLBPlayer> entry : rosterBatters.entrySet()) {
						MLBPlayer batter = entry.getValue();
						if (getBattingOrderForPlayer(batter.getMlbPlayerId()) == 0) {
							System.out.println(batter.getFirstLastName() + " " + batter.getMlbPlayerId());
						}
					}
					System.out.println();
					return false;
				case "PITCHERS":
					System.out.println("\nEligible Pitchers:");
					for (Map.Entry<Integer, MLBPlayer> entry : rosterPitchers.entrySet()) {
						MLBPlayer pitcher = entry.getValue();
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
			        		battingStatsMap.put(gameResults.getLineup()[top][p].getMlbPlayerId(), battingStats);
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

}
