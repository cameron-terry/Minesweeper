package com.cameronterry.minesweeper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Random;
import javafx.util.Pair;
import java.util.Scanner;

class MinefieldBoardTest {
    MinefieldBoard board;
    int rows, cols, numMines;
    Random rand;
    @BeforeEach
    void setUp() {
        int leftLimit = 9;
        int rightLimit = 9;
        // random number between leftLimit and rightLimit
        rand = new Random(42);
        rows = rand.nextInt(rightLimit - leftLimit + 1) + leftLimit;
        cols = rand.nextInt(rightLimit - leftLimit + 1) + leftLimit;
        // numMines = rand.nextInt(rows * cols - 1) + 1;
        numMines = 10;
        board = new MinefieldBoard(rows, cols, numMines);
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void outOfBounds() {
       assertTrue(board.outOfBounds(-1, -1));
       assertTrue(board.outOfBounds(-1, 0));
       assertFalse(board.outOfBounds(0, 0));
       assertFalse(board.outOfBounds(rows - 1, cols - 1));
       assertTrue(board.outOfBounds(rows, cols));
    }

    @Test
    void initializeBoard() {
        // check that the board is initialized with the correct number of rows and columns
        assertEquals(board.getRows(), rows);
        assertEquals(board.getCols(), cols);
        // check that the board is initialized with the correct number of mines
        assertEquals(board.getNumMines(), numMines);

        // check that each entry in the board is a Cell object and is covered
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                assertNotNull(board.getBoard()[r][c]);
                assertSame(board.getBoard()[r][c].getState(), CellState.COVERED);
            }
        }

        System.out.println(board.getBoardStateStr());
    }

    @Test
    void initializeMines() {
        // check that there are the correct number of mines
        int minesPlaced = 0;
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                if (board.getMines()[r][c]) {
                    minesPlaced++;
                }
            }
        }
        assertEquals(minesPlaced, numMines);
        System.out.println(board.getMineStateStr());
    }

    @Test
    void getBoardStateStr() {
        System.out.println(board.getBoardStateStr());
    }

    @Test
    void getOracleStateStr() {
        System.out.println(board.getOracleStateStr());
    }

    @Test
    void getLegalCells() {
        System.out.println(board.getBoardStateStr());
        HashSet<Pair<Integer, Integer>> legalCells = board.getLegalCells();

        while (!legalCells.isEmpty()) {
            for (Pair<Integer, Integer> cell : legalCells) {
                assertFalse(board.getMines()[cell.getKey()][cell.getValue()]);
            }
            System.out.println("Number of legal cells: " + legalCells.size());

            // uncover a random cell
            Pair<Integer, Integer> randomCell = legalCells.iterator().next();
            board.uncover(randomCell.getKey(), randomCell.getValue());

            // update the legal cells
            legalCells = board.getLegalCells();
        }

        System.out.println(board.getBoardStateStr());

    }

    @Test
    void getUpdatedCellCoverage() {
        board.updateCellCoverageCache();
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                CellState cellState = board.getBoard()[r][c].getState();
                if (cellState == CellState.COVERED || cellState == CellState.FLAGGED) {
                    assertTrue(board.getCoveredCells().contains(new Pair<>(r, c)));
                    assertFalse(board.getUncoveredCells().contains(new Pair<>(r, c)));
                } else {
                    assertFalse(board.getCoveredCells().contains(new Pair<>(r, c)));
                    assertTrue(board.getUncoveredCells().contains(new Pair<>(r, c)));
                }
            }
        }
    }

    @Test
    void getGeneratedSolution() {
        System.out.println(board.getBoardStateStr());
        board.generateSolution();
        System.out.println(board.getBoardStateStr());
        board.coverCells();
        System.out.println(board.getBoardStateStr());
        System.out.println(board.getOracleStateStr());
    }

    @Test
    void uncoverSquares() {
        // find board squares that are equal to 0
        HashSet<Pair<Integer, Integer>> zeroSquares = new HashSet<>();
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                int boardRawCellValue = board.getBoard()[r][c].getValue().getValue();
                if (boardRawCellValue == 0) {
                    zeroSquares.add(new Pair<>(r, c));
                }
            }
        }

        // find the first square surrounded by zero on all sides
        Pair<Integer, Integer> firstZeroSquare = null;
        for (Pair<Integer, Integer> zeroSquare : zeroSquares) {
            int r = zeroSquare.getKey();
            int c = zeroSquare.getValue();
            if (r > 0 && r < board.getRows() - 1 && c > 0 && c < board.getCols() - 1) {
                if (board.getBoard()[r - 1][c].getValue().getValue() == 0 &&
                    board.getBoard()[r + 1][c].getValue().getValue() == 0 &&
                    board.getBoard()[r][c - 1].getValue().getValue() == 0 &&
                    board.getBoard()[r][c + 1].getValue().getValue() == 0) {
                    firstZeroSquare = zeroSquare;
                    break;
                }
            }
        }

        // if there is no square surrounded by zero on all sides, then just pick the first square
        if (firstZeroSquare == null) {
            firstZeroSquare = zeroSquares.iterator().next();
        }

        System.out.println("First zero square: (" + firstZeroSquare.getKey() + ", " + firstZeroSquare.getValue() + ")");

        // uncover the square
        board.uncover(firstZeroSquare.getKey(), firstZeroSquare.getValue());
        System.out.println(board.getBoardStateStr());
    }
}