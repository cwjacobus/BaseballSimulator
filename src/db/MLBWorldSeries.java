package db;

public class MLBWorldSeries {
	
	public MLBWorldSeries() {
	}
	
	public MLBWorldSeries(Integer year, String team1, String team2, String winner) {
		this.year = year;
		this.team1 = team1;
		this.team2 = team2;
		this.winner = winner;
	}

	Integer year;
	String team1;
	String team2;
	String winner;
	
	public Integer getYear() {
		return year;
	}

	public void setYear(Integer year) {
		this.year = year;
	}

	public String getTeam1() {
		return team1;
	}

	public void setTeam1(String team1) {
		this.team1 = team1;
	}

	public String getTeam2() {
		return team2;
	}

	public void setTeam2(String team2) {
		this.team2 = team2;
	}

	public String getWinner() {
		return winner;
	}

	public void setWinner(String winner) {
		this.winner = winner;
	}

	public String toString () {
		return year + " " + team1 + " vs. " + team2;
	}

}
