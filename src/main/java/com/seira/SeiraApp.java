package com.seira;

import com.seira.dao.DatabaseManager;
import com.seira.utils.SessionManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class SeiraApp extends Application {

    /**
     * Dipanggil JavaFX saat aplikasi dimulai.
     * @param primaryStage window utama aplikasi
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        DatabaseManager.getInstance().initialize();
        SessionManager.setPrimaryStage(primaryStage);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1100, 700);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

        primaryStage.setTitle("Seira — Fiscal Curator");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(660);

        // Maximize ke seluruh layar
        javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
        primaryStage.setX(screenBounds.getMinX());
        primaryStage.setY(screenBounds.getMinY());
        primaryStage.setWidth(screenBounds.getWidth());
        primaryStage.setHeight(screenBounds.getHeight());

        primaryStage.show();
    }

    /** Titik masuk JVM. */
    public static void main(String[] args) {
        launch(args);
    }
}
