package db;

public class MLBPlayer {
	private Integer mlbPlayerId;
	private String fullName;
	private String primaryPosition;
	private String armThrows;
	private String bats;
	private Integer jerseyNumber;
	
	public MLBPlayer() {
		
	}
	
	public MLBPlayer(Integer mlbPlayerId, String fullName, String primaryPosition, String armThrows, String bats,
			Integer jerseyNumber) {
		this.mlbPlayerId = mlbPlayerId;
		this.fullName = fullName;
		this.primaryPosition = primaryPosition;
		this.armThrows = armThrows;
		this.bats = bats;
		this.jerseyNumber = jerseyNumber;
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

	public String getFirstLastName() {
		String firstLastName = "";
		if (fullName != null && fullName.length() > 0 && fullName.indexOf(",") != -1) {
			String[] flnArray = fullName.split(",");
			firstLastName = flnArray[1].trim() + " " + flnArray[0];
		}
		return firstLastName;
	}
}
