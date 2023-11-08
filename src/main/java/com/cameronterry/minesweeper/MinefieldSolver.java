package com.cameronterry.minesweeper;

import javafx.util.Pair;

import java.util.HashSet;
import java.util.PriorityQueue;


class ProbabilityTuple implements Comparable<ProbabilityTuple> {
    private float probability;
    private int r;
    private int c;

    public ProbabilityTuple(float probability, int r, int c) {
        this.probability = probability;
        this.r = r;
        this.c = c;
    }

    public float getProbability() {
        return probability;
    }

    public int getR() {
        return r;
    }

    public int getC() {
        return c;
    }

    @Override
    public int compareTo(ProbabilityTuple other) {
        // Assuming min-heap; for max-heap, reverse operands
        return Float.compare(this.probability, other.probability);
    }

    @Override
    public String toString() {
        return String.format("(%.2f, (%d, %d))", probability, r, c);
    }
}

public class MinefieldSolver {

    private static final int[][] DIRECTIONS = {{-1, -1}, {-1, 0}, {-1, 1},
                                               {0, -1}, {0, 1},
                                               {1, -1}, {1, 0}, {1, 1}};

    private float[][] boardProbabilities;
    private MinefieldBoard minefieldBoard;

    public MinefieldSolver(MinefieldBoard board) {
        this.minefieldBoard = board;
        boardProbabilities = new float[board.getRows()][board.getCols()];
    }

    public int remainingCoveredNeighbors(int r, int c) {
        // iterate through neighboring cells
        int remainingNeighbors = 0;
        for (int[] direction : DIRECTIONS) {
            int neighborR = r + direction[0];
            int neighborC = c + direction[1];

            // if neighbor is out of bounds, skip
            if (minefieldBoard.outOfBounds(neighborR, neighborC)) {
                continue;
            }

            Cell neighborCell = minefieldBoard.getBoard()[neighborR][neighborC];
            if (neighborCell.getState() == CellState.COVERED) {
                remainingNeighbors++;
            }
        }

        return remainingNeighbors;
    }


    public float calculateProbabilityV0(int r, int c) {
        float maxProb = 0;

        // iterate through neighboring cells
        for (int[] direction : DIRECTIONS) {
            int neighborR = r + direction[0];
            int neighborC = c + direction[1];

            // if neighbor is out of bounds, skip
            if (minefieldBoard.outOfBounds(neighborR, neighborC)) {
                continue;
            }

            float cellRawValue = minefieldBoard.getBoard()[neighborR][neighborC].getValue().getValue();
            int remCovNeighbors = remainingCoveredNeighbors(neighborR, neighborC);


            maxProb = Math.max(maxProb,
                    (remCovNeighbors == 0) ? (float) Double.POSITIVE_INFINITY :
                            cellRawValue / remainingCoveredNeighbors(neighborR, neighborC));
        }
        return maxProb;
    }

    public PriorityQueue getProbabilities(MinefieldBoard minefieldBoard) {
        // iterate through each uncovered cell, and set the probability of covered cells based on these neighbors
        // the score is calculated by {cellValue} / {remainingNeighbors}
        // covered has cell value infinity
        /*
        e.g.
                0 1 - -
                0 1 - 2
                0 1 1 1
                0 0 0 0

                the covered cell in pos (1, 2) should be assigned probability of 100%
                its neighboring cells are [(0, 1), (0, 2), (0, 3), (1, 1), (1, 3), (2, 1), (2, 2), (2, 3)]

                and
                    (0, 1): 1 / 2 = 0.5
                    (0, 2): infinity
                    (0, 3): infinity
                    (1, 1): 1 / 2 = 0.5
                    (1, 3): 2 / 3 = 0.66
                    (2, 1): 1 / 1 = 1
                    (2, 2): 1 / 1 = 1
                    (2, 3): 1 / 1 = 1

                 we take the max of these probabilities, which is 1, and assign it to the covered cell
         */
        this.minefieldBoard = minefieldBoard;
        PriorityQueue<ProbabilityTuple> probabilities = new PriorityQueue<>();
        Cell[][] board = minefieldBoard.getBoard();
        int rows = minefieldBoard.getRows();
        int cols = minefieldBoard.getCols();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c <cols; c++) {
                Cell cell = board[r][c];
                if (cell.getState() == CellState.UNCOVERED) {
                    boardProbabilities[r][c] = 0;
                } else {
                    boardProbabilities[r][c] = calculateProbabilityV0(r, c);
                    probabilities.add(new ProbabilityTuple(boardProbabilities[r][c], r, c));
                }
            }
        }

        // find all mines identified (probability = 1)
        HashSet<Pair<Integer, Integer>> minesIdentified = new HashSet<>();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (boardProbabilities[r][c] == 1.0f) {
                    minesIdentified.add(new Pair<>(r, c));
                }
            }
        }

        int[][] tempBoard = minefieldBoard.getRawBoard();

        // iterate through all mines identified, and update probabilities of neighboring cells
        for (Pair mine : minesIdentified) {
            // find all neighbors of this mine
            int mineR = (int) mine.getKey();
            int mineC = (int) mine.getValue();

            for (int[] direction : DIRECTIONS) {
                int neighborR = mineR + direction[0];
                int neighborC = mineC + direction[1];

                // if neighbor is out of bounds or covered, skip
                if (minefieldBoard.outOfBounds(neighborR, neighborC)
                        || board[neighborR][neighborC].getState() == CellState.COVERED) {
                    continue;
                }

                tempBoard[neighborR][neighborC] -= 1;
                if (tempBoard[neighborR][neighborC] == 0) {
                    // find all neighbors of this neighbor
                    for (int[] neighborDirection : DIRECTIONS) {
                        int neighborNeighborR = neighborR + neighborDirection[0];
                        int neighborNeighborC = neighborC + neighborDirection[1];

                        // if neighbor is out of bounds or uncovered, skip
                        if (minefieldBoard.outOfBounds(neighborNeighborR, neighborNeighborC)
                                || board[neighborNeighborR][neighborNeighborC].getState() == CellState.UNCOVERED
                        || (neighborNeighborR == mineR && neighborNeighborC == mineC)) {
                            continue;
                        }

                        if (boardProbabilities[neighborNeighborR][neighborNeighborC] == 1.0f) {
                            continue;
                        }
                        boardProbabilities[neighborNeighborR][neighborNeighborC] = 0.0f;
                        probabilities.add(
                                new ProbabilityTuple(boardProbabilities[neighborNeighborR][neighborNeighborC],
                                        neighborNeighborR, neighborNeighborC));
                    }
                }
            }

        }


        return probabilities;
    }

    public String getProbabilitiesString(MinefieldBoard minefieldBoard) {
        int rows = minefieldBoard.getRows();
        int cols = minefieldBoard.getCols();

        StringBuilder probabilitiesString = new StringBuilder();
        for (int r = 0; r < rows; r++) {
            StringBuilder probabilitiesRow = new StringBuilder();
            for (int c = 0; c < cols; c++) {
                // append probability fixed to 2 decimal places
                probabilitiesRow.append(String.format("%.2f", boardProbabilities[r][c])).append(" ");
            }
            probabilitiesString.append(probabilitiesRow).append("\n");
        }
        return probabilitiesString.toString();
    }

    public float[][] getBoardProbabilities() {
        return boardProbabilities;
    }
}
