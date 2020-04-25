package baseball;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import db.MLBPlayer;

public class GameState {
	int outs = 0;
	Integer inning = 1;
	int top = 0;
	BaseRunner[] baseRunners = new BaseRunner[3];
	int[] battingOrder = {1, 1};
	MLBPlayer[] currentPitchers = {null, null};
	boolean intentionalWalk = false;
	boolean hitAndRun = false;
	boolean buntAttempt = false;
	int virtualErrorOuts = 0; // Used to determine earned runs
	ArrayList<Integer> baseRunnersReachedByError = new ArrayList<Integer>(); // Used to determine earned runs
	
	public static final int BASES_EMPTY = 0;
	public static final int MAN_ON_FIRST = 1;
	public static final int MAN_ON_SECOND = 2;
	public static final int MAN_ON_FIRST_AND_SECOND = 3;
	public static final int MAN_ON_THIRD = 4;
	public static final int MAN_ON_FIRST_AND_THIRD = 5;
	public static final int MAN_ON_SECOND_AND_THIRD = 6;
	public static final int BASES_LOADED = 7;
	
	public Map<Integer, String> baseSituations  = new HashMap<Integer, String>() {
		private static final long serialVersionUID = 1L;
	{
	    put(BASES_EMPTY, "BASES EMPTY");
	    put(MAN_ON_FIRST, "MAN ON FIRST");
	    put(MAN_ON_SECOND, "MAN ON SECOND");
	    put(MAN_ON_FIRST_AND_SECOND, "MAN ON FIRST AND SECOND");
	    put(MAN_ON_THIRD, "MAN ON THIRD");
	    put(MAN_ON_FIRST_AND_THIRD, "MAN ON FIRST AND THIRD");
	    put(MAN_ON_SECOND_AND_THIRD, "MAN ON SECOND AND THIRD");
	    put(BASES_LOADED, "BASES LOADED");
	}};

	public GameState() {
	}

	public int getOuts() {
		return outs;
	}

	public void setOuts(int outs) {
		this.outs = outs;
	}
	
	public void incrementOuts() {
		this.outs++;
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

	public BaseRunner[] getBaseRunners() {
		return baseRunners;
	}
	
	public BaseRunner getBaseRunner(int base) {
		return baseRunners[base - 1];
	}
	
	public Integer getBaseRunnerId(int base) {
		return baseRunners[base - 1].runnerId;
	}
	
	public Integer getBaseRunnerPitcherResponsibleId(int base) {
		return baseRunners[base - 1].responsiblePitcherId;
	}

	public void setBaseRunners(BaseRunner[] baseRunners) {
		this.baseRunners = baseRunners;
	}
	
	public void setBaseRunner(int base, BaseRunner baseRunner) {
		this.baseRunners[base - 1] = baseRunner;
	}
	
	public int[] getBattingOrder() {
		return battingOrder;
	}

	public void setBattingOrder(int[] battingOrder) {
		this.battingOrder = battingOrder;
	}
	
	public void incrementBattingOrder(int top) {
		this.battingOrder[top] = battingOrder[top] == 9 ? 1 : battingOrder[top] + 1;
	}

	public MLBPlayer[] getCurrentPitchers() {
		return currentPitchers;
	}

	public void setCurrentPitchers(MLBPlayer[] currentPitchers) {
		this.currentPitchers = currentPitchers;
	}

	public void setCurrentPitcher(MLBPlayer currentPitcher, int top) {
		this.currentPitchers[top] = currentPitcher;
	}

	public boolean isIntentionalWalk() {
		return intentionalWalk;
	}

	public void setIntentionalWalk(boolean intentionalWalk) {
		this.intentionalWalk = intentionalWalk;
	}

	public boolean isHitAndRun() {
		return hitAndRun;
	}

	public void setHitAndRun(boolean hitAndRun) {
		this.hitAndRun = hitAndRun;
	}

	public boolean isBuntAttempt() {
		return buntAttempt;
	}

	public void setBuntAttempt(boolean buntAttempt) {
		this.buntAttempt = buntAttempt;
	}
	
	public int getVirtualErrorOuts() {
		return virtualErrorOuts;
	}

	public void setVirtualErrorOuts(int virtualErrorOuts) {
		this.virtualErrorOuts = virtualErrorOuts;
	}
	
	public void incrementVirtualErrorOuts() {
		this.virtualErrorOuts++;
	}

	public ArrayList<Integer> getBaseRunnersReachedByError() {
		return baseRunnersReachedByError;
	}

	public void setBaseRunnersReachedByError(ArrayList<Integer> baseRunnersReachedByError) {
		this.baseRunnersReachedByError = baseRunnersReachedByError;
	}

	public int getCurrentBasesSituation() {
		int baseSituation = BASES_EMPTY;
		if (getBaseRunnerId(1) != 0 && getBaseRunnerId(2) == 0 && getBaseRunnerId(3) == 0) {
			baseSituation = MAN_ON_FIRST;
		}
		else if (getBaseRunnerId(1) == 0 && getBaseRunnerId(2) != 0 && getBaseRunnerId(3) == 0) {
			baseSituation = MAN_ON_SECOND;
		}
		else if (getBaseRunnerId(1) == 0 && getBaseRunnerId(2) == 0 && getBaseRunnerId(3) != 0) {
			baseSituation = MAN_ON_THIRD;
		}
		else if (getBaseRunnerId(1) != 0 && getBaseRunnerId(2) != 0 && getBaseRunnerId(3) == 0) {
			baseSituation = MAN_ON_FIRST_AND_SECOND;
		}
		else if (getBaseRunnerId(1) == 0 && getBaseRunnerId(2) != 0 && getBaseRunnerId(3) != 0) {
			baseSituation = MAN_ON_SECOND_AND_THIRD;
		}
		else if (getBaseRunnerId(1) != 0 && getBaseRunnerId(2) == 0 && getBaseRunnerId(3) != 0) {
			baseSituation = MAN_ON_FIRST_AND_THIRD;
		}
		else if (getBaseRunnerId(1) != 0 && getBaseRunnerId(2) != 0 && getBaseRunnerId(3) != 0) {
			baseSituation = BASES_LOADED;
		}
		return baseSituation;
	}

	public Map<Integer, String> getBaseSituations() {
		return baseSituations;
	}

	public void setBaseSituations(Map<Integer, String> baseSituations) {
		this.baseSituations = baseSituations;
	}

	public boolean isBaseOccupied(int base) {
		return getBaseRunnerId(base) == 0 ? false : true;
	}
}
