package db;

public class MLBFranchise {
	
	private Integer mlbFranchiseId;
	private String fullTeamName;
	private String shortTeamName;
	private Integer firstYearPlayed;

	public MLBFranchise(Integer mlbFranchiseId, String fullTeamName, String shortTeamName, Integer firstYearPlayed) {
		super();
		this.mlbFranchiseId = mlbFranchiseId;
		this.fullTeamName = fullTeamName;
		this.shortTeamName = shortTeamName;
		this.firstYearPlayed = firstYearPlayed;
	}


	public Integer getMlbFranchiseId() {
		return mlbFranchiseId;
	}


	public void setMlbFranchiseId(Integer mlbFranchiseId) {
		this.mlbFranchiseId = mlbFranchiseId;
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

	public Integer getFirstYearPlayed() {
		return firstYearPlayed;
	}

	public void setFirstYearPlayed(Integer firstYearPlayed) {
		this.firstYearPlayed = firstYearPlayed;
	}
}
