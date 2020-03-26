package baseball;

import java.util.HashMap;
import java.util.Map;

public class BaseballSimulator {
	
	public static final int OUTS_PER_INNING = 3;
	public static final int INNINGS_PER_GAME = 9;
	
	static int currentBasesSituation = 0;
	static int[] gameScore = {0, 0};
	static int[] hits = {0, 0};
	static int[] errors = {0, 0};
	static int[][] boxScore = new int[2][25];
	static int inning = 1;
	static int top = 0;
	static boolean walkOff = false;
	
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
		while (inning <= INNINGS_PER_GAME || gameScore[0] == gameScore[1]) {
			for (top = 0; top < 2; top++) {
				System.out.println((top == 0 ? "TOP " : "BOTTOM ") + " INN: " + inning);
				int outs  = 0;
				boolean gameTiedStartOfAB;
				currentBasesSituation = 0;
				while (outs < OUTS_PER_INNING) {
					System.out.println("BATTER UP  OUTS: " + outs + " " + baseSituations.get(currentBasesSituation));
					int rando = getRandomNumberInRange(1, 1000);
					
					System.out.print("BATTER ");
					gameTiedStartOfAB = gameScore[1] == gameScore[0] ? true : false;
					if (rando <= 680) {
						System.out.print("OUT ");
						getOutResult();
						outs++;
					}
					else {
						if (rando > 680 && rando <= 775) {
							if (rando >= 760) {
								System.out.println("HIT BY PITCH");
							}
							else {
								System.out.println("WALKED");
							}
							if (baseSituations.get(currentBasesSituation).equalsIgnoreCase("BASES LOADED")) {
								gameScore[top]++;
								boxScore[top][inning-1]++;
							}
							currentBasesSituation = getBasesSituationWalk(currentBasesSituation);
						}
						else {
							int noOutResult = getNotOutResult();
							getRunsScored(noOutResult, currentBasesSituation, top);
							currentBasesSituation = getBasesSituation(noOutResult, currentBasesSituation);
							hits[top]++;
						}
						if (inning >= 9 && gameScore[1] > gameScore[0] && gameTiedStartOfAB) {
							walkOff = true;
							System.out.println("WALKOFF ");
							break;
						}
					}
				} // outs
				// Did game end after top of inning?
				if (inning >= 9 && gameScore[1] > gameScore[0] && top == 0) {
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
		for (top = 0; top < 2; top++) {
			String team = (top == 0) ? "Vis:  " : "Home: ";
			System.out.print(team + " ");
			for (int i = 1; i <= (inning - 1); i++) {
				if (i == (inning - 1) && (gameScore[1] > gameScore[0] && top == 1) && !walkOff) {
					System.out.print("X "); // Bottom of inning was not necessary
				}
				else {
					System.out.print(boxScore[top][i-1] + " ");
				}
			}
			System.out.println(" " + gameScore[top] + (gameScore[top] < 10 ? " " : "") + " " + hits[top] + (hits[top] < 10 ? " " : "") + " " +  errors[top]);
		}
	}
	
	private static int getNotOutResult() {
		int notOutResult = 1;
		int notOutRando = getRandomNumberInRange(1, 1000);
		if (notOutRando > 1 && notOutRando <= 25) {
			System.out.println("REACHED BY ERROR");
			notOutResult = 1;
			errors[top == 0 ? 1 : 0]++;
		}
		else if (notOutRando > 25 && notOutRando <= 160) {
			System.out.println("HOME RUN");
			notOutResult = 4;
		}
		else if (notOutRando > 170 && notOutRando <= 200) {
			System.out.println("TRIPLED");
			notOutResult = 3;
		}
		else if (notOutRando > 210 && notOutRando < 500) {
			System.out.println("DOUBLED");
			notOutResult = 2;
		}
		else {
			System.out.println("SINGLED");
		}
		
		return notOutResult;
	}
	
	private static boolean getOutResult() {
		boolean doublePlay = false;
		int notOutRando = getRandomNumberInRange(1, 100);
		if (notOutRando > 1 && notOutRando <= 20) {
			System.out.println(outTypes.get(1)); // STRUCK OUT
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
		gameScore[top] += runsScored;
		boxScore[top][inning-1] += runsScored;
		if (runsScored > 0) {
			System.out.println(runsScored + " RUNS SCORED - VIS: " + gameScore[0]  + " HOME: " + gameScore[1]);
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

}
