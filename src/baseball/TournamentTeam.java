package baseball;

public class TournamentTeam {
	
	public TournamentTeam(Integer year, Integer teamId) {
		this.year = year;
		this.teamId = teamId;
	}
	
	Integer year;
	Integer teamId;
	
	public Integer getYear() {
		return year;
	}
	public void setYear(Integer year) {
		this.year = year;
	}
	public Integer getTeamId() {
		return teamId;
	}
	public void setTeamId(Integer teamId) {
		this.teamId = teamId;
	}

}
