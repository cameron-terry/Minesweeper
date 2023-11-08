package com.cameronterry.minesweeper;

import javafx.util.Pair;

import java.util.HashMap;
import java.util.HashSet;

/**
 * CellState: Enum for the state of a cell.
 * <p>
 * Can be: COVERED, UNCOVERED, or FLAGGED
 */
enum CellState {
    COVERED, UNCOVERED, FLAGGED;

    CellState() {
    }

}

/**
 * CellValue: Enum for the value of a cell.
 * Represents the number of mines in the 8 adjacent cells.
 * <p>
 * Can be: MINE, EMPTY, ONE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, or EIGHT
 */
enum CellValue {
    MINE(-1), EMPTY(0), ONE(1),
    TWO(2), THREE(3), FOUR(4),
    FIVE(5), SIX(6), SEVEN(7), EIGHT(8);

    CellValue(final int v) {
        this.value = v;
    }

    int getValue() {
        return this.value;
    }

    private final int value;
}

/**
 * Cell: Class representing a cell in the minefield.
 * <p>
 * A cell has a state CellState and a value CellValue.
 */
class Cell {
    public Cell(CellState state, CellValue value) {
        this.state = state;
        this.value = value;
    }

    CellState getState() {
        return this.state;
    }

    CellValue getValue() {
        return this.value;
    }

    void setState(CellState state) {
        this.state = state;
    }

    void setValue(CellValue value) {
        this.value = value;
    }

    void setCell(CellState state, CellValue value) {
        this.state = state;
        this.value = value;
    }

    private CellState state;
    private CellValue value;
}

class MinefieldBoard {
    MinefieldBoard(int rows, int cols, int numMines) {
        // parameter scaling
        rows = Math.max(Math.min(rows, 30), 9);
        cols = Math.max(Math.min(cols, 30), 9);
        numMines = Math.max(Math.min(numMines, rows * cols), 1);

        this.rows = rows;
        this.cols = cols;
        this.numMines = numMines;
        this.board = new Cell[rows][cols];
        this.mines = new boolean[rows][cols];

        // caching data structures
        this.uncoveredCells = new HashSet<>();
        this.coveredCells = new HashSet<>();
        this.flaggedCells = new HashSet<>();
        this.mineCache = new HashSet<>();

        this.mineFlags = new HashSet<>();
        this.visited = new HashSet<>(); // for dfs

        // setup board
        this.initializeBoardCells();
        this.initializeMines();
        this.generateSolution();
        this.coverCells();
    }

    int dfs(int r, int c) {
        if (this.outOfBounds(r, c) || this.visited.contains(new Pair<>(r, c))) {
            return 0;
        }

        if (this.mines[r][c]) {
            return 1;
        }

        this.visited.add(new Pair<>(r, c));
        int neighborMines = 0;
        int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0},
                {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

        for (int[] direction : directions) {
            neighborMines += this.dfs(r + direction[0], c + direction[1]);
        }
        this.board[r][c].setCell(CellState.UNCOVERED, CellValue.values()[neighborMines + 1]);

        return 0;

    }

    boolean explore(int r, int c) {
        if (this.outOfBounds(r, c) || this.visited.contains(new Pair<>(r, c))) {
            return false;
        }
        CellState state = this.board[r][c].getState();
        if (state == CellState.UNCOVERED || state == CellState.FLAGGED) {
            return false;
        }
        if (this.mines[r][c]) {
            return true;
        }

        this.visited.add(new Pair<>(r, c));
        this.board[r][c].setState(CellState.UNCOVERED);

        if (this.board[r][c].getValue() == CellValue.EMPTY) {
            int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0},
                    {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

            for (int[] direction : directions) {
                this.explore(r + direction[0], c + direction[1]);
            }

        }

        return false;
    }

    boolean uncover(int row, int col) {
        if (this.mines[row][col]) {
            this.board[row][col].setState(CellState.UNCOVERED);
            return true;
        }
        if (this.board[row][col].getState() == CellState.UNCOVERED) {
            return false;
        }

        this.visited.clear();
        boolean res = this.explore(row, col);
        this.updateCellCoverageCache();
        return res;
    }

    void generateSolution() {
        this.visited.clear();
        for (int r = 0; r < this.rows; r++) {
            for (int c = 0; c < this.cols; c++) {
                this.dfs(r, c);
                this.updateCellCoverageCache();
                if (this.getLegalCells().isEmpty()) {
                    return;
                }
            }
        }
        this.visited.clear();
    }

