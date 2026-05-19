package com.seira.utils;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Seira-themed modal dialog builder.
 * Returns the new Stage so caller can show and interact with fields directly.
 */
public class StyledDialog {

    public static final String ACCENT    = "#C87941";
    public static final String DARK      = "#2C1A0E";
    public static final String BG        = "#FDFAF5";
    public static final String SURFACE   = "#F5F0E8";
    public static final String BORDER    = "#E8DDD0";
    public static final String TEXT_MAIN = "#1A0F05";
    public static final String TEXT_SUB  = "#8B7355";
    public static final String FIELD_BG  = "#EDE7DC";

    /** Result holder for confirm/cancel */
    public enum Result { CONFIRM, CANCEL }

    public static class Builder {
        private String title = "Dialog";
        private String subtitle = "";
        private String icon = "✦";
        private String confirmText = "Simpan";
        private String cancelText = "Batal";
        private VBox contentBox = new VBox(14);
        private Runnable onConfirm;
        private Runnable onCancel;

        public Builder title(String t) { this.title = t; return this; }
        public Builder subtitle(String s) { this.subtitle = s; return this; }
        public Builder icon(String i) { this.icon = i; return this; }
        public Builder confirmText(String t) { this.confirmText = t; return this; }
        public Builder cancelText(String t) { this.cancelText = t; return this; }
        public Builder content(javafx.scene.Node... nodes) {
            contentBox.getChildren().addAll(nodes);
            return this;
        }
        public Builder onConfirm(Runnable r) { this.onConfirm = r; return this; }
        public Builder onCancel(Runnable r) { this.onCancel = r; return this; }

        public Stage build() {
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.initOwner(SessionManager.getPrimaryStage());

            // Root container
            VBox root = new VBox(0);
            root.setStyle("-fx-background-color: " + BG + "; -fx-background-radius: 16; -fx-border-color: " + BORDER + "; -fx-border-radius: 16; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 24, 0, 0, 6);");
            root.setPrefWidth(440);

            // Header strip
            HBox header = new HBox(14);
            header.setAlignment(Pos.CENTER_LEFT);
            header.setPadding(new Insets(24, 24, 20, 24));
            header.setStyle("-fx-background-color: linear-gradient(to right, #8B5E1A, #C87941); -fx-background-radius: 14 14 0 0;");

            Label iconLbl = new Label(icon);
            iconLbl.setStyle("-fx-font-size: 22; -fx-text-fill: rgba(255,255,255,0.85);");

            VBox titleBox = new VBox(3);
            HBox.setHgrow(titleBox, Priority.ALWAYS);
            Label titleLbl = new Label(title);
            titleLbl.setStyle("-fx-font-size: 17; -fx-font-weight: bold; -fx-text-fill: white;");
            if (!subtitle.isEmpty()) {
                Label subLbl = new Label(subtitle);
                subLbl.setStyle("-fx-font-size: 12; -fx-text-fill: rgba(255,255,255,0.75);");
                titleBox.getChildren().addAll(titleLbl, subLbl);
            } else {
                titleBox.getChildren().add(titleLbl);
            }

            Button closeBtn = new Button("✕");
            closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 14; -fx-cursor: hand; -fx-border-color: transparent;");
            closeBtn.setOnAction(e -> { if (onCancel != null) onCancel.run(); stage.close(); });

            header.getChildren().addAll(iconLbl, titleBox, closeBtn);

            // Content area
            VBox body = new VBox(14);
            body.setPadding(new Insets(24, 24, 20, 24));
            body.getChildren().addAll(contentBox.getChildren());

            // Divider
            Separator sep = new Separator();
            sep.setStyle("-fx-background-color: " + BORDER + ";");

            // Footer buttons
            HBox footer = new HBox(12);
            footer.setPadding(new Insets(16, 24, 24, 24));
            footer.setAlignment(Pos.CENTER_RIGHT);

            Button cancelBtn = new Button(cancelText);
            cancelBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: " + TEXT_SUB + "; -fx-font-size: 13; " +
                "-fx-padding: 10 20; -fx-background-radius: 8; -fx-border-color: " + BORDER + "; " +
                "-fx-border-width: 1; -fx-border-radius: 8; -fx-cursor: hand;"
            );
            cancelBtn.setOnMouseEntered(e -> cancelBtn.setStyle(cancelBtn.getStyle() + "-fx-background-color: " + SURFACE + ";"));
            cancelBtn.setOnMouseExited(e -> cancelBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: " + TEXT_SUB + "; -fx-font-size: 13; " +
                "-fx-padding: 10 20; -fx-background-radius: 8; -fx-border-color: " + BORDER + "; " +
                "-fx-border-width: 1; -fx-border-radius: 8; -fx-cursor: hand;"
            ));
            cancelBtn.setOnAction(e -> { if (onCancel != null) onCancel.run(); stage.close(); });

