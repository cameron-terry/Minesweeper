package com.cameronterry.minesweeper;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class BoardConfigController {
    @FXML
    private TextField rowsField;
    @FXML
    private TextField colsField;
    @FXML
    private TextField minesField;

    private Runnable okAction;

    public void setInitialValues(int rows, int cols, int mines) {
        rowsField.setText(String.valueOf(rows));
        colsField.setText(String.valueOf(cols));
        minesField.setText(String.valueOf(mines));
    }

    @FXML
    private void onOkClicked(ActionEvent event) {
        if(okAction != null) {
            okAction.run();
        }
    }

    public void setOkAction(Runnable action) {
        this.okAction = action;
    }

    public TextField getRowsField() {
        return rowsField;
    }

    public TextField getColsField() {
        return colsField;
    }

    public TextField getMinesField() {
        return minesField;
    }
}
