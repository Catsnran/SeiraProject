package com.seira.controllers;

import com.seira.dao.DAOFactory;
import com.seira.utils.NavigationManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterControllers {

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;

    @FXML
    private void handleRegister() {
        errorLabel.setVisible(false);
        successLabel.setVisible(false);

        String username = usernameField.getText().trim();
        String email    = emailField.getText().trim();
        String password = passwordField.getText();
        String confirm  = confirmPasswordField.getText();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            showError("Semua field harus diisi."); return;
        }
        if (!email.matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$")) {
            showError("Format email tidak valid."); return;
        }
        if (password.length() < 6) {
            showError("Password minimal 6 karakter."); return;
        }
        if (!password.equals(confirm)) {
            showError("Konfirmasi password tidak cocok."); return;
        }
        if (DAOFactory.getUserDAO().emailExists(email)) {
            showError("Email sudah terdaftar."); return;
        }

        boolean ok = DAOFactory.getUserDAO().register(username, email, password);
        if (ok) {
            successLabel.setText("Akun berhasil dibuat! Silakan login.");
            successLabel.setVisible(true);
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                    javafx.util.Duration.seconds(1.5));
            pause.setOnFinished(e -> NavigationManager.navigateTo("/fxml/Login.fxml"));
            pause.play();
        } else {
            showError("Gagal membuat akun. Coba lagi.");
        }
    }

    @FXML
    private void goToLogin() {
        NavigationManager.navigateTo("/fxml/Login.fxml");
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }
}
