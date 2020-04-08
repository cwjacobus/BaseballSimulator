package baseball;


import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dao.DAO;
import db.MLBBattingStats;
import db.MLBPlayer;

public class BaseballSimulator {
	
	public static final int OUTS_PER_INNING = 3;
	public static final int INNINGS_PER_GAME = 9;
	
	static Game game = new Game();
	static int inning = 1;
	static int top = 0;
	static int[] runnersOnBase = {0, 0, 0};
	static int[] battingOrder = {1, 1};
	static HashMap<Integer, BattingStats> battingStatsMap = new HashMap<Integer, BattingStats>();
	static int visYear = 2019;
	static int homeYear = 2019;
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
		if (args == null || args.length != 4) {
			System.out.println("Invalid args - expecting <year><vis><year><home> - ex. 2019 HOU 2019 NYY");
			return;
		}
		DAO.setConnection();
		franchisesMap = DAO.getDataMap("MLB_FRANCHISE");
		visYear = Integer.parseInt(args[0]);
		homeYear = Integer.parseInt(args[2]);
		String[] teamNames = {args[1], args[3]};
		game.setTeamNames(teamNames);
		if (!setLineup()) {
			return;
		}
		//getBattingStatsFromAPI();
		getBattingStatsFromDB();
		while (inning <= INNINGS_PER_GAME || game.getScore(inning)[0] == game.getScore(inning)[1]) {
			for (top = 0; top < 2; top++) {
				System.out.println((top == 0 ? "\n***TOP " : "***BOTTOM ") + " INN: " + inning + " ***");
				int outs  = 0;
				boolean gameTiedStartOfAB;
				Arrays.fill(runnersOnBase, 0);
				while (outs < OUTS_PER_INNING) {
					System.out.println(game.getLineup()[top][battingOrder[top] - 1].getName() + " UP OUTS: " + outs + " " 
						+ baseSituations.get(getCurrentBasesSituation()) + " " + runnersOnBase[0] + " " + runnersOnBase[1] + " " + runnersOnBase[2]);
					BattingStats currentBatterGameStats = game.getLineup()[top][battingOrder[top] - 1].getGameStats();
					BattingStats currentBatterSeasonStats = battingStatsMap.get(game.getLineup()[top][battingOrder[top] - 1].getId()) == null ? 
						battingStatsMap.get(game.getLineup()[top][battingOrder[top] - 1].getId()) : new BattingStats();
					outs += stealBase(outs, currentBatterSeasonStats);
					if (outs == OUTS_PER_INNING) {
						break;
					}
					int rando = getRandomNumberInRange(1, 1000);
					
					gameTiedStartOfAB = game.getScore(inning)[1] == game.getScore(inning)[0] ? true : false;
					if (rando <= 680) {
						outs += getOutResult(currentBatterGameStats, currentBatterSeasonStats, outs);
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
							updateBasesSituationWalkAndHbp();
						}
						else {
							int noOutResult = getNotOutResult(currentBatterGameStats);
							updateBasesSituationAndScore(noOutResult);
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
		 
		 /*for (int i = 0; i < randoLog.size(); i++) {
			 System.out.println(randoLog.get(i));
		 }*/ 
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
	
	private static int getOutResult(BattingStats currentBatterGameStats, BattingStats currentBatterSeasonStats, int outs) {
		int outsRecorded = 1;
		int notOutRando = getRandomNumberInRange(1, 100);
		if (notOutRando > 1 && notOutRando <= 20) {
			System.out.println(outTypes.get(STRUCK_OUT)); // STRUCK OUT
			currentBatterGameStats.incremenStrikeOuts();
		}
		else if (notOutRando > 20 && notOutRando <= 50) {
			System.out.println(outTypes.get(GROUNDED_OUT) + " TO " + positions.get(getRandomNumberInRange(1, 6))); //GROUNDED OUT
			if (doublePlay(true, outs)) {
				outsRecorded++;
			}
		}
		else if (notOutRando > 50 && notOutRando <= 65) {
			System.out.println(outTypes.get(FLEW_OUT) +  " TO " + positions.get(getRandomNumberInRange(7, 9))); // FLEW OUT
			if (outs < 2 && runnersOnBase[2] != 0) {
				if (updateBasesSituationSacFly(false, currentBatterSeasonStats) == 1) {
					outsRecorded++;
				}
			}
		}
		else if (notOutRando > 65 && notOutRando <= 80) {
			System.out.println(outTypes.get(FLEW_OUT_DEEP) +  " TO " + positions.get(getRandomNumberInRange(7, 9))); // FLEW OUT DEEP
			if (outs < 2 && runnersOnBase[2] != 0) {
				if (updateBasesSituationSacFly(true, currentBatterSeasonStats) == 1) {
					outsRecorded++;
				}
			}
		}
		else if (notOutRando > 80 && notOutRando < 90) {
			System.out.println(outTypes.get(LINED_OUT) + " TO " + positions.get(getRandomNumberInRange(1, 9, 2))); // LINED OUT
		}
		else {
			System.out.println(outTypes.get(POPPED_OUT) + " TO " + positions.get(getRandomNumberInRange(1, 6))); // POPPED OUT
		}
		return outsRecorded;
	}
	
	
	private static void updateBasesSituationAndScore(int event) {
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
						runnersOnBase[b] = runnersOnBase[b-1];
					}
				}
				else if (b == 0 && e == 0) {
					runnersOnBase[b] = game.getLineup()[top][battingOrder[top] - 1].getId();
				}
				else {
					runnersOnBase[0] = 0;
				}
				if (b == 3 && runnersOnBase[2] != 0) {     // If runner on 3rd run scores
					int bo = getBattingOrderForPlayer(runnersOnBase[2]);
					game.getLineup()[top][bo - 1].getGameStats().incrementRuns();
					runsScored++;
				}
			}
		}
		if (runsScored > 0) {
			game.setBoxScore(top, inning, runsScored);
			System.out.println(runsScored + " RUNS SCORED - VIS: " + game.getScore(inning)[0]  + " HOME: " + game.getScore(inning)[1]);
		}
	}
	
