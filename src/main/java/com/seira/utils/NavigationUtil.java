package com.seira.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class NavigationUtil {

    public static void navigateTo(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(NavigationUtil.class.getResource("/fxml/" + fxmlPath));
            Parent root = loader.load();
            Stage stage = SessionManager.getPrimaryStage();
            Scene scene = stage.getScene();
            scene.setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static <T> T navigateToAndGetController(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(NavigationUtil.class.getResource("/fxml/" + fxmlPath));
            Parent root = loader.load();
            Stage stage = SessionManager.getPrimaryStage();
            Scene scene = stage.getScene();
            scene.setRoot(root);
            return loader.getController();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
