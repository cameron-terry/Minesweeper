package com.cameronterry.minesweeper;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

class GameRecord {
    private final int rows;
    private final int cols;
    private final int numMines;
    private final int[][] boardState;

    private final int[][] uncoveredCells;
    private final int[][] flaggedCells;
    private final int highestNumber;
    private final int finalTime;
    private final LocalDateTime dateTime;

    // constructor that accepts a Map
    public GameRecord(Map<String, Object> gameData) {
        int[][] flaggedCells1;
        int[][] uncoveredCells1;
        // The boardSize key in JSON corresponds to rows, cols, and numMines
        List<Double> boardSize = (List<Double>) gameData.get("boardSize");
        this.rows = boardSize.get(0).intValue();
        this.cols = boardSize.get(1).intValue();
        this.numMines = boardSize.get(2).intValue();

        this.highestNumber = ((Double) gameData.get("highestNumber")).intValue();
        this.finalTime = ((Double) gameData.get("finalTime")).intValue();
        this.dateTime = LocalDateTime.parse((String) gameData.get("dateTime"), DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        // Deserialize the finalBoardState properly as a 2D array of integers
        List<List<Double>> boardStateList = (List<List<Double>>) gameData.get("finalBoardState");

        this.boardState = new int[boardStateList.size()][boardStateList.get(0).size()];
        for (int i = 0; i < boardStateList.size(); i++) {
            for (int j = 0; j < boardStateList.get(i).size(); j++) {
                this.boardState[i][j] = boardStateList.get(i).get(j).intValue();
            }
        }

        try {
            List<List<Double>> uncoveredCellsList = (List<List<Double>>) gameData.get("uncoveredCells");
            List<List<Double>> flaggedCellsList = (List<List<Double>>) gameData.get("flaggedCells");

            uncoveredCells1 = new int[uncoveredCellsList.size()][uncoveredCellsList.get(0).size()];
            flaggedCells1 = new int[flaggedCellsList.size()][flaggedCellsList.get(0).size()];

            for (int i = 0; i < boardStateList.size(); i++) {
                for (int j = 0; j < boardStateList.get(i).size(); j++) {
                    uncoveredCells1[i][j] = uncoveredCellsList.get(i).get(j).intValue();
                    flaggedCells1[i][j] = flaggedCellsList.get(i).get(j).intValue();
                }
            }
        } catch (NullPointerException e) {
            uncoveredCells1 = null;
            flaggedCells1 = null;
        }

        this.flaggedCells = flaggedCells1;
        this.uncoveredCells = uncoveredCells1;
    }
    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public int getNumMines() {
        return numMines;
    }

    public int[][] getBoardState() {
        return boardState;
    }

    public int getHighestNumber() {
        return highestNumber;
    }

    public int getFinalTime() {
        return finalTime;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }
}

class MinesweeperGameLoader {

    public static List<GameRecord> loadGamesFromFile(String fileName) {
        List<GameRecord> gameRecords = new ArrayList<>();
        Gson gson = new Gson();

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                Type type = new TypeToken<Map<String, Object>>() {}.getType();
                Map<String, Object> gameData = gson.fromJson(line, type);

                GameRecord record = new GameRecord(gameData);
                gameRecords.add(record);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return gameRecords;
    }
}

// TODO: should store uncovered cells as well
public class MinesweeperLogging {
    private Map<String, Object> gameData;

    MinesweeperLogging() {

    }

    public String saveGame(MinefieldBoard minefieldBoard, int finalTime, LocalDateTime dateTime) {
        int rows = minefieldBoard.getRows();
        int cols = minefieldBoard.getCols();
        int numMines = minefieldBoard.getNumMines();

        // save the board state
        int[] boardInfo = new int[3];
        boardInfo[0] = rows;
        boardInfo[1] = cols;
        boardInfo[2] = numMines;

        int[][] boardState = new int[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c <cols; c++) {
                boardState[r][c] = minefieldBoard.getBoard()[r][c].getValue().getValue();
            }
        }

        int[][] uncoveredCells = minefieldBoard.getUncoveredCellsAsArray();
        int[][] flaggedCells = minefieldBoard.getFlaggedCellsAsArray();

        // Create a map or a POJO class to hold your game data
        gameData = new HashMap<>();
        gameData.put("boardSize", boardInfo);
        gameData.put("uncoveredCells", uncoveredCells);
        gameData.put("flaggedCells", flaggedCells);
        gameData.put("finalBoardState", boardState);
        gameData.put("highestNumber", minefieldBoard.getHighestNeighbor());
        gameData.put("finalTime", finalTime);
        gameData.put("dateTime", dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // Convert map to JSON string using Gson
        Gson gson = new Gson();
        return gson.toJson(gameData);
    }

    public static void saveGameToFile(String jsonString, String fileName) {
        // Use try-with-resources to manage FileWriter
        try (FileWriter file = new FileWriter(fileName, true)) { // true to append, false to overwrite.
            file.write(jsonString + System.lineSeparator()); // Append a new line after the JSON string
            // No need to explicitly close the file, try-with-resources handles it
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Object> getGameData() {
        return gameData;
    }
}

