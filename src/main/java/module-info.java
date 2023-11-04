module com.cameronterry.minesweeper {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.cameronterry.minesweeper to javafx.fxml;
    exports com.cameronterry.minesweeper;
}