package baseball;

import db.MLBTeam;

public class TournamentTeam {
	
	// Used for tournament and world series play
	
	public TournamentTeam(Integer year, MLBTeam mlbTeam) {
		this.year = year;
		this.mlbTeam = mlbTeam;
	}
	
	Integer year;
	MLBTeam mlbTeam;
	
	public Integer getYear() {
		return year;
	}
	public void setYear(Integer year) {
		this.year = year;
	}
	
	public MLBTeam getMlbTeam() {
		return mlbTeam;
	}
	
	public void setMlbTeam(MLBTeam mlbTeam) {
		this.mlbTeam = mlbTeam;
	}

	public String toString () {
		return year + " " + mlbTeam.getFullTeamName();
	}

}
