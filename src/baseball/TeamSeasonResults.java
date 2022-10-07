package baseball;

public class TeamSeasonResults implements Comparable<TeamSeasonResults> {
	private Integer teamId = 0;
	private Integer wins = 0;
	private Integer losses = 0;
	private Integer runsFor = 0;
	private Integer runsAgainst = 0;
	
	public Integer getTeamId() {
		return teamId;
	}
	public void setTeamId(Integer teamId) {
		this.teamId = teamId;
	}
	public Integer getWins() {
		return wins;
	}
	public void setWins(Integer wins) {
		this.wins = wins;
	}
	public Integer getLosses() {
		return losses;
	}
	public void setLosses(Integer losses) {
		this.losses = losses;
	}
	public Integer getRunsFor() {
		return runsFor;
	}
	public void setRunsFor(Integer runsFor) {
		this.runsFor = runsFor;
	}
	public Integer getRunsAgainst() {
		return runsAgainst;
	}
	public void setRunsAgainst(Integer runsAgainst) {
		this.runsAgainst = runsAgainst;
	}
	
	@Override
    public int compareTo(TeamSeasonResults teamSeasonResults) {
        return (int)(teamSeasonResults.getWins() - this.wins);
    }
	
	public String toString () {
		return teamId.toString();
	}

}