	private static void updateBasesSituationWalkAndHbp() {
		// No need for checking empty, 2, 3, or 23
		if (getCurrentBasesSituation() == BASES_LOADED || getCurrentBasesSituation() == MAN_ON_FIRST_AND_SECOND) { // 123 or 12
			// if 123 runner 3 scores
			if (getCurrentBasesSituation() == BASES_LOADED) {
				game.incrementBoxScore(top, inning); // run scores
				int bo = getBattingOrderForPlayer(runnersOnBase[2]);
				game.getLineup()[top][bo - 1].getGameStats().incrementRuns();
				System.out.println("1 RUN SCORED - VIS: " + game.getScore(inning)[0]  + " HOME: " + game.getScore(inning)[1]);
			}
			runnersOnBase[2] = runnersOnBase[1]; // runner 2->3
		}
		if (runnersOnBase[0] != 0) { // Runner on first
			runnersOnBase[1] = runnersOnBase[0]; // runner 1->2
		}
		runnersOnBase[0] = game.getLineup()[top][battingOrder[top] - 1].getId();
		
	}
	
	private static int updateBasesSituationSacFly(boolean deep, BattingStats currentBatterSeasonStats) {
		int outAdvancing = 0;
		int bo = getBattingOrderForPlayer(runnersOnBase[2]);
		int sacRando = getRandomNumberInRange(0, 5) + currentBatterSeasonStats.getSpeedRating();
		if (deep || (!deep && currentBatterSeasonStats.getSpeedRating() > 2)) {  // If going to try to score on fly ball
			System.out.println(game.getLineup()[top][bo - 1].getName() + " TAGGING UP ON A FLY BALL");
			if (deep ||(sacRando > 5)) { // safe
				game.incrementBoxScore(top, inning); // run scores
				game.getLineup()[top][bo - 1].getGameStats().incrementRuns();
				System.out.println("SAC FLY - 1 RUN SCORED - VIS: " + game.getScore(inning)[0]  + " HOME: " + game.getScore(inning)[1]);
			}
			else { // out
				outAdvancing = 1;
				System.out.println("OUT AT THE PLATE");
			}
			runnersOnBase[2] = 0;
		}
		return outAdvancing;
	}
	