    int getHighestNeighbor() {
        int highestNeighbor = 0;

        for (int r = 0; r < this.rows; r++) {
            for (int c = 0; c < this.cols; c++) {
                int rawCellValue = this.board[r][c].getValue().getValue();
                highestNeighbor = Math.max(highestNeighbor, rawCellValue);
            }
        }

        return highestNeighbor;
    }

    void flagCell(int row, int col) {
        if (this.board[row][col].getState() == CellState.COVERED) {
            this.board[row][col].setState(CellState.FLAGGED);
        } else if (this.board[row][col].getState() == CellState.FLAGGED) {
            this.board[row][col].setState(CellState.COVERED);
        }
        this.updateCellCoverageCache();
    }

    void coverCells() {
        for (int r = 0; r < this.rows; r++) {
            for (int c = 0; c < this.cols; c++) {

                    if (this.board[r][c].getState() == CellState.UNCOVERED) {
                        this.board[r][c] = new Cell(CellState.COVERED, this.board[r][c].getValue());
                    }
            }
        }

        this.updateCellCoverageCache();
    }

    boolean outOfBounds(int r, int c) {
        return Math.min(r, c) < 0 || r >= this.rows || c >= this.cols;
    }

    void updateCellCoverageCache() {
        this.uncoveredCells.clear();
        this.coveredCells.clear();
        this.flaggedCells.clear();

        for (int r = 0; r < this.rows; r++) {
            for (int c = 0; c < this.cols; c++) {
                CellState state = this.board[r][c].getState();

                if (state == CellState.UNCOVERED) {
                    this.uncoveredCells.add(new Pair<>(r, c));
                } else if (state == CellState.COVERED) {
                    this.coveredCells.add(new Pair<>(r, c));
                } else if (state == CellState.FLAGGED) {
                    this.flaggedCells.add(new Pair<>(r, c));
                }
            }
        }
    }

    void initializeBoardCells() {
        for (int r = 0; r < this.rows; r++) {
            for (int c = 0; c < this.cols; c++) {
                this.board[r][c] = new Cell(CellState.COVERED, CellValue.EMPTY);
                this.coveredCells.add(new Pair<>(r, c));
            }
        }
    }

    void initializeMines() {
        int minesPlaced = 0;
        while (minesPlaced < this.numMines) {
            int r = (int) (Math.random() * this.rows);
            int c = (int) (Math.random() * this.cols);
            if (!this.mines[r][c]) {
                this.mines[r][c] = true;
                this.mineCache.add(new Pair<>(r, c));
                minesPlaced++;
            }
        }

        // set cell values
        for (int r = 0; r < this.rows; r++) {
            for (int c = 0; c < this.cols; c++) {
                if (this.mines[r][c]) {
                    this.board[r][c] = new Cell(CellState.COVERED, CellValue.MINE);
                }
            }
        }
    }

    String getBoardStateStr() {
        StringBuilder board_str = new StringBuilder("Minefield(grid_size=(" + this.rows + ", " + this.cols + "), mines=" + this.numMines + ")\n");

        for (int r = 0; r < this.rows; r++) {
            StringBuilder row = new StringBuilder();
            for (int c = 0; c < this.cols; c++) {
                Cell cell = this.board[r][c];

                if (cell.getState() == CellState.UNCOVERED) {
                    if (cell.getValue() == CellValue.MINE) {
                        row.append("X ");
                    } else {
                        row.append(cell.getValue().getValue());
                        row.append(" ");
                    }

                } else if (cell.getState() == CellState.FLAGGED) {
                    row.append("✓ ");
                } else {
                    row.append("- ");
                }

            }
            board_str.append(row).append("\n");
        }

        return board_str.toString();
    }

