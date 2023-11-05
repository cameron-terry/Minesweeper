package com.cameronterry.minesweeper;

import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Pair;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

public class MinesweeperController {
    @FXML
    private Label statusLabel;

    @FXML
    private Label minesLabel;

    @FXML
    private Label timerLabel;

    @FXML
    private Button startButton;

    @FXML
    private GridPane minesweeperGrid;

    private boolean timerOn = false;

    private IntegerProperty secondsPassed = new SimpleIntegerProperty(0);
    private Timeline timeline;

    private MinefieldBoard minefieldBoard;

    private HashMap<String, String> asciiMapping;

    private int boardRows = 9, boardCols = 9, boardMines = 10;

    private ImageView getImage(String imagePath, int width, int height) {
        Image minesweeperImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream(imagePath)));
        ImageView minesweeperImageView = new ImageView(minesweeperImage);
        minesweeperImageView.setFitWidth(width);
        minesweeperImageView.setFitHeight(height);
        minesweeperImageView.setPreserveRatio(false);

        return minesweeperImageView;
    }

    private void setStatusLabel(String imagePath) {
        ImageView minesweeperImageView = getImage(imagePath, 50, 50);
        statusLabel.setGraphic(minesweeperImageView);
        statusLabel.setPadding(Insets.EMPTY);
        statusLabel.setStyle("-fx-background-color: transparent;");
        statusLabel.setText("");
    }

    private HashMap<Integer, Color> numberColorMapping = new HashMap<>();

    {
        numberColorMapping.put(1, Color.BLUE);
        numberColorMapping.put(2, Color.GREEN);
        numberColorMapping.put(3, Color.RED);
        numberColorMapping.put(4, Color.DARKBLUE);
        numberColorMapping.put(5, Color.DARKRED);
        numberColorMapping.put(6, Color.DARKCYAN);
        numberColorMapping.put(7, Color.BLACK);
        numberColorMapping.put(8, Color.GRAY);
    }

    private Button getCellButton(int row, int col) {
        return (Button) minesweeperGrid.getChildren().get(row * minefieldBoard.getCols() + col);
    }

    @FXML
    private void onConfigureBoard(ActionEvent event) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("board-config-form.fxml"));
            VBox boardConfigBox = fxmlLoader.load();

            // Secondary controller for handling BoardConfigForm.fxml
            BoardConfigController configController = fxmlLoader.getController();
            configController.setInitialValues(boardRows, boardCols, boardMines);

            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setTitle("Configure Board");
            popupStage.setScene(new Scene(boardConfigBox));

            // Set the action for when the OK button is clicked
            configController.setOkAction(() -> {
                boardRows = Integer.parseInt(configController.getRowsField().getText());
                boardCols = Integer.parseInt(configController.getColsField().getText());
                boardMines = Integer.parseInt(configController.getMinesField().getText());

                 // Initialize board with the provided parameters
                 minefieldBoard = new MinefieldBoard(boardRows, boardCols, boardMines);
                 minefieldBoard.updateCellCoverageCache();

                 // reset the grid with the new board
                    minesweeperGrid.getChildren().clear();
                    minesweeperGrid.getRowConstraints().clear();
                    minesweeperGrid.getColumnConstraints().clear();
                    populateGrid();

                    for (int r = 0; r < minefieldBoard.getRows(); r++) {
                        for (int c = 0; c < minefieldBoard.getCols(); c++) {
                            this.updateCell(r, c);
                            Button cellButton = getCellButton(r, c);
                            cellButton.setStyle("-fx-text-fill: black; -fx-font-size: 20px;");
                            // clear images
                            cellButton.setGraphic(null);

                        }
                    }

                    // Reset timer to 0
                    secondsPassed.set(0);

                    // Bind the timerLabel text property to the secondsPassed property with a custom string format
                    timerLabel.textProperty().bind(Bindings.createStringBinding(() ->
                            "Time: " + formatTime(secondsPassed.get()), secondsPassed));

                popupStage.close();
            });

            this.stopTimer();
            this.setStatusLabel("/images/minesweeper_default.png");

            popupStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private String formatTime(int secondsPassed) {
        // Format the seconds into hours, minutes, and seconds
        int hours = secondsPassed / 3600;
        int minutes = (secondsPassed % 3600) / 60;
        int seconds = secondsPassed % 60;
        return (hours > 0 ? String.format("%02d:%02d:%02d", hours, minutes, seconds) : String.format("%02d:%02d", minutes, seconds));
    }

    public void stopTimer() {
        if (timeline != null) {
            timeline.stop();
            timerOn = false;
            timerLabel.textProperty().unbind(); // Unbind the property
            timerLabel.setText("Time: " + formatTime(secondsPassed.get())); // Update one last time manually
        }
    }

    @FXML
    protected void onMinesweeperButtonClick() {
        this.setStatusLabel("/images/minesweeper_default.png");

        minefieldBoard = new MinefieldBoard(boardRows, boardCols, boardMines);
        minefieldBoard.updateCellCoverageCache();
        for (int r = 0; r < minefieldBoard.getRows(); r++) {
            for (int c = 0; c < minefieldBoard.getCols(); c++) {
                this.updateCell(r, c);
                Button cellButton = getCellButton(r, c);
                cellButton.setStyle("-fx-text-fill: black; -fx-font-size: 20px;");
                // clear images
                cellButton.setGraphic(null);

            }
        }
        // Reset timer to 0
        secondsPassed.set(0);

        // Bind the timerLabel text property to the secondsPassed property with a custom string format
        timerLabel.textProperty().bind(Bindings.createStringBinding(() ->
                "Time: " + formatTime(secondsPassed.get()), secondsPassed));

    }

    public void initialize() {
        this.setStatusLabel("/images/minesweeper_default.png");

        // Initialize the MinefieldBoard with 9 rows, 9 columns, and 10 mines.
        minefieldBoard = new MinefieldBoard(9, 9, 10);
        asciiMapping = new HashMap<>();
        asciiMapping.put("-1", "");
        asciiMapping.put("0", " ");
        for (int i = 1; i < 9; i++) {
            asciiMapping.put(Integer.toString(i), Integer.toString(i));
        }

        // Populate the GridPane with buttons
        populateGrid();

        // Reset timer to 0
        secondsPassed.set(0);
        // Bind the timerLabel text property to the secondsPassed property
        timerLabel.textProperty().bind(Bindings.createStringBinding(() ->
                "Time: " + formatTime(secondsPassed.get()), secondsPassed));
    }

    private void populateGrid() {
        // Define column constraints so that each cell has an equal width
        for (int i = 0; i < minefieldBoard.getCols(); i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(100.0 / minefieldBoard.getCols()); // Equal width
            minesweeperGrid.getColumnConstraints().add(column);
        }

        // Define row constraints so that each cell has an equal height
        for (int i = 0; i < minefieldBoard.getRows(); i++) {
            RowConstraints row = new RowConstraints();
            row.setPercentHeight(100.0 / minefieldBoard.getRows()); // Equal height
            minesweeperGrid.getRowConstraints().add(row);
        }

        // Set the grid's gaps to a minimal value to use the space more efficiently
        minesweeperGrid.setHgap(2); // horizontal gap
        minesweeperGrid.setVgap(2); // vertical gap


        // Add buttons to the grid
        for (int row = 0; row < minefieldBoard.getRows(); row++) {
            for (int col = 0; col < minefieldBoard.getCols(); col++) {
                Button cellButton = new Button();
                cellButton.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                // set font-size to 20px
                cellButton.setStyle("-fx-font-size: 20px; -fx-font-weight: bold");
                int finalRow = row;
                int finalCol = col;
                cellButton.setOnMouseClicked(e ->
                {
                    if (e.getButton() == MouseButton.PRIMARY) {
                        handleCellClick(finalRow, finalCol);
                    } else if (e.getButton() == MouseButton.SECONDARY) {
                        minefieldBoard.flagCell(finalRow, finalCol);
                        this.minefieldBoard.updateCellCoverageCache();
                        this.updateCell(finalRow, finalCol);
                    }
                });
                minesweeperGrid.add(cellButton, col, row);
                GridPane.setFillWidth(cellButton, true);
                GridPane.setFillHeight(cellButton, true);
            }
        }

        // Force the grid to grow and fill the available vertical space.
        VBox.setVgrow(minesweeperGrid, Priority.ALWAYS);
    }


    private void handleCellClick(int row, int col) {
        if (!timerOn) {
            // Create a Timeline that updates every second
            timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
                secondsPassed.set(secondsPassed.get() + 1);
            }));
            timeline.setCycleCount(Timeline.INDEFINITE); // Let the timeline run indefinitely
            timeline.play(); // Start the timeline
            timerOn = true;
        }
        boolean hitMine = minefieldBoard.uncover(row, col);
        for (int r = 0; r < minefieldBoard.getRows(); r++) {
            for (int c = 0; c < minefieldBoard.getCols(); c++) {
                this.updateCell(r, c);
            }
        }
        // win/lose conditions
        if (hitMine || minefieldBoard.getLegalCells().isEmpty()) {
            stopTimer();
            if (hitMine) {
                setStatusLabel("/images/minesweeper_loss.png");
            }
            else {
                setStatusLabel("/images/minesweeper_win.png");
            }
            // Load the image outside the loop
            String mineImage = (hitMine) ? "/images/mine.png" : "/images/flag.png";
            Button cellButton;

            for (Pair<Integer, Integer> mine : minefieldBoard.getMineCache()) {
                int rMine = mine.getKey();
                int cMine = mine.getValue();

                if (minefieldBoard.getBoard()[rMine][cMine].getState() == CellState.FLAGGED) {
                    continue;
                }

                minefieldBoard.getBoard()[rMine][cMine].setState(CellState.UNCOVERED);
                cellButton = getCellButton(rMine, cMine);

                // Set the button graphic to the ImageView with the mine image
                ImageView individualMineImageView = getImage(mineImage, (int) cellButton.getWidth(), (int) cellButton.getHeight());

                cellButton.setGraphic(individualMineImageView);
                cellButton.setPadding(Insets.EMPTY);
                cellButton.setStyle("-fx-background-color: transparent;");
            }

            minefieldBoard.updateCellCoverageCache();
            for (int r = 0; r < minefieldBoard.getRows(); r++) {
                for (int c = 0; c < minefieldBoard.getCols(); c++) {
                    this.updateCell(r, c);
                    cellButton = getCellButton(r, c);
                    cellButton.setDisable(true);
                }
            }

            // change flagged wrong cells to misflag.png picture
            for (Pair<Integer, Integer> flagged: minefieldBoard.getFlaggedCells()) {
                if (!minefieldBoard.getMineCache().contains(flagged)) {
                    int rFlagged = flagged.getKey();
                    int cFlagged = flagged.getValue();
                    minefieldBoard.getBoard()[rFlagged][cFlagged].setState(CellState.UNCOVERED);
                    int index = rFlagged * minefieldBoard.getCols() + cFlagged;
                    cellButton = (Button) minesweeperGrid.getChildren().get(index);
                    ImageView misflagImageView = getImage("/images/misflag.png", (int) cellButton.getWidth(), (int) cellButton.getHeight());

                    cellButton.setGraphic(misflagImageView);
                    cellButton.setPadding(Insets.EMPTY);
                    cellButton.setStyle("-fx-background-color: transparent;");
                }
            }
        }
    }

    private void updateCell(int row, int col) {
        // Here, update the button based on the state of the cell.

        Button cellButton = getCellButton(row, col);
        Cell cell = minefieldBoard.getBoard()[row][col];

        // Logic to update the button text and style based on the cell state
        // For example:
        if (minefieldBoard.getUncoveredCells().contains(new Pair<>(row, col))) {
            CellValue cellValue = cell.getValue();
            int rawCellValue = cell.getValue().getValue();

            if (cellValue == CellValue.EMPTY) {
                ImageView emptyImageView = getImage("/images/empty.png", (int) cellButton.getWidth(), (int) cellButton.getHeight());
                cellButton.setGraphic(emptyImageView);
                cellButton.setPadding(Insets.EMPTY);
                cellButton.setStyle("-fx-background-color: transparent;");

            } else {
                Color textColor = numberColorMapping.getOrDefault(rawCellValue, Color.BLACK);
                String colorStyle = String.format("-fx-text-fill: #%02X%02X%02X; -fx-font-size: 20px;",
                        (int) (textColor.getRed() * 255),
                        (int) (textColor.getGreen() * 255),
                        (int) (textColor.getBlue() * 255));

                cellButton.setStyle(colorStyle);
                cellButton.setText(asciiMapping.get(Integer.toString(rawCellValue)));
                cellButton.setDisable(true);
            }
        } else if (minefieldBoard.getCoveredCells().contains(new Pair<>(row, col))) {
            cellButton.setText("");
            cellButton.setDisable(false);
            cellButton.setGraphic(null);
            // set the cellButton back to normal
            cellButton.setStyle("-fx-text-fill: black; -fx-font-size: 20px;");
        } else if (minefieldBoard.getFlaggedCells().contains(new Pair<>(row, col))) {
            ImageView flagImageView = getImage("/images/flag.png", (int) cellButton.getWidth(), (int) cellButton.getHeight());
            cellButton.setGraphic(flagImageView);
            cellButton.setPadding(Insets.EMPTY);
            cellButton.setStyle("-fx-background-color: transparent;");

        }

        this.minesLabel.setText("Mines: " + Math.max(0, minefieldBoard.getNumMines() - minefieldBoard.getFlaggedCells().size()));
    }

    // Other methods for game logic...
}





