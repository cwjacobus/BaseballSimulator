package db;

public class Team {
	Integer mlbTeamId;
	Integer teamId;
	String fullTeamName;
	String shortTeamName;
	String league;
	Integer firstYearPlayed;
	boolean active;

	public Team(Integer teamId, String fullTeamName, String shortTeamName, String league, Integer firstYearPlayed, boolean active) {
		this.teamId = teamId;
		this.fullTeamName = fullTeamName;
		this.shortTeamName = shortTeamName;
		this.league = league;
		this.firstYearPlayed = firstYearPlayed;
		this.active = active;
	}

	public Integer getMlbTeamId() {
		return mlbTeamId;
	}

	public void setMlbTeamId(Integer mlbTeamId) {
		this.mlbTeamId = mlbTeamId;
	}

	public int getTeamId() {
		return teamId;
	}

	public void setTeamId(Integer teamId) {
		this.teamId = teamId;
	}

	public String getFullTeamName() {
		return fullTeamName;
	}

	public void setFullTeamName(String fullTeamName) {
		this.fullTeamName = fullTeamName;
	}

	public String getShortTeamName() {
		return shortTeamName;
	}

	public void setShortTeamName(String shortTeamName) {
		this.shortTeamName = shortTeamName;
	}

	public String getLeague() {
		return league;
	}

	public void setLeague(String league) {
		this.league = league;
	}

	public Integer getFirstYearPlayed() {
		return firstYearPlayed;
	}

	public void setFirstYearPlayed(Integer firstYearPlayed) {
		this.firstYearPlayed = firstYearPlayed;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

}