	private static void updateBasesSituationDoublePlay() {
		boolean runScores = getCurrentBasesSituation() == BASES_LOADED || getCurrentBasesSituation() == MAN_ON_FIRST_AND_THIRD;
		if (getCurrentBasesSituation() == MAN_ON_FIRST_AND_SECOND || getCurrentBasesSituation() == BASES_LOADED) {
			runnersOnBase[2] = runnersOnBase[1]; // 2->3
		}
		runnersOnBase[1] = 0;
		runnersOnBase[0] = 0;
		if (runScores) {
			game.incrementBoxScore(top, inning); // run scores
			System.out.println("RUN SCORES - VIS: " + game.getScore(inning)[0]  + " HOME: " + game.getScore(inning)[1]);
			int bo = getBattingOrderForPlayer(runnersOnBase[2]);
			game.getLineup()[top][bo - 1].getGameStats().incrementRuns();
		}
	}
	
	private static boolean doublePlay(boolean ground, int outs) {
		boolean dp = false;
		int dpRando = getRandomNumberInRange(1, 4);
		
		//TBD factor in speed of runner
		// Ground ball, less than 2 outs, man on first
		if (ground && outs != 2 && dpRando > 1 && runnersOnBase[0] != 0) {
			dp = true;
			updateBasesSituationDoublePlay();
			System.out.println("DOUBLE PLAY");
		}
		return dp;
	}
	
	private static int stealBase(int outs, BattingStats currentBatterSeasonStats) {
		int outStealing = 0;
		if (runnersOnBase[0] != 0 && runnersOnBase[1] == 0) {
			int bo = getBattingOrderForPlayer(runnersOnBase[0]);
			int stealRando = getRandomNumberInRange(0, 5) + currentBatterSeasonStats.getSpeedRating();
			if (stealRando > 5) {
				double stealPctg = (double)currentBatterSeasonStats.getStolenBases()/(currentBatterSeasonStats.getStolenBases() + currentBatterSeasonStats.getCaughtStealing());
				System.out.print(game.getLineup()[top][bo - 1].getName() + " ATTEMPTING TO STEAL SECOND - SR: " + currentBatterSeasonStats.getSpeedRating() + " SP: " + df.format(stealPctg));
				if (getRandomNumberInRange(1, 10) < stealPctg*10) { // safe
					System.out.println("- SAFE!");
					runnersOnBase[1] = runnersOnBase[0];
				}
				else { // out
					System.out.println("- OUT!");
					outStealing = 1;
				}
				runnersOnBase[0] = 0;
				System.out.println("OUTS: " + (outs + outStealing) + " " + baseSituations.get(getCurrentBasesSituation()) + " " + runnersOnBase[0] + " " + runnersOnBase[1] + " " + runnersOnBase[2]);
			}
		}
		return outStealing;
	}
	
	private static int getRandomNumberInRange(int min, int max) {
		if (min >= max) {
			throw new IllegalArgumentException("max must be greater than min");
		}
		int rando = (int)(Math.random() * ((max - min) + 1)) + min;
		randoLog.add(rando + " " + min + " to " + max);
		return rando;
	}
	
	private static int getRandomNumberInRange(int min, int max, int excluding) {
		if (min >= max) {
			throw new IllegalArgumentException("max must be greater than min");
		}
		int rando = (int)(Math.random() * ((max - min) + 1)) + min;
		if (rando == excluding) {
			rando = getRandomNumberInRange(min, max, excluding);
		}
		randoLog.add(rando + " " + min + " to " + max + " ex: " + excluding);
		return rando;
	}
	
