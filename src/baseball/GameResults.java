package baseball;

import java.util.ArrayList;

public class GameResults {
	public static final int NUM_OF_PLAYERS_IN_LINEUP = 9;
	int[] hits = {0, 0};
	int[] errors = {0, 0};
	int[][] boxScore = new int[2][25];
	boolean walkOff = false;
	Player[][] lineup;
	ArrayList<ArrayList<Player>> pitchers = new ArrayList<ArrayList<Player>>(2);
	String[] teamNames = new String[2];
	int[] years = {2019, 2019};
	
	public GameResults() {
		pitchers.add(new ArrayList<Player>());
		pitchers.add(new ArrayList<Player>());
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
	
	public void incrementBoxScore(int top, int inning) {
		this.boxScore[top][inning - 1]++;
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
	
	public Player[] getLineup(int top) {
		return lineup[top];
	}

	public void setLineup(Player[][] lineup) {
		this.lineup = lineup;
	}

	public ArrayList<ArrayList<Player>> getPitchers() {
		return pitchers;
	}

	public void setPitchers(ArrayList<ArrayList<Player>> pitchers) {
		this.pitchers = pitchers;
	}
	
	public void addPitcher(Player pitcher, int top) {
		this.pitchers.get(top).add(pitcher);
	}

	public String[] getTeamNames() {
		return teamNames;
	}

	public void setTeamNames(String[] teamNames) {
		this.teamNames = teamNames;
	}

	public int[] getYears() {
		return years;
	}

	public void setYears(int[] years) {
		this.years = years;
	}
	
	public void setVisYear(int visYear) {
		this.years[0] = visYear;
	}
	
	public void setHomeYear(int homeYear) {
		this.years[1] = homeYear;
	}

}
