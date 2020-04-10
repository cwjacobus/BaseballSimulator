package db;

import baseball.PitchingStats;

public class MLBPitchingStats {
	private Integer mlbPitchingStatsId;
	private Integer mlbPlayerId;
	private Integer mlbTeamId;
	private Integer year;
	private PitchingStats pitchingStats;

	public MLBPitchingStats(Integer mlbPlayerId, Integer mlbTeamId, Integer year, PitchingStats pitchingStats) {
		this.mlbPlayerId = mlbPlayerId;
		this.mlbTeamId = mlbTeamId;
		this.year = year;
		this.pitchingStats = pitchingStats;
	}

	public Integer getMlbPitchingStatsId() {
		return mlbPitchingStatsId;
	}

	public void setMlbPitchingStatsId(Integer mlbPitchingStatsId) {
		this.mlbPitchingStatsId = mlbPitchingStatsId;
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

	public PitchingStats getPitchingStats() {
		return pitchingStats;
	}

	public void setPitchingStats(PitchingStats pitchingStats) {
		this.pitchingStats = pitchingStats;
	}

}