	private static boolean setLineup() {
		Player[][] lineup = new Player[2][Game.NUM_OF_PLAYERS_IN_LINEUP];
		for (int t = 0 ; t < 2; t++) {
			ArrayList<Integer> randomLineup = getRandomLineupByPosition();
			for (int p = 0 ; p < Game.NUM_OF_PLAYERS_IN_LINEUP; p++) {
				lineup[t][p] = new Player();
				Integer position = randomLineup.get(p);
				if (!positions.get(position).equals("P")) {
					MLBPlayer mlbPlayer = DAO.getMlbPlayerWithMostPlateAppearances((Integer)franchisesMap.get(game.getTeamNames()[t]), t == 0 ? visYear : homeYear, positions.get(position));
					if (mlbPlayer != null && mlbPlayer.getMlbPlayerId() != null) {
						lineup[t][p].setName(mlbPlayer.getFullName());
						lineup[t][p].setId(mlbPlayer.getMlbPlayerId());
					}
					else {
						System.out.println("No players at: " + positions.get(position) + " for " + game.getTeamNames()[t]);
						return false;
					}
				}
				else { // Temp placeholder for Pitcher
					lineup[t][p].setName("Romine, Austin"); 
					lineup[t][p].setId(519222);
				}
			}
		}
		
		/*lineup[0][0].setName("Jose Altuve");
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
		lineup[1][8].setName("Mike Tauchman");*/
		
		game.setLineup(lineup);
		return true;
	}
	
	private static void outputBoxScore() {
		for (int top = 0; top < 2; top++) {
			String team = (top == 0) ? "\n" + game.getTeamNames()[top] : game.getTeamNames()[top];
			team += team.length() < 3 ? " " : "";
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
				BattingStats playerSeasonStats = battingStatsMap.get(player.getId()) == null ? battingStatsMap.get(player.getId()) : new BattingStats();
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
	
	static int getBattingOrderForPlayer(int id) {
		int order = 1;
		for (Player p : game.getLineup(top)) {
			if (p.getId() == id) {
				return order;
			}
			order++;
		}
		return order;
	}
	
	static int getCurrentBasesSituation() {
		int baseSituation = BASES_EMPTY;
		if ((runnersOnBase[0] != 0 && runnersOnBase[1] == 0 && runnersOnBase[2] == 0)) {
			baseSituation = MAN_ON_FIRST;
		}
		else if ((runnersOnBase[0] == 0 && runnersOnBase[1] != 0 && runnersOnBase[2] == 0)) {
			baseSituation = MAN_ON_SECOND;
		}
		else if ((runnersOnBase[0] == 0 && runnersOnBase[1] == 0 && runnersOnBase[2] != 0)) {
			baseSituation = MAN_ON_THIRD;
		}
		else if ((runnersOnBase[0] != 0 && runnersOnBase[1] != 0 && runnersOnBase[2] == 0)) {
			baseSituation = MAN_ON_FIRST_AND_SECOND;
		}
		else if ((runnersOnBase[0] == 0 && runnersOnBase[1] != 0 && runnersOnBase[2] != 0)) {
			baseSituation = MAN_ON_SECOND_AND_THIRD;
		}
		else if ((runnersOnBase[0] != 0 && runnersOnBase[1] == 0 && runnersOnBase[2] != 0)) {
			baseSituation = MAN_ON_FIRST_AND_THIRD;
		}
		else if ((runnersOnBase[0] != 0 && runnersOnBase[1] != 0 && runnersOnBase[2] != 0)) {
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
			        		battingStatsMap.put(game.getLineup()[top][p].getId(), battingStats);
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
			HashMap<Object, Object> visitorBattingStats = DAO.getDataMap("MLB_BATTING_STATS", (Integer)franchisesMap.get(game.getTeamNames()[0]), visYear);
			HashMap<Object, Object> homeBattingStats = DAO.getDataMap("MLB_BATTING_STATS", (Integer)franchisesMap.get(game.getTeamNames()[1]), homeYear);
			for (Map.Entry<Object, Object> entry : visitorBattingStats.entrySet()) {
				MLBBattingStats battingStats = (MLBBattingStats)entry.getValue();
				battingStatsMap.put(battingStats.getMlbPlayerId(), battingStats.getBattingStats());
			}
			for (Map.Entry<Object, Object> entry : homeBattingStats.entrySet()) {
				MLBBattingStats battingStats = (MLBBattingStats)entry.getValue();
				battingStatsMap.put(battingStats.getMlbPlayerId(), battingStats.getBattingStats());
			}
		}

}
