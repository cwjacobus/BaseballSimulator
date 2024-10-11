package baseball;

import java.util.HashMap;
import java.util.Map;

public class BaseballConstants {
	
	static final int OUTS_PER_INNING = 3;
	static final int INNINGS_PER_GAME = 9;
	static final int NUM_OF_PLAYERS_IN_LINEUP = 9;
	static final int MAX_WS_SERIES = 7;
	
	// MLBDivisions 2013-2022
	static final int AL_EAST = 0;
	static final int AL_CENTRAL = 1;
	static final int AL_WEST = 2;
	static final int NL_EAST = 3;
	static final int NL_CENTRAL = 4;
	static final int NL_WEST = 5;
	static Map<Integer, String> mlbDivisionNames  = new HashMap<Integer, String>() {
		private static final long serialVersionUID = 1L;
	{
		put(AL_EAST, "AL East");
		put(AL_CENTRAL, "AL Central");
		put(AL_WEST, "AL West");
		put(NL_EAST, "NL East");
		put(NL_CENTRAL, "NL Central");
		put(NL_WEST, "NL West");
	}};
	static Integer[][] mlbDivisionTeams = {{110, 111, 147, 139, 141}, {145, 114, 116, 142, 118}, {117, 108, 133, 136, 140},
		{144, 146, 121, 143, 120}, {112, 113, 158, 134, 138}, {115, 119, 135, 137, 109}};
	static final int STRUCK_OUT = 0;
	static final int GROUNDED_OUT = 1;
	static final int FLEW_OUT = 2;
	static final int FLEW_OUT_DEEP = 3;
	static final int LINED_OUT = 4;
	static final int POPPED_OUT = 5;
	static Map<Integer, String> positions  = new HashMap<Integer, String>() {
		private static final long serialVersionUID = 1L;
	{
		put(1, "P");
		put(2, "C");
		put(3, "1B");
		put(4, "2B");
		put(5, "3B");
		put(6, "SS");
		put(7, "LF");
		put(8, "CF");
		put(9, "RF");
		put(10, "DH");
	}};
	static Map<Integer, String> outTypes  = new HashMap<Integer, String>() {
		private static final long serialVersionUID = 1L;
	{
		put(STRUCK_OUT, "STRUCK OUT");
		put(GROUNDED_OUT, "GROUNDED OUT");
		put(FLEW_OUT, "FLEW OUT");
		put(FLEW_OUT_DEEP, "FLEW OUT DEEP");
		put(LINED_OUT, "LINED OUT");
		put(POPPED_OUT, "POPPED OUT");
	}};
	// List of 2 way players since 1900
	// Constant list as opposed to pulled from DB since you need to know before their
	// stats are imported.  Will have to be maintained going forward.
	static Integer[] twoWayPlayers = {111886, 117619, 114666, 119191, 115867, 114211, 113094, 121578, 124535, 124173, 111434, 121564, 122453, 110879, 119941, 123898, 112645, 113165, 118102, 
		119260, 116511, 117780, 115350, 117044, 150449, 122280, 543238, 660271};
}
