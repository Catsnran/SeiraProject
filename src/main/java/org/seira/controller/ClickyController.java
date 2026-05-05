package org.seira.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

/**
 * A cheery controller for testing interactivity and state (non-)persistence.
 * It is probably not a good idea to keep this.
 */
public class ClickyController {
    private long count = 0;
    @FXML
    private void clicky(ActionEvent ev) {
        Button btn = (Button) ev.getSource();
        count += 1;
        btn.setText(String.valueOf(count));
    }
}