    String getBoardStateStr(HashMap<Pair<Integer, Integer>, String> markSquares) {
        StringBuilder board_str = new StringBuilder("Minefield(grid_size=(" + this.rows + ", " + this.cols + "), mines=" + this.numMines + ")\n");

        for (int r = 0; r < this.rows; r++) {
            StringBuilder row = new StringBuilder();
            for (int c = 0; c < this.cols; c++) {
                if (markSquares.containsKey(new Pair<>(r, c))) {
                    row.append(markSquares.get(new Pair<>(r, c)));
                    row.append(" ");
                    continue;
                }
                Cell cell = this.board[r][c];

                if (cell.getState() == CellState.UNCOVERED) {
                    if (cell.getValue() == CellValue.MINE) {
                        row.append("X ");
                    } else {
                        row.append(cell.getValue().getValue());
                        row.append(" ");
                    }

                } else if (cell.getState() == CellState.FLAGGED) {
                    row.append("✓ ");
                } else {
                    row.append("- ");
                }

            }
            board_str.append(row).append("\n");
        }

        return board_str.toString();
    }
    String getOracleStateStr() {
        HashMap<String, String> asciiMapping = new HashMap<>();
        asciiMapping.put("-1", "X");
        for (int i = 0; i < 9; i++) {
            asciiMapping.put(Integer.toString(i), Integer.toString(i));
        }

        StringBuilder board_str = new StringBuilder("Oracle:Minefield(grid_size=(" + this.rows + ", " + this.cols + "), mines=" + this.numMines + ")\n");
        for (int r = 0; r < this.rows; r++) {
            StringBuilder row = new StringBuilder();
            for (int c = 0; c < this.cols; c++) {
                Cell cell = this.board[r][c];
                int cellValue = cell.getValue().getValue();

                if (cell.getState() == CellState.UNCOVERED) {
                    row.append(cellValue);
                    row.append(" ");
                } else if (cell.getState() == CellState.FLAGGED) {
                    row.append("✓ ");
                } else {
                    row.append(asciiMapping.get(Integer.toString(cellValue)));
                    row.append(" ");
                }
            }
            board_str.append(row).append("\n");
        }

        return board_str.toString();
    }

    String getMineStateStr() {
        StringBuilder board_str = new StringBuilder("Minefield(grid_size=(" + this.rows + ", " + this.cols + "), mines=" + this.numMines + ")\n");
        for (int r = 0; r < this.rows; r++) {
            StringBuilder row = new StringBuilder();
            for (int c = 0; c < this.cols; c++) {
                if (this.mines[r][c]) {
                    row.append("X ");
                } else {
                    row.append("- ");
                }
            }
            board_str.append(row).append("\n");
        }
        return board_str.toString();
    }

    HashSet<Pair<Integer, Integer>> getLegalCells() {
        HashSet<Pair<Integer, Integer>> legalCells = new HashSet<>();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                // if cell is not covered and not a mine
                if (!(this.board[r][c].getState() == CellState.UNCOVERED || this.mines[r][c])) {
                    legalCells.add(new Pair<>(r, c));
                }
            }
        }
        return legalCells;
    }

    private final int rows, cols, numMines;
    private Cell[][] board;
    private boolean[][] mines;

    private HashSet<Pair<Integer, Integer>> uncoveredCells, coveredCells, flaggedCells, mineFlags, mineCache, visited;

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public int getNumMines() {
        return numMines;
    }

    public boolean[][] getMines() {
        return mines;
    }

    public Cell[][] getBoard() {
        return board;
    }

    public int[][] getRawBoard() {
        int[][] rawBoard = new int[this.rows][this.cols];
        for (int r = 0; r < this.rows; r++) {
            for (int c = 0; c <this.cols; c++) {
                rawBoard[r][c] = this.board[r][c].getValue().getValue();
            }
        }
        return rawBoard;
    }

    public void setBoard(Cell[][] board) {
        this.board = board;
    }

    public HashSet<Pair<Integer, Integer>> getUncoveredCells() {
        return uncoveredCells;
    }

    public int[][] getUncoveredCellsAsArray() {
        int[][] uncoveredCellsArray = new int[this.rows][this.cols];
        for (int r = 0; r < this.rows; r++) {
            for (int c = 0; c < this.cols; c++) {
                uncoveredCellsArray[r][c] = (this.uncoveredCells.contains(new Pair<>(r, c)) ? 1 : 0);
            }
        }
        return uncoveredCellsArray;
    }

    public HashSet<Pair<Integer, Integer>> getCoveredCells() {
        return coveredCells;
    }

    public int[][] getCoveredCellsAsArray() {
        int[][] coveredCellsArray = new int[this.rows][this.cols];
        for (int r = 0; r < this.rows; r++) {
            for (int c = 0; c < this.cols; c++) {
                coveredCellsArray[r][c] = (this.coveredCells.contains(new Pair<>(r, c)) ? 1 : 0);
            }
        }
        return coveredCellsArray;
    }

    public HashSet<Pair<Integer, Integer>> getFlaggedCells() {
        return flaggedCells;
    }

    public int[][] getFlaggedCellsAsArray() {
        int[][] flaggedCellsArray = new int[this.rows][this.cols];
        for (int r = 0; r < this.rows; r++) {
            for (int c = 0; c < this.cols; c++) {
                flaggedCellsArray[r][c] = (this.flaggedCells.contains(new Pair<>(r, c)) ? 1 : 0);
            }
        }
        return flaggedCellsArray;
    }

    public HashSet<Pair<Integer, Integer>> getMineCache() {
        return mineCache;
    }


}
