package baseball;

public class Game {
	public static final int NUM_OF_PLAYERS_IN_LINEUP = 9;
	int currentBasesSituation = 0;
	int[] hits = {0, 0};
	int[] errors = {0, 0};
	int[][] boxScore = new int[2][25];
	boolean walkOff = false;
	Player[][] lineup;
	String[] teamNames = new String[2];

	public Game() {
		this.lineup = new Player[2][NUM_OF_PLAYERS_IN_LINEUP];
	}

	public int getCurrentBasesSituation() {
		return currentBasesSituation;
	}

	public void setCurrentBasesSituation(int currentBasesSituation) {
		this.currentBasesSituation = currentBasesSituation;
	}

	public int[] getScore(int inning) {
		int[] score = {0, 0};
		for (int i = 0; i < inning; i++) {
			score[0] += getBoxScore(0, i);
			score[1] += getBoxScore(1, i);
		}
		return score;
	}

	public int[] getHits() {
		return hits;
	}

	public void setHits(int top, int hits) {
		this.hits[top] = hits;
	}
	
	public void incrementHits(int top) {
		this.hits[top]++;
	}

	public int[] getErrors() {
		return errors;
	}

	public void setErrors(int top, int errors) {
		this.errors[top] = errors;
	}
	
	public void incrementErrors(int top) {
		this.errors[top]++;
	}

	public int getBoxScore(int top, int inning) {
		return boxScore[top][inning];
	}

	public void setBoxScore(int top, int inning, int score) {
		this.boxScore[top][inning - 1] += score;
	}

	public boolean isWalkOff() {
		return walkOff;
	}

	public void setWalkOff(boolean walkOff) {
		this.walkOff = walkOff;
	}

	public Player[][] getLineup() {
		return lineup;
	}

	public void setLineup(Player[][] lineup) {
		this.lineup = lineup;
	}

	public String[] getTeamNames() {
		return teamNames;
	}

	public void setTeamNames(String[] teamNames) {
		this.teamNames = teamNames;
	}

}
