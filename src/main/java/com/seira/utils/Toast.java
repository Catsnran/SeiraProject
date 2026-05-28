package com.seira.utils;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * Beautiful toast system adhering to the Warm Editorial design language.
 */
public class Toast {

    public static void show(String message) {
        show(message, "✦", "#C87941");
    }

    public static void showSuccess(String message) {
        show(message, "✓", "#27AE60");
    }

    public static void showError(String message) {
        show(message, "✕", "#C0392B");
    }

    private static void show(String message, String icon, String colorHex) {
        Platform.runLater(() -> {
            Stage owner = SessionManager.getPrimaryStage();
            if (owner == null) return;

            Stage toastStage = new Stage();
            toastStage.initOwner(owner);
            toastStage.initStyle(StageStyle.TRANSPARENT);

            HBox root = new HBox(12);
            root.setAlignment(Pos.CENTER_LEFT);
            root.setPadding(new Insets(12, 22, 12, 22));
            root.setStyle(
                "-fx-background-color: #FDFAF5; " +
                "-fx-background-radius: 12; " +
                "-fx-border-color: " + colorHex + "; " +
                "-fx-border-radius: 12; " +
                "-fx-border-width: 1.5; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 12, 0, 0, 4);"
            );

            Label iconLbl = new Label(icon);
            iconLbl.setStyle("-fx-text-fill: " + colorHex + "; -fx-font-size: 16; -fx-font-weight: bold;");

            Label msgLbl = new Label(message);
            msgLbl.setStyle("-fx-text-fill: #1A0F05; -fx-font-size: 13; -fx-font-weight: bold; -fx-font-family: 'System';");

            root.getChildren().addAll(iconLbl, msgLbl);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            toastStage.setScene(scene);

            toastStage.setOnShowing(e -> {
                root.applyCss();
                root.layout();
                double w = root.prefWidth(-1);
                if (w <= 0) w = 300;
                double x = owner.getX() + owner.getWidth() / 2 - w / 2;
                double y = owner.getY() + owner.getHeight() - 110;
                
                toastStage.setX(x);
                toastStage.setY(y);
            });

            // Smooth fade-in
            root.setOpacity(0);
            toastStage.show();

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            
            // Stay and then smooth fade-out
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), root);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setDelay(Duration.millis(2500));
            fadeOut.setOnFinished(evt -> toastStage.close());

            fadeIn.play();
            fadeIn.setOnFinished(evt -> fadeOut.play());
        });
    }
}
