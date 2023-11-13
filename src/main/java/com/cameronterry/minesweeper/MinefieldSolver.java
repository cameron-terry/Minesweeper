package com.cameronterry.minesweeper;

import javafx.util.Pair;

import java.util.HashSet;
import java.util.PriorityQueue;


record ProbabilityTuple(float probability, int r, int c) implements Comparable<ProbabilityTuple> {

    @Override
    public int compareTo(ProbabilityTuple other) {
        // Config for min-heap; for max-heap, reverse operands
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

    private final float[][] boardProbabilities;
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
                            cellRawValue / remCovNeighbors);
        }
        return maxProb;
    }

    public PriorityQueue getProbabilities(MinefieldBoard minefieldBoard) {
        // iterate through each uncovered cell, and set the probability of covered cells based on these neighbors
        // the score is calculated by {cellValue} / {remainingNeighbors}
        // covered has score of 0
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
                    (0, 2): undef
                    (0, 3): undef
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
        // find all mines identified (probability = 1)
        HashSet<Pair<Integer, Integer>> minesIdentified = new HashSet<>();
        int[][] tempBoard = minefieldBoard.getRawBoard();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Cell cell = board[r][c];
                if (cell.getState() == CellState.UNCOVERED) {
                    boardProbabilities[r][c] = 0;
                } else {
                    float cellProb = calculateProbabilityV0(r, c);
                    if (cellProb == 1.0f) {
                        minesIdentified.add(new Pair<>(r, c));
                    }
                    boardProbabilities[r][c] = cellProb;
                    probabilities.add(new ProbabilityTuple(cellProb, r, c));
                }
            }
        }

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

                        // if neighbor is out of bounds, uncovered, or a mine, skip
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
