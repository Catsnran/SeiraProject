package com.seira;

import com.seira.dao.DatabaseManager;
import com.seira.utils.SessionManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Entry point aplikasi Seira — Fiscal Curator.
 * Menginisialisasi database, memuat CSS, dan menampilkan halaman login pertama kali.
 */
public class SeiraApp extends Application {

    /**
     * Dipanggil JavaFX saat aplikasi dimulai.
     * @param primaryStage window utama aplikasi
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        DatabaseManager.getInstance().initialize();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1100, 700);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

        primaryStage.setTitle("Seira — Fiscal Curator");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(660);
        primaryStage.show();

        SessionManager.setPrimaryStage(primaryStage);
    }

    /** Titik masuk JVM. */
    public static void main(String[] args) {
        launch(args);
    }
}
