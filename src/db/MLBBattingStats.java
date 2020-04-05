package db;

import baseball.BattingStats;

public class MLBBattingStats {
	private Integer mlbPlayerId;
	private BattingStats battingStats;

	public MLBBattingStats(Integer mlbPlayerId, BattingStats battingStats) {
		this.mlbPlayerId = mlbPlayerId;
		this.battingStats = battingStats;
	}

	public Integer getMlbPlayerId() {
		return mlbPlayerId;
	}

	public void setMlbPlayerId(Integer mlbPlayerId) {
		this.mlbPlayerId = mlbPlayerId;
	}

	public BattingStats getBattingStats() {
		return battingStats;
	}

	public void setBattingStats(BattingStats battingStats) {
		this.battingStats = battingStats;
	}

}
