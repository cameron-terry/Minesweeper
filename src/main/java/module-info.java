module com.cameronterry.minesweeper {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;


    opens com.cameronterry.minesweeper to javafx.fxml;
    exports com.cameronterry.minesweeper;
}