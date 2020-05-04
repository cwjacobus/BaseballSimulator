package db;

import baseball.FieldingStats;

public class MLBFieldingStats {
	private Integer mlbFieldingStatsId;
	private Integer mlbPlayerId;
	private Integer mlbTeamId;
	private Integer year;
	private String position;
	private FieldingStats fieldingStats;
	
	public MLBFieldingStats () {
		this.fieldingStats = new FieldingStats();
	}

	public MLBFieldingStats(Integer mlbPlayerId, Integer mlbTeamId, Integer year, String position, FieldingStats fieldingStats) {
		this.mlbPlayerId = mlbPlayerId;
		this.mlbTeamId = mlbTeamId;
		this.year = year;
		this.position = position;
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

	public FieldingStats getFieldingStats() {
		return fieldingStats;
	}

	public void setFieldingStats(FieldingStats fieldingStats) {
		this.fieldingStats = fieldingStats;
	}

}
