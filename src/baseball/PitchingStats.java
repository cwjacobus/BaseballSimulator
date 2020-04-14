package baseball;

public class PitchingStats {
	private double inningsPitched;
	private int walks;
	private int strikeouts;
	private int runsAllowed;
	private int earnedRunsAllowed;
	private int homeRunsAllowed;
	private int stolenBasesAllowed;
	private int hitBatters;
	private int hitsAllowed;
	private int holds;
	private int saves;
	private int gamesStarted;
	private int balks;
	private int wildPitches;
	
	public PitchingStats() {
	}
	
	public PitchingStats(double inningsPitched, int earnedRunsAllowed, int runsAllowed, int walks, int strikeouts, int homeRunsAllowed,
			int stolenBasesAllowed, int hitBatters, int hitsAllowed, int holds, int saves, int gamesStarted, int balks,
			int wildPitches) {
		this.inningsPitched = inningsPitched;
		this.earnedRunsAllowed = earnedRunsAllowed;
		this.runsAllowed = runsAllowed;
		this.walks = walks;
		this.strikeouts = strikeouts;
		this.homeRunsAllowed = homeRunsAllowed;
		this.stolenBasesAllowed = stolenBasesAllowed;
		this.hitBatters = hitBatters;
		this.hitsAllowed = hitsAllowed;
		this.holds = holds;
		this.saves = saves;
		this.gamesStarted = gamesStarted;
		this.balks = balks;
		this.wildPitches = wildPitches;
	}

	public double getInningsPitched() {
		return inningsPitched;
	}

	public void setInningsPitched(double inningsPitched) {
		this.inningsPitched = inningsPitched;
	}
	
	public void incrementInningsPitched(int incrementBy) {
		for (int i = 1; i <= incrementBy; i++) {
			String iPString = Double.toString(inningsPitched);
			if (iPString != null && iPString.length() > 0 && iPString.contains(".")) {
				String ipStringArray[] = iPString.split("\\.");
				if (ipStringArray[1].equals("0") || ipStringArray[1].equals("1")) {
					inningsPitched = Double.parseDouble(ipStringArray[0] + "." + (Integer.parseInt(ipStringArray[1]) + 1));
				}
				else {
					inningsPitched = Double.parseDouble((Integer.parseInt(ipStringArray[0]) + 1) + ".0");
				}
			}
		}
	}

	public int getRunsAllowed() {
		return runsAllowed;
	}

	public void setRunsAllowed(int runsAllowed) {
		this.runsAllowed = runsAllowed;
	}
	
	public void incrementRunsAllowed() {
		this.runsAllowed++;
	}

	public int getEarnedRunsAllowed() {
		return earnedRunsAllowed;
	}

	public void setEarnedRunsAllowed(int earnedRunsAllowed) {
		this.earnedRunsAllowed = earnedRunsAllowed;
	}
	public void incrementEarnedRunsAllowed() {
		this.earnedRunsAllowed++;
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

	public int getStrikeouts() {
		return strikeouts;
	}

	public void setStrikeouts(int strikeouts) {
		this.strikeouts = strikeouts;
	}
	
	public void incrementStrikeouts() {
		this.strikeouts++;
	}

	public int getHomeRunsAllowed() {
		return homeRunsAllowed;
	}

	public void setHomeRunsAllowed(int homeRunsAllowed) {
		this.homeRunsAllowed = homeRunsAllowed;
	}
	
	public void incrementHomeRunsAllowed() {
		this.homeRunsAllowed++;
	}

	public int getStolenBasesAllowed() {
		return stolenBasesAllowed;
	}

	public void setStolenBasesAllowed(int stolenBasesAllowed) {
		this.stolenBasesAllowed = stolenBasesAllowed;
	}

	public int getHitBatters() {
		return hitBatters;
	}

	public void setHitBatters(int hitBatters) {
		this.hitBatters = hitBatters;
	}

	public int getHitsAllowed() {
		return hitsAllowed;
	}

	public void setHitsAllowed(int hitsAllowed) {
		this.hitsAllowed = hitsAllowed;
	}
	
	public void incrementHitsAllowed() {
		this.hitsAllowed++;
	}

	public int getHolds() {
		return holds;
	}

	public void setHolds(int holds) {
		this.holds = holds;
	}

	public int getSaves() {
		return saves;
	}

	public void setSaves(int saves) {
		this.saves = saves;
	}

	public int getGamesStarted() {
		return gamesStarted;
	}

	public void setGamesStarted(int gamesStarted) {
		this.gamesStarted = gamesStarted;
	}

	public int getBalks() {
		return balks;
	}

	public void setBalks(int balks) {
		this.balks = balks;
	}

	public int getWildPitches() {
		return wildPitches;
	}

	public void setWildPitches(int wildPitches) {
		this.wildPitches = wildPitches;
	}
	
	public int getOuts() {
		int outs = 0;
		String iPString = Double.toString(inningsPitched);
		if (iPString != null && iPString.length() > 0 && iPString.contains(".")) {
			outs = (Integer.parseInt(iPString.split("\\.")[0]) * 3) + Integer.parseInt(iPString.split("\\.")[1]);
		}
		return outs;
	}
	
	public double getStrikeoutRate () {
		double soRate = 0.0;
		int outs = getOuts();
		if (outs > 0) {
			soRate = (double)strikeouts/getOuts();
		}
		return soRate;
	}
	
	public double getWalkRate () {
		double bbRate = 0.0;
		if ((walks + hitsAllowed + hitBatters) > 0) {
			bbRate = (double)walks/(walks + hitsAllowed + hitBatters);
		}
		return bbRate;
	}
	
	/*
	 {"sport_pitching_tm":{"copyRight":" Copyright 2020 MLB Advanced Media, L.P.  Use of any content on this page acknowledges agreement to the terms posted here http://gdx.mlb.com/components/copyright.txt  ",
	 "queryResults":{"created":"2020-04-09T18:51:43","totalSize":"1",
	 "row":{"gidp":"9","h9":"9.39","sac":"2","np":"1729","tr":"0","gf":"0","sport_code":"mlb","bqs":"1","hgnd":"34","sho":"0","bq":"9","gidp_opp":"52","bk":"0",
	 "kbb":"2.74","sport_id":"1","hr9":"2.26","sv":"0","slg":".512","bb":"39","whip":"1.41","avg":".265","ops":".843","team_full":"New York Yankees","db":"23",
	 "league_full":"American League","team_abbrev":"NYY","hfly":"33","so":"107","tbf":"468","bb9":"3.27","league_id":"103","wp":"0","sf":"1","team_seq":"1","hpop":"0",
	 "league":"AL","hb":"3","cs":"2","pgs":"78.6","season":"2019","sb":"9","go_ao":"0.89","ppa":"3.69","cg":"0","player_id":"282332","gs":"22","ibb":"0","team_id":"147",
	 "pk":"0","go":"97","hr":"27","irs":"0","wpct":".385","era":"4.95","babip":".294","end_date":"2019-10-30T00:00:00","rs9":"5.11","qs":"6","league_short":"American",
	 "g":"23","ir":"0","hld":"1","k9":"8.97","sport":"MLB","team_short":"NY Yankees","l":"8","svo":"0","h":"112","ip":"107.1","obp":".331","w":"5","hldr":"45","ao":"109",
	 "s":"1105","r":"64","spct":"63.9","pip":"16.1","ab":"422","er":"59"}}}}
	 */
}
