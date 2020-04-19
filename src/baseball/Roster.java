package baseball;
import java.util.HashMap;

import db.MLBPlayer;

public class Roster {
	private HashMap<Integer, MLBPlayer> pitchers;  // MLBPlayers from DB
	private HashMap<Integer, MLBPlayer> batters;   // MLBPlayers from DB

	public Roster() {
		// TODO Auto-generated constructor stub
	}

	public HashMap<Integer, MLBPlayer> getBatters() {
		return batters;
	}

	public void setBatters(HashMap<Integer, MLBPlayer> batters) {
		this.batters = batters;
	}

	public HashMap<Integer, MLBPlayer> getPitchers() {
		return pitchers;
	}

	public void setPitchers(HashMap<Integer, MLBPlayer> pitchers) {
		this.pitchers = pitchers;
	}

}
