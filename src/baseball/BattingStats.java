package baseball;

public class BattingStats {
	
	private int atBats;
	private int hits;
	private int doubles;
	private int triples;
	private int homeRuns;
	private int walks;
	private int strikeOuts;
	private int hitByPitch;
	private int errors;
	private int runs;
	private int rbis;
	private int stolenBases;
	private int plateAppearances;

	public BattingStats() {
		// TODO Auto-generated constructor stub
	}

	public int getAtBats() {
		return atBats;
	}

	public void setAtBats(int atBats) {
		this.atBats = atBats;
	}
	
	public void incrementAtBats() {
		this.atBats++;
	}

	public int getHits() {
		return hits;
	}

	public void setHits(int hits) {
		this.hits = hits;
	}
	
	public void incrementHits() {
		this.hits++;
	}

	public int getSingles() {
		return hits - (homeRuns + triples + doubles);
	}

	public int getDoubles() {
		return doubles;
	}

	public void setDoubles(int doubles) {
		this.doubles = doubles;
	}
	
	public void incrementDoubles() {
		this.doubles++;
	}
	
	public int getTriples() {
		return triples;
	}

	public void setTriples(int triples) {
		this.triples = triples;
	}
	
	public void incrementTriples() {
		this.triples++;
	}

	public int getHomeRuns() {
		return homeRuns;
	}

	public void setHomeRuns(int homeRuns) {
		this.homeRuns = homeRuns;
	}
	
	public void incrementHomeRuns() {
		this.homeRuns++;
	}

	public int getWalks() {
		return walks;
	}

	public void setWalks(int walks) {
		this.walks = walks;
	}
	
	public void incrementWalks() {
		this.walks++;
	}

	public int getStrikeOuts() {
		return strikeOuts;
	}

	public void setStrikeOuts(int strikeOuts) {
		this.strikeOuts = strikeOuts;
	}
	
	public void incremenStrikeOuts() {
		this.strikeOuts++;
	}

	public int getHitByPitch() {
		return hitByPitch;
	}

	public void setHitByPitch(int hitByPitch) {
		this.hitByPitch = hitByPitch;
	}

	public int getErrors() {
		return errors;
	}

	public void setErrors(int errors) {
		this.errors = errors;
	}

	public int getRuns() {
		return runs;
	}

	public void setRuns(int runs) {
		this.runs = runs;
	}

	public int getRbis() {
		return rbis;
	}

	public void setRbis(int rbis) {
		this.rbis = rbis;
	}

	public int getStolenBases() {
		return stolenBases;
	}

	public void setStolenBases(int stolenBases) {
		this.stolenBases = stolenBases;
	}

	public int getPlateAppearances() {
		return plateAppearances;
	}

	public void setPlateAppearances(int plateAppearances) {
		this.plateAppearances = plateAppearances;
	}

}