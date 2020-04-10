package baseball;
import java.util.HashMap;

public class Roster {
	private HashMap<Object, Object> pitchers;  // MLBPlayers from DB
	private HashMap<Object, Object> batters;   // MLBPlayers from DB

	public Roster() {
		// TODO Auto-generated constructor stub
	}

	public HashMap<Object, Object> getBatters() {
		return batters;
	}

	public void setBatters(HashMap<Object, Object> batters) {
		this.batters = batters;
	}

	public HashMap<Object, Object> getPitchers() {
		return pitchers;
	}

	public void setPitchers(HashMap<Object, Object> pitchers) {
		this.pitchers = pitchers;
	}

}