            Button confirmBtn = new Button(confirmText);
            confirmBtn.setStyle(
                "-fx-background-color: " + ACCENT + "; -fx-text-fill: white; -fx-font-size: 13; " +
                "-fx-font-weight: bold; -fx-padding: 10 24; -fx-background-radius: 8; " +
                "-fx-border-color: transparent; -fx-cursor: hand;"
            );
            confirmBtn.setOnMouseEntered(e -> confirmBtn.setStyle(
                "-fx-background-color: #A8622E; -fx-text-fill: white; -fx-font-size: 13; " +
                "-fx-font-weight: bold; -fx-padding: 10 24; -fx-background-radius: 8; " +
                "-fx-border-color: transparent; -fx-cursor: hand;"
            ));
            confirmBtn.setOnMouseExited(e -> confirmBtn.setStyle(
                "-fx-background-color: " + ACCENT + "; -fx-text-fill: white; -fx-font-size: 13; " +
                "-fx-font-weight: bold; -fx-padding: 10 24; -fx-background-radius: 8; " +
                "-fx-border-color: transparent; -fx-cursor: hand;"
            ));
            confirmBtn.setOnAction(e -> { if (onConfirm != null) onConfirm.run(); });

            footer.getChildren().addAll(cancelBtn, confirmBtn);

            root.getChildren().addAll(header, body, sep, footer);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            stage.setScene(scene);
            stage.sizeToScene();

            // Center on owner
            stage.setOnShowing(e -> {
                Stage owner = SessionManager.getPrimaryStage();
                if (owner != null) {
                    stage.setX(owner.getX() + owner.getWidth() / 2 - stage.getWidth() / 2);
                    stage.setY(owner.getY() + owner.getHeight() / 2 - stage.getHeight() / 2);
                }
            });

            return stage;
        }
    }

    /** Shortcut: create a styled Label for a field group */
    public static Label fieldLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 10; -fx-font-weight: bold; -fx-text-fill: " + TEXT_SUB + "; -fx-letter-spacing: 0.8;");
        return lbl;
    }

    /** Shortcut: styled TextField */
    public static TextField field(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle(
            "-fx-background-color: " + FIELD_BG + "; -fx-border-color: transparent; " +
            "-fx-border-radius: 8; -fx-background-radius: 8; -fx-pref-height: 42; " +
            "-fx-font-size: 13; -fx-text-fill: " + TEXT_MAIN + "; -fx-prompt-text-fill: #B09070; -fx-padding: 0 14;"
        );
        tf.focusedProperty().addListener((obs, old, focused) -> {
            if (focused) tf.setStyle(tf.getStyle().replace("transparent; -fx-border-color", BORDER + "; -fx-border-color: " + ACCENT));
            else tf.setStyle(
                "-fx-background-color: " + FIELD_BG + "; -fx-border-color: transparent; " +
                "-fx-border-radius: 8; -fx-background-radius: 8; -fx-pref-height: 42; " +
                "-fx-font-size: 13; -fx-text-fill: " + TEXT_MAIN + "; -fx-prompt-text-fill: #B09070; -fx-padding: 0 14;"
            );
        });
        return tf;
    }

    /** Shortcut: styled ComboBox */
    public static <T> ComboBox<T> combo() {
        ComboBox<T> cb = new ComboBox<>();
        cb.setMaxWidth(Double.MAX_VALUE);
        cb.setStyle(
            "-fx-background-color: " + FIELD_BG + "; -fx-border-color: transparent; " +
            "-fx-border-radius: 8; -fx-background-radius: 8; -fx-pref-height: 42;"
        );
        return cb;
    }

    /** A VBox grouping label + control */
    public static VBox fieldGroup(String labelText, javafx.scene.Node control) {
        VBox group = new VBox(6);
        group.getChildren().addAll(fieldLabel(labelText), control);
        return group;
    }

    /** Error label */
    public static Label errorLabel() {
        Label lbl = new Label();
        lbl.setVisible(false);
        lbl.setManaged(false);
        lbl.setWrapText(true);
        lbl.setStyle(
            "-fx-text-fill: #C0392B; -fx-font-size: 12; " +
            "-fx-background-color: #FDECEA; -fx-background-radius: 6; -fx-padding: 8 12;"
        );
        return lbl;
    }

    public static void showError(Label lbl, String msg) {
        lbl.setText(msg);
        lbl.setVisible(true);
        lbl.setManaged(true);
    }
}
