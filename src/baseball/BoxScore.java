package baseball;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import db.MLBPlayer;
import db.MLBTeam;

public class BoxScore {
	public static final int NUM_OF_PLAYERS_IN_LINEUP = 9;
	int hits = 0;
	int errors = 0;
	int[] runsScored = new int[50];
	boolean walkOff = false;
	ArrayList<ArrayList<MLBPlayer>> batters = new ArrayList<ArrayList<MLBPlayer>>();
	LinkedHashMap<Integer, MLBPlayer> pitchers = new LinkedHashMap<Integer, MLBPlayer>();
	MLBTeam team;
	int year = 2019;
	
	public BoxScore() {
		for (int i = 0; i < NUM_OF_PLAYERS_IN_LINEUP; i++) {
			batters.add(new ArrayList<MLBPlayer>());
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

	public void incrementRunsScored(int inning) {
		this.runsScored[inning - 1]++;
	}
	
	public boolean isWalkOff() {
		return walkOff;
	}

	public void setWalkOff(boolean walkOff) {
		this.walkOff = walkOff;
	}

	public ArrayList<ArrayList<MLBPlayer>> getBatters() {
		return batters;
	}

	public void setBatters(ArrayList<ArrayList<MLBPlayer>> batters) {
		this.batters = batters;
	}

	public LinkedHashMap<Integer, MLBPlayer> getPitchers() {
		return pitchers;
	}

	public void setPitchers(LinkedHashMap<Integer, MLBPlayer> pitchers) {
		this.pitchers = pitchers;
	}

	public MLBTeam getTeam() {
		return team;
	}

	public void setTeam(MLBTeam team) {
		this.team = team;
	}

	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		this.year = year;
	}
	
	public String getTeamAndYearDisplay() {
		return team.getShortTeamName() + "(" + year + ")";
	}
	
	public String toString() {
		return getTeamAndYearDisplay() + " " + getFinalScore();
	}
}
