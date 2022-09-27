package db;

import java.util.ArrayList;

public class MLBPlayer {
	private Integer mlbPlayerId;
	private String fullName;
	private String primaryPosition;
	private String armThrows;
	private String bats;
	private Integer jerseyNumber;
	private MLBBattingStats mlbBattingStats;
	private MLBPitchingStats mlbPitchingStats;
	private ArrayList<MLBFieldingStats> mlbFieldingStats;
	
	public MLBPlayer() {
	}
	
	public MLBPlayer(Integer mlbPlayerId, String fullName, String primaryPosition) {
		this.mlbPlayerId = mlbPlayerId;
		this.fullName = fullName;
		this.primaryPosition = primaryPosition;
		this.mlbBattingStats = new MLBBattingStats();
		this.mlbPitchingStats = new MLBPitchingStats();
		this.mlbFieldingStats = new ArrayList<>();
	}
	
	
	public MLBPlayer(Integer mlbPlayerId, String fullName, String primaryPosition, String armThrows, String bats, Integer jerseyNumber) {
		this.mlbPlayerId = mlbPlayerId;
		this.fullName = fullName;
		this.primaryPosition = primaryPosition;
		this.armThrows = armThrows;
		this.bats = bats;
		this.jerseyNumber = jerseyNumber;
		this.mlbBattingStats = new MLBBattingStats();
		this.mlbPitchingStats = new MLBPitchingStats();
		this.mlbFieldingStats = new ArrayList<>();
	}
	
	public MLBPlayer(Integer mlbPlayerId, String fullName, String primaryPosition, String armThrows, String bats, 
			Integer jerseyNumber, ArrayList<MLBFieldingStats> mlbFieldingStats) {
		this.mlbPlayerId = mlbPlayerId;
		this.fullName = fullName;
		this.primaryPosition = primaryPosition;
		this.armThrows = armThrows;
		this.bats = bats;
		this.jerseyNumber = jerseyNumber;
		this.mlbBattingStats = new MLBBattingStats();
		this.mlbPitchingStats = new MLBPitchingStats();
		this.mlbFieldingStats = mlbFieldingStats;
	}

	public Integer getMlbPlayerId() {
		return mlbPlayerId;
	}

	public void setMlbPlayerId(Integer mlbPlayerId) {
		this.mlbPlayerId = mlbPlayerId;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getPrimaryPosition() {
		return primaryPosition;
	}

	public void setPrimaryPosition(String primaryPosition) {
		this.primaryPosition = primaryPosition;
	}

	public String getArmThrows() {
		return armThrows;
	}

	public void setArmThrows(String armThrows) {
		this.armThrows = armThrows;
	}

	public String getBats() {
		return bats;
	}

	public void setBats(String bats) {
		this.bats = bats;
	}

	public Integer getJerseyNumber() {
		return jerseyNumber;
	}

	public void setJerseyNumber(Integer jerseyNumber) {
		this.jerseyNumber = jerseyNumber;
	}

	public MLBBattingStats getMlbBattingStats() {
		return mlbBattingStats;
	}

	public void setMlbBattingStats(MLBBattingStats mlbBattingStats) {
		this.mlbBattingStats = mlbBattingStats;
	}

	public MLBPitchingStats getMlbPitchingStats() {
		return mlbPitchingStats;
	}

	public void setMlbPitchingStats(MLBPitchingStats mlbPitchingStats) {
		this.mlbPitchingStats = mlbPitchingStats;
	}

	public ArrayList<MLBFieldingStats> getMlbFieldingStats() {
		return mlbFieldingStats;
	}

	public void setMlbFieldingStats(ArrayList<MLBFieldingStats> mlbFieldingStats) {
		this.mlbFieldingStats = mlbFieldingStats;
	}

	public String getFirstLastName() {
		String firstLastName = "";
		if (fullName != null && fullName.length() > 0 && fullName.indexOf(",") != -1) {
			String[] flnArray = fullName.split(",");
			firstLastName = flnArray[1].trim() + " " + flnArray[0];
		}
		return firstLastName;
	}
	
	public String getPrimaryPositionByFieldingStats() {
		String primaryPositionByFieldingStats = primaryPosition;
		if (mlbFieldingStats != null && mlbFieldingStats.size() > 0) {
			primaryPositionByFieldingStats = mlbFieldingStats.get(0).getPosition();
		}
		return primaryPositionByFieldingStats;
	}
	
	public String toString () {
		return fullName;
	}
}
