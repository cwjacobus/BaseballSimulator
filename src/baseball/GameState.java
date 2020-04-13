package baseball;

public class GameState {
	int outs = 0;
	Integer inning = 1;
	int top = 0;
	int[] runnersOnBase = {0, 0, 0};
	int[] battingOrder = {1, 1};
	Player[] currentPitchers = {null, null};

	public GameState() {
		//pitchers.add(new ArrayList<Player>());
		//pitchers.add(new ArrayList<Player>());
	}

	public int getOuts() {
		return outs;
	}

	public void setOuts(int outs) {
		this.outs = outs;
	}

	public Integer getInning() {
		return inning;
	}

	public void setInning(Integer inning) {
		this.inning = inning;
	}
	
	public void incrementInning() {
		this.inning++;
	}

	public int getTop() {
		return top;
	}

	public void setTop(int top) {
		this.top = top;
	}

	public int[] getRunnersOnBase() {
		return runnersOnBase;
	}

	public void setRunnersOnBase(int[] runnersOnBase) {
		this.runnersOnBase = runnersOnBase;
	}

	public int[] getBattingOrder() {
		return battingOrder;
	}

	public void setBattingOrder(int[] battingOrder) {
		this.battingOrder = battingOrder;
	}

	public Player[] getCurrentPitchers() {
		return currentPitchers;
	}

	public void setCurrentPitchers(Player[] currentPitchers) {
		this.currentPitchers = currentPitchers;
	}



}
