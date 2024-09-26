package baseball;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import dao.DAO;
import db.MLBBattingStats;
import db.MLBFieldingStats;
import db.MLBPitchingStats;
import db.MLBPlayer;
import db.MLBTeam;
import db.MLBWorldSeries;

import static baseball.BaseballConstants.*;

public class BaseballSimulator {
	
	static boolean simulationMode = true;
	static boolean autoBeforeMode = false;
	static boolean gameMode = false;
	static boolean seasonSimulationMode = false;
	static boolean seasonSimulationPlayoffsMode = false;
	static boolean tournamentMode = false;
	static boolean allStarGameMode = false;
	static boolean worldSeriesMode = false;
	static int autoBeforeInning = 1000;
	static GameState gameState = new GameState();
	static BoxScore[] boxScores = new BoxScore[2];
	static BoxScore[][] seriesBoxScores;
	static Roster[] rosters;
	static MLBPlayer[] setupMen = new MLBPlayer[2];
	static MLBPlayer[] closers = new MLBPlayer[2];
	static DecimalFormat df = new DecimalFormat(".000");
	static DecimalFormat eraDf = new DecimalFormat("0.00");
	static List<String> randoLog = new ArrayList<String>();
	static ArrayList<MLBTeam> allMlbTeamsList;
	static boolean useDH = true;
	static HashMap<Integer, MLBPlayer> seasonPitchingStats = new HashMap<>();
	static HashMap<Integer, MLBPlayer> seasonBattingStats = new HashMap<>();
	static List<HashMap<Integer, Integer>> battersOnMultTeams = new ArrayList<>();
	static List<HashMap<Integer, Integer>> pitchersOnMultTeams = new ArrayList<>();
	static HashMap<String, Integer> disabledList = new HashMap<>();
	static String visTeamImportFile = null;
	static String homeTeamImportFile = null;
	static String importDir = "C:\\Users\\cjaco\\Documents\\Sports\\BBSim\\";

