package org.seira.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.seira.SeiraApp;

import java.io.IOException;

public class ConsoleController {
    @FXML private HBox root;
    @FXML private Node main;
    @FXML private VBox navbar;
    private String currentContent = "dashboard";

    @FXML
    private void tabClick(ActionEvent ev) {
        Button btn = (Button) ev.getSource();
        currentContent = btn.getId();
        try {
            switchMain(currentContent + ".fxml");
        } catch (IOException e) {
            throw new RuntimeException("Can't open FXML file", e);
        }
        // update .selected class
        for (Node node: navbar.getChildren()) {
            if (node instanceof Button && node.getId() != null) {
                if (node.getId().equals(currentContent))
                    node.getStyleClass().add("selected");
                else
                    node.getStyleClass().remove("selected");
            }
        }
    }

    /**
     * Switch the main content of the console.
     */
    private void switchMain(String fxmlPath) throws IOException {
        root.getChildren().remove(main);
        // this is a weird dependency but ok
        FXMLLoader loader = new FXMLLoader(SeiraApp.class.getResource(fxmlPath));
        main = loader.load();
        root.getChildren().add(main);
    }

    public void initialize() throws IOException {
        switchMain(currentContent + ".fxml");
    }
}
