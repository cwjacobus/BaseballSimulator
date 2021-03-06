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
	private int runs;
	private int rbis;
	private int stolenBases;
	private int plateAppearances;
	private int caughtStealing;

	public BattingStats() {
	}
	
	public BattingStats(int atBats, int hits, int doubles, int triples, int homeRuns, int walks, int strikeOuts,
			int hitByPitch, int runs, int rbis, int stolenBases, int plateAppearances, int caughtStealing) {
		this.atBats = atBats;
		this.hits = hits;
		this.doubles = doubles;
		this.triples = triples;
		this.homeRuns = homeRuns;
		this.walks = walks;
		this.strikeOuts = strikeOuts;
		this.hitByPitch = hitByPitch;
		this.runs = runs;
		this.rbis = rbis;
		this.stolenBases = stolenBases;
		this.plateAppearances = plateAppearances;
		this.caughtStealing = caughtStealing;
	}


	public int getAtBats() {
		return atBats;
	}

	public void setAtBats(int atBats) {
		this.atBats = atBats;
	}
	
	public void decrementAtBats() {
		this.atBats--;
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

	public int getRuns() {
		return runs;
	}

	public void setRuns(int runs) {
		this.runs = runs;
	}
	
	public void incrementRuns() {
		this.runs++;
	}

	public int getRbis() {
		return rbis;
	}

	public void setRbis(int rbis) {
		this.rbis = rbis;
	}
	
	public void incrementRbis() {
		this.rbis++;
	}

	public int getStolenBases() {
		return stolenBases;
	}

	public void setStolenBases(int stolenBases) {
		this.stolenBases = stolenBases;
	}
	
	public void incrementStolenBases() {
		this.stolenBases++;
	}

	public int getPlateAppearances() {
		return plateAppearances;
	}

	public void setPlateAppearances(int plateAppearances) {
		this.plateAppearances = plateAppearances;
	}
	
	public void incrementPlateAppearances() {
		this.plateAppearances++;
	}
	
	public int getCaughtStealing() {
		return caughtStealing;
	}

	public void setCaughtStealing(int caughtStealing) {
		this.caughtStealing = caughtStealing;
	}
	
	public void incrementCaughtStealing() {
		this.caughtStealing++;
	}

	public int getSpeedRating() {
		int sr = 0;
		if (stolenBases > 20) { 
			sr = 5;
		}
		else if (stolenBases > 10 && stolenBases <= 20) { // 11-20
			sr = 4;
		}
		else if (stolenBases > 6 && stolenBases <= 10) {  // 7-10
			sr = 3;
		}
		else if (stolenBases > 2 && stolenBases <= 6) {  // 3-6
			sr = 2;
		}
		else if (stolenBases > 0) { // 1-2
			sr = 1;
		}
		return sr;
	}
	
	public double getBattingAverage() {
		return atBats > 0 ? (double)hits/atBats : 0.0;
	}
	
	public double getOnBasePercentage() {
		return plateAppearances > 0 ? (double)(hits + walks + hitByPitch)/plateAppearances : 0.0;
	}
	
	public double getSluggingPercentage() {
		return atBats > 0 ? (double)(getSingles() + (doubles*2) + (triples*3)+ (homeRuns*4))/atBats: 0.0;
	}
	
	public double getStrikeoutRate () {
		int outs = plateAppearances - (hits + walks + hitByPitch);
		return outs > 0 ? (double)strikeOuts/outs : 0.0;
	}
	
	public double getWalkRate () {
		return (walks + hits + hitByPitch) > 0 ? (double)walks/(walks + hits + hitByPitch) : 0.0;
	}
	
	public double getHomeRunRate () {
		return homeRuns > 0 ? (double)homeRuns/hits : 0.0;
	}
}
