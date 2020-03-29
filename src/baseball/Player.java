package baseball;

public class Player {
	
	private String name;
	private int id;
	private BattingStats gameStats;
	
	public Player() {
		this.gameStats = new BattingStats();
	}

	public Player(String name, int id) {
		this.name = name;
		this.id = id;
		this.gameStats = new BattingStats();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public BattingStats getGameStats() {
		return gameStats;
	}

	public void setGameStats(BattingStats gameStats) {
		this.gameStats = gameStats;
	}

}
