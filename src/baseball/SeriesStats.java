package baseball;

import java.util.HashMap;

import db.MLBPlayer;

public class SeriesStats {
	int hits = 0;
	int errors = 0;
	int runsScored = 0;
	HashMap<Integer, MLBPlayer> pitchers = new HashMap<Integer, MLBPlayer>();
	HashMap<Integer, MLBPlayer> batters = new HashMap<Integer, MLBPlayer>();
	String teamName;
	int year = 2019;
	int seriesLength;
	
	public SeriesStats(String teamName, int year, int seriesLength) {
		this.teamName = teamName;
		this.year = year;
		this.seriesLength = seriesLength;
	}

	public int getHits() {
		return hits;
	}

	public void setHits(int hits) {
		this.hits = hits;
	}

	public int getErrors() {
		return errors;
	}

	public void setErrors(int errors) {
		this.errors = errors;
	}

	public int getRunsScored() {
		return runsScored;
	}

	public void setRunsScored(int runsScored) {
		this.runsScored = runsScored;
	}

	public HashMap<Integer, MLBPlayer> getPitchers() {
		return pitchers;
	}

	public void setPitchers(HashMap<Integer, MLBPlayer> pitchers) {
		this.pitchers = pitchers;
	}

	public HashMap<Integer, MLBPlayer> getBatters() {
		return batters;
	}

	public void setBatters(HashMap<Integer, MLBPlayer> batters) {
		this.batters = batters;
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

	public int getSeriesLength() {
		return seriesLength;
	}

	public void setSeriesLength(int seriesLength) {
		this.seriesLength = seriesLength;
	}

	
}
