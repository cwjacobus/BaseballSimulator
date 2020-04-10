package baseball;

public class Player {
	
	private String name;
	private int id;
	private BattingStats battingStats;
	private PitchingStats pitchingStats;
	private String position;
	
	public Player() {
		this.battingStats = new BattingStats();
		this.pitchingStats = new PitchingStats();
	}

	public Player(String name, int id, String position) {
		this.name = name;
		this.id = id;
		this.battingStats = new BattingStats();
		this.pitchingStats = new PitchingStats();
		this.position = position;
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

	public BattingStats getBattingStats() {
		return battingStats;
	}

	public void setBattingStats(BattingStats battingStats) {
		this.battingStats = battingStats;
	}

	public PitchingStats getPitchingStats() {
		return pitchingStats;
	}

	public void setPitchingStats(PitchingStats pitchingStats) {
		this.pitchingStats = pitchingStats;
	}

	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}

}
