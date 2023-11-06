package com.cameronterry.minesweeper;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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

    @FXML
    private HBox leftStarsBox;

    @FXML
    private HBox rightStarsBox;

    private boolean timerOn = false;

    private IntegerProperty secondsPassed = new SimpleIntegerProperty(0);
    private Timeline timeline;

    private MinefieldBoard minefieldBoard;

    private final HashMap<String, String> asciiMapping = new HashMap<>();

    {
        asciiMapping.put("-1", "");
        for (int i = 0; i < 9; i++) {
            asciiMapping.put(Integer.toString(i), Integer.toString(i));
        }
    }

    private int boardRows = 9, boardCols = 9, boardMines = 10;

    private final MinesweeperLogging logger = new MinesweeperLogging();

    public Polygon createStar(double centerX, double centerY, double innerRadius, double outerRadius, int numRays, Color fillColor) {
        Polygon star = new Polygon();
        double deltaAngle = Math.PI / numRays;

        for (int i = 0; i < numRays * 2; i++) {
            double radius = (i % 2 == 0) ? outerRadius : innerRadius;
            double angle = i * deltaAngle;
            double x = centerX + radius * Math.sin(angle);
            double y = centerY - radius * Math.cos(angle);
            star.getPoints().addAll(x, y);
        }

        star.setFill(fillColor);
        // border for the star
         star.setStroke(Color.BLACK); // Color of the star's border
         star.setStrokeWidth(1); // Width of the star's border

        return star;
    }

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

    private final HashMap<Integer, Color> numberColorMapping = new HashMap<>();

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
                leftStarsBox.getChildren().clear();
                rightStarsBox.getChildren().clear();

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

    private void loadFinishedGame(GameRecord record) {
        leftStarsBox.getChildren().clear();
        rightStarsBox.getChildren().clear();
        setStatusLabel("/images/minesweeper_win.png");
        displayWinStars(record.getHighestNumber());

        boardRows = record.getRows();
        boardCols = record.getCols();
        boardMines = record.getNumMines();
        minefieldBoard = new MinefieldBoard(boardRows, boardCols, boardMines);
        for (int r = 0; r < minefieldBoard.getRows(); r++) {
            for (int c = 0; c < minefieldBoard.getCols(); c++) {
                int rawCellValue = record.getBoardState()[r][c];
                if (rawCellValue == -1) {
                    minefieldBoard.getBoard()[r][c].setCell(CellState.FLAGGED, CellValue.MINE);
                } else {
                    CellValue recordCellValue = CellValue.values()[record.getBoardState()[r][c] + 1];
                    minefieldBoard.getBoard()[r][c].setCell(CellState.UNCOVERED, recordCellValue);
                }
            }
        }
        minefieldBoard.updateCellCoverageCache();

        // reset the grid with the new board
        minesweeperGrid.getChildren().clear();
        minesweeperGrid.getRowConstraints().clear();
        minesweeperGrid.getColumnConstraints().clear();
        populateGrid();

        for (int r = 0; r < boardRows; r++) {
            for (int c = 0; c < boardCols; c++) {
                this.updateCell(r, c);
                Button cellButton = getCellButton(r, c);
                cellButton.setStyle("-fx-font-size: 20px;");
                // clear images
                cellButton.setGraphic(null);
                cellButton.setDisable(true);

                // set the color of the number
                int rawCellValue = record.getBoardState()[r][c];
                if (rawCellValue > 0) {
                    Color textColor = numberColorMapping.getOrDefault(rawCellValue, Color.BLACK);
                    String colorStyle = String.format("-fx-text-fill: #%02X%02X%02X; -fx-font-size: 20px;",
                            (int) (textColor.getRed() * 255),
                            (int) (textColor.getGreen() * 255),
                            (int) (textColor.getBlue() * 255));

                    cellButton.setStyle(colorStyle);
                } else if (rawCellValue == -1) {
                    // make the button red
                    cellButton.setStyle("-fx-background-color: red; -fx-font-size: 20px;");
                    cellButton.setText("^");
                } else {
                    cellButton.setStyle("-fx-background-color: darkgray; -fx-font-size: 20px;");
                    cellButton.setText("-");
                }

                if (rawCellValue == record.getHighestNumber()) {
                    Color textColor = numberColorMapping.getOrDefault(rawCellValue, Color.BLACK);
                    String colorStyle = String.format("-fx-text-fill: #%02X%02X%02X; -fx-font-size: 20px; -fx-border-color: gold; -fx-border-width: 2px;",
                            (int) (textColor.getRed() * 255),
                            (int) (textColor.getGreen() * 255),
                            (int) (textColor.getBlue() * 255));

                    cellButton.setStyle(colorStyle);
                }
            }
        }


        // Reset timer to 0
        secondsPassed = new SimpleIntegerProperty(record.getFinalTime());

        // Bind the timerLabel text property to the secondsPassed property with a custom string format
        timerLabel.textProperty().bind(Bindings.createStringBinding(() ->
                "Time: " + formatTime(secondsPassed.get()), secondsPassed));

        this.minesLabel.setText("Mines: " + minefieldBoard.getNumMines());
    }

    @FXML
    private void onSaveGame(ActionEvent event) {
        String gameState = logger.saveGame(minefieldBoard, secondsPassed.get(), LocalDateTime.now());
        MinesweeperLogging.saveGameToFile(gameState, "unfinished_games.json");
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
        leftStarsBox.getChildren().clear();
        rightStarsBox.getChildren().clear();
        setStatusLabel("/images/minesweeper_default.png");

        minefieldBoard = new MinefieldBoard(boardRows, boardCols, boardMines);
        minefieldBoard.updateCellCoverageCache();
        for (int r = 0; r < minefieldBoard.getRows(); r++) {
            for (int c = 0; c < minefieldBoard.getCols(); c++) {
                updateCell(r, c);
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


    @FXML
    private void onShowRecentFinishedGames() {
        Stage recentGamesStage = new Stage();
        recentGamesStage.setTitle("Recent Games");

        // Create a TableView for the game records
        TableView<GameRecord> tableView = new TableView<>();

        // Define the game info column
        TableColumn<GameRecord, GameRecord> gameInfoColumn = new TableColumn<>("Game Information");
        gameInfoColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));

        gameInfoColumn.setCellFactory(column -> new TableCell<GameRecord, GameRecord>() {
            @Override
            protected void updateItem(GameRecord item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    int neighborMax = item.getHighestNumber();
                    Circle colorCircle = new Circle(neighborMax, numberColorMapping.getOrDefault(neighborMax, Color.BLACK));
                    String gameInfo = String.format("(%d, %d) → %d | %s",
                            item.getRows(), item.getCols(), item.getNumMines(),
                            formatSecondsAsMMSS(item.getFinalTime()));
                    Text gameInfoText = new Text(gameInfo);

                    // Create a HBox to hold the circle and the text
                    HBox hbox = new HBox(colorCircle, gameInfoText);
                    hbox.setSpacing(10); // Set spacing between circle and text
                    hbox.setAlignment(Pos.CENTER_LEFT); // Align contents to the left

                    // Set the graphic of the cell to our HBox
                    setGraphic(hbox);
                }
            }
        });

        // Define the date column
        TableColumn<GameRecord, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(cellData -> {
            GameRecord record = cellData.getValue();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");
            return new SimpleStringProperty(record.getDateTime().format(dateTimeFormatter));
        });

        // Add columns to the TableView
        tableView.getColumns().add(gameInfoColumn);
        tableView.getColumns().add(dateColumn);

        // Populate the TableView with game records
        List<GameRecord> previousGames = MinesweeperGameLoader.loadGamesFromFile("finished_games.json");
        Collections.reverse(previousGames); // Assuming you want to reverse the list order
        tableView.getItems().setAll(previousGames);

        // Event listener for selection
        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                loadFinishedGame(newSelection);
            }
        });

        // Make sure TableView takes up all available space
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Create the scene and show the stage
        Scene scene = new Scene(new BorderPane(tableView), 400, 300); // You might want to adjust the size
        recentGamesStage.setScene(scene);
        recentGamesStage.show();
    }

    private String formatSecondsAsMMSS(int totalSecs) {
        int minutes = totalSecs / 60;
        int seconds = totalSecs % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public void initialize() {
        leftStarsBox.getChildren().clear();
        rightStarsBox.getChildren().clear();
        this.setStatusLabel("/images/minesweeper_default.png");

        // Initialize the MinefieldBoard with 9 rows, 9 columns, and 10 mines.
        minefieldBoard = new MinefieldBoard(9, 9, 10);

        // Populate the GridPane with buttons
        populateGrid();

        // Reset timer to 0
        secondsPassed.set(0);
        // Bind the timerLabel text property to the secondsPassed property
        timerLabel.textProperty().bind(Bindings.createStringBinding(() ->
                "Time: " + formatTime(secondsPassed.get()), secondsPassed));
    }

    public void displayWinStars(int numberOfStars) {
        leftStarsBox.getChildren().clear();
        rightStarsBox.getChildren().clear();

        int starsRight = numberOfStars / 2;
        int starsLeft = numberOfStars - starsRight;

        for (int i = 0; i < starsLeft; i++) {
            Polygon leftStar = createStar(0, 0, 10, 20, 5, numberColorMapping.getOrDefault(numberOfStars, Color.GOLD));
            leftStarsBox.getChildren().add(leftStar);
        }

        for (int i = 0; i < starsRight; i++) {
            Polygon rightStar = createStar(0, 0, 10, 20, 5, numberColorMapping.getOrDefault(numberOfStars, Color.GOLD));
            rightStarsBox.getChildren().add(rightStar);
        }
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

                int starsToDisplay = minefieldBoard.getHighestNeighbor();
                displayWinStars(starsToDisplay);
                saveFinishedGameResult();
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

    private void saveFinishedGameResult() {
        String gameJSON = logger.saveGame(minefieldBoard, secondsPassed.get(), LocalDateTime.now());
        MinesweeperLogging.saveGameToFile(gameJSON, "finished_games.json");
    }

    private void updateCell(int row, int col) {
        // Here, update the button based on the state of the cell.

        Button cellButton = getCellButton(row, col);
        Cell cell = minefieldBoard.getBoard()[row][col];

        // Logic to update the button text and style based on the cell state
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
}





