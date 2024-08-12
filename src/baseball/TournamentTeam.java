package baseball;

import db.MLBTeam;

public class TournamentTeam {
	
	// Used for tournament and world series play
	
	public TournamentTeam(Integer year, MLBTeam mlbTeam) {
		this.year = year;
		this.mlbTeam = mlbTeam;
	}
	
	public TournamentTeam(Integer year, MLBTeam visMlbTeam, MLBTeam homeMlbTeam) {
		this.year = year;
		this.mlbTeam = visMlbTeam;
		this.wsHomeMlbTeam = homeMlbTeam;
	}
	
	Integer year;
	MLBTeam mlbTeam;
	MLBTeam wsHomeMlbTeam;
	
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
	
	public MLBTeam getWsHomeMlbTeam() {
		return wsHomeMlbTeam;
	}

	public void setWsHomeMlbTeam(MLBTeam wsHomeMlbTeam) {
		this.wsHomeMlbTeam = wsHomeMlbTeam; // Only needed for WS
	}

	public String toString () {
		return year + " " + mlbTeam.getFullTeamName() + (wsHomeMlbTeam != null ? " vs. " + wsHomeMlbTeam.getFullTeamName() : "");
	}

}
