package db;

import java.util.Arrays;

import baseball.FieldingStats;

public class MLBFieldingStats {
	private Integer mlbFieldingStatsId;
	private Integer mlbPlayerId;
	private Integer mlbTeamId;
	private Integer year;
	private String position;
	private Integer games;
	private FieldingStats fieldingStats;
	
	public static final String[] outfieldPositions = {"CF","LF","RF","OF"};
	
	public MLBFieldingStats () {
		this.fieldingStats = new FieldingStats();
	}

	public MLBFieldingStats(Integer mlbPlayerId, Integer mlbTeamId, Integer year, String position, Integer games, FieldingStats fieldingStats) {
		this.mlbPlayerId = mlbPlayerId;
		this.mlbTeamId = mlbTeamId;
		this.year = year;
		this.position = position;
		this.games = games;
		this.fieldingStats = fieldingStats;
	}

	public Integer getMlbFieldingStatsId() {
		return mlbFieldingStatsId;
	}

	public void setMlbFieldingStatsId(Integer mlbFieldingStatsId) {
		this.mlbFieldingStatsId = mlbFieldingStatsId;
	}

	public Integer getMlbPlayerId() {
		return mlbPlayerId;
	}

	public void setMlbPlayerId(Integer mlbPlayerId) {
		this.mlbPlayerId = mlbPlayerId;
	}

	public Integer getMlbTeamId() {
		return mlbTeamId;
	}

	public void setMlbTeamId(Integer mlbTeamId) {
		this.mlbTeamId = mlbTeamId;
	}

	public Integer getYear() {
		return year;
	}

	public void setYear(Integer year) {
		this.year = year;
	}

	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}

	public Integer getGames() {
		return games;
	}

	public void setGames(Integer games) {
		this.games = games;
	}

	public FieldingStats getFieldingStats() {
		return fieldingStats;
	}

	public void setFieldingStats(FieldingStats fieldingStats) {
		this.fieldingStats = fieldingStats;
	}
	
	public int getFieldingRating() {
		int fieldingRating = 0;
		
		return fieldingRating;
	}
	
	public int getOutfielderArmRating() {
		int armRating = 0;
		
		if (Arrays.asList(outfieldPositions).contains(position)) {
			int outfieldAssists = getFieldingStats().getAssists();
			if (outfieldAssists >= 9) {
				armRating = 5;
			} 
			else if (outfieldAssists >= 7 && outfieldAssists < 9) {
				armRating = 4;
			}
			else if (outfieldAssists >= 5 && outfieldAssists < 7) {
				armRating = 3;
			}
			else if (outfieldAssists >= 3 && outfieldAssists < 5) {
				armRating = 2;
			}
			else if (outfieldAssists >= 1 && outfieldAssists < 3) {
				armRating = 1;
			}
		}
		return armRating;
	}
	
	public String toString() {
		return mlbPlayerId + " " + games + "at" + position + " " + mlbTeamId;
	}

}