	public static void main(String[] args) {
		// Argument examples
		// TOURNAMENT All_Time_Tournament.txt
		// SEASON 2023
		// WORLDSERIES 1970 1979 outputFile.txt
		// 1978 NYY 1996 NYY SIM 7
		// 1978 NYY 2022 HOU SIM 7 V 1978NYY.txt H 2022HOU.txt
		// 1978 AAS 1978 NAS SIM 1 V 1978_AL_Allstars.txt H 1978_NL_Allstars.txt
		// 1978 NYY 1996 NYY AUTO 8
		// 1978 NYY 2022 HOU GAME
		// 1978 NYY 2022 HOU GAME V 1978NYY.txt H 2022HOU.txt
		
		// TBD Make processing args a method
		
		int seriesLength = 1;
		int seasonSimYear = 0;
		boolean bestOfSeries = false;
		if (args == null || args.length < 2 ||
		   (args.length == 2 && !(args[0].equalsIgnoreCase("SEASON") || args[0].equalsIgnoreCase("TOURNAMENT"))) || 
		   (args.length == 3 && !(args[0].equalsIgnoreCase("TOURNAMENT") || args[0].equalsIgnoreCase("WORLDSERIES"))) ||
		   (args.length == 4 && !args[0].equalsIgnoreCase("WORLDSERIES")) ||
		   (args.length == 5 && !args[4].equalsIgnoreCase("GAME")) ||
		   (args.length == 6 && !(args[4].equalsIgnoreCase("SIM") || args[4].equalsIgnoreCase("AUTO")))) {
				System.out.println("Invalid args - expecting <visYear> <vis> <homeYear> <homeYear> <MODE>[SIM|GAME|AUTO] <AUTO_AFTER>|<SERIES_LENGTH> - ex. 2019 HOU 2019 NYY SIM 7 or 2019 HOU 2019 NYY AUTO 9");
				return;
		}
		DAO.setConnection();
		rosters = new Roster[2];
		allMlbTeamsList = DAO.getAllTeamsList();
		int years[] = {0, 0};
		MLBTeam[] teams = {null, null};
		LinkedHashMap<Integer, HashMap<Integer, MLBPlayer>> allBatters = null;
		LinkedHashMap<Integer, HashMap<Integer, MLBPlayer>> allPitchers = null;
		HashMap<Integer, HashMap<Integer, ArrayList<MLBFieldingStats>>> allFielders = null;
		SeriesStats[] seriesStats = new SeriesStats[2];
		int gameNumber = 0;
		HashMap<Integer, Integer> teamSeasonGameIndexMap = new HashMap<>();
		Map<Integer, ArrayList<Integer>> seasonSched  = new HashMap<>();
		Map<Integer, Map<Integer, TeamSeasonResults>> seasonResults  = new HashMap<>();
		rosters[0] = new Roster();
		rosters[1] = new Roster();
		// Start process args
		if (args.length > 4 && args[4] != null) {  // GAME SIM or AUTO
			if (args[4].equalsIgnoreCase("GAME")) {
				autoBeforeInning = 0;
				simulationMode = false;
				autoBeforeMode = false;
				gameMode = true;
			}
			if (args[4].equalsIgnoreCase("SIM")) {
				autoBeforeInning = 1000;
				simulationMode = true;
				autoBeforeMode = false;
				gameMode = false;
				if (args.length > 5 && args[5] != null) {
					if (args[5].charAt(0) == 'B') {
						bestOfSeries = true;
						seriesLength = Integer.parseInt(args[5].substring(1, args[5].length()));
						if (seriesLength != 3 && seriesLength != 5 && seriesLength != 7) {
							System.out.println("Best of series can only be 3, 5 or 7 games");
							return;
						}
					}
					else {
						seriesLength = Integer.parseInt(args[5]);
					}
				}
			}
			else if (args[4].equalsIgnoreCase("AUTO")) {
				simulationMode = false;
				autoBeforeMode = true;
				gameMode = false;
				if (args.length > 5 && args[5] != null) {
					try {
						autoBeforeInning = Integer.parseInt(args[5]);
					}
					catch (Exception e) {
						autoBeforeInning = 1000;
					}
				}
			}
			// Set allStarGameMode
			if (args[1].equalsIgnoreCase("AAS") || args[1].equalsIgnoreCase("NAS") ||
				args[3].equalsIgnoreCase("AAS") || args[3].equalsIgnoreCase("NAS")) {
					allStarGameMode = true;
					if (args.length < 9) {
						System.out.println("Rosters must be imported for an all star game.");
						return;
					}
					if (seriesLength != 1) {
						// Limit AS games to a single game as there can be ties until code is changed to handle series with ties
						System.out.println("All star game can not be a multiple game series.");
						return;
					}
			}
			// Get import file names from arguments
			int fileNameArgOrder1 = args[4].equalsIgnoreCase("SIM") ? 7 : 6;  // arg order diff for SIM v GAME/AUTO
			int fileNameArgOrder2 = args[4].equalsIgnoreCase("SIM") ? 9 : 8;
			if (args.length > fileNameArgOrder1 - 1 && args[fileNameArgOrder1 - 1] != null) {
				if (args[fileNameArgOrder1 - 1].equalsIgnoreCase("V") && args.length > fileNameArgOrder1 && args[fileNameArgOrder1] != null) {
					visTeamImportFile = args[fileNameArgOrder1];
				}
				else if (args[fileNameArgOrder1 - 1].equalsIgnoreCase("H") && args.length > fileNameArgOrder1 && args[fileNameArgOrder1] != null) {
					homeTeamImportFile = args[fileNameArgOrder1];
				}
			}
			if (args.length > fileNameArgOrder2 - 1 && args[fileNameArgOrder2 - 1] != null) {
				if (args[fileNameArgOrder2 - 1].equalsIgnoreCase("V") && args.length > fileNameArgOrder2 && args[fileNameArgOrder2] != null) {
					visTeamImportFile = args[fileNameArgOrder2];
				}
				else if (args[fileNameArgOrder2 - 1].equalsIgnoreCase("H") && args.length > fileNameArgOrder2 && args[fileNameArgOrder2] != null) {
					homeTeamImportFile =  args[fileNameArgOrder2];
				}
			}
			
			String[] teamNames = {args[1].toUpperCase(), args[3].toUpperCase()};
			years[0] = Integer.parseInt(args[0]);
			years[1] = Integer.parseInt(args[2]);
			for (int t = 0; t < 2; t++) {
				int index = t == 0 ? 1 : 3;
				if (args[index].equalsIgnoreCase("AAS") || args[index].equalsIgnoreCase("NAS")) {
					String aSFullTeamName = "League All Stars";
					String aSShortTeamName = args[index];
					String aSLeague;
					if (args[index].equalsIgnoreCase("AAS")) {
						aSFullTeamName = "American " + aSFullTeamName;
						aSLeague = "AL";
					}
					else {
						aSFullTeamName = "National " + aSFullTeamName;
						aSLeague = "NL";
					}
					teams[t] = new MLBTeam(0, null, aSFullTeamName, aSShortTeamName, aSLeague, null, null);
				}
				else {
					teams[t] = getTeamByYearAndShortName(years[t], teamNames[t], allMlbTeamsList);
				}
				if (teams[t] == null) {
					System.out.println("Invalid team: " + teamNames[t] + " " + years[t]);
					System.out.println("\nValid teams: ");
					for (MLBTeam team : allMlbTeamsList) {
						int lastYear = (team.getLastYearPlayed() == null || team.getLastYearPlayed() == 0) ? Calendar.getInstance().get(Calendar.YEAR) : team.getLastYearPlayed();
						System.out.println(team.getFullTeamName() + "(" + team.getShortTeamName() + ") " + team.getFirstYearPlayed() + "-" + lastYear);
					}
					return;
				}
			}
			ArrayList<Integer> teamSched = new ArrayList<>();
			teamSched.add(new Integer(teams[0].getTeamId()));
			seasonSched.put(teams[1].getTeamId(), teamSched); // Schedule with one game
		} // GAME SIM or AUTO
		else if (args[0].equalsIgnoreCase("SEASON")) { // Season sim mode
			simulationMode = true;
			seasonSimulationMode = true;
			autoBeforeMode = false;
			gameMode = false;
			if (args.length > 1 && args[1] != null) {
				seasonSimYear = Integer.parseInt(args[1]);
			}
			if (seasonSimYear < 2013) {
				System.out.println("Season simulations only years 2013-2023!"); // 2013 HOU moved to AL West to make 6 Div of 5 teams
				return;
			}
			createSchedule(seasonSimYear, seasonSched);
			years[0] = years[1] = Integer.parseInt(args[1]);
			allBatters = DAO.getBattersMapByYear(years[0]);
			allPitchers = DAO.getPitchersMapByYear(years[0]);
			allFielders = DAO.getFieldingStatsMapByYear(years[0]);
		}
		else if (args[0].equalsIgnoreCase("TOURNAMENT")) {
			int tournamentIterations = 1;
			if (args.length > 2 && args[2] != null) {
				try {
					tournamentIterations = Integer.parseInt(args[2]);
				}
				catch (Exception e) {
					System.out.println("Invalid number of tournaments!");
					return;
				}
			}
			tournamentMode = true;
	        List<TournamentTeam> seededTournamentTeams = importTournamentTeamsFromFile(args[1]);
	        
	        if (seededTournamentTeams != null) {
	        	List<TournamentTeam> alSeededTournamentTeams = new ArrayList<TournamentTeam>();
	        	List<TournamentTeam> nlSeededTournamentTeams = new ArrayList<TournamentTeam>();
	        	seededTournamentTeams.stream().filter(entry -> entry.getMlbTeam().getLeague().equals("AL")).forEach(entry -> alSeededTournamentTeams.add(entry));
	        	seededTournamentTeams.stream().filter(entry -> entry.getMlbTeam().getLeague().equals("NL")).forEach(entry -> nlSeededTournamentTeams.add(entry));
	        	if (alSeededTournamentTeams.size() != 6 || nlSeededTournamentTeams.size() != 6) {
		        	System.out.println("Only support tournaments with 6 AL and 6 NL teams!"); // 12 (6 AL/6 NL) is size of current number of playoff teams
		        	return;
		        }
	        	String tournamentWinner;
	        	Map<String, Integer> tournamentWinnersMap = new HashMap<>();
	        	for (int t = 0; t < tournamentIterations; t++) {
	        		System.out.println("\nTournament # " + (t + 1));
	        		seriesBoxScores = new BoxScore[seriesLength][2];
	        		tournamentWinner = playTournamentGames(0, alSeededTournamentTeams, nlSeededTournamentTeams, false, (tournamentIterations > 1));
	        		if (tournamentWinnersMap.get(tournamentWinner) == null) {
	        			tournamentWinnersMap.put(tournamentWinner, 1);
	        		}
	        		else {
	        			tournamentWinnersMap.put(tournamentWinner, tournamentWinnersMap.get(tournamentWinner) + 1);
	        		}
	        	}
	        	Map<String, Integer> sortedTournamentWinnersMap = tournamentWinnersMap
	        		.entrySet()
	        		.stream()
	        		.sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
	        		.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue(), (e1, e2) -> e2, LinkedHashMap::new));
	        	System.out.println();
	        	if (tournamentIterations > 1) {
	        		System.out.println("Tournament Winners");
	        		for (Map.Entry<String, Integer> winner : sortedTournamentWinnersMap.entrySet()) {
	        			System.out.println(winner.getKey() + " " + winner.getValue());
	        		}
	        	}
			}
	        return;
		}
		else if (args[0].equalsIgnoreCase("WORLDSERIES")) {
			worldSeriesMode = true;
			Integer startYear;
			Integer endYear;
			FileOutputStream fileOutputStream = null;
			BufferedOutputStream bufferedOutputStream = null;
			PrintStream printStream = null;
			if (args.length > 3) {
				try {
					fileOutputStream = new FileOutputStream(importDir + args[3]);
					bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
					printStream = new PrintStream(bufferedOutputStream);
					System.setOut(printStream);
				}
				catch (FileNotFoundException e) {
					System.out.println("Invalid export location!");
					return;
				}
			}
			try {
				startYear = Integer.parseInt(args[1]);
				endYear = Integer.parseInt(args[2]);
			}
			catch (Exception e) {
				System.out.println("Invalid year!");
				return;
			}
			List<MLBWorldSeries> worldSeries = DAO.getMLBWorldSeriesList(startYear, endYear);
			playWorldSeriesGames(worldSeries);
			if (fileOutputStream != null && bufferedOutputStream != null && printStream != null) {
				try {
					fileOutputStream.flush();
					fileOutputStream.close();
					bufferedOutputStream.close();
					printStream.close();
				}
				catch (IOException e) {
					System.out.println("IOException: " + e.getMessage());
				}
			}
			return;
			
		} // End parameter processing
		for (int t = 0; t < 2; t++) {
			battersOnMultTeams.add(DAO.getBattersOnMultipleTeamsByPrimaryTeam(years[t])); 
			pitchersOnMultTeams.add(DAO.getPitchersOnMultipleTeamsByPrimaryTeam(years[t]));
		}
		seriesBoxScores = new BoxScore[seriesLength][2];
		for (Map.Entry<Integer, ArrayList<Integer>> team : seasonSched.entrySet()) { // home teams
			for (Integer opp : team.getValue()) { // away teams
				if (seasonSimulationMode) {
					teams[0] = getTeamByYearAndTeamId(years[0], opp, allMlbTeamsList);
					teams[1] = getTeamByYearAndTeamId(years[1], team.getKey(), allMlbTeamsList);
					// Get teams division
					int[] teamDivisions = {getDivisionByTeamId(mlbDivisionTeams, teams[0].getTeamId()), getDivisionByTeamId(mlbDivisionTeams, teams[1].getTeamId())};
					TeamSeasonResults teamResults;
					Map<Integer, TeamSeasonResults> divisionResults;
					for (int t = 0; t < 2; t++) {
						if (seasonResults.get(teamDivisions[t]) == null) {
							divisionResults = new HashMap<>();
							teamResults = new TeamSeasonResults();
							teamResults.setTeamId(teams[t].getTeamId());
							divisionResults.put(teams[t].getTeamId(), teamResults);
							seasonResults.put(teamDivisions[t], divisionResults);
						}
						else if (seasonResults.get(teamDivisions[t]).get(teams[t].getTeamId()) == null) {
							divisionResults = seasonResults.get(teamDivisions[t]);
							teamResults = new TeamSeasonResults();
							teamResults.setTeamId(teams[t].getTeamId());
							divisionResults.put(teams[t].getTeamId(), teamResults);
						}
					}
					gameNumber++;
				}
				
				if (!setUpDataAndPlayGames(teams, years, seriesLength, bestOfSeries, seriesStats, allFielders, allBatters, allPitchers, 
						(seasonSimulationMode ? teamSeasonGameIndexMap : null), gameNumber, false, false)) {
							return; // Invalid lineup import
				}
				
				if (seasonSimulationMode) {
					Integer visTeamSeasonGameIndex = teamSeasonGameIndexMap.get(teams[0].getTeamId());
					Integer homeTeamSeasonGameIndex = teamSeasonGameIndexMap.get(teams[1].getTeamId());
					teamSeasonGameIndexMap.put(teams[0].getTeamId(), visTeamSeasonGameIndex != null ? visTeamSeasonGameIndex + 1 : 2);
					teamSeasonGameIndexMap.put(teams[1].getTeamId(), homeTeamSeasonGameIndex != null ? homeTeamSeasonGameIndex + 1 : 2);
					updateSeasonResults(seasonResults, boxScores);
				}
			} //away teams
		} // home teams
		if (seriesLength > 1) { // Series results and calculations
			outputSeriesResults(seriesLength, seriesStats, bestOfSeries);
		}
		else if (seasonSimulationMode) {
			outputSeasonResults(seasonResults, seasonSimYear);
			postSeason(seasonResults, seasonSimYear);
		}
	}
	
	private static void closeFileReader(BufferedReader reader) {
		try {
			if (reader != null) {
				reader.close();
			}
		}
		catch (IOException e) {	
		}
	}
	
	private static void createSchedule(Integer schedYear, Map<Integer, ArrayList<Integer>> seasonSched) {
		int division = 0;
		for (MLBTeam team : allMlbTeamsList) { // 8 4 3 2 
			ArrayList<Integer> teamSched = new ArrayList<>();
			if (team.getLastYearPlayed() != null && team.getLastYearPlayed() != 0) {  //active team
				continue;
			}
			for (int i = 0; i < 6; i++) {
				if (Arrays.asList(mlbDivisionTeams[i]).contains(team.getTeamId())) {
					division = i;
					for (int j = 0; j < mlbDivisionTeams[i].length; j++) {
						if (mlbDivisionTeams[i][j] != team.getTeamId()) {
							for (int k = 0; k < 8; k++) {
								teamSched.add(mlbDivisionTeams[i][j]);  // 8 x per div opponent
							}
						}
					}
				}
			}
			int leagueOpponentCount = 1;
			for (MLBTeam team2 : allMlbTeamsList) {
				if (team2.getLastYearPlayed() != null && team2.getLastYearPlayed() != 0) {  //active team
					continue;
				}
				if (leagueOpponentCount == 16) {
					leagueOpponentCount = 0;
				}
				if (team2.getLeague().equals(team.getLeague()) && !Arrays.asList(mlbDivisionTeams[division]).contains(team2.getTeamId())) {
					/*int numberOfLeagueOpponentGames = leagueOpponentCount < 5 ? 4 : 3;
					for (int j = 0; j < numberOfLeagueOpponentGames; j++) {
						teamSched.add(team2.getTeamId());  // 4 x per league opponent for 4 teams and 3 x league opponent for other 6 teams
					}
					leagueOpponentCount++;*/
					for (int j = 0; j < 4; j++) {
						teamSched.add(team2.getTeamId());  // 4 x per league opponent
					}
				}
			}
			Integer[] interleagueOpponents = team.getLeague().equals("AL") ? mlbDivisionTeams[division+3] : mlbDivisionTeams[division-3];
			for (int i = 0; i < interleagueOpponents.length; i++) {
				for (int j = 0; j < 2; j++) {
					teamSched.add(interleagueOpponents[i]);  // 2 x per interleague opponent
				}
			}
			seasonSched.put(team.getTeamId(), teamSched);
		}
		/*
		for (Map.Entry<Integer, ArrayList<Integer>> team : seasonSched.entrySet()) { 
			Integer teamId = team.getKey();
			ArrayList<Integer> teamSched = team.getValue();
			System.out.print("Sched for " + teamId + ": ");
			for (int i = 0; i < teamSched.size(); i++) {
				System.out.print(teamSched.get(i) + " ");
			}
			System.out.println();
        }*/ 
	}
	
	private static boolean setUpDataAndPlayGames(MLBTeam[] teams, int[] years, int seriesLength, boolean bestOfSeries, SeriesStats[] seriesStats, HashMap<Integer, 
		HashMap<Integer, ArrayList<MLBFieldingStats>>> allFielders, LinkedHashMap<Integer, HashMap<Integer, MLBPlayer>> allBatters, 
		LinkedHashMap<Integer, HashMap<Integer, MLBPlayer>> allPitchers, HashMap<Integer, Integer> teamSeasonGameIndexMap, int gameNumber, boolean postSeason,
		boolean multiIterationTournament) {
			
		for (int t = 0; t < 2; t++) {
			boxScores[t] = new BoxScore();
			boxScores[t].setTeam(teams[t]);
			boxScores[t].setYear(years[t]);
			if (!seasonSimulationMode) {
				rosters[t].setPitchers(DAO.getPitchersMapByTeamAndYear(teams[t].getTeamId(), years[t]));
				rosters[t].setBatters(DAO.getBattersMapByTeamAndYear(teams[t].getTeamId(), years[t]));
				HashMap<Integer, ArrayList<MLBFieldingStats>> fieldingStatsMap = DAO.getFieldingStatsMapByTeamAndYear(teams[t].getTeamId(), years[t]);
				rosters[t].getBatters().entrySet().stream().forEach(entry -> entry.getValue().setMlbFieldingStats(fieldingStatsMap.get(entry.getValue().getMlbPlayerId())));
			}
			else {
				rosters[t].setBatters(allBatters.get(teams[t].getTeamId()));
				rosters[t].setPitchers(allPitchers.get(teams[t].getTeamId()));
				HashMap<Integer, ArrayList<MLBFieldingStats>> fieldingStatsMap = allFielders.get(teams[t].getTeamId());
				if (years[t] >= 1999) {
					rosters[t].getBatters().entrySet().stream().forEach(entry -> entry.getValue().setMlbFieldingStats(fieldingStatsMap.get(entry.getValue().getMlbPlayerId())));
				}
			}
			if ((rosters[t].getBatters().size() == 0 || rosters[t].getPitchers().size() == 0) && !allStarGameMode) {
				System.out.println("Players for " + years[t] + " " + teams[t].getFullTeamName() + " not found in database.  Import player stats from API.");
				ArrayList<MLBTeam> teamList = new ArrayList<MLBTeam>();
				teamList.add(teams[t]);
				importTeam(years[t], t, teamList);
			}
			if (!allStarGameMode) {
				closers[t] = getPitcher(t, "SV", 0, null);
				if (years[t] >= 1999) {
					setupMen[t] = getPitcher(t, "HD", 0, null);
				}
			}
			seriesStats[t] = new SeriesStats(teams[t], years[t], seriesLength);
		}
		// Home team determines using DH
		useDH = determineUseDH(teams[1].getLeague(), years[1]);
		//useDH = (teams[1].getLeague().equalsIgnoreCase("AL") && years[1] >= 1973) || (teams[1].getLeague().equalsIgnoreCase("NL") && (years[1] == 2020 || years[1] >= 2022));
		boolean importedLineup = visTeamImportFile != null || homeTeamImportFile != null;
		ArrayList<ArrayList<ArrayList<MLBPlayer>>> lineupBatters = new ArrayList<>();
		List<List<MLBPlayer>> importedPitcherRotation = new ArrayList<>();
		List<List<MLBPlayer>> importedAllstarSubs = new ArrayList<>();
		if (!importedLineup) {
			lineupBatters = setOptimalBattingLineup(teams, years);
			boxScores[0].setBatters(lineupBatters.get(0));
			boxScores[1].setBatters(lineupBatters.get(1));
			if (areLineupsIncomplete(lineupBatters)) {
				System.out.println("INCOMPLETE LINEUPS! RERUN WITH IMPORTED LINEUP FILES");
				return false;
			}
			
			/*while (incompleteLineups) {
				handleIncompleteLineup();
				lineupBatters.set(0, boxScores[0].getBatters());
				lineupBatters.set(1, boxScores[1].getBatters());
				importedLineup = true;
				incompleteLineups = areLineupsIncomplete(lineupBatters);
			} */
		}
		else { // imported lineups
			if (visTeamImportFile != null) {
				lineupBatters.add(new ArrayList<>());
				importedPitcherRotation.add(new ArrayList<>());
				importedAllstarSubs.add(new ArrayList<>());
				if (!handleImportLineupCommand("IMPORT v " + visTeamImportFile, true, importedPitcherRotation, importedAllstarSubs)) {
					return false;
				}
				lineupBatters.set(0, boxScores[0].getBatters());
			}
			if (homeTeamImportFile != null) {
				lineupBatters.add(new ArrayList<>());
				importedPitcherRotation.add(new ArrayList<>());
				importedAllstarSubs.add(new ArrayList<>());
				if (!handleImportLineupCommand("IMPORT h " + homeTeamImportFile, true, importedPitcherRotation, importedAllstarSubs)) {
					return false;
				}
				lineupBatters.set(1, boxScores[1].getBatters());
			}
		}
		Map<String, Integer> boSeriesWins  = new HashMap<String, Integer>() {
			private static final long serialVersionUID = 1L;
		{
		    put(boxScores[0].getTeamAndYearDisplay(), 0);
		    put(boxScores[1].getTeamAndYearDisplay(), 0);
		}};
		for (int s = 0; s < seriesLength; s++) {
			gameState = new GameState();
			boxScores = new BoxScore[2];
			for (int t = 0; t < 2; t++) {
				if (bestOfSeries && t == 0 && ((seriesLength != 3 && s == 2) || (seriesLength == 7 && s == 5) || (seriesLength == 5 && s == 4))) { // home/vis change
					swapHomeVisitorForBestOfSeries(teams, years, lineupBatters, seriesStats);
					useDH = determineUseDH(teams[1].getLeague(), years[1]);
				}
				// Move setting of starting pitcher
				// reset useDh
				// change 9th hitter in lineupBatters to starting pitcher if useDH is false else to previous DH
				boxScores[t] = new BoxScore();
				boxScores[t].setYear(years[t]);
				boxScores[t].setBatters(lineupBatters.get(t));
				boxScores[t].setTeam(teams[t]);
				clearPlayerGameData(boxScores[t]);
				MLBPlayer startingPitcher;
				if (importedLineup && importedPitcherRotation.get(t) != null && importedPitcherRotation.get(t).size() > 0) {
					MLBPlayer importedPitcher = importedPitcherRotation.get(t).get(s % importedPitcherRotation.get(t).size());
					startingPitcher = new MLBPlayer(importedPitcher.getMlbPlayerId(), importedPitcher.getFullName(), "P", importedPitcher.getArmThrows(), 
						importedPitcher.getBats(), importedPitcher.getJerseyNumber(), importedPitcher.getSeasonStarted());
				}
				else {
					if (!seasonSimulationMode || teamSeasonGameIndexMap == null) {
						int rotationRange = rosters[t].getPitchers().size() < 5 ? rosters[t].getPitchers().size() - 1 : 4; // In case team has less than 5 starters
						startingPitcher = getPitcher(t, "GS", seriesLength > 1 ? s % (rotationRange + 1) : getRandomNumberInRange(0, rotationRange), null);
					}
					else {
						Integer teamSeasonGameIndex =  teamSeasonGameIndexMap.get(teams[t].getTeamId()) != null ? teamSeasonGameIndexMap.get(teams[t].getTeamId()) : 1;
						startingPitcher = getPitcher(t, "GS",  (teamSeasonGameIndex - 1) % 5, null);
					}
				}
				gameState.setCurrentPitcher(startingPitcher, t);
				boxScores[t].getPitchers().put(startingPitcher.getMlbPlayerId(), startingPitcher);
				if (!useDH) {
					boxScores[t].getBatters().set(8, new ArrayList<MLBPlayer>());  // Clear out prior games pitcher spots
					boxScores[t].getBatters().get(8).add(startingPitcher); // Set pitcher as batting ninth, if no DH
				}	
				else if (bestOfSeries && ((seriesLength != 3 && s == 2) || (seriesLength == 7 && s == 5) || (seriesLength == 5 && s == 4))) {  // Set DH
					boxScores[t].getBatters().set(8, new ArrayList<MLBPlayer>());  // Clear out prior games 9th batter
					ArrayList<Integer> playersInLineupList = new ArrayList<Integer>();
					boxScores[t].getBatters().stream().forEach(entry -> entry.stream().forEach(entry2 -> playersInLineupList.add(entry2.getMlbPlayerId())));
					MLBPlayer dh = getMlbPlayerWithMostPlateAppearances(teams[t].getTeamId(), years[t], playersInLineupList, t, battersOnMultTeams.get(t));
					dh.setPrimaryPosition("DH");
					boxScores[t].getBatters().get(8).add(dh); // Set DH as batting ninth
				}
			}
			if (simulationMode || autoBeforeMode) { // Set game started for SIM mode
				gameState.setGameStarted(true);
			}
			playBall(gameState, boxScores, ((gameNumber > 0 && !postSeason) ? gameNumber : s + 1), multiIterationTournament, importedPitcherRotation, importedAllstarSubs);
			if (seriesLength > 1) {
				seriesBoxScores[s] = boxScores;
				updateSeriesOrSeasonStatsFromBoxScores(seriesStats, boxScores, gameState.getPitchersOfRecord(), false);
			}
			else if (seasonSimulationMode) {
				updateSeriesOrSeasonStatsFromBoxScores(seriesStats, boxScores, gameState.getPitchersOfRecord(), true);
			}
			if (bestOfSeries) {
				if (boxScores[0].getFinalScore() > boxScores[1].getFinalScore()) {
					boSeriesWins.put(boxScores[0].getTeamAndYearDisplay(), boSeriesWins.get(boxScores[0].getTeamAndYearDisplay()) + 1);
				}
				else {
					boSeriesWins.put(boxScores[1].getTeamAndYearDisplay(), boSeriesWins.get(boxScores[1].getTeamAndYearDisplay()) + 1);
				}
				int winsNeededToWinSeries = seriesLength/2 + 1;
				if (boSeriesWins.get(boxScores[0].getTeamAndYearDisplay()) == winsNeededToWinSeries || 
					boSeriesWins.get(boxScores[1].getTeamAndYearDisplay()) == winsNeededToWinSeries) {
						break;
				}
			}
		} // series loop
		return true;
	}
	
	private static void swapHomeVisitorForBestOfSeries(MLBTeam[] teams, int[] years, ArrayList<ArrayList<ArrayList<MLBPlayer>>> lineupBatters, SeriesStats[] seriesStats) {
		int tempY = years[0];
		years[0] = years[1];
		years[1] = tempY;
		MLBTeam tempT = teams[0];
		teams[0] = teams[1];
		teams[1] = tempT;
		MLBPlayer tempC = closers[0];
		closers[0] = closers[1];
		closers[1] = tempC;
		MLBPlayer tempS = setupMen[0];
		setupMen[0] = setupMen[1];
		setupMen[1] = tempS;
		Roster tempR = rosters[0];
		rosters[0] = rosters[1];
		rosters[1] = tempR;
		SeriesStats tempSS = seriesStats[0];
		seriesStats[0] = seriesStats[1];
		seriesStats[1] = tempSS;
		Collections.swap(lineupBatters, 0, 1);
	}
	
	private static void updateSeriesOrSeasonStatsFromBoxScores(SeriesStats[] seriesStats,  BoxScore[] boxScores, Map<String, Integer> pitchersOfRecord, boolean season) {
		for (int t = 0; t < 2; t++) {
			Set<Integer> keys = boxScores[t].getPitchers().keySet();
			ArrayList<ArrayList<MLBPlayer>> batters = boxScores[t].getBatters();
	        for(Integer k : keys){
	            MLBPlayer p1 = boxScores[t].getPitchers().get(k);
	            MLBPlayer p2 = !season ? seriesStats[t].getPitchers().get(p1.getMlbPlayerId()) : seasonPitchingStats.get(p1.getMlbPlayerId());
	            if (p2 == null) {
	            	p2 = new MLBPlayer(p1.getMlbPlayerId(), p1.getFullName(), p1.getPrimaryPosition());
	            }
	            PitchingStats ps1 = p1.getMlbPitchingStats().getPitchingStats();
	            PitchingStats ps2 = p2.getMlbPitchingStats().getPitchingStats();
	            ps2.setHomeRunsAllowed(ps1.getHomeRunsAllowed() + ps2.getHomeRunsAllowed());
	            ps2.setEarnedRunsAllowed(ps1.getEarnedRunsAllowed() + ps2.getEarnedRunsAllowed());
	            ps2.setRunsAllowed(ps1.getRunsAllowed() + ps2.getRunsAllowed());
	            ps2.setHitsAllowed(ps1.getHitsAllowed() + ps2.getHitsAllowed());
	            ps2.setStrikeouts(ps1.getStrikeouts() + ps2.getStrikeouts());
	            ps2.setWalks(ps1.getWalks() + ps2.getWalks());
	            ps2.addInningsPitched(Double.toString(ps1.getInningsPitched()));
	            ps2.setSaves(ps1.getSaves() + ps2.getSaves());
	            ps2.setBlownSaves(ps1.getBlownSaves() + ps2.getBlownSaves());
	            ps2.setHolds(ps1.getHolds() + ps2.getHolds());
	            if (p2.getMlbPlayerId().intValue() == pitchersOfRecord.get("W").intValue()) {
	            	ps2.setWins(ps2.getWins() + 1);
	            }
	            else if (p2.getMlbPlayerId().intValue() == pitchersOfRecord.get("L").intValue()) {
	            	ps2.setLosses(ps2.getLosses() + 1);
	            }
	            p2.getMlbPitchingStats().setPitchingStats(ps2);
	            if (!season) {
	            	seriesStats[t].getPitchers().put(p1.getMlbPlayerId(), p2);
	            }
	            else {
	            	seasonPitchingStats.put(p1.getMlbPlayerId(), p2);
	            }
	        }
	        for (ArrayList<MLBPlayer>  playerList : batters){
	        	for (MLBPlayer b1 : playerList) {
	        		MLBPlayer b2 = !season ? seriesStats[t].getBatters().get(b1.getMlbPlayerId()) : seasonBattingStats.get(b1.getMlbPlayerId());
	        		if (b2 == null) {
	        			b2 = new MLBPlayer(b1.getMlbPlayerId(), b1.getFullName(), b1.getPrimaryPosition());
	        		}
	        		BattingStats bs1 = b1.getMlbBattingStats().getBattingStats();
	        		BattingStats bs2 = b2.getMlbBattingStats().getBattingStats();
	        		bs2.setHomeRuns(bs1.getHomeRuns() + bs2.getHomeRuns());
	        		bs2.setAtBats(bs1.getAtBats() + bs2.getAtBats());
	        		bs2.setHits(bs1.getHits() + bs2.getHits());
	        		bs2.setStrikeOuts(bs1.getStrikeOuts() + bs2.getStrikeOuts());
	        		bs2.setWalks(bs1.getWalks() + bs2.getWalks());
	        		bs2.setRuns(bs1.getRuns() + bs2.getRuns());
	        		bs2.setPlateAppearances(bs1.getPlateAppearances() + bs2.getPlateAppearances());
	        		bs2.setDoubles(bs1.getDoubles() + bs2.getDoubles());
	        		bs2.setTriples(bs1.getTriples() + bs2.getTriples());
	        		bs2.setHitByPitch(bs1.getHitByPitch() + bs2.getHitByPitch());
	        		bs2.setRbis(bs1.getRbis() + bs2.getRbis());
	        		bs2.setStolenBases(bs1.getStolenBases() + bs2.getStolenBases());
	        		bs2.setCaughtStealing(bs1.getCaughtStealing() + bs2.getCaughtStealing());
	        		b2.getMlbBattingStats().setBattingStats(bs2);
	        		if (!season) {
		            	seriesStats[t].getBatters().put(b1.getMlbPlayerId(), b2);
	        		}
	        		else {
	        			seasonBattingStats.put(b1.getMlbPlayerId(), b2);
	        		}
	        	}
	        }
		}
	}
	
	private static void updateSeasonResults(Map<Integer, Map<Integer, TeamSeasonResults>> seasonResults, BoxScore[] boxScores) {
		// Update team results
		int visTeamId = boxScores[0].getTeam().getTeamId();
		int homeTeamId = boxScores[1].getTeam().getTeamId();
		for (Map.Entry<Integer, Map<Integer, TeamSeasonResults>> divisionTeams : seasonResults.entrySet()) {
			for (Map.Entry<Integer, TeamSeasonResults> teamResults : divisionTeams.getValue().entrySet()) {
				TeamSeasonResults teamResult = teamResults.getValue();
				if (teamResult.getTeamId() == visTeamId) {
					if (boxScores[0].getFinalScore() > boxScores[1].getFinalScore()) { 
						teamResult.setWins(teamResult.getWins() + 1);
					}
					else {
						teamResult.setLosses(teamResult.getLosses() + 1);
					}
				}
				else if (teamResult.getTeamId() == homeTeamId) {
					if (boxScores[1].getFinalScore() > boxScores[0].getFinalScore()) { 
						teamResult.setWins(teamResult.getWins() + 1);
					}
					else {
						teamResult.setLosses(teamResult.getLosses() + 1);
					}
				}
			}
		}
	}
	
	private static void outputSeriesResults(int seriesLength, SeriesStats[] seriesStats, boolean bestOfSeries) {
		System.out.println("\nSeries Stats");
		outputBoxScore(seriesStats);
		HashMap<String, Integer> totalWins = new HashMap<String, Integer>();
		HashMap<String, Integer> totalRuns = new HashMap<String, Integer>();
		totalWins.put(boxScores[0].getTeam().getShortTeamName() + "" + boxScores[0].getYear(), 0);
		totalWins.put(boxScores[1].getTeam().getShortTeamName() + "" + boxScores[1].getYear(), 0);
		totalRuns.put(boxScores[0].getTeam().getShortTeamName() + "" + boxScores[0].getYear(), 0);
		totalRuns.put(boxScores[1].getTeam().getShortTeamName() + "" + boxScores[1].getYear(), 0);
		determineSeriesMVP(seriesStats);
		System.out.println("Series Game Results:");
		int seriesGameIndex = 0;
		for (BoxScore[] bsArray : seriesBoxScores) {
			if (bsArray[0] == null || bsArray[1] == null) {
				break;
			}
			int winner = bsArray[0].getFinalScore() > bsArray[1].getFinalScore() ? 0 : 1;
			System.out.println(displayTeamName(winner, bsArray) + " " + bsArray[winner].getFinalScore() + (bsArray[winner].getFinalScore() > 9 ? " " : "  ") +  
				displayTeamName(winner==1?0:1, bsArray) + " " + bsArray[winner==1?0:1].getFinalScore());
			totalWins.put(bsArray[winner].getTeam().getShortTeamName() + "" + bsArray[winner].getYear(), totalWins.get(bsArray[winner].getTeam().getShortTeamName() + "" + bsArray[winner].getYear()) + 1);
			totalRuns.put(bsArray[0].getTeam().getShortTeamName() + "" + bsArray[0].getYear(), totalRuns.get(bsArray[0].getTeam().getShortTeamName() + "" + bsArray[0].getYear()) + bsArray[0].getFinalScore());
			totalRuns.put(bsArray[1].getTeam().getShortTeamName() + "" + bsArray[1].getYear(), totalRuns.get(bsArray[1].getTeam().getShortTeamName() + "" + bsArray[1].getYear()) + bsArray[1].getFinalScore());
			seriesGameIndex++;
		}
		System.out.println("\nTotals:");
		int homeWinner = totalWins.get(boxScores[1].getTeam().getShortTeamName() + "" + boxScores[1].getYear()) > totalWins.get(boxScores[0].getTeam().getShortTeamName() + "" + boxScores[0].getYear()) ? 1 : 0;
		if (bestOfSeries) {
			System.out.println(displayTeamName(homeWinner, boxScores) + " in " + seriesGameIndex + " games");
		}
		else {
			System.out.println(displayTeamName(homeWinner, boxScores) + ": " + totalWins.get(boxScores[homeWinner].getTeam().getShortTeamName() + "" + boxScores[homeWinner].getYear()) + "(" + df.format((double)totalWins.get(boxScores[homeWinner].getTeam().getShortTeamName() + 
			"" + boxScores[homeWinner].getYear())/seriesGameIndex) +  ") " + displayTeamName(homeWinner==1?0:1, boxScores) + ": " + totalWins.get(boxScores[homeWinner==1?0:1].getTeam().getShortTeamName() + "" + boxScores[homeWinner==1?0:1].getYear()) + 
			"(" + df.format((double)totalWins.get(boxScores[homeWinner==1?0:1].getTeam().getShortTeamName() + "" + boxScores[homeWinner==1?0:1].getYear())/seriesGameIndex) + ")");
		}
		System.out.println("Average Score:"); 
		System.out.println(displayTeamName(homeWinner, boxScores) + ": " + df.format((double)totalRuns.get(boxScores[homeWinner].getTeam().getShortTeamName() + "" + 
			boxScores[homeWinner].getYear())/seriesGameIndex) + " " + displayTeamName(homeWinner==1?0:1, boxScores) + ": " + df.format((double)totalRuns.get(boxScores[homeWinner==1?0:1].getTeam().getShortTeamName() + 
			"" + boxScores[homeWinner==1?0:1].getYear())/seriesGameIndex));
	}
	
	@SuppressWarnings("unchecked")
	private static void determineSeriesMVP(SeriesStats[] seriesStats) {
		// Determine offensive MVP
		Map<Integer, Integer> mvpOffenseCandidates = new LinkedHashMap<Integer, Integer>();
		for (int t = 0; t < 2; t++) {
			seriesStats[t].getBatters().entrySet().stream().forEach(entry -> mvpOffenseCandidates.put(entry.getValue().getMlbPlayerId(), 
				entry.getValue().getMlbBattingStats().getBattingStats().getHomeRuns() + entry.getValue().getMlbBattingStats().getBattingStats().getHits() +
				entry.getValue().getMlbBattingStats().getBattingStats().getRbis() + entry.getValue().getMlbBattingStats().getBattingStats().getRuns() +
				entry.getValue().getMlbBattingStats().getBattingStats().getStolenBases()/2));
		}
		LinkedHashMap<Integer, Integer> sortedMvpOffenseCandidates = mvpOffenseCandidates.entrySet().stream().sorted(Entry.comparingByValue())
			.collect(LinkedHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), Map::putAll);
		// Get last sorted player (most points)
		Entry<Integer, Integer> mvpOffenseId = (Entry<Integer, Integer>)sortedMvpOffenseCandidates.entrySet().toArray()[sortedMvpOffenseCandidates.size() - 1];
		MLBPlayer mvpOffense = seriesStats[0].getBatters().get(mvpOffenseId.getKey());
		if (mvpOffense == null) {
			mvpOffense = seriesStats[1].getBatters().get(mvpOffenseId.getKey());
		}
		System.out.println("\nSeries Offensive MVP: " + mvpOffense.getFullName() + " H: " + mvpOffense.getMlbBattingStats().getBattingStats().getHits() + 
			" HR: " + mvpOffense.getMlbBattingStats().getBattingStats().getHomeRuns() + " RBI: " + mvpOffense.getMlbBattingStats().getBattingStats().getRbis() + 
			" R: " + mvpOffense.getMlbBattingStats().getBattingStats().getRuns() + " SB: " + mvpOffense.getMlbBattingStats().getBattingStats().getStolenBases() + "\n");
	}
	
	private static void outputSeasonResults(Map<Integer, Map<Integer, TeamSeasonResults>> seasonResults, Integer seasonSimYear) {
		int division = 0;
		int divisionLeaderWins = 0;
		String divisionName;
		System.out.println("\n"  + seasonSimYear + " MLB Standings");
		for (Map.Entry<Integer, Map<Integer, TeamSeasonResults>> divisionTeams : seasonResults.entrySet()) {
			divisionName = mlbDivisionNames.get(division);
			System.out.println(divisionName + (divisionName.length() <= 8 ? "\t" : "") + "\t\t\tW    L    GB    RF  RA");
			List<TeamSeasonResults> sortedTeamSeasonResults = new ArrayList<>(divisionTeams.getValue().values());
			Collections.sort(sortedTeamSeasonResults);
			for (TeamSeasonResults tsr : sortedTeamSeasonResults) {
				divisionLeaderWins = (divisionLeaderWins == 0) ? tsr.getWins() : divisionLeaderWins;
				String teamName = getTeamByYearAndTeamId(seasonSimYear, tsr.getTeamId(), allMlbTeamsList).getFullTeamName(); 
				System.out.print(teamName);
				for (int tab = 32; tab >= 8; tab-=8) {
					if (teamName.length() < tab) {
						System.out.print("\t");
					}
				}
				String gb = (divisionLeaderWins - tsr.getWins() != 0) ? divisionLeaderWins - tsr.getWins() + ".0" : "    ";
				System.out.println(tsr.getWins() + (tsr.getWins() >= 100 ? "  " : "   ") + tsr.getLosses() + (tsr.getLosses() >= 100 ? "  " : "   ") 
					+ gb + (gb.length() >= 4 ? "  " : "   ") + tsr.getRunsFor() + (tsr.getRunsFor() >= 100 ? "  " : "   ") + tsr.getRunsAgainst());
			}
			System.out.println();
			division++;
			divisionLeaderWins = 0;
		}
		// Output season leaders
		System.out.println(seasonSimYear + " Batting Leaders");
		System.out.println("HR");
		Map<Integer, MLBPlayer> sortedSeasonBattingStats =  sortHashMapByValue(seasonBattingStats, "HR");
		sortedSeasonBattingStats.entrySet().stream().filter(entry -> entry.getValue().getMlbBattingStats().getBattingStats().getHomeRuns() > 35)
			.forEach(entry -> System.out.println(entry.getValue().getFullName() + " " + entry.getValue().getMlbBattingStats().getBattingStats().getHomeRuns()));
		System.out.println("\nAVG");
		sortedSeasonBattingStats =  sortHashMapByValue(seasonBattingStats, "AVG");
		sortedSeasonBattingStats.entrySet().stream().filter(entry -> ((double)entry.getValue().getMlbBattingStats().getBattingStats().getHits() / 
			entry.getValue().getMlbBattingStats().getBattingStats().getAtBats()) > .280 && entry.getValue().getMlbBattingStats().getBattingStats().getPlateAppearances() > 100)
			.forEach(entry -> System.out.println(entry.getValue().getFullName() + " " + (df.format((double)entry.getValue().getMlbBattingStats().getBattingStats().getHits() / 
			entry.getValue().getMlbBattingStats().getBattingStats().getAtBats()))));
		
		// Output DL
		/*System.out.println("\nDL: " + disabledList.size());
		disabledList.entrySet().stream().forEach(entry -> System.out.println(entry.getValue() + " " + entry.getKey()));*/
	}
	
	private static void postSeason(Map<Integer, Map<Integer, TeamSeasonResults>> seasonResults, Integer seasonSimYear) {
		HashMap<Integer, TeamSeasonResults> alPlayoffTeams = new HashMap<>();
		HashMap<Integer, TeamSeasonResults> nlPlayoffTeams = new HashMap<>();
		HashMap<Integer, TeamSeasonResults> alTeams = new HashMap<>();
		HashMap<Integer, TeamSeasonResults> nlTeams = new HashMap<>();
		List<TeamSeasonResults> sortedTeamSeasonResults;
		System.out.println("\n"  + seasonSimYear + " Post Season");
		for (Map.Entry<Integer, Map<Integer, TeamSeasonResults>> divisionTeams : seasonResults.entrySet()) {
			sortedTeamSeasonResults = new ArrayList<>(divisionTeams.getValue().values());
			Collections.sort(sortedTeamSeasonResults);
			int teamIndex = 0;
			boolean alTeam = (divisionTeams.getKey() == AL_EAST || divisionTeams.getKey() == AL_CENTRAL || divisionTeams.getKey() == AL_WEST);
			for (TeamSeasonResults tsr: sortedTeamSeasonResults) {
				if (teamIndex == 0) {
					if (alTeam) {
						alPlayoffTeams.put(tsr.getTeamId(), tsr);
					}
					else {
						nlPlayoffTeams.put(tsr.getTeamId(), tsr);
					}
				}
				else  {
					if (alTeam) {
						alTeams.put(tsr.getTeamId(), tsr);
					}
					else {  // NL team
						nlTeams.put(tsr.getTeamId(), tsr);
					}
				}
				teamIndex++;
			}
		}
		List<TeamSeasonResults> alSeededPlayoffTeams = new ArrayList<>(alPlayoffTeams.values());
		Collections.sort(alSeededPlayoffTeams);
		List<TeamSeasonResults> nlSeededPlayoffTeams = new ArrayList<>(nlPlayoffTeams.values());
		Collections.sort(nlSeededPlayoffTeams);
		List<TeamSeasonResults> sortedAlTeams = new ArrayList<>(alTeams.values());
		Collections.sort(sortedAlTeams);
		List<TeamSeasonResults> sortedNlTeams = new ArrayList<>(nlTeams.values());
		Collections.sort(sortedNlTeams);
		int numberOfWildcards = seasonSimYear >= 2022 ? 3 : 2;
		for (int w = 0; w < numberOfWildcards; w++) {
			// Add wild card teams
			alSeededPlayoffTeams.add(sortedAlTeams.get(w));
			nlSeededPlayoffTeams.add(sortedNlTeams.get(w));
		}
		System.out.println(seasonSimYear + " AL Playoff teams");
		int seedIndex = 1;
		for (TeamSeasonResults tr: alSeededPlayoffTeams) {
			System.out.println(seedIndex + ". " + getTeamByYearAndTeamId(seasonSimYear, tr.getTeamId(), allMlbTeamsList).getFullTeamName());
			seedIndex++;
		}
		System.out.println("\n"  + seasonSimYear + " NL Playoff teams");
		seedIndex = 1;
		for (TeamSeasonResults tr: nlSeededPlayoffTeams) {
			System.out.println(seedIndex + ". " + getTeamByYearAndTeamId(seasonSimYear, tr.getTeamId(), allMlbTeamsList).getFullTeamName());
			seedIndex++;
		}
		if (seasonSimYear >= 2013 && seasonSimYear != 2020) { // Only support post season game play 2013 and beyond
			System.out.println("\n"  + seasonSimYear + " Play Playoff games");
			playPostSeasonGames(seasonSimYear, alSeededPlayoffTeams, nlSeededPlayoffTeams);
		}
		else  {
			System.out.println("\nDon't currently support post season game play before 2013 or 2020");
		}
	}
	
	private static void playPostSeasonGames(int seasonSimYear, List<TeamSeasonResults> alSeededPlayoffTeams, List<TeamSeasonResults> nlSeededPlayoffTeams) {
		List<TournamentTeam> alSeededTournamentTeams = new ArrayList<>();
		List<TournamentTeam> nlSeededTournamentTeams = new ArrayList<>();
		alSeededPlayoffTeams.stream().forEach(entry -> alSeededTournamentTeams.add(new TournamentTeam(seasonSimYear, new MLBTeam(entry.getTeamId()))));
		nlSeededPlayoffTeams.stream().forEach(entry -> nlSeededTournamentTeams.add(new TournamentTeam(seasonSimYear, new MLBTeam(entry.getTeamId()))));
		playTournamentGames(seasonSimYear, alSeededTournamentTeams, nlSeededTournamentTeams, true, false); // play post season playoff games
	}
	
	private static void playWorldSeriesGames(List<MLBWorldSeries> worldSeries) {
		System.out.println("World Series Reenactment - Years: " + worldSeries.get(0).getYear() + 
			(worldSeries.size() > 1 ? " to " + worldSeries.get(worldSeries.size() - 1).getYear() : "") + "\n");
		boolean aLHome = false;
		// WS HFA rules
		// 1902 -> 1993 AL odd years NL even years
		// 1995 -> 2001 AL even years NL odd years
		// 2002 -> 2016 AS game winner
		// 2017 -> 2023 best record
		// TBD below 2010-2023 (14 years)
		MLBTeam[] wsTeams = {null, null};
		int[] wsYears = {0, 0};
		for (MLBWorldSeries ws : worldSeries) {
			if ((ws.getYear() % 2 == 1 && ws.getYear() <= 1994) || (ws.getYear() % 2 == 0 && (ws.getYear() >= 1995 && ws.getYear() <= 2001)) || 
				(ws.getYear() >= 2002 && ws.getYear() <= 2009)) {
					aLHome = true;
			}
			else {
				aLHome = false;
			}
			SeriesStats[] wsStats = new SeriesStats[2];
			MLBTeam team1 =  getTeamByYearAndFullName(ws.getYear(), ws.getTeam1(), allMlbTeamsList);
			MLBTeam team2 =  getTeamByYearAndFullName(ws.getYear(), ws.getTeam2(), allMlbTeamsList);
			if ((team1.getLeague().equals("AL") && aLHome) || (team1.getLeague().equals("NL") && !aLHome)) {
				wsTeams[0] = team2;  // AL home, NL vis
				wsTeams[1] = team1;
			}
			else {
				wsTeams[0] = team1; // NL home, AL vis
				wsTeams[1] = team2;
			}
			wsYears[0] = wsYears[1] = ws.getYear();
			System.out.println("\n" + ws.getYear() + " World Series: " + wsTeams[0] + " at " + wsTeams[1]);
			battersOnMultTeams = new ArrayList<>();  // clear out battersOnMultTeams from prev season
			pitchersOnMultTeams = new ArrayList<>(); // clear out pitchersOnMultTeams from prev season
			for (int t = 0; t < 2; t++) {
				battersOnMultTeams.add(DAO.getBattersOnMultipleTeamsByPrimaryTeam(ws.getYear())); 
				pitchersOnMultTeams.add(DAO.getPitchersOnMultipleTeamsByPrimaryTeam(ws.getYear()));
			}
			seriesBoxScores = new BoxScore[MAX_WS_SERIES][2];
			if (setUpDataAndPlayGames(wsTeams, wsYears, MAX_WS_SERIES, true, wsStats, null, null, null, null, 1, true, false)) {
				outputSeriesResults(MAX_WS_SERIES, wsStats, true);
				System.out.println("Actual " + ws.getYear() + " WS winner: " + ws.getWinner());
			}
			else {
				System.out.println("World Series: " + ws + " could not be run");
				return;
			}
		}
	}
	
	private static String playTournamentGames(int seasonSimYear, List<TournamentTeam> alSeededTournamentTeams, List<TournamentTeam> nlSeededTournamentTeams, 
			boolean mlbPlayoffs, boolean multiIterationTournament) {
		String tournamentWinner = "";
		MLBTeam[] tournamentMlbTeams = {null, null};
		TournamentTeam[] tournamentTeams = {null, null};
		HashMap<Integer, ArrayList<Integer>> nlTournamentWinnersBySeed = new HashMap<>();
		HashMap<Integer, ArrayList<Integer>> alTournamentWinnersBySeed = new HashMap<>();
		List<TournamentTeam> seededTournamentTeams;
		int[] tournamentYears = {0, 0};
		boolean season2013To2021 = mlbPlayoffs && seasonSimYear >= 2013 && seasonSimYear <= 2021;
		for (int round = 0; round < 4; round++) {
			nlTournamentWinnersBySeed.put(round, new ArrayList<Integer>());
			alTournamentWinnersBySeed.put(round, new ArrayList<Integer>());
			if (!mlbPlayoffs) {
				System.out.println("Round #" + (round + 1));
			}
			for (int seriesIndex = 0; seriesIndex < 2; seriesIndex++) { // 2 series in first 2 rounds, 1 in last 2
				if (round == 0 && season2013To2021 && seriesIndex > 0) {
					break;  // Only 1 series in WC round 2013-2021
				}
				if (round > 1 && seriesIndex > 0) {
					break;  // Only 1 series in ALCS and NLCS or WS (rounds 3 and 4)
				}
				for (int league = 0; league < 2; league++) {
					if (round > 2 && league > 0) {
						break;  // Only 1 WS 
					}
					seededTournamentTeams = (league == 0) ? alSeededTournamentTeams : nlSeededTournamentTeams;
					SeriesStats[] tournamentSeriesStats = new SeriesStats[2];
					Map<String, Integer> tournamentSeriesWinsMap = new HashMap<>();
					if (!mlbPlayoffs || (mlbPlayoffs && seasonSimYear >= 2013 && seasonSimYear != 2020)) {
						int seriesMax;
						TournamentTeam higherSeededTeam;
						TournamentTeam lowerSeededTeam;
						if (round == 0) {
							if (league == 0 && mlbPlayoffs) {
								System.out.println("ALWC #" + (seriesIndex + 1));
							}
							else if (league == 1 && mlbPlayoffs){
								System.out.println("NLWC #" + (seriesIndex + 1));
							}
							seriesMax = 5;
							if (mlbPlayoffs) {
								seriesMax = season2013To2021 ? 1 : 3; // WC rounds best of 3 or 1 game
							}
							int higherSeed = (seriesIndex == 0) ? 2 : 3;
							int lowerSeed = (seriesIndex == 0) ? 5 : 4;
							if (season2013To2021) {  // 4 seed plays 5 seed 1 WC game 
								higherSeed = 3;
								lowerSeed = 4;
							}
							higherSeededTeam = seededTournamentTeams.get(higherSeed);
							lowerSeededTeam = seededTournamentTeams.get(lowerSeed);	
						}
						else if (round == 1) { // Second Round
							seriesMax = 5;
							higherSeededTeam = seededTournamentTeams.get(seriesIndex == 0 ? 0 : 1);
							if (league == 0) {
								if (mlbPlayoffs) {
									System.out.println("ALDS #" + (seriesIndex + 1));
								}
								if (seriesIndex == 0) {
									if (season2013To2021) {
										lowerSeededTeam = seededTournamentTeams.get(alTournamentWinnersBySeed.get(round-1).get(0));  // only 1 WC winner
									}
									else {
										lowerSeededTeam = seededTournamentTeams.get(alTournamentWinnersBySeed.get(round-1).get(1));
									}
								}
								else { // Second series
									if (season2013To2021) {
										lowerSeededTeam = seededTournamentTeams.get(2);  // 2 v 3 in ALDS
									}
									else {
										lowerSeededTeam = seededTournamentTeams.get(alTournamentWinnersBySeed.get(round-1).get(0));
									}
								}
							}
							else {
								if (mlbPlayoffs) {
									System.out.println("NLDS  #" + (seriesIndex + 1));
								}
								if (seriesIndex == 0) {
									if (season2013To2021) {
										lowerSeededTeam = seededTournamentTeams.get(nlTournamentWinnersBySeed.get(round-1).get(0));  // only 1 WC winner
									}
									else {
										lowerSeededTeam = seededTournamentTeams.get(nlTournamentWinnersBySeed.get(round-1).get(1));
									}
								}
								else { // Second series
									if (season2013To2021) {
										lowerSeededTeam = seededTournamentTeams.get(2);  // 2 v 3 in NLDS
									}
									else {
										lowerSeededTeam = seededTournamentTeams.get(nlTournamentWinnersBySeed.get(round-1).get(0));
									}
								}
							}
						}
						else if (round == 2) { // Third Round (semis)
							seriesMax = 7;
							if (league == 0) {
								if (mlbPlayoffs) {
									System.out.println("ALCS");
								}
								higherSeededTeam = seededTournamentTeams.get(alTournamentWinnersBySeed.get(round-1).get(0));
								lowerSeededTeam = seededTournamentTeams.get(alTournamentWinnersBySeed.get(round-1).get(1));
							}
							else {
								if (mlbPlayoffs) {
									System.out.println("NLCS");
								}
								higherSeededTeam = seededTournamentTeams.get(nlTournamentWinnersBySeed.get(round-1).get(0));
								lowerSeededTeam = seededTournamentTeams.get(nlTournamentWinnersBySeed.get(round-1).get(1));
							}
						}
						else { // Champ round
							if (mlbPlayoffs) {
								System.out.println("World Series");
							}
							seriesMax = 7;
							// AL always higher seed
							higherSeededTeam = alSeededTournamentTeams.get(alTournamentWinnersBySeed.get(round-1).get(0));
							lowerSeededTeam = nlSeededTournamentTeams.get(nlTournamentWinnersBySeed.get(round-1).get(0));
						}
						
						tournamentTeams[0] = new TournamentTeam(lowerSeededTeam.getYear(), getTeamByYearAndTeamId(lowerSeededTeam.getYear(), lowerSeededTeam.getMlbTeam().getTeamId(), allMlbTeamsList));
						tournamentTeams[1] = new TournamentTeam(higherSeededTeam.getYear(), getTeamByYearAndTeamId(higherSeededTeam.getYear(), higherSeededTeam.getMlbTeam().getTeamId(), allMlbTeamsList));
						tournamentYears[0] = tournamentTeams[0].getYear();
						tournamentYears[1] = tournamentTeams[1].getYear();
						tournamentMlbTeams[0] = tournamentTeams[0].getMlbTeam();
						tournamentMlbTeams[1] = tournamentTeams[1].getMlbTeam();
						tournamentSeriesWinsMap.put(tournamentTeams[0].getYear() + " " + tournamentTeams[0].getMlbTeam().getFullTeamName(), 0);
						tournamentSeriesWinsMap.put(tournamentTeams[1].getYear() + " " + tournamentTeams[1].getMlbTeam().getFullTeamName(), 0);
						seasonSimulationMode = false;
						seasonSimulationPlayoffsMode = mlbPlayoffs;
						tournamentMode = !mlbPlayoffs;
						seriesBoxScores = new BoxScore[seriesMax][2];
						if (!mlbPlayoffs) {  // Already have multiple players
							battersOnMultTeams = new ArrayList<>();  // clear out battersOnMultTeams from prev season
							pitchersOnMultTeams = new ArrayList<>(); // clear out pitchersOnMultTeams from prev season
							for (int t = 0; t < 2; t++) {
								battersOnMultTeams.add(DAO.getBattersOnMultipleTeamsByPrimaryTeam(tournamentYears[t]));
								pitchersOnMultTeams.add(DAO.getPitchersOnMultipleTeamsByPrimaryTeam(tournamentYears[t]));
							}
						}
						setUpDataAndPlayGames(tournamentMlbTeams, tournamentYears, seriesMax, true, tournamentSeriesStats, null, null, null, null, 1, true, multiIterationTournament);
						System.out.println();
						int seriesLength = 0;
						if (seriesMax == 1) {
							seriesBoxScores[0] = boxScores; // if WC series is one game
						}
						for (BoxScore[] bsArray : seriesBoxScores) {
							if (bsArray[0] == null || bsArray[1] == null) {
								break;
							}
							seriesLength++;
							if (bsArray[0].getFinalScore() > bsArray[1].getFinalScore()) {
								tournamentSeriesWinsMap.put(bsArray[0].getYear() + " " + bsArray[0].getTeam().getFullTeamName(), 
									tournamentSeriesWinsMap.get(bsArray[0].getYear() + " " + bsArray[0].getTeam().getFullTeamName()) + 1);
								System.out.println((tournamentMode ? bsArray[0].getYear() + " " : "") + bsArray[0].getTeam().getShortTeamName() + " " + bsArray[0].getFinalScore() + " " + 
									(tournamentMode ? bsArray[1].getYear() + " " : "") + bsArray[1].getTeam().getShortTeamName() + " " + bsArray[1].getFinalScore());
							}
							else {
								tournamentSeriesWinsMap.put(bsArray[1].getYear() + " " + bsArray[1].getTeam().getFullTeamName(), 
									tournamentSeriesWinsMap.get(bsArray[1].getYear() + " " + bsArray[1].getTeam().getFullTeamName()) + 1);
								System.out.println((tournamentMode ? bsArray[1].getYear() + " " : "") + bsArray[1].getTeam().getShortTeamName() + " " + bsArray[1].getFinalScore() + " " + 
									(tournamentMode ? bsArray[0].getYear() + " " : "") + bsArray[0].getTeam().getShortTeamName() + " " + bsArray[0].getFinalScore());
							}
						}
						int winner = tournamentSeriesWinsMap.get(tournamentTeams[0].getYear() + " " + tournamentTeams[0].getMlbTeam().getFullTeamName()) > 
							tournamentSeriesWinsMap.get(tournamentTeams[1].getYear() + " " + tournamentTeams[1].getMlbTeam().getFullTeamName()) ? 0 : 1;
						System.out.println((tournamentMode ? tournamentTeams[winner].getYear() + " " : "" ) + tournamentTeams[winner].getMlbTeam().getFullTeamName() + 
							" over " + (tournamentMode ? tournamentTeams[winner == 1 ? 0 : 1].getYear() + " " : "" ) + tournamentTeams[winner == 1 ? 0 : 1].getMlbTeam().getFullTeamName() + " in " + seriesLength + "\n");
						
						if (round == 3) {
							System.out.println("Final Round Stats");
							outputSeriesResults(7, tournamentSeriesStats, true);
							tournamentWinner = tournamentTeams[winner].getYear() + " " + tournamentTeams[winner].getMlbTeam().getFullTeamName();
						}
						
						int winningSeed = 0;
						ArrayList<Integer> tournamentWinnersBySeed;
						for (TournamentTeam winningPlayoffTeam : seededTournamentTeams) {
							if (winningPlayoffTeam.getMlbTeam().getTeamId() == tournamentTeams[winner].getMlbTeam().getTeamId() && 
								winningPlayoffTeam.getYear() == tournamentTeams[winner].getYear() &&
								league == 0) {
									tournamentWinnersBySeed = alTournamentWinnersBySeed.get(round);
									tournamentWinnersBySeed.add(winningSeed);
									alTournamentWinnersBySeed.put(new Integer(round), tournamentWinnersBySeed);
							}
							else if (winningPlayoffTeam.getMlbTeam().getTeamId() == tournamentTeams[winner].getMlbTeam().getTeamId() && 
								winningPlayoffTeam.getYear() == tournamentTeams[winner].getYear() &&
								league == 1) {
									tournamentWinnersBySeed = nlTournamentWinnersBySeed.get(round);
									tournamentWinnersBySeed.add(winningSeed);
									nlTournamentWinnersBySeed.put(new Integer(round), tournamentWinnersBySeed);
							}
							winningSeed++;
						}
					}
					else { // Before 2022
						System.out.println("Don't currently support post season before 2013 or 2020");
					}
				}
			}
		}
		return tournamentWinner;
	}
	
	private static void playBall(GameState gameState, BoxScore[] boxScores, int gameNumber, boolean multiIterationTournament, 
			List<List<MLBPlayer>> importedPitcherRotation, List<List<MLBPlayer>> importedAllstarSubs) {
		printlnToScreen("\nStarting pitchers for Game #" + gameNumber +  " " + boxScores[0].getTeam().getShortTeamName() + ": " + 
			gameState.getCurrentPitchers()[0].getFirstLastName() + " v " + boxScores[1].getTeam().getShortTeamName() + ": " + gameState.getCurrentPitchers()[1].getFirstLastName());
		boolean tiedAllStarGame = false;
		while (gameState.getInning() <= INNINGS_PER_GAME || boxScores[0].getScore(gameState.getInning()) == boxScores[1].getScore(gameState.getInning())) {
			int inning = gameState.getInning();
			Scanner myObj = null;
			for (int top = 0; top < 2; top++) {
				BoxScore boxScore = boxScores[top];
				Roster roster = rosters[top];
				ArrayList<ArrayList<MLBPlayer>> batters = boxScore.getBatters();
				gameState.setTop(top);
				gameState.setOuts(0);
				gameState.setVirtualErrorOuts(0);
				gameState.setBaseRunnersReachedByError(new ArrayList<Integer>());
				if (top == 0) {
					printToScreen("\n\n*** INNING " + inning + " ***\n");
				}
				printToScreen("\n*** " + displayTeamName(top, boxScores) + " BATTING IN " + (top == 0 ? "TOP " : "BOTTOM ") + "INN: " + inning + " : ");
				printlnToScreen("SCORE - " + displayTeamName(0, boxScores) + ": " + boxScores[0].getScore(gameState.getInning())  + " " +
					displayTeamName(1, boxScores) + ": " + boxScores[1].getScore(gameState.getInning()) + " ***");
				boolean gameTiedStartOfAB;
				Arrays.fill(gameState.getBaseRunners(), new BaseRunner());
				
				if (top == 0 && gameState.getInning() == 6) { // Set winning pitcher after 5 innings
					if (boxScores[0].getScore(gameState.getInning()) > boxScores[1].getScore(gameState.getInning())) {
						gameState.setPitcherOfRecord("W", gameState.getCurrentPitchers()[0].getMlbPlayerId());
					}
					else if (boxScores[1].getScore(gameState.getInning()) > boxScores[0].getScore(gameState.getInning())) {
						gameState.setPitcherOfRecord("W", gameState.getCurrentPitchers()[1].getMlbPlayerId());
					}
				}
				
				if (allStarGameMode) { // Check for all star subs
					if (gameState.getInning() != 1 && gameState.getInning() % 2 == 1) { // change pitchers
						int currentPitcherIndex = 0;
						for (MLBPlayer p : importedPitcherRotation.get(top==0?1:0)) {
							if (p.getFullName().equals(gameState.getCurrentPitchers()[top==0?1:0].getFullName())) {
								break;
							}
							currentPitcherIndex++;
						}
						int nextPitcherIndex = currentPitcherIndex < 4 ? currentPitcherIndex + 1: 0;
						changePitcher(importedPitcherRotation.get(top==0?1:0).get(nextPitcherIndex), gameState.getTop()==0?1:0, null);
					}
					int numOfBattersToBeSubbed = useDH ? 9 : 8;
					if (gameState.getInning() == 6) {  // field position subs
						for (int lp = 0; lp < numOfBattersToBeSubbed; lp++) {
							MLBPlayer newPlayer = importedAllstarSubs.get(top).get(lp);
							batters.get(lp).add(newPlayer);
							System.out.println("Batter subbed to: " + newPlayer.getFirstLastName());
						}
					}
				}
				
				while (gameState.getOuts() < OUTS_PER_INNING) {
					if (gameState.getOuts() == 2 && gameState.getCurrentBasesSituation() != GameState.BASES_EMPTY) {
						gameState.setHitAndRun(true);  // start runners with 2 outs and runners on
					}
					MLBPlayer currentBatter = batters.get(gameState.getBattingOrder()[top] - 1).get(batters.get(gameState.getBattingOrder()[top] - 1).size() - 1);
					MLBPlayer currentPitcher = gameState.getCurrentPitchers()[top==0?1:0];
					BattingStats currentBatterGameStats = currentBatter.getMlbBattingStats().getBattingStats();
					BattingStats currentBatterSeasonStats = getBattersSeasonBattingStats(roster, currentBatter.getMlbPlayerId());
					PitchingStats currentPitcherGameStats = currentPitcher != null ? currentPitcher.getMlbPitchingStats().getPitchingStats() : null;
					if ((simulationMode || (autoBeforeMode && inning < autoBeforeInning)) && !allStarGameMode) {  // Only look to change pitchers in SIM mode and not AS game
						int pitcherYear = boxScores[top==0?1:0].getYear();
						ArrayList<Integer> excludingPitchers = new ArrayList<Integer>(boxScores[top==0?1:0].getPitchers().keySet());
						// No Holds before 1999
						if (pitcherYear >= 1999 && gameState.getInning() >= 7 && Math.abs(boxScores[1].getScore(9) - boxScores[0].getScore(9)) < 4 && !gameState.isCloserPitching(top) && !gameState.isSetupManPitching(top) && 
							((top == 0 && boxScores[1].getScore(9) > boxScores[0].getScore(9)) || top == 1 && (boxScores[0].getScore(9) > boxScores[1].getScore(9)))) { 
								if (closers[top==0?1:0] != null) {
									excludingPitchers.add(closers[top==0?1:0].getMlbPlayerId()); // Exclude closer when getting setup man
								}
								MLBPlayer newPitcher = getPitcher(top==0?1:0, "HD", 0, excludingPitchers);
								if (newPitcher != null) {
									currentPitcherGameStats = changePitcher(newPitcher, gameState.getTop()==0?1:0, null);
									gameState.setSetupManIsPitching(true, top);
								}
						}
						else if (gameState.getInning() >= 9 && Math.abs(boxScores[1].getScore(9) - boxScores[0].getScore(9)) < 4 && !gameState.isCloserPitching(top) && ((top == 0 && 
							boxScores[1].getScore(9) > boxScores[0].getScore(9)) || top == 1 && (boxScores[0].getScore(9) > boxScores[1].getScore(9)))) { 
								MLBPlayer newPitcher = getPitcher(top==0?1:0, "SV", 0, excludingPitchers);
								if (newPitcher != null) {
									currentPitcherGameStats = changePitcher(newPitcher, gameState.getTop()==0?1:0, null);
									gameState.setCloserIsPitching(true, top);
									gameState.setSetupManIsPitching(false, top);
								}
						}
						else if (currentPitcherGameStats.getBattersFaced() > 30 || currentPitcherGameStats.getEarnedRunsAllowed() > 6 || 
							(currentPitcherGameStats.getEarnedRunsAllowed() > 3 && currentPitcherGameStats.getBattersFaced() > 20)) {
								// Exclude closer and set up man when getting mid/long reliever
								if (setupMen[top==0?1:0] != null) {
									excludingPitchers.add(setupMen[top==0?1:0].getMlbPlayerId());
								}
								if (closers[top==0?1:0] != null) {
									excludingPitchers.add(closers[top==0?1:0].getMlbPlayerId());
								}
								MLBPlayer newPitcher = getPitcher(top==0?1:0, "IP", getRandomNumberInRange(0, 3), excludingPitchers);
								if (newPitcher != null) {
									currentPitcherGameStats = changePitcher(newPitcher, gameState.getTop()==0?1:0, null);
								}
						}
						else if (gameState.getInning() > 9 && gameState.isCloserPitching(top) && gameState.getOuts() == 0) { // Check if we're in extra innings and closer is still pitching (Blown save)
							// Exclude closer and set up man when getting mid/long reliever
							if (setupMen[top==0?1:0] != null) {
								excludingPitchers.add(setupMen[top==0?1:0].getMlbPlayerId());
							}
							if (closers[top==0?1:0] != null) {
								excludingPitchers.add(closers[top==0?1:0].getMlbPlayerId());
							}
							MLBPlayer newPitcher = getPitcher(top==0?1:0, "IP", getRandomNumberInRange(0, 3), excludingPitchers);
							if (newPitcher != null) {
								currentPitcherGameStats = changePitcher(newPitcher, gameState.getTop()==0?1:0, null);
								gameState.setCloserIsPitching(false, top);
							}
						}
					}
					PitchingStats currentPitcherSeasonStats = currentPitcher != null ? getPitchersSeasonPitchingStats(rosters[top==0?1:0], currentPitcher.getMlbPlayerId()) : null;
					String runnerOnFirst = "1B:" + (gameState.getBaseRunnerId(1) == 0 ? "<>" : getPlayerFromId(gameState.getBaseRunnerId(1)).getFirstLastName() + 
						"(" + getPlayerFromId(gameState.getBaseRunnerId(1)).getMlbBattingStats().getBattingStats().getSpeedRating() + ")");
					String runnerOnSecond = "2B:" + (gameState.getBaseRunnerId(2) == 0 ? "<>" : getPlayerFromId(gameState.getBaseRunnerId(2)).getFirstLastName() + 
							"(" + getPlayerFromId(gameState.getBaseRunnerId(2)).getMlbBattingStats().getBattingStats().getSpeedRating() + ")");
					String runnerOnThird = "3B:" + (gameState.getBaseRunnerId(3) == 0 ? "<>" : getPlayerFromId(gameState.getBaseRunnerId(3)).getFirstLastName() + 
							"(" + getPlayerFromId(gameState.getBaseRunnerId(3)).getMlbBattingStats().getBattingStats().getSpeedRating() + ")");
					printlnToScreen(currentBatter.getFirstLastName() + " UP OUTS: " + gameState.getOuts() + " "  + gameState.getBaseSituations().get(gameState.getCurrentBasesSituation()) + 
						" (" + runnerOnFirst + " " + runnerOnSecond + " " + runnerOnThird + ")");	
					// Look for injury (.001 chance)
					if (getRandomNumberInRange(1, 1000) == 1) {
						int injuredPlayerIndex = getRandomNumberInRange(1, 10);
						MLBPlayer injuredPlayer = null;
						if (injuredPlayerIndex == 10) { // Batter
							injuredPlayer = currentBatter;
						}
						else if (injuredPlayerIndex == 9) { // Pitcher
							injuredPlayer = currentPitcher;
						}
						else { // 1 - 8
							ArrayList<ArrayList<MLBPlayer>> fielders = boxScores[top==0?1:0].getBatters();
							injuredPlayer = fielders.get(injuredPlayerIndex -1).get(fielders.get(injuredPlayerIndex -1).size() - 1);
						}
						disabledList.put(injuredPlayer.getFullName() + ":" + injuredPlayer.getMlbPlayerId() + ":" + displayTeamName(top, boxScores), new Integer(getRandomNumberInRange(1, 162)));
					}
					if (gameMode || (autoBeforeMode && inning >= autoBeforeInning)) {
						myObj = new Scanner(System.in);
						printToScreen("PITCH: ");
					    String command = myObj.nextLine();
					    if (currentPitcherGameStats == null && command.toUpperCase().indexOf("SUBP") == -1 && command.toUpperCase().indexOf("PITCH") == -1 && 
					    		command.toUpperCase().indexOf("LINEUP") == -1) { // Allow PITCHERS, PITCHCHECK, LINEUP and SUBP commands
					    	printToScreen("PITCHER MUST BE CHANGED! (SUBP)\n");
					    	continue;
					    }
					    if (!processCommand(command, currentPitcherGameStats, currentBatter)) {
					    	continue;
					    }
					}
					if (simulationMode || (autoBeforeMode && inning < autoBeforeInning)) { // Steal 2? Only in sim and autoBefore mode before auto inning
						if (isRunnerStealing(2)) {
							int sbOuts = stealBase(2);
							if (sbOuts > 0) {
								currentPitcherGameStats.incrementInningsPitchedBy(1);
								gameState.setOuts(gameState.getOuts() + sbOuts);
							}
						}
					}
					if (gameState.getOuts() == OUTS_PER_INNING) {
						gameState.setHitAndRun(false);  // clear hit and run, if on
						gameState.setInfieldIn(false);  // clear infield in, if on
						break;
					}
					int rando = getRandomNumberInRange(1, 1000);
					gameTiedStartOfAB = boxScores[1].getScore(inning) == boxScores[0].getScore(inning);
					long onBaseEndPoint = 1000 - Math.round(((currentBatterSeasonStats.getOnBasePercentage() + currentPitcherSeasonStats.getOnBasePercentage())/2.0)*1000);
					onBaseEndPoint -= currentPitcherGameStats.getBattersFaced(); // pitcher fatigue
					onBaseEndPoint += currentPitcher.getArmThrows().equals(currentBatter.getBats()) ? 10 : -10; // R v L adjust
					onBaseEndPoint -= gameState.isInfieldIn() ? 25 : 0;  // Infield in increases hit chances but can prevent run from 3rd scoring
					if (rando <= onBaseEndPoint) { // OUT
						int outResult = getOutResult(currentBatter, currentBatterSeasonStats, currentPitcherGameStats, currentPitcherSeasonStats);
						gameState.setOuts(gameState.getOuts() + outResult);
						currentBatterGameStats.incrementAtBats();
						currentPitcherGameStats.incrementInningsPitchedBy(outResult);
						// Did game end as result of a tag up or fielders choice?
						if (inning >= 9 && boxScores[1].getScore(inning) > boxScores[0].getScore(inning) && gameTiedStartOfAB) {
							boxScore.setWalkOff(true);
							printlnToScreen("WALKOFF ");
							break;
						}
					}
					else {
						long bbEndPoint = Math.round(((currentBatterSeasonStats.getWalkRate() + currentPitcherSeasonStats.getWalkRate())/2.0)*(1000 - onBaseEndPoint));
						if (rando < (bbEndPoint + onBaseEndPoint)) {
							printlnToScreen("WALKED");
							currentBatterGameStats.incrementWalks();
							currentPitcherGameStats.incrementWalks();
							updateBasesSituationNoRunnersAdvance(currentBatter);
						}
						else if (rando >= (bbEndPoint + onBaseEndPoint) && rando < ((bbEndPoint + onBaseEndPoint) + 10)) { // Hard coded HBP rate ~3%
							printlnToScreen("HIT BY PITCH");
							updateBasesSituationNoRunnersAdvance(currentBatter);
						}
						else { // HIT or ERROR
							int noOutResult = getNotOutResult(currentBatterGameStats, currentBatterSeasonStats, currentPitcherGameStats, currentPitcherSeasonStats);
							boolean reachedByError = false;
							if (noOutResult == 0) {
								reachedByError = true;
								gameState.incrementVirtualErrorOuts();
								gameState.getBaseRunnersReachedByError().add(currentBatter.getMlbPlayerId());
								noOutResult = 1;
							}
							if (noOutResult == 1 && (getRandomNumberInRange(0, 5) + currentBatterGameStats.getSpeedRating()) > 4) { // infield single/error?
								// Man on 2  with h + r or 2 outs, runner 2->3, otherwise same as a walk
								if ((gameState.isHitAndRun() || gameState.getOuts() == 2) && gameState.getCurrentBasesSituation() == GameState.MAN_ON_SECOND) {
									updateBasesSituationRunnersAdvance(noOutResult, currentBatter, reachedByError, true);
								}
								else {
									updateBasesSituationNoRunnersAdvance(currentBatter);
								}
								printlnToScreen("STAYED IN INFIELD");
							}
							else {  // hit into outfield
								updateBasesSituationRunnersAdvance(noOutResult, currentBatter, reachedByError, false);
							}
							currentBatterGameStats.incrementAtBats();
							if (!reachedByError) {
								boxScore.incrementHits();
								currentBatterGameStats.incrementHits();
								currentPitcherGameStats.incrementHitsAllowed();
							}
						}
						currentBatter.getMlbBattingStats().setBattingStats(currentBatterGameStats);
						if (inning >= 9 && boxScores[1].getScore(inning) > boxScores[0].getScore(inning) && gameTiedStartOfAB) {
							boxScore.setWalkOff(true);
							printlnToScreen("WALKOFF ");
							break;
						}
						boxScore.incrementBaseRunners();
					}
					currentPitcherGameStats.incrementBattersFaced();
					currentBatterGameStats.incrementPlateAppearances();
					gameState.incrementBattingOrder(top);
					gameState.setHitAndRun(false);  // clear hit and run, if on
					gameState.setInfieldIn(false);  // clear infield in, if on
				} // outs
				// Did game end after top of inning?
				if (inning >= 9 && boxScores[1].getScore(inning) > boxScores[0].getScore(inning) && top == 0) {
					printlnToScreen("GAME OVER AFTER " + (inning - 1) + " 1/2");
					break;
				}
			} // top/bottom
			if (allStarGameMode && gameState.getInning() == 9 && boxScores[0].getScore(inning) == boxScores[1].getScore(inning)) { // Tied all star game
				printlnToScreen("ALL STAR GAME ENDS IN A TIE AFTER 9 INNINGS " + boxScores[0].getScore(inning) + " to " + boxScores[1].getScore(inning));
				tiedAllStarGame= true;
				gameState.incrementInning();
				break;
			}
			gameState.incrementInning();
		} // inning
		if (!tiedAllStarGame) {
			printlnToScreen("GAME OVER! WP: " + getPlayerFromId(gameState.getPitchersOfRecord().get("W")).getFirstLastName() + " LP: " + getPlayerFromId(gameState.getPitchersOfRecord().get("L")).getFirstLastName());
			gameState.setGameStarted(false);
			int winningPitcherId = gameState.getPitchersOfRecord().get("W");
			// Were there extra innings?
			if (gameState.getInning() > (INNINGS_PER_GAME + 1)) {
				printlnToScreen("EXTRA INNINGS: " + (gameState.getInning() - 1));
			}
			int gameEndingVisPitcherId = gameState.getCurrentPitchers()[0].getMlbPlayerId();
			int gameEndingHomePitcherId = gameState.getCurrentPitchers()[1].getMlbPlayerId();
			// Make sure pitcher can't get both win and save
			if (gameState.getSaveOppty(0) && boxScores[0].getFinalScore() > boxScores[1].getFinalScore() && winningPitcherId != gameEndingVisPitcherId) {
				printlnToScreen("SAVE FOR " + gameState.getCurrentPitchers()[0].getFirstLastName());
				gameState.getCurrentPitchers()[0].getMlbPitchingStats().getPitchingStats().setSaves(1);
			} 
			else if (gameState.getSaveOppty(1) && boxScores[1].getFinalScore() > boxScores[0].getFinalScore() && winningPitcherId != gameEndingHomePitcherId) {
				printlnToScreen("SAVE FOR " + gameState.getCurrentPitchers()[1].getFirstLastName());
				gameState.getCurrentPitchers()[1].getMlbPitchingStats().getPitchingStats().setSaves(1);
			}
		}
		// Output Box Score
		if (!seasonSimulationMode && !seasonSimulationPlayoffsMode && !multiIterationTournament) {
			outputBoxScore(boxScores, false, gameState.getInning(), gameState.getPitchersOfRecord());
		}
		else {
			String gameMode = (seasonSimulationPlayoffsMode ? "Playoff" : "Season");
			if (tournamentMode) {
				gameMode = "Tournament";
			}
			System.out.print(boxScores[1].getYear() + " " + gameMode + " game #" + gameNumber + " " + boxScores[0].getTeam().getFullTeamName() + 
				" " + boxScores[0].getFinalScore() + " at " + boxScores[1].getTeam().getFullTeamName() + " " + boxScores[1].getFinalScore());
			MLBPlayer visStarter = boxScores[0].getPitchers().entrySet().iterator().next().getValue();
			MLBPlayer homeStarter = boxScores[1].getPitchers().entrySet().iterator().next().getValue();
			String specialGame = "";
			if (boxScores[0].getHits() == 0 || boxScores[1].getHits() == 0) {
				 specialGame = "NO HITTER!";
			}
			if (boxScores[0].getBaseRunners() == 0 || boxScores[1].getBaseRunners() == 0) {
				 specialGame = "PERFECT GAME!";
			}
			System.out.println(" ("  + visStarter.getFullName() + " v " + homeStarter.getFullName() + ") " + specialGame);
		}
		 
	  /*JSONObject bsJSON = new JSONObject(boxScores[0]);
		System.out.println(bsJSON);
		 
	  /*for (int i = 0; i < randoLog.size(); i++) {
			System.out.println(randoLog.get(i));
	    } */
	}
	
	private static void clearPlayerGameData(BoxScore bs) {
		for (int i = 0 ; i <= NUM_OF_PLAYERS_IN_LINEUP - 1; i++) {
			ArrayList<MLBPlayer> posPlayers = bs.getBatters().get(i);
			for (MLBPlayer p : posPlayers) {
				p.setMlbBattingStats(new MLBBattingStats());
			}
		}
	}
	
	private static int getNotOutResult(BattingStats batterGameStats, BattingStats batterSeasonStats, PitchingStats pitcherGameStats, PitchingStats pitcherSeasonStats) {
		long errorEndPoint = 25;
		long hrEndPoint = Math.round(((double)(batterSeasonStats.getHomeRunRate() + pitcherSeasonStats.getHomeRunsAllowedRate())/2.0)*1000) + errorEndPoint;
		hrEndPoint = batterSeasonStats.getHomeRuns() == 0 ? 40 : hrEndPoint; // If 0 home runs, lower HR chances (pitchers)
		long triplesEndPoint = Math.round((((double)batterSeasonStats.getTriples()/batterSeasonStats.getHits())*1000)) + hrEndPoint;
		triplesEndPoint = batterSeasonStats.getTriples() == 0 ? 8 + hrEndPoint : triplesEndPoint; // Give some chance if batter has 0 triples
		long doublesEndPoint = Math.round((((double)batterSeasonStats.getDoubles()/batterSeasonStats.getHits())*1000)) + triplesEndPoint;
		int notOutResult = 1;
		int notOutRando = getRandomNumberInRange(1, 1000);
		if (notOutRando > 1 && notOutRando <= errorEndPoint) {
			printlnToScreen("REACHED BY ERROR");
			notOutResult = 0;
			boxScores[gameState.getTop()==0?1:0].incrementErrors();
		}
		else if (notOutRando > errorEndPoint && notOutRando <= hrEndPoint) {
			printlnToScreen("HOME RUN");
			notOutResult = 4;
			batterGameStats.incrementHomeRuns();
			pitcherGameStats.incrementHomeRunsAllowed();
		}
		else if (notOutRando > hrEndPoint && notOutRando <= triplesEndPoint) {
			printlnToScreen("TRIPLE");
			notOutResult = 3;
			batterGameStats.incrementTriples();
		}
		else if (notOutRando > triplesEndPoint && notOutRando < doublesEndPoint) {
			printlnToScreen("DOUBLE");
			notOutResult = 2;
			batterGameStats.incrementDoubles();
		}
		else {
			printlnToScreen("SINGLE");
		}
		return notOutResult;
	}
	
	private static int getOutResult(MLBPlayer currentBatter, BattingStats batterSeasonStats, PitchingStats pitcherGameStats, PitchingStats pitcherSeasonStats) {
		int outsRecorded = 1;
		int notOutRando = getRandomNumberInRange(1, 100);
		long soEndPoint = Math.round(((pitcherSeasonStats.getStrikeoutRate()+batterSeasonStats.getStrikeoutRate())/2)*100);
		long outIncrement = Math.round((double)((100 - soEndPoint)/4));
		if (notOutRando > 1 && notOutRando <= soEndPoint) { // STRUCK OUT
			printlnToScreen(outTypes.get(STRUCK_OUT)); 
			currentBatter.getMlbBattingStats().getBattingStats().incremenStrikeOuts();
			pitcherGameStats.incrementStrikeouts();
			if (gameState.isHitAndRun() && gameState.getOuts() < 2) {
				int baseToSteal = gameState.isBaseOccupied(2) ? 3 : 2;
				if (stealBase(baseToSteal) == 1) {
					outsRecorded++;
				}
				// If less than 3 outs and 12 then 1 -> 2 trailing the attempted steal
				if ((gameState.getOuts() + outsRecorded) < 3 && gameState.getCurrentBasesSituation() == GameState.MAN_ON_FIRST_AND_SECOND) {
					gameState.setBaseRunner(2, gameState.getBaseRunner(1));
					gameState.setBaseRunner(1, new BaseRunner());
				}
			}
		}
		else if (notOutRando > soEndPoint && notOutRando <= soEndPoint + outIncrement) {
			String groundBallRecipientPosition = positions.get(getRandomNumberInRange(1, 6, 2));  // Excluding C for now
			printlnToScreen(outTypes.get(GROUNDED_OUT) + " TO " + groundBallRecipientPosition); //GROUNDED OUT
			if (isDoublePlay(true)) {
				outsRecorded++; // 2nd out
			}
			else {
				fieldersChoice(groundBallRecipientPosition, currentBatter);
			}
		}
		else if (notOutRando > soEndPoint + outIncrement && notOutRando <= soEndPoint + (outIncrement*2)) {  // Fly ball to outfield
			boolean deep = notOutRando < (soEndPoint + outIncrement*1.5); // 50/50 not deep/deep
			String outfielderPosition = positions.get(getRandomNumberInRange(7, 9));
			printlnToScreen(outTypes.get(deep ? FLEW_OUT_DEEP : FLEW_OUT) +  " TO " + outfielderPosition); // FLEW OUT
			if (gameState.getOuts() < 2) {
				if (simulationMode || (autoBeforeMode && gameState.getInning() < autoBeforeInning)) {
					if (gameState.isBaseOccupied(3)) {  // Only tag up if there is a runner on 3rd
						MLBPlayer runnerOnThird = getPlayerFromId(gameState.getBaseRunnerId(3));
						// Sac fly if in sim mode or autoBefore mode before sim mode stops, less than 2 outs, runner > 2 speed (if deep, no dependency on runners speed)
						if (deep || (!deep && runnerOnThird.getMlbBattingStats().getBattingStats().getSpeedRating() > 2)) {
							if (updateBasesSituationTagUp(3, runnerOnThird, deep, outfielderPosition, currentBatter) == 1) {
								outsRecorded++;
							}
						}
					}
				}
				else { // game mode
					Scanner myObj = null;
					for (int base = 1; base < 4; base++) {
						if ((base == 3 && gameState.isBaseOccupied(3)) ||
						    (base != 3 && gameState.isBaseOccupied(base) && !gameState.isBaseOccupied(base + 1))) { 
								MLBPlayer runnerOnAdvancing = getPlayerFromId(gameState.getBaseRunnerId(base));
								myObj = new Scanner(System.in);
								printToScreen("Tag up from " + base + "?");
								String command = myObj.nextLine();
								if (command != null && command.equalsIgnoreCase("Y")) {
									if (updateBasesSituationTagUp(base, runnerOnAdvancing, deep, outfielderPosition, currentBatter) == 1) {
										outsRecorded++;
									}
									break; // only allow 1 tag up per fly ball
								}
						}
					}
				}
			}
		}
		else if (notOutRando > soEndPoint + (outIncrement*2) && notOutRando < soEndPoint + (outIncrement*3)) {
			printlnToScreen(outTypes.get(LINED_OUT) + " TO " + positions.get(getRandomNumberInRange(1, 6, 2))); // LINED OUT
			if (isDoublePlay(false)) {
				outsRecorded++; // 2nd out
			}
		}
		else {
			printlnToScreen(outTypes.get(POPPED_OUT) + " TO " + positions.get(getRandomNumberInRange(1, 6))); // POPPED OUT
		}
		return outsRecorded;
	}
	
	private static void updateBasesSituationRunnersAdvance(int event, MLBPlayer currentBatter, boolean error, boolean infieldSingle) {
	  /*int basesSituation = (currentBasesSituation << event) + (int)Math.pow(2, (event - 1));
		if (basesSituation > 7) {
			basesSituation = basesSituation % 8;
		}*/
		// if hit and run and !3 and !4 and not bases empty treat like +1
		if (gameState.isHitAndRun() && gameState.getCurrentBasesSituation() != GameState.BASES_EMPTY && event < 3 && !infieldSingle) {
			event++;
		}
		MLBPlayer currentPitcher = gameState.getCurrentPitchers()[gameState.getTop()==0?1:0];
		for (int e = 1; e <= event; e++) {
			for (int base = 4; base >= 1; base--) {
				if (base > 1) {
					if (base != 4) {
						gameState.setBaseRunner(base, gameState.getBaseRunner(base - 1));
					}
				}
				else if (e == 1) {
					gameState.setBaseRunner(base, new BaseRunner(currentBatter.getMlbPlayerId(), currentPitcher.getMlbPlayerId()));
				}
				else {
					gameState.setBaseRunner(base, new BaseRunner());
				}
				if (base == 4 && gameState.isBaseOccupied(3)) {     // If runner on 3rd run scores
					runScores(!error);
				}
			}
		}
		// Backup base runner for hit and run
		if (gameState.isHitAndRun() && gameState.getCurrentBasesSituation() != GameState.BASES_EMPTY && event < 4 && !infieldSingle) {
			gameState.setBaseRunner(event - 1, gameState.getBaseRunner(event));
			gameState.setBaseRunner(event, new BaseRunner());
		}
	}
	
	// For walks, hit by pitch and some infield singles
	private static void updateBasesSituationNoRunnersAdvance(MLBPlayer currentBatter) { 
		MLBPlayer currentPitcher = gameState.getCurrentPitchers()[gameState.getTop()==0?1:0];
		// No need for checking 0, 2, 3, or 23
		if (gameState.getCurrentBasesSituation() == GameState.BASES_LOADED || gameState.getCurrentBasesSituation() == GameState.MAN_ON_FIRST_AND_SECOND) { // 123 or 12
			// if 123 runner 3 scores
			if (gameState.getCurrentBasesSituation() == GameState.BASES_LOADED) {
				runScores(true);
			}
			gameState.setBaseRunner(3, gameState.getBaseRunner(2));
		}
		if (gameState.isBaseOccupied(1)) { // Runner on first
			gameState.setBaseRunner(2, gameState.getBaseRunner(1));
		}
		gameState.setBaseRunner(1, new BaseRunner(currentBatter.getMlbPlayerId(), currentPitcher.getMlbPlayerId()));
	}
	
	private static void updateBasesSituationSacBunt(MLBPlayer currentBatter) { 
		PitchingStats currentPitcherGameStats = boxScores[gameState.getTop()==0?1:0].getPitchers().get(gameState.getCurrentPitchers()[gameState.getTop()==0?1:0].getMlbPlayerId()).getMlbPitchingStats().getPitchingStats();
		int buntRando = getRandomNumberInRange(0, 100);
		if (buntRando <= 80) { // 80 %
			printlnToScreen("SUCCESSFUL BUNT!");
			if (gameState.isBaseOccupied(2)) { // Runner on 2 or 12
				gameState.setBaseRunner(3, gameState.getBaseRunner(2));
				if (gameState.isBaseOccupied(1)) {  // If 12, 1->2
					gameState.setBaseRunner(2, gameState.getBaseRunner(1));
					gameState.setBaseRunner(1, new BaseRunner());
				}
				else {
					gameState.setBaseRunner(2, new BaseRunner()); // If 2, 2 is now empty
				}
			}
			else if (gameState.isBaseOccupied(1)) { // Runner on 1 or 13
				gameState.setBaseRunner(2, gameState.getBaseRunner(1));
				gameState.setBaseRunner(1, new BaseRunner());
			}
		}
		else if (buntRando > 80 && buntRando < 94) { // 13 %
			printlnToScreen("UNSUCCESSFUL BUNT!"); 
			fieldersChoice("P", currentBatter);
			currentBatter.getMlbBattingStats().getBattingStats().incrementAtBats();
		}
		else { // 7%
			printToScreen("UNSUCCESSFUL BUNT!");
			if (gameState.isBaseOccupied(1)) {
				printlnToScreen(" DOUBLE PLAY!");
				updateBasesSituationDoublePlayGround();
				gameState.incrementOuts();
				currentPitcherGameStats.incrementInningsPitchedBy(1);
			}
			else {
				printlnToScreen(null);
				fieldersChoice("P", currentBatter);
			}
			currentBatter.getMlbBattingStats().getBattingStats().incrementAtBats();
		}
		gameState.incrementOuts();
		currentPitcherGameStats.incrementInningsPitchedBy(1);
	}
	
	private static void updateBasesSituationSqueezeBunt(MLBPlayer currentBatter, boolean suicide) {
		MLBPlayer currentPitcher = gameState.getCurrentPitchers()[gameState.getTop()==0?1:0];
		PitchingStats currentPitcherGameStats = boxScores[gameState.getTop()==0?1:0].getPitchers().get(currentPitcher.getMlbPlayerId()).getMlbPitchingStats().getPitchingStats();
		int buntRando = getRandomNumberInRange(0, 100);
		int successEndPoint = suicide ? 55 : 45;  // Prob a little less for safety, but no DP risk
		successEndPoint += currentBatter.getMlbBattingStats().getBattingStats().getSpeedRating()*4;
		if (buntRando <= successEndPoint) { 
			printlnToScreen("SUCCESSFUL SQUEEZE BUNT! " + buntRando);
			runScores(true);
			if (gameState.isBaseOccupied(2)) { // Runner on 23
				gameState.setBaseRunner(3, gameState.getBaseRunner(2));
				gameState.setBaseRunner(2, new BaseRunner());
			}
			else { // Runner on 3
				gameState.setBaseRunner(3, new BaseRunner());
			}
			gameState.setBaseRunner(1, new BaseRunner(currentBatter.getMlbPlayerId(), currentPitcher.getMlbPlayerId()));
		}
		else if (buntRando > successEndPoint) { 
			printlnToScreen("UNSUCCESSFUL SQUEEZE! " + buntRando); 
			if (suicide) {
				printlnToScreen(getPlayerNameFromId(gameState.getBaseRunnerId(3)) + " OUT AT THE PLATE!");
				if (gameState.getCurrentBasesSituation() == GameState.MAN_ON_SECOND_AND_THIRD) {
					gameState.setBaseRunner(3, gameState.getBaseRunner(2)); // 2->3
					gameState.setBaseRunner(2, new BaseRunner()); 
				}
				else if (gameState.getCurrentBasesSituation() == GameState.MAN_ON_THIRD){
					gameState.setBaseRunner(3, new BaseRunner());
				}
				if (buntRando > 85) {  // strike out double play
					printlnToScreen(currentBatter.getFirstLastName() + " STRUCK OUT! DOUBLE PLAY!");
					currentBatter.getMlbBattingStats().getBattingStats().incremenStrikeOuts();
					currentPitcherGameStats.incrementStrikeouts();
					gameState.incrementOuts();
					currentPitcherGameStats.incrementInningsPitchedBy(1);
				}
				else { // Batter safe at first
					gameState.setBaseRunner(1, new BaseRunner(currentBatter.getMlbPlayerId(), currentPitcher.getMlbPlayerId()));
				}
			} else { // safety
				updateBasesSituationFieldersChoice(currentBatter, "P");
			}
			currentBatter.getMlbBattingStats().getBattingStats().incrementAtBats();
			gameState.incrementOuts();
			currentPitcherGameStats.incrementInningsPitchedBy(1);
		}
	}
	
	private static int updateBasesSituationTagUp(int fromBase, MLBPlayer runnerAdvancing, boolean deep, String outfielderPosition, MLBPlayer currentBatter) {
		int outAdvancing = 0;
		MLBPlayer outfielder = null;
		int outfielderArmRating = 0;
		int sacRando = getRandomNumberInRange(0, 5) + runnerAdvancing.getMlbBattingStats().getBattingStats().getSpeedRating();
		if (!deep) {
			outfielder = getBoxScorePlayerFromPosition(outfielderPosition, gameState.getTop()==0?1:0);
			outfielderArmRating = getOutfielderArmRating(outfielder, boxScores[gameState.getTop()==0?1:0].getYear());
			sacRando -= outfielderArmRating;
		}
		if (deep && fromBase == 3) { // Tagging on deep FB from third should be almost a sure thing
			sacRando += 2;
		}
		if (!deep && fromBase != 3) { // Not deep 1 or 2
			sacRando -= 4;
		}
		if (deep && fromBase != 3) { // Deep 1 or 2
			sacRando -= 2;
		}
		printlnToScreen(runnerAdvancing.getFirstLastName() + " TAGGING UP ON A FLY BALL FROM " + fromBase);
		if (sacRando >= 3) { // safe
			printlnToScreen("SAFE!");
			if (fromBase == 3) {
				currentBatter.getMlbBattingStats().getBattingStats().decrementAtBats();
				runScores(true);
			}
			else {
				gameState.setBaseRunner(fromBase + 1, gameState.getBaseRunner(fromBase));
			}
		}
		else { // out
			outAdvancing = 1;
			printlnToScreen("OUT!");
		}
		gameState.setBaseRunner(fromBase, new BaseRunner());
		return outAdvancing;
	}
	
	private static void updateBasesSituationDoublePlayGround() {
		if ((gameState.getCurrentBasesSituation() == GameState.BASES_LOADED || gameState.getCurrentBasesSituation() == GameState.MAN_ON_FIRST_AND_THIRD) && gameState.getOuts() == 0) {
			runScores(false);
		}
		if (gameState.getCurrentBasesSituation() == GameState.MAN_ON_FIRST_AND_THIRD) {
			gameState.setBaseRunner(3, new BaseRunner());
		}
		else if (gameState.getCurrentBasesSituation() == GameState.MAN_ON_FIRST_AND_SECOND || gameState.getCurrentBasesSituation() == GameState.BASES_LOADED) {
			gameState.setBaseRunner(3, gameState.getBaseRunner(2));
		}
		gameState.setBaseRunner(2, new BaseRunner());
		gameState.setBaseRunner(1, new BaseRunner());
	}
	
	private static void updateBasesSituationDoublePlayNonGround() {
		// Limited to the hit and run scenarios of 13, 1, 12, 2
		if (gameState.getCurrentBasesSituation() == GameState.MAN_ON_FIRST_AND_THIRD || gameState.getCurrentBasesSituation() == GameState.MAN_ON_FIRST) {
			gameState.setBaseRunner(1, new BaseRunner()); // Man on first doubled off
		}
		else if (gameState.getCurrentBasesSituation() == GameState.MAN_ON_FIRST_AND_SECOND || gameState.getCurrentBasesSituation() == GameState.MAN_ON_SECOND) {
			gameState.setBaseRunner(2, new BaseRunner()); // Man on second doubled off
		}
	}
	
	private static void updateBasesSituationFieldersChoice(MLBPlayer currentBatter, String groundBallRecipientPosition) {
		MLBPlayer currentPitcher = gameState.getCurrentPitchers()[gameState.getTop()==0?1:0];
		if (gameState.getCurrentBasesSituation() == GameState.BASES_LOADED) {  // Loaded
			gameState.setBaseRunner(3, gameState.getBaseRunner(2));
			if (gameState.isInfieldIn() || (groundBallRecipientPosition.equals("1B") || groundBallRecipientPosition.equals("3B") || groundBallRecipientPosition.equals("P"))) {
				gameState.setBaseRunner(2, gameState.getBaseRunner(1));
				printlnToScreen(getPlayerNameFromId(gameState.getBaseRunnerId(3)) + " OUT AT THE PLATE!");
			}
			else {  // IF back and GB to SS or 2B
				runScores(true);
				gameState.setBaseRunner(2, new BaseRunner());
				printlnToScreen(getPlayerNameFromId(gameState.getBaseRunnerId(1)) + " OUT AT THE SECOND!");
			}
			gameState.setBaseRunner(1, new BaseRunner(currentBatter.getMlbPlayerId(), currentPitcher.getMlbPlayerId()));
		}
		else if (gameState.getCurrentBasesSituation() == GameState.MAN_ON_SECOND_AND_THIRD) { // 23
			if (!gameState.isInfieldIn() && (groundBallRecipientPosition.equals("SS") || groundBallRecipientPosition.equals("2B"))) { // Both runners hold if infield in or 1, 3 or P
				runScores(true);
				if (groundBallRecipientPosition.equals("2B")) {  // 2B runner 2->3
					gameState.setBaseRunner(3, gameState.getBaseRunner(2));
				}
				else {
					gameState.setBaseRunner(3, new BaseRunner()); // SS 2 holds
				}
			}
		}	
		else if (gameState.getCurrentBasesSituation() == GameState.MAN_ON_THIRD) { // 3
			// Run scores if IF back and GB to SS or 2B
			if (!gameState.isInfieldIn() && (groundBallRecipientPosition.equals("SS") || groundBallRecipientPosition.equals("2B"))) {
				runScores(true);
				gameState.setBaseRunner(3, new BaseRunner());
			}
		}
		else if (gameState.getCurrentBasesSituation() == GameState.MAN_ON_SECOND) { // 2
			// Runner advances 2->3 for GB to right and batter out at 1
			if (groundBallRecipientPosition.equals("2B") || groundBallRecipientPosition.equals("1B")) {
				gameState.setBaseRunner(3, gameState.getBaseRunner(2));
				gameState.setBaseRunner(2, new BaseRunner());
			}
			// GB to left and hit and run - batter safe and runner out at 3
			else if (gameState.isHitAndRun() && (groundBallRecipientPosition.equals("3B") || groundBallRecipientPosition.equals("SS") || groundBallRecipientPosition.equals("P"))) {
				printlnToScreen(getPlayerNameFromId(gameState.getBaseRunnerId(2)) + " OUT AT THIRD!");
				gameState.setBaseRunner(1, new BaseRunner(currentBatter.getMlbPlayerId(), currentPitcher.getMlbPlayerId()));
				gameState.setBaseRunner(2, new BaseRunner());
			}
		}
		else if (gameState.getCurrentBasesSituation() == GameState.MAN_ON_FIRST_AND_THIRD) { // 13 (H+R assumes only runner on 1 is running)
			if (gameState.isHitAndRun()) {
				printlnToScreen(getPlayerNameFromId(currentBatter.getMlbPlayerId()) + " OUT AT FIRST!");
				gameState.setBaseRunner(2, gameState.getBaseRunner(1));
				gameState.setBaseRunner(1, new BaseRunner());
			}
			else {
				printlnToScreen(getPlayerNameFromId(gameState.getBaseRunnerId(1)) + " OUT AT SECOND!");
				gameState.setBaseRunner(1, new BaseRunner(currentBatter.getMlbPlayerId(), currentPitcher.getMlbPlayerId()));
			}
			// Run scores if IF back and GB to SS or 2B
			if (!gameState.isInfieldIn() && (groundBallRecipientPosition.equals("SS") || groundBallRecipientPosition.equals("2B"))) {
				runScores(true);
				gameState.setBaseRunner(3, new BaseRunner());
			}
		}
		else if (gameState.getCurrentBasesSituation() == GameState.MAN_ON_FIRST_AND_SECOND) { // 12
			if (gameState.isHitAndRun()) {
				printlnToScreen(getPlayerNameFromId(currentBatter.getMlbPlayerId()) + " OUT AT FIRST!");
				gameState.setBaseRunner(3, gameState.getBaseRunner(2));
				gameState.setBaseRunner(2, gameState.getBaseRunner(1));
				gameState.setBaseRunner(1, new BaseRunner());
			}
			else {
				printlnToScreen(getPlayerNameFromId(gameState.getBaseRunnerId(2)) + " OUT AT THIRD!");
				gameState.setBaseRunner(2, gameState.getBaseRunner(1));
				gameState.setBaseRunner(1, new BaseRunner(currentBatter.getMlbPlayerId(), currentPitcher.getMlbPlayerId()));
			}
		}
		else if (gameState.getCurrentBasesSituation() == GameState.MAN_ON_FIRST) { // 1
			if (gameState.isHitAndRun()) {
				printlnToScreen(getPlayerNameFromId(currentBatter.getMlbPlayerId()) + " OUT AT FIRST!");
				gameState.setBaseRunner(2, gameState.getBaseRunner(1));
				gameState.setBaseRunner(1, new BaseRunner());
			}
			else {
				printlnToScreen(getPlayerNameFromId(gameState.getBaseRunnerId(1)) + " OUT AT SECOND!");
				gameState.setBaseRunner(1, new BaseRunner(currentBatter.getMlbPlayerId(), currentPitcher.getMlbPlayerId()));
			}
		}
	}
	
	private static boolean isDoublePlay(boolean ground) {
		boolean dp = false;
		if (ground) { // GROUND DP
			if (gameState.isBaseOccupied(1) && gameState.getOuts() < 2) { // less than 2 outs and runner on 1st
				MLBPlayer runnerOnFirst =  getPlayerFromId(gameState.getBaseRunnerId(1));
				BattingStats bs = runnerOnFirst.getMlbBattingStats().getBattingStats();
				int dpRando = getRandomNumberInRange(0, 5) + bs.getSpeedRating();
				dpRando += gameState.isHitAndRun() ? 3 : 0; // hit and run lessens chance of a DP
				// Ground ball, less than 2 outs, man on first
				if (dpRando < 5) {
					dp = true;
					printlnToScreen("DOUBLE PLAY");
					updateBasesSituationDoublePlayGround();
				}
			}
		}
		else { // NON-GROUND DP
			if (gameState.isValidHitAnRunScenario() && gameState.isHitAndRun() && gameState.getOuts() < 2) {
				dp = true;
				printlnToScreen("DOUBLE PLAY");
				updateBasesSituationDoublePlayNonGround();
			}
		}
		return dp;
	}
	
	private static void fieldersChoice(String groundBallRecipientPosition, MLBPlayer currentBatter) {
		if (gameState.getCurrentBasesSituation() == GameState.BASES_EMPTY || gameState.getOuts() == 2) {
				return; // No FC
		}
		else {
			printlnToScreen("FIELDER'S CHOICE");
			updateBasesSituationFieldersChoice(currentBatter, groundBallRecipientPosition);
		}
	}
	
	private static void runScores(boolean rbi) {
		boolean gameWasTied = boxScores[0].getScore(gameState.getInning()) == boxScores[1].getScore(gameState.getInning());
		BoxScore boxScore = boxScores[gameState.getTop()];
		MLBPlayer runner = getBoxScoreBatterFromId(gameState.getBaseRunnerId(3));
		int responsiblePitcherId = gameState.getBaseRunner(3).getResponsiblePitcherId();
		PitchingStats pitcherGameStats = boxScores[gameState.getTop()==0?1:0].getPitchers().get(responsiblePitcherId).getMlbPitchingStats().getPitchingStats();
		runner.getMlbBattingStats().getBattingStats().incrementRuns();
		pitcherGameStats.incrementRunsAllowed();
		if (gameState.getBaseRunnersReachedByError().contains(runner.getMlbPlayerId())) {
			gameState.getBaseRunnersReachedByError().remove(runner.getMlbPlayerId());
		}
		else {
			if ((gameState.getOuts() + gameState.getVirtualErrorOuts()) < 3) {
				pitcherGameStats.incrementEarnedRunsAllowed();
			}	
		}
		boxScore.incrementRunsScored(gameState.getInning()); // run scores
		if (gameWasTied) { // Lead change
			gameState.setPitcherOfRecord("L", responsiblePitcherId);
			if (gameState.getInning() > 5) {
				gameState.setPitcherOfRecord("W", gameState.getCurrentPitchers()[gameState.getTop()].getMlbPlayerId());
			}
		}
		if (boxScores[0].getScore(gameState.getInning()) == boxScores[1].getScore(gameState.getInning()) && gameState.getSaveOppty(gameState.getTop()==0?1:0)) { // Game is tied in a save situation, blown save
			gameState.getCurrentPitchers()[gameState.getTop()==0?1:0].getMlbPitchingStats().getPitchingStats().setBlownSaves(1);
		}
		if (rbi) {
			MLBPlayer currentBatter = boxScore.getBatters().get(gameState.getBattingOrder()[gameState.getTop()] - 1).get(boxScore.getBatters().get(gameState.getBattingOrder()[gameState.getTop()] - 1).size() - 1);
			currentBatter.getMlbBattingStats().getBattingStats().incrementRbis();
		}
		printlnToScreen("RUN SCORES - " + displayTeamName(0, boxScores) + ": " + boxScores[0].getScore(gameState.getInning())  + " " + 
			displayTeamName(1, boxScores) + ": " + boxScores[1].getScore(gameState.getInning()));
	}
	
	private static boolean isRunnerStealing(int baseToSteal) {
		boolean runnerIsStealing = false;
		int fromBase = baseToSteal - 1;
		if (!gameState.isBaseOccupied(fromBase) || (baseToSteal != 4 && gameState.isBaseOccupied(baseToSteal))) {
			return false;
		}
		MLBPlayer runnerStealingPlayer = getPlayerFromId(gameState.getBaseRunnerId(fromBase));
		int stealRando = getRandomNumberInRange(1, 100);
		if ((baseToSteal < 4 && stealRando < runnerStealingPlayer.getMlbBattingStats().getBattingStats().getSpeedRating()*5) || (baseToSteal == 4 && stealRando > 95)) { // Rarely try to steal home
			runnerIsStealing = true;
		}
		return runnerIsStealing;
	}
	
	private static int stealBase(int baseToSteal) {
		int outStealing = 0;
		int fromBase = baseToSteal - 1;  
		MLBPlayer runnerStealingPlayer = getPlayerFromId(gameState.getBaseRunnerId(fromBase));
		BattingStats bs = runnerStealingPlayer.getMlbBattingStats().getBattingStats(); 
		double stealPctg = ((bs.getStolenBases() + bs.getCaughtStealing()) != 0) ? (double)bs.getStolenBases()/(bs.getStolenBases() + bs.getCaughtStealing()) : 0.2;  // Give a chance if no SB
		printToScreen(runnerStealingPlayer.getFirstLastName() + " ATTEMPTING TO STEAL " + baseToSteal + " - SR: " + bs.getSpeedRating() + " SP: " + df.format(stealPctg));
		int safeOutStealRando = getRandomNumberInRange(1, 10);
		BattingStats boxScoreRunnerStats = getBoxScoreBatterFromId(runnerStealingPlayer.getMlbPlayerId()).getMlbBattingStats().getBattingStats();
		if ((baseToSteal < 4 && safeOutStealRando < Math.round(stealPctg*10)) || (baseToSteal < 4 && safeOutStealRando == 10)) { // safe/out - rarely stealing home is safe
			printlnToScreen("- SAFE!");
			boxScoreRunnerStats.incrementStolenBases();
			if (baseToSteal == 4) {
				runScores(false);
			}
			else {
				gameState.setBaseRunner(baseToSteal, gameState.getBaseRunner(fromBase));
			}
		}
		else { // out
			printlnToScreen("- OUT!");
			boxScoreRunnerStats.incrementCaughtStealing();
			outStealing = 1;
		}
		gameState.setBaseRunner(fromBase, new BaseRunner());
		printlnToScreen(gameState.getBaseSituations().get(gameState.getCurrentBasesSituation()) + " (" + getPlayerNameFromId(gameState.getBaseRunnerId(1)) + 
			":" + getPlayerNameFromId(gameState.getBaseRunnerId(2)) + ":" + getPlayerNameFromId(gameState.getBaseRunnerId(3)) + ")");
		return outStealing;
	}
	
	private static int getRandomNumberInRange(int min, int max) {
		if (min >= max) {
			throw new IllegalArgumentException("max must be greater than min");
		}
		int rando = (int)(Math.random()*((max - min) + 1)) + min;
		randoLog.add(rando + " " + min + " to " + max);
		return rando;
	}
	
	private static int getRandomNumberInRange(int min, int max, int excluding) {
		if (min >= max) {
			throw new IllegalArgumentException("max must be greater than min");
		}
		int rando = (int)(Math.random()*((max - min) + 1)) + min;
		if (rando == excluding) {
			rando = getRandomNumberInRange(min, max, excluding);
		}
		randoLog.add(rando + " " + min + " to " + max + " ex: " + excluding);
		return rando;
	}
	
	public static HashMap<Integer, MLBPlayer> sortHashMapByValue(HashMap<Integer, MLBPlayer> hm, String type) 
    { 
        // Create a list from elements of HashMap 
        List<Map.Entry<Integer, MLBPlayer>> list = new LinkedList<Map.Entry<Integer, MLBPlayer>>(hm.entrySet()); 
        Collections.sort(list, new Comparator<Map.Entry<Integer, MLBPlayer>>() { 
            public int compare(Map.Entry<Integer, MLBPlayer> o1,  
                               Map.Entry<Integer, MLBPlayer> o2) { 
            	if (type.equals("SB")) {
            		return (o1.getValue().getMlbBattingStats().getBattingStats().getStolenBases() >= o2.getValue().getMlbBattingStats().getBattingStats().getStolenBases() ? -1 : 1); 
            	}
            	else if (type.equals("H")){
            		if (o1.getValue().getMlbBattingStats().getBattingStats().getHits() == o2.getValue().getMlbBattingStats().getBattingStats().getHits()) {
            			return 0;
            		}
            		return (o1.getValue().getMlbBattingStats().getBattingStats().getHits() > o2.getValue().getMlbBattingStats().getBattingStats().getHits() ? -1 : 1);
            	}
            	else if (type.equals("HR")){
            		if (o1.getValue().getMlbBattingStats().getBattingStats().getHomeRuns() == o2.getValue().getMlbBattingStats().getBattingStats().getHomeRuns()) {
            			return 0;
            		}
            		return (o1.getValue().getMlbBattingStats().getBattingStats().getHomeRuns() > o2.getValue().getMlbBattingStats().getBattingStats().getHomeRuns() ? -1 : 1);
            	}
            	else if (type.equals("RBI")){
            		if (o1.getValue().getMlbBattingStats().getBattingStats().getRbis() == o2.getValue().getMlbBattingStats().getBattingStats().getRbis()) {
            			return 0;
            		}
            		return (o1.getValue().getMlbBattingStats().getBattingStats().getRbis() > o2.getValue().getMlbBattingStats().getBattingStats().getRbis() ? -1 : 1);
            	}
            	else if (type.equals("PA")){
            		if (o1.getValue().getMlbBattingStats().getBattingStats().getPlateAppearances() == o2.getValue().getMlbBattingStats().getBattingStats().getPlateAppearances()) {
            			return 0;
            		}
            		return (o1.getValue().getMlbBattingStats().getBattingStats().getPlateAppearances() > o2.getValue().getMlbBattingStats().getBattingStats().getPlateAppearances() ? -1 : 1);
            	}
            	else if (type.equals("AVG")){
            		int ab1 = o1.getValue().getMlbBattingStats().getBattingStats().getAtBats();
            		int ab2 = o2.getValue().getMlbBattingStats().getBattingStats().getAtBats();
            		double avg1 = ab1 != 0 ? ((double)o1.getValue().getMlbBattingStats().getBattingStats().getHits() / ab1) : 0.0;
            		double avg2 = ab2 != 0 ? ((double)o2.getValue().getMlbBattingStats().getBattingStats().getHits() / ab2) : 0.0;
            		if (avg1 == avg2) {
            			return 0;
            		}
            		return (avg1 > avg2 ? -1 : 1);
            	}
            	else if (type.equals("GS")){
            		// Use innings pitched as next sort criteria if tied
            		if (o1.getValue().getMlbPitchingStats().getPitchingStats().getGamesStarted() == o2.getValue().getMlbPitchingStats().getPitchingStats().getGamesStarted()) {
            			return (o1.getValue().getMlbPitchingStats().getPitchingStats().getInningsPitched() >= o2.getValue().getMlbPitchingStats().getPitchingStats().getInningsPitched() ? -1 : 1);
            		}
            		return (o1.getValue().getMlbPitchingStats().getPitchingStats().getGamesStarted() > o2.getValue().getMlbPitchingStats().getPitchingStats().getGamesStarted() ? -1 : 1);
            	}
            	else if (type.equals("SV")){
            		if (o1.getValue().getMlbPitchingStats().getPitchingStats().getSaves() == o2.getValue().getMlbPitchingStats().getPitchingStats().getSaves()) {
            			return 0;
            		}
            		return (o1.getValue().getMlbPitchingStats().getPitchingStats().getSaves() > o2.getValue().getMlbPitchingStats().getPitchingStats().getSaves() ? -1 : 1);
            	}
            	else if (type.equals("HD")){
            		if (o1.getValue().getMlbPitchingStats().getPitchingStats().getHolds() == o2.getValue().getMlbPitchingStats().getPitchingStats().getHolds()) {
            			return 0;
            		}
            		return (o1.getValue().getMlbPitchingStats().getPitchingStats().getHolds() > o2.getValue().getMlbPitchingStats().getPitchingStats().getHolds() ? -1 : 1);
            	}
            	else if (type.equals("IP")){
            		if (o1.getValue().getMlbPitchingStats().getPitchingStats().getInningsPitched() == o2.getValue().getMlbPitchingStats().getPitchingStats().getInningsPitched()) {
            			return 0;
            		}
            		return (o1.getValue().getMlbPitchingStats().getPitchingStats().getInningsPitched() > o2.getValue().getMlbPitchingStats().getPitchingStats().getInningsPitched() ? -1 : 1);
            	}
            	else {
            		return 0;
            	}
            } 
        });  
        // put data from sorted list to hashmap  
        HashMap<Integer, MLBPlayer> temp = new LinkedHashMap<Integer, MLBPlayer>(); 
        for (Map.Entry<Integer, MLBPlayer> aa : list) { 
            temp.put(aa.getKey(), aa.getValue()); 
        } 
        return temp; 
    }
	
	// Lineup is home/vis->batting order (1-9) -> List of Players
	private static ArrayList<ArrayList<ArrayList<MLBPlayer>>> setOptimalBattingLineup(MLBTeam[] teams, int[] years) {
		ArrayList<ArrayList<ArrayList<MLBPlayer>>> batters = new ArrayList<ArrayList<ArrayList<MLBPlayer>>>();
		ArrayList<Integer> playersInLineupList;
		HashMap<Integer, MLBPlayer> battingStatsSortedByStatMap;
		ArrayList<String> positionsUsed;
		String statType = "";
		MLBPlayer player;
		boolean useFieldingStats = false;
		for (int t = 0; t < 2; t++) {
			int nextOFPositionNeededIndex = 7;
			int ofCount = 0;
			positionsUsed = new ArrayList<String>();
			playersInLineupList = new ArrayList<Integer>();
			batters.add(new ArrayList<ArrayList<MLBPlayer>>());
			useFieldingStats = years[t] >= 1999;
			// Get random starter 1-5
			for (int i = 1 ; i <= NUM_OF_PLAYERS_IN_LINEUP; i++) {  // 1 - 9
				batters.get(t).add(new ArrayList<MLBPlayer>());
				if ((!useDH || !useFieldingStats) && i == NUM_OF_PLAYERS_IN_LINEUP) {
					break;
				}
				if (i == 1) { 
					statType = "SB";
				}
				else if (i == 3) {
					statType = "RBI";
				}
				else if (i == 4) {
					statType = "HR";
				}
				else {
					statType = "H";
				}
				battingStatsSortedByStatMap = sortHashMapByValue(rosters[t].getBatters(), statType);
				int index = 0;
				for (Map.Entry<Integer, MLBPlayer> sortedPlayer : battingStatsSortedByStatMap.entrySet()) {
					player = sortedPlayer.getValue();
					// Skip players who were primarily on another team
					if (battersOnMultTeams.get(t).containsKey(player.getMlbPlayerId()) && teams[t].getTeamId() != battersOnMultTeams.get(t).get(player.getMlbPlayerId())) {
						index++;
						continue;
					}
					String playerPosition;
					if ((player.getMlbFieldingStats() == null || player.getMlbFieldingStats().isEmpty()) && useDH && useFieldingStats) { // DH
						playerPosition = "DH";
					}
					else {
						playerPosition = player.getPrimaryPositionByFieldingStats();
						if (playerPosition.equals("DH")) {
							index++;
							continue;
						}
						if (playerPosition.equals("OF")) {
							if (ofCount == 3) {
								index++;
								continue;
							}
							playerPosition = positions.get(nextOFPositionNeededIndex);
						}
					}
					boolean positionNeeded = !positionsUsed.contains(playerPosition);
					if (!useDH && playerPosition.equals("DH")) { // Can't need a DH if useDH is false
						positionNeeded = false;
					}
					if (!positionNeeded && player.getMlbFieldingStats() != null) { // Check if player played other positions that are needed
						for (MLBFieldingStats fs : player.getMlbFieldingStats()) {
							if (!positionsUsed.contains(fs.getPosition()) && !playersInLineupList.contains(sortedPlayer.getValue().getMlbPlayerId())) {
								positionNeeded = true;
								playerPosition = fs.getPosition();
							}
						}
					}
					if (!playersInLineupList.contains(sortedPlayer.getValue().getMlbPlayerId()) && positionNeeded) {  // Not already in lineup, save P for end
						playersInLineupList.add(sortedPlayer.getValue().getMlbPlayerId());
						batters.get(t).get(i-1).add(new MLBPlayer(player.getMlbPlayerId(), player.getFullName(), playerPosition, player.getArmThrows(), player.getBats(), 
							player.getJerseyNumber(), player.getSeasonStarted(), player.getMlbFieldingStats()));
						positionsUsed.add(playerPosition);
						if (Arrays.asList(MLBFieldingStats.outfieldPositions).contains(playerPosition)) {
							nextOFPositionNeededIndex++;
							ofCount++;
						}
						break;
					}
					index++;
				}
				if (index >= rosters[t].getBatters().size()) {  // Check if player is missing 1-8
					ArrayList<String> missingPositions = getMissingPositionFromLineup(positionsUsed);
					if (missingPositions.size() == 1) { // Missing one player
						MLBPlayer missingPlayer = getOneMorePlayerToFillOutLineup(missingPositions.get(0), battingStatsSortedByStatMap, playersInLineupList, useFieldingStats);
						if (missingPlayer != null) {
							playersInLineupList.add(missingPlayer.getMlbPlayerId());
							batters.get(t).get(i-1).add(new MLBPlayer(missingPlayer.getMlbPlayerId(), missingPlayer.getFullName(), missingPositions.get(0), missingPlayer.getArmThrows(),
								missingPlayer.getBats(), missingPlayer.getJerseyNumber(), missingPlayer.getSeasonStarted(), missingPlayer.getMlbFieldingStats()));
							positionsUsed.add(missingPositions.get(0));
						}
					}
					if (positionsUsed.size() < 8) { // Still not enough players in lineup
						if (years[t] == 1980 && teams[t].getShortTeamName().equals("PHI")) {
							// Very specific manual override needed to run 1980 World Series
							// Problem is Pete Rose is listed as a 3B in DB but he played 1B in 1980
							// Cant change his position in DB as it will break '76 Reds
							// Manually insert Mike Schmidt at 1B batting 8th
							String firstBase = positions.get(3);
							MLBPlayer mikeSchmidt = rosters[t].getBatters().get(121836);
							playersInLineupList.add(mikeSchmidt.getMlbPlayerId());
							batters.get(t).get(7).add(new MLBPlayer(mikeSchmidt.getMlbPlayerId(), mikeSchmidt.getFullName(), firstBase, mikeSchmidt.getArmThrows(), mikeSchmidt.getBats(), 
								mikeSchmidt.getJerseyNumber(), null, mikeSchmidt.getMlbFieldingStats()));
							positionsUsed.add(firstBase);  // 1B
							
						}
						else {
							System.out.println("Can not create a lineup for the " + years[t] + " " + teams[t].getFullTeamName() + " with players:");
							for (Map.Entry<Integer, MLBPlayer> mapElement : rosters[t].getBatters().entrySet()) {
								System.out.println(mapElement.getValue().getFullName() + "<" + mapElement.getValue().getMlbPlayerId() + "> " + mapElement.getValue().getPrimaryPositionByFieldingStats());
							}
						}
					}
					//return batters;
				}
			}
			//batters.get(t).add(new ArrayList<MLBPlayer>()); // For DH/P in 9
			if (useDH && !positionsUsed.contains("DH")) { // Set DH at ninth if no DH already set
				player =  getMlbPlayerWithMostPlateAppearances(teams[t].getTeamId(), years[t], playersInLineupList, t, battersOnMultTeams.get(t));
				player.setPrimaryPosition("DH");
				batters.get(t).get(8).add(new MLBPlayer(player.getMlbPlayerId(), player.getFullName(), player.getPrimaryPosition(), player.getArmThrows(), player.getBats(), 
					player.getJerseyNumber(), player.getSeasonStarted()));
			}
			printlnToScreen(null);
		}
		return batters;
	}
	
	/*private static boolean setRandomLineup() {
		ArrayList<ArrayList<MLBPlayer>> batters;
		BoxScore boxScore;
		MLBPlayer mlbPlayer;
		int pitcherDHLineupPosition = 1;
		for (int t = 0 ; t < 2; t++) {
			boxScore = boxScores[t];
			batters = boxScore.getBatters();
			// Get random starter 1-5
			MLBPlayer startingPitcher = DAO.getStartingPitcherByIndex((Integer)franchisesMap.get(boxScore.getTeam().getShortTeamName()), boxScore.getYear(), getRandomNumberInRange(1, 5));  
			gameState.getCurrentPitchers()[t] = new MLBPlayer(startingPitcher.getFullName(), startingPitcher.getMlbPlayerId(), "P");
			boxScore.getPitchers().put(startingPitcher.getMlbPlayerId(), new MLBPlayer(startingPitcher.getFullName(), startingPitcher.getMlbPlayerId(), "P"));
			ArrayList<Integer> randomLineup = getRandomLineupByPosition();
			ArrayList<Integer> outfielderIdList = new ArrayList<>();
			ArrayList<Integer> battersPlayerIdList = new ArrayList<>();
			for (int p = 0 ; p < NUM_OF_PLAYERS_IN_LINEUP; p++) {
				Integer position = randomLineup.get(p);
				if (!positions.get(position).equals("P")) {
					mlbPlayer = DAO.getMlbPlayerWithMostPlateAppearances((Integer)franchisesMap.get(boxScore.getTeam().getShortTeamName()), boxScore.getYear(), positions.get(position));
					if (mlbPlayer != null && mlbPlayer.getMlbPlayerId() != null) {
						batters.get(p).add(new MLBPlayer(mlbPlayer.getFullName(), mlbPlayer.getMlbPlayerId(), positions.get(position)));
						battersPlayerIdList.add(mlbPlayer.getMlbPlayerId());
					}
					else {
						// No specific OF positions before 1987
						if (positions.get(position).equals("LF") || positions.get(position).equals("CF") || positions.get(position).equals("RF")) {
							mlbPlayer = DAO.getMlbPlayerWithMostPlateAppearances((Integer)franchisesMap.get(boxScore.getTeam().getShortTeamName()), boxScore.getYear(), "OF", outfielderIdList);
							if (mlbPlayer != null && mlbPlayer.getMlbPlayerId() != null) {
								batters.get(p).add(new MLBPlayer(mlbPlayer.getFullName(), mlbPlayer.getMlbPlayerId(), positions.get(position)));
								outfielderIdList.add(mlbPlayer.getMlbPlayerId());
								battersPlayerIdList.add(mlbPlayer.getMlbPlayerId());
							}
							else {
								System.out.println("No players at: OF for " + boxScore.getTeam().getShortTeamName());
								return false;
							}
						}
						else {
							System.out.println("No players at: " + positions.get(position) + " for " + boxScore.getTeam().getShortTeamName());
							return false;
						}
					}
				}
				else { 
					pitcherDHLineupPosition = p;
				}
			}
			// Set DH/P
			// Use P v DH
			if (Arrays.asList(nationalLeagueTeams).contains(boxScores[1].getTeam().getShortTeamName()) || boxScores[1].getYear() < 1973) {
				batters.get(pitcherDHLineupPosition).add(new MLBPlayer(gameState.getCurrentPitchers()[t].getName(), gameState.getCurrentPitchers()[t].getMlbPlayerId(), "P"));
			}
			else { // DH
				mlbPlayer = DAO.getMlbPlayerWithMostPlateAppearances((Integer)franchisesMap.get(boxScore.getTeam().getShortTeamName()), boxScore.getYear(), battersPlayerIdList);
				batters.get(pitcherDHLineupPosition).add(new MLBPlayer(mlbPlayer.getFullName(), mlbPlayer.getMlbPlayerId(), "DH"));
			}
		}
		return true;
	}*/
	
	private static void outputBoxScore(SeriesStats[] seriesStats) {
		BoxScore[] boxScores = new BoxScore[2];
		for (int top = 0; top < 2; top++) {
			boxScores[top] = new BoxScore();
			boxScores[top].setTeam(seriesStats[top].getTeam());
			boxScores[top].setBatters(new ArrayList<ArrayList<MLBPlayer>>());
			boxScores[top].setYear(seriesStats[top].getYear());
			LinkedHashMap<Integer, MLBPlayer> pitchers = new LinkedHashMap<Integer, MLBPlayer>();
			for (Map.Entry<Integer, MLBPlayer> entry : seriesStats[top].getPitchers().entrySet()) {
				pitchers.put(entry.getKey(), entry.getValue());
			}
			boxScores[top].setPitchers(pitchers);
			ArrayList<MLBPlayer> batters = new ArrayList<MLBPlayer>();
			for (Map.Entry<Integer, MLBPlayer> entry : seriesStats[top].getBatters().entrySet()) {
				batters.add(entry.getValue());
			}
			boxScores[top].getBatters().add(batters);
		}
		outputBoxScore(boxScores, true, null, null);
	}
	
	private static void outputBoxScore(BoxScore[] boxScores, boolean series, Integer gameLength, Map<String, Integer> pitchersOfRecord) {
		BoxScore boxScore;
		String[] displayTeamYearString = {"",""};
		displayTeamYearString[0] = boxScores[0].getTeam().getShortTeamName().equals(boxScores[1].getTeam().getShortTeamName()) ? boxScores[0].getTeamAndYearDisplay() : boxScores[0].getTeam().getShortTeamName();
		displayTeamYearString[1] = boxScores[0].getTeam().getShortTeamName().equals(boxScores[1].getTeam().getShortTeamName()) ? boxScores[1].getTeamAndYearDisplay() : boxScores[1].getTeam().getShortTeamName();
		boolean allStarGameTie = boxScores[0].getFinalScore() == boxScores[1].getFinalScore() && allStarGameMode;
		if (!series) {
			int winner = boxScores[0].getFinalScore() > boxScores[1].getFinalScore() ? 0 : 1;
			System.out.println("\n" + boxScores[winner].getYear() + " " + boxScores[winner].getTeam().getFullTeamName() + " " + boxScores[winner].getFinalScore() + " " + 
				boxScores[winner==0?1:0].getYear() + " " + boxScores[winner==0?1:0].getTeam().getFullTeamName() + " " + boxScores[winner==0?1:0].getFinalScore());
			for (int top = 0; top < 2; top++) {
				boxScore = boxScores[top];
				if (top == 0)  {
					System.out.println();	
				}
				String team = displayTeamYearString[top];
				team += team.length() < 3 ? " " : "";
				System.out.print(team + " ");
				for (int i = 1; i < gameLength; i++) {
					if (i == (gameLength - 1) && (boxScores[1].getScore(i) > boxScores[0].getScore(i) && top == 1) && !boxScore.isWalkOff()) {
						System.out.print("X "); // Bottom of inning was not necessary
					}
					else {
						System.out.print(boxScore.getRunsScored(i) + (boxScores[top == 0 ? 1: 0].getRunsScored(i) < 10 ? " " : "  "));
					}
				}
				System.out.println(" " + boxScore.getFinalScore() + (boxScore.getFinalScore() < 10 ? " " : "") + " " + boxScore.getHits() + 
					(boxScore.getHits() < 10 ? " " : "") + " " +  boxScore.getErrors());
			}
		}
		for (int top = 0; top < 2; top++) {
			ArrayList<ArrayList<MLBPlayer>> batters = boxScores[top].getBatters();
			System.out.println();
			System.out.print(displayTeamYearString[top] + " ");
			System.out.print("Batting\t\t");
			if (displayTeamYearString[top].length() < 8) {
				System.out.print("\t");
			}
			String seriesOnlyString = series ? "SB   CS   " : "";
			System.out.println("AB   R    H    RBI  BB   K    " + seriesOnlyString + " AVG  OBP  SLG");
			for (ArrayList<MLBPlayer> playerList : batters) {
				for (MLBPlayer batter : playerList) {
					BattingStats gameStats = batter.getMlbBattingStats().getBattingStats();
					BattingStats playerSeasonStats = getBattersSeasonBattingStats(rosters[top], batter.getMlbPlayerId());
					String playerOutput = batter.getFullName() + " " + batter.getPrimaryPosition();
					System.out.print(playerOutput);
					for (int tab = 32; tab >= 8; tab-=8) {
						if (playerOutput.length() < tab) {
							System.out.print("\t");
						}
					}
					System.out.print(gameStats.getAtBats() + padSpaces("  ", gameStats.getAtBats()));
					System.out.print(gameStats.getRuns() + padSpaces("  ", gameStats.getRuns()));
					System.out.print(gameStats.getHits() + padSpaces("  ", gameStats.getHits()));
					System.out.print(gameStats.getRbis() + padSpaces("  ", gameStats.getRbis()));
					System.out.print(gameStats.getWalks() + padSpaces("  ", gameStats.getWalks()));
					System.out.print(gameStats.getStrikeOuts() + padSpaces("  ", gameStats.getStrikeOuts()));
					if (series) {
						System.out.print(gameStats.getStolenBases() + padSpaces("  ", gameStats.getStolenBases()));
						System.out.print(gameStats.getCaughtStealing() + padSpaces("  ", gameStats.getCaughtStealing()));
					}
					double ba = series ? gameStats.getBattingAverage() : playerSeasonStats.getBattingAverage();
					double obp = series ? gameStats.getOnBasePercentage() : playerSeasonStats.getOnBasePercentage();
					double sp = series ? gameStats.getSluggingPercentage() : playerSeasonStats.getSluggingPercentage();
					System.out.print(ba == 1.0 ? "1.00 " : df.format(ba) + " ");
					System.out.print(obp == 1.0 ? "1.00 " : df.format(obp) + " ");
					System.out.print(sp == 1.0 ? "1.00 " : df.format(sp) + " ");
					System.out.println();
				}
			}
		}
		System.out.println();
		String[] doublesString = {"", ""};
		String[] triplesString = {"", ""};
		String[] homeRunsString = {"", ""};
		for (int top = 0; top < 2; top++) {
			ArrayList<ArrayList<MLBPlayer>> batters = boxScores[top].getBatters();
			for (ArrayList<MLBPlayer> playerList : batters) {
				for (MLBPlayer batter : playerList) {
					int numD = batter.getMlbBattingStats().getBattingStats().getDoubles();
					int numT = batter.getMlbBattingStats().getBattingStats().getTriples();
					int numH = batter.getMlbBattingStats().getBattingStats().getHomeRuns();
					if (numD > 0) {
						doublesString[top] += (batter.getFirstLastName() + (numD > 1 ? "(" + numD + ")" : "") + ", ");
					}
					if (numT > 0) {
						triplesString[top] += (batter.getFirstLastName() + (numT > 1 ? "(" + numT + ")" : "") + ", ");
					}
					if (numH > 0) {
						homeRunsString[top] += (batter.getFirstLastName() + (numH > 1 ? "(" + numH + ")" : "") + ", ");
					}
				}
			}
		}
		if (doublesString[0].length() > 0 || doublesString[1].length() > 0)  {
			System.out.println("2B");
			for (int top = 0; top < 2; top++) {
				if (doublesString[top].length() == 0) {
					continue;
				}
				System.out.println(displayTeamYearString[top]);
				System.out.println(doublesString[top].substring(0, doublesString[top].length()-2));
			}
		}
		if (triplesString[0].length() > 0 || triplesString[1].length() > 0)  {
			System.out.println("3B");
			for (int top = 0; top < 2; top++) {
				if (triplesString[top].length() == 0) {
					continue;
				}
				System.out.println(displayTeamYearString[top]);
				System.out.println(triplesString[top].substring(0, triplesString[top].length()-2));
			}
		}
		if (homeRunsString[0].length() > 0 || homeRunsString[1].length() > 0)  {
			System.out.println("HR");
			for (int top = 0; top < 2; top++) {
				if (homeRunsString[top].length() == 0) {
					continue;
				}
				System.out.println(displayTeamYearString[top]);
				System.out.println(homeRunsString[top].substring(0, homeRunsString[top].length()-2));
			}
		}
		System.out.println();
		for (int top = 0; top < 2; top++) {
			System.out.println();
			System.out.print(displayTeamYearString[top] + " ");
			System.out.print("Pitching\t\t");
			if (displayTeamYearString[top].length() < 8) {
				System.out.print("\t");
			}
			String seriesOnlyString = series ? "W    L    S    BS   H    " : "";
			System.out.println("IP     H    R    ER   BB   K    HR   " + seriesOnlyString + "ERA");
			HashMap<Integer, MLBPlayer> pitchers = boxScores[top].getPitchers();
			for (Map.Entry<Integer, MLBPlayer> entry : pitchers.entrySet()) {
				PitchingStats ps = entry.getValue().getMlbPitchingStats().getPitchingStats();
				PitchingStats pitcherSeasonStats = getPitchersSeasonPitchingStats(rosters[top], entry.getValue().getMlbPlayerId());
				String pitcherNameString = entry.getValue().getFirstLastName();
				if (!allStarGameTie) { // No winner, loser or save in tie game (AS game only)
					if (!series && entry.getValue().getMlbPlayerId().intValue() == pitchersOfRecord.get("W").intValue()) {
						pitcherNameString += " (W)";
					}
					else if (!series && entry.getValue().getMlbPlayerId().intValue() == pitchersOfRecord.get("L").intValue()) {
						pitcherNameString += " (L)";
					}
					if (!series && ps.getSaves() > 0) {
						pitcherNameString += " (S)";
					}
				}
				if (!series && ps.getBlownSaves() > 0) {
					pitcherNameString += " (BS)";
				}
				else if (!series && ps.getHolds() > 0) {
					pitcherNameString += " (H)";
				}
				System.out.print(pitcherNameString);
				System.out.print("\t");
				if (pitcherNameString.length() < 24) {
					System.out.print("\t");
				}
				if (pitcherNameString.length() < 16) {
					System.out.print("\t");
				}
				System.out.print(ps.getInningsPitched() + padSpaces("  ", ps.getInningsPitched()));
				System.out.print(ps.getHitsAllowed() + padSpaces("  ", ps.getHitsAllowed()));
				System.out.print(ps.getRunsAllowed() + padSpaces("  ", ps.getRunsAllowed()));
				System.out.print(ps.getEarnedRunsAllowed() + padSpaces("  ", ps.getEarnedRunsAllowed()));
				System.out.print(ps.getWalks() + padSpaces("  ", ps.getWalks()));
				System.out.print(ps.getStrikeouts() + padSpaces("  ", ps.getStrikeouts()));
				System.out.print(ps.getHomeRunsAllowed() + padSpaces("  ", ps.getHomeRunsAllowed()));
				if (series) {
					System.out.print(ps.getWins() + padSpaces("  ", ps.getWins()));
					System.out.print(ps.getLosses() + padSpaces("  ", ps.getLosses()));
					System.out.print(ps.getSaves() + padSpaces("  ", ps.getSaves()));
					System.out.print(ps.getBlownSaves() + padSpaces("  ", ps.getBlownSaves()));
					System.out.print(ps.getHolds() + padSpaces("  ", ps.getHolds()));
				}
				double era = series ? ps.getEarnedRunAverage() : pitcherSeasonStats.getEarnedRunAverage();
				System.out.println(eraDf.format(era));
			}
		}
	}
	
	private static int getBattingOrderForPlayer(int id, int top) {
		// Returns 0 if not found
		int order = 1;
		ArrayList<ArrayList<MLBPlayer>> batters = boxScores[top].getBatters();
		for (ArrayList<MLBPlayer> playerList : batters) {
			for (MLBPlayer p : playerList) {
				if (p.getMlbPlayerId() == id) {
					return order;
				}
			}
			order++;
		}
		return 0;
	}
	
	private static MLBPlayer getBoxScorePlayerFromPosition(String position, int top) {
		// For now assumes batting order and positions stay in sync (no double switches)
		MLBPlayer player = null;
		ArrayList<ArrayList<MLBPlayer>> batters = boxScores[top].getBatters();
		for (ArrayList<MLBPlayer> playerList : batters) {
			for (MLBPlayer p : playerList) {
				if (p.getPrimaryPosition().equals(position)) {
					return playerList.get(playerList.size() - 1); // get current player at that position
				}
			}
		}
		return player;
	}
	
	private static int getDivisionByTeamId(Integer[][] divisions, int teamId) {
		int division = 0;
		for (Integer[] div : divisions) {
			for (Integer team : div) {
				if (teamId == team) {
					return division;
				}
			}
			division++;
		}
		return 0;
	}
	
	private static MLBPlayer getOneMorePlayerToFillOutLineup(String missingPosition, HashMap<Integer, MLBPlayer> battingStatsSortedByStatMap,
		ArrayList<Integer> playersInLineupList, boolean useFieldingStats) {
			// Currently limited to looking for outfielder
			MLBPlayer player = null;
			boolean playedOutfield = false;
			for (Map.Entry<Integer, MLBPlayer> mapElement : battingStatsSortedByStatMap.entrySet()) {
				playedOutfield = false;
				player = null;
				if (useFieldingStats) {
					ArrayList<MLBFieldingStats> playerFieldingStatsList = mapElement.getValue().getMlbFieldingStats();
					if (playerFieldingStatsList == null) {
						continue;
					}
					for (MLBFieldingStats playerFieldingStats : playerFieldingStatsList) {
						if (Arrays.asList(MLBFieldingStats.outfieldPositions).contains(playerFieldingStats.getPosition()) && !playersInLineupList.contains(mapElement.getKey())) {
							player = mapElement.getValue();
							playedOutfield = true;
							break;
						}
					}
				}
				else {
					if (Arrays.asList(MLBFieldingStats.outfieldPositions).contains(mapElement.getValue().getPrimaryPosition()) && !playersInLineupList.contains(mapElement.getKey())) {
						player = mapElement.getValue();
						playedOutfield = true;
						break;
					}
				}
				if (player != null) {
					break;
				}
			}
			if (Arrays.asList(MLBFieldingStats.outfieldPositions).contains(missingPosition) && playedOutfield) {
				return player;
			}
			return null;
	}
	
	private static ArrayList<String> getMissingPositionFromLineup(ArrayList<String> positionsUsed) {
		// Get missing position (Not including P or DH)
		ArrayList<String> positionsList = new ArrayList<>(positions.values());
		positionsList.removeAll(positionsUsed);
		positionsList.remove("P");
		positionsList.remove("DH");
		return positionsList;
	}
	
	private static MLBPlayer getPlayerFromId(int id) {
		MLBPlayer player = null;
		for (int top = 0 ; top < 2; top++) {
			player = rosters[top].getBatters().get(id);
			if (player == null) {
				player = rosters[top].getPitchers().get(id);
			}
			if (player != null) {
				break;
			}
		}
		return player;
	}
	
	private static String getPlayerNameFromId(int id) {
		MLBPlayer player = getPlayerFromId(id);
		return player != null ? player.getFirstLastName() : "<>";
	}
	
	private static MLBPlayer getBoxScoreBatterFromId(int id) {
		if (id == 0) {
			return null;
		}
		int bo = getBattingOrderForPlayer(id, gameState.getTop());
		return boxScores[gameState.getTop()].getBatters().get(bo - 1).get(boxScores[gameState.getTop()].getBatters().get(bo - 1).size() - 1);
	}
	
	private static BattingStats getBattersSeasonBattingStats(Roster roster, int id) {
		if (roster.getBatters().get(id) == null) {
			return new BattingStats(75, 10, 2, 0, 0, 2, 44, 0, 4, 3, 0, 100, 0); // Default pitcher batting stats
		}
		return roster.getBatters().get(id).getMlbBattingStats().getBattingStats();
	}
	
	private static PitchingStats getPitchersSeasonPitchingStats(Roster roster, int id) {
		if (roster.getPitchers().get(id) == null) {
			return new PitchingStats();
		}
		return roster.getPitchers().get(id).getMlbPitchingStats().getPitchingStats();
	}
	
	private static Integer getOutfielderArmRating(MLBPlayer outfielder, int year) {
		// Get arm rating from API
		// Note: Fielding stats seem to only be from 1999 - present
		Integer armRating = 0;
		ArrayList<MLBFieldingStats> fieldingStatsList = outfielder.getMlbFieldingStats();
		if (fieldingStatsList != null) {
			for (MLBFieldingStats fieldingStat : fieldingStatsList) {
				if (fieldingStat.getPosition().equals(outfielder.getPrimaryPosition())) {
					armRating = fieldingStat.getOutfielderArmRating();
					break;
				}
			}
		}
		return armRating;
	}
	/*
	private static ArrayList<Integer> getRandomLineupByPosition() {
		ArrayList<Integer> randomLineup = new ArrayList<Integer>();
		for (int i = 1; i < 10; i++) {
			randomLineup.add(new Integer(i));
        }
        Collections.shuffle(randomLineup);
        return randomLineup;
	} */
	
	// For play mode
	private static boolean processCommand(String command, PitchingStats currentPitcherGameStats, MLBPlayer currentBatter) {
		if (command == null || command.length() == 0 || command.equalsIgnoreCase("SWING")) { // Swing at pitch (same if just CR entered)
			gameState.setGameStarted(true);
			return true;
		}
		command = command.toUpperCase();
		MLBPlayer currentPitcher = gameState.getCurrentPitchers()[gameState.getTop()==0?1:0];
		boolean doubleSteal = command.toUpperCase().indexOf("DOUBLESTEAL") != -1;
		String[] commandArray;
		if (command.toUpperCase().indexOf("STEAL") != -1) {
			commandArray = command.split(" ");
			if (commandArray.length < 2 && !doubleSteal) {
				System.out.print("INVALID COMMAND!\n");
				return false;
			}
			int baseToSteal = 3;
			if (!doubleSteal) {
				try {
					baseToSteal = Integer.parseInt(commandArray[1]);
				}
				catch (Exception e) {
					baseToSteal = 0;
				}
			}
			else {
				if (gameState.getCurrentBasesSituation() != GameState.MAN_ON_FIRST_AND_SECOND) {
					System.out.print("CAN ONLY DOUBLE STEAL WITH MAN ON FIRST AND SECOND!\n");
					return false;
				}
			}
			if ((baseToSteal < 2 || baseToSteal > 4 ) ||
			   ((baseToSteal == 2 || baseToSteal == 3) && (!gameState.isBaseOccupied(baseToSteal-1) || gameState.isBaseOccupied(baseToSteal))) ||
			    (baseToSteal == 4 && !gameState.isBaseOccupied(baseToSteal-1))) {
					System.out.print(baseToSteal + " INVALID BASE TO STEAL!\n");
					return false;
			}
			else {
				int sbOuts = stealBase(baseToSteal);
				if (sbOuts > 0) {
					currentPitcherGameStats.incrementInningsPitchedBy(1);
					gameState.setOuts(gameState.getOuts() + sbOuts);
				}
			}
			if (doubleSteal) {
				System.out.print("DOUBLE STEAL\n");
				MLBPlayer runnerStealing2Player = getPlayerFromId(gameState.getBaseRunnerId(1));
				BattingStats boxScoreRunnerStats = getBoxScoreBatterFromId(runnerStealing2Player.getMlbPlayerId()).getMlbBattingStats().getBattingStats();
				boxScoreRunnerStats.incrementStolenBases();
				gameState.setBaseRunner(2, gameState.getBaseRunner(1));  // 1->2
				gameState.setBaseRunner(1, new BaseRunner());
			}
			return false;
		} // END STEAL
		else if (command.toUpperCase().indexOf("AUTO") != -1) {
			try {
				autoBeforeInning = Integer.parseInt(command.substring(command.length()-1));
			}
			catch (Exception e) {
			}
			autoBeforeMode = true;
			gameMode = false;
			gameState.setGameStarted(true);
			return true;
		}
		else if (command.toUpperCase().indexOf("BATTERS") != -1) {
			commandArray = command.split(" ");
			if (commandArray.length < 2) {
				System.out.print("INVALID COMMAND!\n");
				return false;
			}
			int top = commandArray[1].equalsIgnoreCase("HOME") || commandArray[1].equalsIgnoreCase("H") || commandArray[1].equalsIgnoreCase("1") ? 1 : 0;
			HashMap<Integer, MLBPlayer> rosterBatters = rosters[top].getBatters();
			System.out.println("\nEligible Pinch hitters:");
			System.out.println("Name\t\t\t\tID      AVG  SR HR");
			for (Map.Entry<Integer, MLBPlayer> entry : rosterBatters.entrySet()) {
				MLBPlayer batter = entry.getValue();
				if (getBattingOrderForPlayer(batter.getMlbPlayerId(), top) == 0) {
					String playerOutput = batter.getFullName() + " " + batter.getPrimaryPositionByFieldingStats();
					System.out.print(playerOutput);
					for (int tab = 32; tab >= 8; tab-=8) {
						if (playerOutput.length() < tab) {
							System.out.print("\t");
						}
					}
					System.out.print(batter.getMlbPlayerId() + " ");
					System.out.print(df.format(batter.getMlbBattingStats().getBattingStats().getBattingAverage()) + "  ");
					System.out.print(batter.getMlbBattingStats().getBattingStats().getSpeedRating() + "  ");
					System.out.print(batter.getMlbBattingStats().getBattingStats().getHomeRuns() + "  ");
					System.out.println();
				}
			}
			System.out.println();
			return false;
		}
		else if (command.toUpperCase().indexOf("PITCHERS") != -1) {
			return handlePitchersCommand(command);
		}
		else if (command.toUpperCase().indexOf("LINEUP") != -1) {
			commandArray = command.split(" ");
			if (commandArray.length < 2) {
				System.out.print("INVALID COMMAND!\n");
				return false;
			}
			int top = commandArray[1].equalsIgnoreCase("HOME") || commandArray[1].equalsIgnoreCase("H") || commandArray[1].equalsIgnoreCase("1") ? 1 : 0;
			ArrayList<ArrayList<MLBPlayer>> gameBatters = boxScores[top].getBatters();
			System.out.println(boxScores[top].getTeamAndYearDisplay());
			for (ArrayList<MLBPlayer> lineup : gameBatters) {
				MLBPlayer batter = lineup.get(lineup.size() -1); // get current player at that lineup spot
				System.out.println(batter.getFullName() + "<" + batter.getMlbPlayerId() + "> " + batter.getPrimaryPosition());
			}
			System.out.println();
			return false;
		}
		else if (command.toUpperCase().indexOf("DOUBLESWITCH") != -1) {
			commandArray = command.split(" ");
			if (commandArray.length < 4) {
				System.out.print("INVALID COMMAND!\n");
				return false;
			}
			if (useDH) {
				System.out.print("Double switch can only be done without DH!\n");
				return false;
			}
			int newPitcherId = 0;
			int newBatterId = 0;
			int lineUpPos = 0;
			try {
				newPitcherId = Integer.parseInt(commandArray[1]);
				newBatterId = Integer.parseInt(commandArray[2]);
				lineUpPos = Integer.parseInt(commandArray[3]);
			}
			catch (Exception e) {
				System.out.print("INVALID ARGUMENT!\n");
				return false;
			}
			int top = gameState.getTop();
			HashMap<Integer, MLBPlayer> gamePitchers = boxScores[top==0?1:0].getPitchers();
			HashMap<Integer, MLBPlayer> rosterPitchers = rosters[top==0?1:0].getPitchers();
			ArrayList<ArrayList<MLBPlayer>> gameBatters = boxScores[top==0?1:0].getBatters();
			HashMap<Integer, MLBPlayer> rosterBatters = rosters[top==0?1:0].getBatters();
			// Check lineup position of current pitcher
			if (gameBatters.get(lineUpPos - 1).get(gameBatters.get(lineUpPos - 1).size() - 1).getMlbPlayerId() == newPitcherId) {
				System.out.print("Can not replace previous pitcher's lineup position with new pitcher!\n");
				return false;
			}
			MLBPlayer newPitcherFromRoster = (MLBPlayer)rosterPitchers.get(newPitcherId);
			MLBPlayer newBatterFromRoster = (MLBPlayer)rosterBatters.get(newBatterId);
			if (newPitcherFromRoster == null) {
				System.out.print("No pitcher found for " + newPitcherId + "!\n");
				return false;
			}
			if (gamePitchers.get(newPitcherId) != null) {
				System.out.print("Pitcher has already pitched in this game " + newPitcherId + "!\n");
				return false;
			}
			if (newBatterFromRoster == null) {
				System.out.print("No batter found for " + newBatterId + "!\n");
				return false;
			}
			if (getBattingOrderForPlayer(newBatterId, top) != 0) {
				System.out.print("Batter has already hit in this game " + newBatterId + "!\n");
				return false;
			}
			MLBPlayer newPitcher = new MLBPlayer(newPitcherFromRoster.getMlbPlayerId(), newPitcherFromRoster.getFullName(), newPitcherFromRoster.getPrimaryPosition(), 
				newPitcherFromRoster.getArmThrows(), newPitcherFromRoster.getBats(), newPitcherFromRoster.getJerseyNumber(), newPitcherFromRoster.getSeasonStarted());
			MLBPlayer replacedPitcher = gameState.getCurrentPitchers()[top==0?1:0];
			int pitchBo = getBattingOrderForPlayer(replacedPitcher.getMlbPlayerId(), top==0?1:0);
			changePitcher(newPitcher, top==0?1:0, lineUpPos);
			MLBPlayer replacedPlayer = gameBatters.get(lineUpPos - 1).get(gameBatters.get(lineUpPos - 1).size() - 1);
			MLBPlayer newBatter = new MLBPlayer(newBatterFromRoster.getMlbPlayerId(), newBatterFromRoster.getFullName(), replacedPlayer.getPrimaryPositionByFieldingStats(), 
				newBatterFromRoster.getArmThrows(), newBatterFromRoster.getBats(), newBatterFromRoster.getJerseyNumber(), newBatterFromRoster.getSeasonStarted(), 
				newBatterFromRoster.getMlbFieldingStats());
			gameBatters.get(pitchBo - 1).add(newBatter);
			System.out.println("DOUBLE SWITCH: Batter changed to: " + newBatterFromRoster.getFirstLastName() + " at lineup position: " + lineUpPos + "\n");
			return false;
		} // End DOUBLESWITCH
		else if (command.toUpperCase().indexOf("SUBP") != -1) {
			commandArray = command.split(" ");
			if (commandArray.length < 3) {
				System.out.print("INVALID COMMAND!\n");
				return false;
			}
			int newPitcherId = 0;
			try {
				newPitcherId = Integer.parseInt(commandArray[2]);
			}
			catch (Exception e) {
				System.out.print("INVALID COMMAND!\n");
				return false;
			}
			int top = commandArray[1].equalsIgnoreCase("HOME") || commandArray[1].equalsIgnoreCase("H") || commandArray[1].equalsIgnoreCase("1") ? 1 : 0;
			HashMap<Integer, MLBPlayer> gamePitchers = boxScores[top].getPitchers();
			HashMap<Integer, MLBPlayer> rosterPitchers = rosters[top].getPitchers();
			MLBPlayer newPitcherFromRoster = (MLBPlayer)rosterPitchers.get(newPitcherId);
			if (newPitcherFromRoster == null) {
				System.out.print("No pitcher found for " + newPitcherId + "!\n");
				return false;
			}
			if (gamePitchers.get(newPitcherId) != null) {
				System.out.print("Pitcher has already pitched in this game " + newPitcherId + "!\n");
				return false;
			}
			MLBPlayer newPitcher = new MLBPlayer(newPitcherFromRoster.getMlbPlayerId(), newPitcherFromRoster.getFullName(), newPitcherFromRoster.getPrimaryPosition(), 
				newPitcherFromRoster.getArmThrows(), newPitcherFromRoster.getBats(), newPitcherFromRoster.getJerseyNumber(), newPitcherFromRoster.getSeasonStarted());
			changePitcher(newPitcher, top, null);
			System.out.println();
			return false;
		}
		else if (command.toUpperCase().indexOf("SUBB") != -1) {
			commandArray = command.split(" ");
			if (commandArray.length < 2) {
				System.out.print("INVALID COMMAND!\n");
				return false;
			}
			int newBatterId = 0;
			try {
				newBatterId = Integer.parseInt(commandArray[1]);
			}
			catch (Exception e) {
				System.out.print("INVALID COMMAND!\n");
				return false;
			}
			int top = gameState.getTop();
			ArrayList<ArrayList<MLBPlayer>> gameBatters = boxScores[top].getBatters();
			HashMap<Integer, MLBPlayer> rosterBatters = rosters[top].getBatters();
			MLBPlayer newBatterFromRoster = (MLBPlayer)rosterBatters.get(newBatterId);
			if (newBatterFromRoster == null) {
				System.out.print("No batter found for " + newBatterId + "!\n");
				return false;
			}
			if (getBattingOrderForPlayer(newBatterId, top) != 0) {
				System.out.print("Batter has already hit in this game " + newBatterId + "!\n");
				return false;
			}
			MLBPlayer newBatter = new MLBPlayer(newBatterFromRoster.getMlbPlayerId(), newBatterFromRoster.getFullName(), currentBatter.getPrimaryPositionByFieldingStats(), 
				newBatterFromRoster.getArmThrows(), newBatterFromRoster.getBats(), newBatterFromRoster.getJerseyNumber(), newBatterFromRoster.getSeasonStarted(), 
				newBatterFromRoster.getMlbFieldingStats());
			int bo = gameState.getBattingOrder()[top];
			gameBatters.get(bo - 1).add(newBatter);
			if (currentBatter.getPrimaryPosition().equals("P")) {  // Pinch hitting for pitcher
				if (newBatterFromRoster.getPrimaryPosition().equals("P")) {
					changePitcher(newBatter, top, null);  // P pinch hitting for P
				}
				else {
					changePitcher(null, top, null);  // Remove pitcher
				}
			}
			System.out.println("Batter changed to: " + newBatterFromRoster.getFirstLastName() + "\n");
			return false;
		}
		else if (command.toUpperCase().indexOf("SUBR") != -1) {
			commandArray = command.split(" ");
			if (commandArray.length < 3) {
				System.out.print("INVALID COMMAND!\n");
				return false;
			}
			int base = 0;
			try { 
				base = Integer.parseInt(commandArray[2]);
			}
			catch (Exception e) {
			}
			if ((base < 1 || base > 3) || !gameState.isBaseOccupied(base)) {
				System.out.print("No base runner on " + base + "!\n");
				return false;
			}
			int pinchRunnerId = 0;
			try { 
				pinchRunnerId = Integer.parseInt(commandArray[1]);
			}
			catch (Exception e) {
				System.out.print("INVALID COMMAND!\n");
				return false;
			}
			int top = gameState.getTop();
			ArrayList<ArrayList<MLBPlayer>> gameBatters = boxScores[top].getBatters();
			HashMap<Integer, MLBPlayer> rosterBatters = rosters[top].getBatters();
			int replacedRunnerId = gameState.getBaseRunnerId(base);
			MLBPlayer pinchRunnerFromRoster = (MLBPlayer)rosterBatters.get(pinchRunnerId);
			MLBPlayer replacedRunnerFromRoster = (MLBPlayer)rosterBatters.get(replacedRunnerId);
			if (pinchRunnerFromRoster == null) {
				System.out.print("No player found for " + pinchRunnerId + "!\n");
				return false;
			}
			if (getBattingOrderForPlayer(pinchRunnerId, top) != 0) {
				System.out.print("Player has already appeared in this game " + pinchRunnerId + "!\n");
				return false;
			}
			MLBPlayer pinchRunner = new MLBPlayer(pinchRunnerFromRoster.getMlbPlayerId(), pinchRunnerFromRoster.getFullName(), replacedRunnerFromRoster.getPrimaryPositionByFieldingStats(), 
				pinchRunnerFromRoster.getArmThrows(), pinchRunnerFromRoster.getBats(), pinchRunnerFromRoster.getJerseyNumber(), pinchRunnerFromRoster.getSeasonStarted(), 
				pinchRunnerFromRoster.getMlbFieldingStats());
			int bo = getBattingOrderForPlayer(replacedRunnerId, top);
			gameBatters.get(bo - 1).add(pinchRunner);
			if (replacedRunnerFromRoster.getPrimaryPosition().equals("P")) {  // Pinch running for pitcher
				if (pinchRunnerFromRoster.getPrimaryPosition().equals("P")) {
					changePitcher(pinchRunner, top, null);  // P pinch running for P
				}
				else {
					changePitcher(null, top, null);  // Remove pitcher
				}
			}
			gameState.setBaseRunner(base, new BaseRunner(pinchRunner.getMlbPlayerId(), currentPitcher.getMlbPlayerId()));
			System.out.println("Pinch running at " + base + " : " + pinchRunner.getFirstLastName() + "\n");
			return false;
		} // End SUBR
		/*else if (command.toUpperCase().indexOf("IMPORT") != -1) {
			return handleImportLineupCommand(command, false, null); // TODO: set importedPitcherRotation
		} // End IMPORT
		*/
		else {  // Commands without parameters
			switch (command) {
				case "SIM":
					simulationMode = true;
					autoBeforeInning = 1000;
					autoBeforeMode = false; 
					gameMode = false;
					gameState.setGameStarted(true);
					return true;
				case "OUTFIELDERS":
					System.out.println("\nCurrent outfielders:");
					for (int p = 7; p < 10; p++) {
						MLBPlayer outfielder = getBoxScorePlayerFromPosition(positions.get(p), gameState.getTop()==1?0:1);
						int armRating = getOutfielderArmRating(outfielder, boxScores[gameState.getTop()==1?0:1].getYear());
						System.out.println(outfielder.getFirstLastName() + "(" + armRating + ")");
					}
					System.out.println();
					return false;
				case "INTBB":
					System.out.println(currentBatter.getFirstLastName() + " was intentionally walked");
					updateBasesSituationNoRunnersAdvance(currentBatter);
					gameState.incrementBattingOrder(gameState.getTop());
					gameState.setHitAndRun(false);  // clear hit and run, if on
					gameState.setInfieldIn(false);  // clear infield in, if on
					return false;
				case "SACBUNT":
					if (gameState.getCurrentBasesSituation() == GameState.BASES_LOADED || gameState.getCurrentBasesSituation() == GameState.MAN_ON_SECOND_AND_THIRD || 
							gameState.getCurrentBasesSituation() == GameState.MAN_ON_THIRD || gameState.getCurrentBasesSituation() == GameState.BASES_EMPTY ||
							gameState.getOuts() >= 2) {
						System.out.println("CAN NOT SACIFICE BUNT IN THIS SITUATION!");
					}
					else {
						System.out.println(currentBatter.getFirstLastName() + " attempted a sacrifice bunt");
						updateBasesSituationSacBunt(currentBatter);
						gameState.incrementBattingOrder(gameState.getTop());
						gameState.setHitAndRun(false);  // clear hit and run, if on
						gameState.setInfieldIn(false);  // clear infield in, if on
					}
					return false;
				case "SUICIDESQUEEZE":
				case "SAFETYSQUEEZE":
					boolean suicide = command.equalsIgnoreCase("SUICIDESQUEEZE");
					if (!(gameState.getCurrentBasesSituation() == GameState.MAN_ON_THIRD || gameState.getCurrentBasesSituation() == GameState.MAN_ON_SECOND_AND_THIRD) ||
							gameState.getOuts() >= 2) {
						System.out.println("CAN NOT SQUEEZE BUNT IN THIS SITUATION!");
					}
					else {
						System.out.println(currentBatter.getFirstLastName() + " attempted a squeeze bunt");
						updateBasesSituationSqueezeBunt(currentBatter, suicide);
						gameState.incrementBattingOrder(gameState.getTop());
						gameState.setHitAndRun(false);  // clear hit and run, if on
						gameState.setInfieldIn(false);  // clear infield in, if on
					}
					return false;
				case "HITRUN":
					if (!gameState.isValidHitAnRunScenario()) {
						System.out.println("NOT A VALID HIT AND RUN SCENARIO WITH " + gameState.getOuts() + " OUTS AND " + gameState.getBaseSituations().get(gameState.getCurrentBasesSituation()));
						return false;
					}
					else {
						System.out.println("HIT AND RUN");
						gameState.setHitAndRun(true);
						return true;
					}
				case "INFIELDIN":
					if (!gameState.isBaseOccupied(3) || gameState.getOuts() >= 2) {
						System.out.println("NOT A VALID SITUATION FOR INFIELD TO BE IN WITH " + gameState.getOuts() + " OUTS AND " + gameState.getBaseSituations().get(gameState.getCurrentBasesSituation()));
						return false;
					}
					else {
						System.out.println("INFIELD IN");
						gameState.setInfieldIn(true);
						return true;
					}
				case "PITCHERCHECK":
					System.out.println("CURRENT PITCHER STATUS: " + currentPitcher.getFirstLastName() + " BF:" + currentPitcher.getMlbPitchingStats().getPitchingStats().getBattersFaced() +
						" ER: " + currentPitcher.getMlbPitchingStats().getPitchingStats().getEarnedRunsAllowed());
					return false;
				case "?":
					System.out.println("COMMANDS - SIM, AUTO<ing#>, STEAL<base#>, PITCHERS <HOME|VIS>, SUBP <HOME|VIS> <id#>, BATTERS <HOME|VIS>, SUBB <id#>, OUTFIELDERS, "
						+ "INTBB, SUBR <id#> <base#>, SACBUNT, HITRUN");
					System.out.println("HITRUN, INFIELDIN, PITCHERCHECK, LINEUP <HOME|VIS>, DOUBLESTEAL, "
							+ "SUICIDESQUEEZE SAFETYSQUEEZE DOUBLESWITCH <pitcherId#> <batterId#> <lineupPos#> IMPORT <HOME|VIS> <fileName>");
					return false;
				default:
					System.out.println("UNKNOWN COMMAND!");
					return false;
			}
		}
	}
	
	private static List<TournamentTeam> importTournamentTeamsFromFile(String importFile) {
		List<TournamentTeam> importedTeams = new ArrayList<TournamentTeam>();
		String[] lineArray = {"", ""};
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(importDir + importFile));
			String line = reader.readLine();
			while (line != null) {
				lineArray = line.split(" ");
				if (lineArray.length != 2) {
					System.out.println("Invalid input line: " + line);
					closeFileReader(reader);
					return null;
				}
				MLBTeam matchingTeam = null;
				int year = Integer.parseInt(lineArray[0]);
				final String team1String = lineArray[1];
				matchingTeam = allMlbTeamsList.stream()
						  .filter(t -> t.getShortTeamName().equalsIgnoreCase(team1String) && t.getFirstYearPlayed() <= year && 
									(t.getLastYearPlayed() == null || t.getLastYearPlayed() == 0 || t.getLastYearPlayed() >= year))
						  .findAny().orElse(null);
				if (matchingTeam == null) {
					System.out.println("No MLB Team matching: " + lineArray[0] + " " + lineArray[1]);
					closeFileReader(reader);
					return null;
				}
				TournamentTeam tTeam = new TournamentTeam(year, matchingTeam);
				importedTeams.add(tTeam);
				line = reader.readLine();
			}
		}
		catch (IOException e) {
			System.out.println("Lineup file " + importFile + " not found.  Import failed!");
			closeFileReader(reader);
			return null;
		}
		catch (NumberFormatException e) {
			System.out.println("Invalid year: " + lineArray[0]);
			closeFileReader(reader);
			return null;
		}
		finally {
			closeFileReader(reader);
		}
		return importedTeams;
	}
	
	//@SuppressWarnings("resource")
    private static boolean handleImportLineupCommand(String command, boolean fromIncompleteLineup, List<List<MLBPlayer>> importedPitcherRotation,
    		List<List<MLBPlayer>> importedAllstarSubs) {
    	String[] commandArray = command.split(" ");
		if (commandArray.length < 3) {
			System.out.print("INVALID COMMAND!\n");
			return false;
		}
		if (gameState != null && gameState.isGameStarted()) {
			System.out.print("Lineup can not be imported if game has already started!\n");
			return false;
		}
		int top = commandArray[1].equalsIgnoreCase("HOME") || commandArray[1].equalsIgnoreCase("H") || commandArray[1].equals("1") ? 1 : 0;
		String lineupFileName = commandArray[2];
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(importDir + lineupFileName));
			String line = reader.readLine();
			String id = null;
			String lineupPos = null;
			ArrayList<String> positionsUsed = new ArrayList<String>();
			int lineupCount = 0;
			ArrayList<MLBPlayer> importBatters = new ArrayList<>();
			boolean DHFoundInLineup = false;
			while (line != null) {
				// skip comments
				if (line.contains("/")) {
					line = reader.readLine();
					continue;
				}
				lineupCount++;
				if (line.contains("<") && line.contains(">")) {
					id = line.substring(line.indexOf("<") + 1, line.indexOf(">")).trim();
					lineupPos = line.substring(line.indexOf(">") + 1, line.length()).trim();
					if (lineupPos.equalsIgnoreCase("DH")) {
						DHFoundInLineup = true;
					}
				}
				System.out.println(line);
				MLBPlayer player;
				try {
					player = getPlayerFromId(Integer.parseInt(id));
					if (player == null) {
						if (!allStarGameMode) {
							System.out.println("Player " + id + " not found in " + boxScores[top].getTeamAndYearDisplay() + " roster.  Checking all players.");
						}
						player = DAO.getMLBBatterFromMLBPlayerIdAndYear(Integer.parseInt(id), boxScores[top].getYear());
						if (player != null && player.getFullName() != null) {
							rosters[top].getBatters().put(player.getMlbPlayerId(), player); // Add batter from another team
						}
						else {
							player = DAO.getMLBPitcherFromMLBPlayerIdAndYear(Integer.parseInt(id), boxScores[top].getYear());
							if (player != null) {
								rosters[top].getBatters().put(player.getMlbPlayerId(), player); // Add pitcher from another team
							}
						}
					}
				}
				catch (NumberFormatException e) {
					System.out.println("Invalid player ID: " + id + ".  Import failed!");
					closeFileReader(reader);
					return false;
				}
				if (player == null) {
					System.out.println("Player " + id + " not found.  Import failed!");
					closeFileReader(reader);
					return false;
				}
				if (!positions.containsValue(lineupPos)) {
					System.out.println("Position: " + lineupPos + " not valid.  Import failed!");
					closeFileReader(reader);
					return false;
					
				}
				if (positionsUsed.contains(lineupPos) && !lineupPos.equalsIgnoreCase("P") && !allStarGameMode) {
					System.out.println("Position: " + lineupPos + " is already used.  Import failed!");
					closeFileReader(reader);
					return false;
				}
				if ((lineupCount < 10) || (allStarGameMode && lineupCount > 14)) {
					MLBPlayer importBatter = new MLBPlayer(player.getMlbPlayerId(), player.getFullName(), lineupPos, player.getArmThrows(), 
						player.getBats(), player.getJerseyNumber(), player.getSeasonStarted());
					
					if (allStarGameMode && lineupCount > 14) {
						importedAllstarSubs.get(top).add(importBatter);
					}
					else {
						importBatters.add(importBatter);
					}
				}
				else {
					importedPitcherRotation.get(top).add(new MLBPlayer(player.getMlbPlayerId(), player.getFullName(), lineupPos, 
						player.getArmThrows(), player.getBats(), player.getJerseyNumber(), player.getSeasonStarted()));
					rosters[top].getPitchers().put(player.getMlbPlayerId(), player);
				}
				if (!(lineupPos.equalsIgnoreCase("P") && positionsUsed.contains("P"))) {
					positionsUsed.add(lineupPos);
				}
				line = reader.readLine(); // Get next line in import file
			}
			if (lineupCount < 10 || lineupCount > 14  && !allStarGameMode) {
				System.out.println("Must have 10 - 14 players in lineup! (9 batters and 1-5 pitchers).  Import failed!");
				return false;
			}
			if (lineupCount < 14  && allStarGameMode) {
				System.out.println("Must have 14 players in lineup! (9 batters 5 pitchers and 0-8 position subs).  Import failed!");
				return false;
			}
			if (useDH && !DHFoundInLineup) {
				System.out.println("Must provide a DH when playing " + boxScores[1].getTeamAndYearDisplay() + ".  Import failed!");
				return false;
			}
			if (!useDH && DHFoundInLineup) {
				System.out.println("Can't use a DH when playing " + boxScores[1].getTeamAndYearDisplay() + ".  Import failed!");
				return false;
			}
			// Add imported players only if file was valid and we have a complete lineup
			lineupCount = 0;
			for (MLBPlayer importBatter : importBatters) {
				lineupCount++;
				if (boxScores[top].getBatters() != null && boxScores[top].getBatters().get(lineupCount-1).size() > 0 && !allStarGameMode) { // Only remove if lineup has already been created
					boxScores[top].getBatters().get(lineupCount-1).remove(0);
				}
				boxScores[top].getBatters().get(lineupCount-1).add(importBatter);
			}
			if (gameState.getCurrentPitchers()[top] != null && !allStarGameMode) { // Only remove if lineup has already been created
				boxScores[top].getPitchers().remove(gameState.getCurrentPitchers()[top].getMlbPlayerId());
			}
			MLBPlayer firstGameStarter = importedPitcherRotation.get(top).get(0);
			boxScores[top].getPitchers().put(firstGameStarter.getMlbPlayerId(), firstGameStarter);
			gameState.setCurrentPitcher(firstGameStarter, top);
			System.out.println("\nLineup imported for: " + boxScores[top].getTeamAndYearDisplay() + "\n");
		}
		catch (IOException e) {
			System.out.println("Lineup file " + lineupFileName + " not found.  Import failed!");
			return false;
		}
		finally {
			closeFileReader(reader);
		}
		return fromIncompleteLineup ? true : false;
    }
    
    private static boolean handlePitchersCommand(String command) {
    	String[] commandArray = command.split(" ");
		if (commandArray.length < 2) {
			System.out.print("INVALID COMMAND!\n");
			return false;
		}
		boolean allPitchers = commandArray.length > 2 && (commandArray[2].toUpperCase().equals("ALL") || commandArray[2].toUpperCase().equals("A"));
		int top = commandArray[1].equalsIgnoreCase("HOME") || commandArray[1].equalsIgnoreCase("H") || commandArray[1].equalsIgnoreCase("1") ? 1 : 0;
		HashMap<Integer, MLBPlayer> gamePitchers = boxScores != null ? boxScores[top].getPitchers() : null;
		HashMap<Integer, MLBPlayer> rosterPitchers = rosters[top].getPitchers();
		System.out.println(!allPitchers ? "\nEligible Pitchers:" : "\nAll Pitchers:");
		System.out.println("Name\t\t\tID      ERA   GS  H   S   BS  HRA");
		for (Map.Entry<Integer, MLBPlayer> entry : rosterPitchers.entrySet()) {
			MLBPlayer pitcher = entry.getValue();
			if ((gamePitchers != null && gamePitchers.get(pitcher.getMlbPlayerId()) == null) || allPitchers) {
				System.out.print(pitcher.getFirstLastName());
				for (int tab = 24; tab >= 8; tab-=8) {
					if (pitcher.getFirstLastName().length() < tab) {
						System.out.print("\t");
					}
				}
				System.out.print(pitcher.getMlbPlayerId() + "  ");
				System.out.print(eraDf.format(pitcher.getMlbPitchingStats().getPitchingStats().getEarnedRunAverage()) + "  ");
				System.out.print(pitcher.getMlbPitchingStats().getPitchingStats().getGamesStarted() + padSpaces(" ", pitcher.getMlbPitchingStats().getPitchingStats().getGamesStarted()));
				System.out.print(pitcher.getMlbPitchingStats().getPitchingStats().getHolds() + padSpaces(" ", pitcher.getMlbPitchingStats().getPitchingStats().getHolds()));
				System.out.print(pitcher.getMlbPitchingStats().getPitchingStats().getSaves() + padSpaces(" ", pitcher.getMlbPitchingStats().getPitchingStats().getSaves()));
				System.out.print(pitcher.getMlbPitchingStats().getPitchingStats().getBlownSaves() + padSpaces(" ", pitcher.getMlbPitchingStats().getPitchingStats().getBlownSaves()));
				System.out.print(pitcher.getMlbPitchingStats().getPitchingStats().getHomeRunsAllowed() + "  ");
				System.out.println();
			}
		}
		System.out.println();
		return false; 
    }
    /*
    private static void handleIncompleteLineup() {
    Scanner myObj;
		while(true) {
			myObj = new Scanner(System.in);
			System.out.print("INCOMPLETE LINEUP: ");
			String command = myObj.nextLine();
			if (command.toUpperCase().indexOf("PITCHERS") != -1) {
				handlePitchersCommand(command);
				continue;
			}
			else if (command.toUpperCase().indexOf("IMPORT") != -1) {
				boolean validLineup = handleImportLineupCommand(command, true, null); // TODO: set importedPitcherRotation
				if (validLineup) {
					break;
				}
			}
			else {
				System.out.println("INVALID COMMAND!");  
			}
		}
		myObj.close();
    } */
    
    private static boolean areLineupsIncomplete(ArrayList<ArrayList<ArrayList<MLBPlayer>>> lineups) {
    	for (int top = 0; top < 2; top++) {
    		ArrayList<ArrayList<MLBPlayer>> lineup = lineups.get(top);
    		for (int bo = 0 ; bo <= NUM_OF_PLAYERS_IN_LINEUP - 2; bo++) { // only check 1-8 for now
    			if (lineup.get(bo) == null || lineup.get(bo).size() == 0) {
    				return true;
    			}
    		}
    	}
    	return false;
    }
	
    private static PitchingStats changePitcher(MLBPlayer newPitcher, int top, Integer dsLineupPos) {
		if (newPitcher != null) {
			boolean enteredWithSaveOppty = gameState.getSaveOppty(top);
			int scoreDiff = boxScores[top].getScore(gameState.getInning()) - boxScores[top==0?1:0].getScore(gameState.getInning());
			if (scoreDiff > 0  && gameState.getInning() > 5 &&  /* Added greater than 5th innning as there must be a win eligible pitcher involved */
			  ((scoreDiff <= 3 && gameState.getCurrentBasesSituation() != GameState.BASES_LOADED) ||(scoreDiff <= 4  && gameState.getCurrentBasesSituation() == GameState.BASES_LOADED))) {
					printlnToScreen(newPitcher.getFirstLastName() + " enters game with save opportunity");
					gameState.setSaveOppty(true, top);
			}
			MLBPlayer previousPitcher = gameState.getCurrentPitchers()[top];
			if (!gameState.isGameStarted()) { // Remove scheduled starter from box score only if game hasn't started
				boxScores[top].getPitchers().remove(gameState.getCurrentPitchers()[top].getMlbPlayerId());
			}
			boxScores[top].getPitchers().put(newPitcher.getMlbPlayerId(), newPitcher);
			gameState.setCurrentPitcher(newPitcher, top);
			int bo = previousPitcher != null ? getBattingOrderForPlayer(previousPitcher.getMlbPlayerId(), top) : NUM_OF_PLAYERS_IN_LINEUP; // Check if pitcher needs to go into batting order (assumes end of order for P)
			if (!useDH) {
				int lineupPos = dsLineupPos == null ? bo : dsLineupPos;
				boxScores[top].getBatters().get(lineupPos - 1).add(newPitcher);
			}
			if (enteredWithSaveOppty && previousPitcher.getMlbPitchingStats().getPitchingStats().getOuts() > 0) {
				previousPitcher.getMlbPitchingStats().getPitchingStats().setHolds(1);
			}
			printlnToScreen("Pitcher changed to: " + newPitcher.getFirstLastName());
			return newPitcher.getMlbPitchingStats().getPitchingStats();
		}
		else {
			gameState.setCurrentPitcher(null, top);
			return null;
		}
	}
	
	private static void importTeam(Integer year, int top, ArrayList<MLBTeam> mlbTeamsList) {
		HashMap<Integer, MLBPlayer> newBattersMap = new HashMap<Integer, MLBPlayer>();
		HashMap<Integer, MLBPlayer> newPitchersMap = new HashMap<Integer, MLBPlayer>();
		HashMap<Integer, MLBPlayer> battersMap = new HashMap<Integer, MLBPlayer>();
		HashMap<Integer, MLBPlayer> pitchersMap = new HashMap<Integer, MLBPlayer>();
		HashMap<Integer, ArrayList<MLBFieldingStats>> fieldingStatsMap = new HashMap<>();
		List<Integer> allMLBPlayersIdList = DAO.getAllMlbPlayerIdsList();
		ArrayList<MLBBattingStats> battingStatsList = DBImport.importBattingStats(mlbTeamsList, year, newBattersMap, allMLBPlayersIdList);
		ArrayList<MLBPitchingStats> pitchingStatsList = DBImport.importPitchingStats(mlbTeamsList, year, newPitchersMap, allMLBPlayersIdList);
		ArrayList<Object> fieldingStatsList = DBImport.importFieldingStats(mlbTeamsList, battingStatsList, year);
		ArrayList<Integer> battersPlayerIdList = new ArrayList<>();
		battingStatsList.stream().forEach(entry -> battersPlayerIdList.add(entry.getMlbPlayerId()));
		battersMap = DBImport.importMlbPlayers(year, battersPlayerIdList);
		ArrayList<Integer>pitchersPlayerIdList = new ArrayList<>(); 
		pitchingStatsList.stream().forEach(entry -> pitchersPlayerIdList.add(entry.getMlbPlayerId()));
		pitchersMap = DBImport.importMlbPlayers(year, pitchersPlayerIdList);
		for (Map.Entry<Integer, MLBPlayer> entry : battersMap.entrySet()) {
			for (MLBBattingStats mlbBattingStats : battingStatsList) {
				if (mlbBattingStats.getMlbPlayerId() == entry.getKey() && mlbBattingStats.getMlbTeamId() == mlbTeamsList.get(0).getTeamId()) {
					entry.getValue().setMlbBattingStats(mlbBattingStats);
					break;
				}
			}
		}
		for (Map.Entry<Integer, MLBPlayer> entry : pitchersMap.entrySet()) {
			MLBPitchingStats mlbPitchingStats;
			for (Object o : pitchingStatsList) {
				mlbPitchingStats = (MLBPitchingStats)o;
				if (mlbPitchingStats.getMlbPlayerId() == entry.getKey() && mlbPitchingStats.getMlbTeamId() == mlbTeamsList.get(0).getTeamId()) {
					entry.getValue().setMlbPitchingStats(mlbPitchingStats);
					break;
				}
			}
		}
		// build fieldingStatsMap from fieldingStatsList to use for roster batter's fielding stats
		for (Object o : fieldingStatsList) {
			MLBFieldingStats mlbFieldingStats = (MLBFieldingStats)o;
			Integer playerId = mlbFieldingStats.getMlbPlayerId();
			ArrayList<MLBFieldingStats> mlbFieldingStatsList = fieldingStatsMap.get(playerId) == null ? new ArrayList<>() : fieldingStatsMap.get(playerId);
			mlbFieldingStatsList.add(mlbFieldingStats);
			fieldingStatsMap.put(playerId, mlbFieldingStatsList);
		}
		for (Map.Entry<Integer, MLBPlayer> entry : battersMap.entrySet()) {
			for (Object o : fieldingStatsList) {
				MLBFieldingStats mlbFieldingStats = (MLBFieldingStats)o;
				if (mlbFieldingStats.getMlbPlayerId().intValue() == entry.getKey().intValue() && 
					mlbFieldingStats.getMlbTeamId() == mlbTeamsList.get(0).getTeamId()) {
						entry.getValue().setMlbFieldingStats(fieldingStatsMap.get(mlbFieldingStats.getMlbPlayerId()));
						break;
				}
			}
		}
		rosters[top].setPitchers(sortHashMapByValue(pitchersMap, "GS"));
		rosters[top].setBatters(battersMap);
	}
	
	private static MLBPlayer getPitcher(int top, String sortBy, int index, ArrayList<Integer> excludingPitchers) {
		MLBPlayer rosterPitcher = null;
		rosters[top].setPitchers(sortHashMapByValue(rosters[top].getPitchers(), sortBy));
		int skipIndex = 0;
		if (excludingPitchers != null && excludingPitchers.size() > 0) {
			for (Map.Entry<Integer, MLBPlayer> rosterEntry : rosters[top].getPitchers().entrySet()) {
				// Skip pitchers that have been used and starters if you are looking for a mid reliever
				if (excludingPitchers.contains(rosterEntry.getKey()) ||
				   (pitchersOnMultTeams.get(top).containsKey(rosterEntry.getKey()) && 
					boxScores[top].getTeam().getTeamId() != pitchersOnMultTeams.get(top).get(rosterEntry.getKey())) ||
				   (sortBy.equalsIgnoreCase("IP") && rosterEntry.getValue().getMlbPitchingStats().getPitchingStats().getGamesStarted() > 10)) {
						continue;
				}
				if (index == skipIndex) {
					rosterPitcher = rosterEntry.getValue();
					break;
				}
				skipIndex++;
			}
		}
		else {
			rosterPitcher = rosters[top].getPitchers().get(rosters[top].getPitchers().keySet().toArray()[index]);
		}
		if (rosterPitcher != null) {
			return new MLBPlayer(rosterPitcher.getMlbPlayerId(), rosterPitcher.getFullName(), "P", rosterPitcher.getArmThrows(), rosterPitcher.getBats(), 
				rosterPitcher.getJerseyNumber(), rosterPitcher.getSeasonStarted());
		}
		else {
			return null;
		}
	}
	
	private static MLBPlayer getMlbPlayerWithMostPlateAppearances(Integer teamId, Integer year, ArrayList<Integer> excludingBatters, int top, HashMap<Integer, Integer> battersOnMultTeams) {
		MLBPlayer rosterBatter = null;
		rosters[top].setBatters(sortHashMapByValue(rosters[top].getBatters(), "PA"));
		if (excludingBatters != null && excludingBatters.size() > 0) {
			for (Map.Entry<Integer, MLBPlayer> rosterEntry : rosters[top].getBatters().entrySet()) {
				// Skip players who are already in lineup or were primarily on another team
				if (excludingBatters.contains(rosterEntry.getKey()) ||
					battersOnMultTeams.containsKey(rosterEntry.getKey()) && boxScores[top].getTeam().getTeamId() != battersOnMultTeams.get(rosterEntry.getKey())) {
						continue;
				}
				rosterBatter = rosterEntry.getValue();
				break;
			}
		}
		else {
			return null;
		}
		return new MLBPlayer(rosterBatter.getMlbPlayerId(), rosterBatter.getFullName(), "P", rosterBatter.getArmThrows(), rosterBatter.getBats(), 
			rosterBatter.getJerseyNumber(), rosterBatter.getSeasonStarted());
	}
	
	private static String displayTeamName(int top, BoxScore[] boxScores) {
		// If playing same team/different years, include year in team name display or non-playoff tournament
		boolean useYear = (boxScores[0].getTeam().getShortTeamName().equals(boxScores[1].getTeam().getShortTeamName())) ||
						  (tournamentMode && !seasonSimulationPlayoffsMode);
		return useYear ? boxScores[top].getTeamAndYearDisplay() : boxScores[top].getTeam().getShortTeamName();
	}
	
	private static MLBTeam getTeamByYearAndFullName(Integer year, String fullName, List<MLBTeam> allTeams) {
		for (MLBTeam team : allTeams) {
			// Null last year means active in current year
			int lastYear = (team.getLastYearPlayed() == null || team.getLastYearPlayed() == 0) ? Calendar.getInstance().get(Calendar.YEAR) : team.getLastYearPlayed();
			if (team.getFirstYearPlayed() <= year && lastYear >= year && team.getFullTeamName().equalsIgnoreCase(fullName)) {
				return team;
			}
		}
		return null;
	}
	
	private static MLBTeam getTeamByYearAndShortName(Integer year, String shortName, List<MLBTeam> allTeams) {
		for (MLBTeam team : allTeams) {
			// Null last year means active in current year
			int lastYear = (team.getLastYearPlayed() == null || team.getLastYearPlayed() == 0) ? Calendar.getInstance().get(Calendar.YEAR) : team.getLastYearPlayed();
			if (team.getFirstYearPlayed() <= year && lastYear >= year && team.getShortTeamName().equalsIgnoreCase(shortName)) {
				return team;
			}
		}
		return null;
	}
	
	private static MLBTeam getTeamByYearAndTeamId(Integer year, Integer teamId, List<MLBTeam> allTeams) {
		for (MLBTeam team : allTeams) {
			// Null last year means active in current year
			int lastYear = (team.getLastYearPlayed() == null || team.getLastYearPlayed() == 0) ? Calendar.getInstance().get(Calendar.YEAR) : team.getLastYearPlayed();
			if (team.getFirstYearPlayed() <= year && lastYear >= year && team.getTeamId() == teamId) {
				return team;
			}
		}
		return null;
	}
	
	private static String padSpaces(String defSpaces, double stat) {
		String spaces = defSpaces;
		if (stat < 100.0) {
			spaces += " ";
		}
		if (stat < 10.0) {
			spaces += " ";
		}
		return spaces;
	}
	
	private static void printlnToScreen(String text) {
		if (!seasonSimulationMode && !seasonSimulationPlayoffsMode && !tournamentMode) {
			System.out.println(text != null ? text : "");
		}
		
	}
	
	private static void printToScreen(String text) {
		if (!seasonSimulationMode && !seasonSimulationPlayoffsMode && !tournamentMode) {
			System.out.print(text);
		}
	}
	
	private static boolean determineUseDH(String league, int year)  {
		boolean useDH = (league.equalsIgnoreCase("AL") && year >= 1973) || year == 2020 || year >= 2022;
		
		return useDH;
	}
	
}
