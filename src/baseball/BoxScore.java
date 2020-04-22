package baseball;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class BoxScore {
	public static final int NUM_OF_PLAYERS_IN_LINEUP = 9;
	int hits = 0;
	int errors = 0;
	int[] runsScored = new int[50];
	boolean walkOff = false;
	ArrayList<ArrayList<Player>> batters = new ArrayList<ArrayList<Player>>();
	LinkedHashMap<Integer, Player> pitchers = new LinkedHashMap<Integer, Player>();
	String teamName;
	int year = 2019;
	
	public BoxScore() {
		for (int i = 0; i < NUM_OF_PLAYERS_IN_LINEUP; i++) {
			batters.add(new ArrayList<Player>());
		}
	}

	public int getScore(int inning) {
		int score = 0;
		for (int i = 0; i < inning; i++) {
			score += runsScored[i];
		}
		return score;
	}

	public int getHits() {
		return hits;
	}

	public void setHits(int hits) {
		this.hits = hits;
	}
	
	public void incrementHits() {
		this.hits++;
	}

	public int getErrors() {
		return errors;
	}

	public void setErrors(int errors) {
		this.errors = errors;
	}
	
	public void incrementErrors() {
		this.errors++;
	}

	public int getRunsScored(int inning) {
		return runsScored[inning - 1];
	}
	
	public int getFinalScore() {
		return getScore(runsScored.length - 1);
	}

	public void setRunsScored(int inning, int runsScored) {
		this.runsScored[inning - 1] += runsScored;
	}
	
	public boolean isWalkOff() {
		return walkOff;
	}

	public void setWalkOff(boolean walkOff) {
		this.walkOff = walkOff;
	}

	public ArrayList<ArrayList<Player>> getBatters() {
		return batters;
	}

	public void setBatters(ArrayList<ArrayList<Player>> batters) {
		this.batters = batters;
	}

	public LinkedHashMap<Integer, Player> getPitchers() {
		return pitchers;
	}

	public void setPitchers(LinkedHashMap<Integer, Player> pitchers) {
		this.pitchers = pitchers;
	}

	public String getTeamName() {
		return teamName;
	}

	public void setTeamName(String teamName) {
		this.teamName = teamName;
	}

	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		this.year = year;
	}

	

}
