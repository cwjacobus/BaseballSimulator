package db;

public class TeamPlayer {
	private Integer teamPlayerId;
	private Integer mlbTeamId;
	private Integer mlbPlayerId;
	private Integer year;

	public TeamPlayer(Integer mlbTeamId, Integer mlbPlayerId, Integer year) {
		this.mlbTeamId = mlbTeamId;
		this.mlbPlayerId = mlbPlayerId;
		this.year = year;
	}

	public Integer getTeamPlayerId() {
		return teamPlayerId;
	}

	public void setTeamPlayerId(Integer teamPlayerId) {
		this.teamPlayerId = teamPlayerId;
	}

	public Integer getMlbTeamId() {
		return mlbTeamId;
	}

	public void setMlbTeamId(Integer mlbTeamId) {
		this.mlbTeamId = mlbTeamId;
	}

	public Integer getMlbPlayerId() {
		return mlbPlayerId;
	}

	public void setMlbPlayerId(Integer mlbPlayerId) {
		this.mlbPlayerId = mlbPlayerId;
	}

	public Integer getYear() {
		return year;
	}

	public void setYear(Integer year) {
		this.year = year;
	}

}
