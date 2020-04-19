package db;

import baseball.BattingStats;

public class MLBBattingStats {
	private Integer mlbBattingStatsId;
	private Integer mlbPlayerId;
	private Integer mlbTeamId;
	private Integer year;
	private BattingStats battingStats;
	
	public MLBBattingStats () {
		this.battingStats = new BattingStats();
	}

	public MLBBattingStats(Integer mlbPlayerId, Integer mlbTeamId, Integer year, BattingStats battingStats) {
		this.mlbPlayerId = mlbPlayerId;
		this.mlbTeamId = mlbTeamId;
		this.year = year;
		this.battingStats = battingStats;
	}

	public Integer getMlbBattingStatsId() {
		return mlbBattingStatsId;
	}

	public void setMlbBattingStatsId(Integer mlbBattingStatsId) {
		this.mlbBattingStatsId = mlbBattingStatsId;
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

	public BattingStats getBattingStats() {
		return battingStats;
	}

	public void setBattingStats(BattingStats battingStats) {
		this.battingStats = battingStats;
	}

}
