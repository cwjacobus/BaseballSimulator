package db;

public class MLBTeam {
	private Integer mlbTeamId;
	private Integer teamId;
	private Integer mlbFranchiseId;
	private String fullTeamName;
	private String shortTeamName;
	private String league;
	private Integer firstYearPlayed;
	private Integer lastYearPlayed;

	public MLBTeam(Integer teamId, Integer mlbFranchiseId, String fullTeamName, String shortTeamName, String league, Integer firstYearPlayed, Integer lastYearPlayed) {
		this.teamId = teamId;
		this.mlbFranchiseId = mlbFranchiseId;
		this.fullTeamName = fullTeamName;
		this.shortTeamName = shortTeamName;
		this.league = league;
		this.firstYearPlayed = firstYearPlayed;
		this.lastYearPlayed = lastYearPlayed;
	}
	
	public MLBTeam(Integer teamId) {
		this.teamId = teamId;
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

	public Integer getMlbFranchiseId() {
		return mlbFranchiseId;
	}

	public void setMlbFranchiseId(Integer mlbFranchiseId) {
		this.mlbFranchiseId = mlbFranchiseId;
	}

	public Integer getFirstYearPlayed() {
		return firstYearPlayed;
	}

	public void setFirstYearPlayed(Integer firstYearPlayed) {
		this.firstYearPlayed = firstYearPlayed;
	}

	public Integer getLastYearPlayed() {
		return lastYearPlayed;
	}

	public void setLastYearPlayed(Integer lastYearPlayed) {
		this.lastYearPlayed = lastYearPlayed;
	}
	
	public String toString () {
		return fullTeamName;
	}

}
