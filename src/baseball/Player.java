package baseball;

public class Player {
	
	private String name;
	private int id;
	private Stats gameStats;
	
	public Player() {
		this.gameStats = new Stats();
	}

	public Player(String name, int id) {
		this.name = name;
		this.id = id;
		this.gameStats = new Stats();
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

	public Stats getGameStats() {
		return gameStats;
	}

	public void setGameStats(Stats gameStats) {
		this.gameStats = gameStats;
	}

}
