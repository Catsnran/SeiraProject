package com.seira.controllers;

import com.seira.dao.DAOFactory;
import com.seira.models.User;
import com.seira.utils.NavigationManager;
import com.seira.utils.SessionManager;
import com.seira.utils.TokenManager;
import javafx.application.Platform;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

public class LoginControllers {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML
    public void initialize() {
        emailField.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.TAB) passwordField.requestFocus(); });
        passwordField.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) handleLogin(); });

        // auto-login jika ada token
        Platform.runLater(() -> {
            try {
                String token = TokenManager.loadToken();
                if (token != null) {        
                    User cachedUser = DAOFactory.getUserDAO().findByEmail(token);
                    if (cachedUser != null) {
                        SessionManager.setCurrentUser(cachedUser);
                        NavigationManager.navigateTo("/fxml/Main.fxml");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        int passwordLength = password.length();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Email dan password tidak boleh kosong.");
            return;
        }

        User user = DAOFactory.getUserDAO().login(email, password);
        if (user != null) {
            SessionManager.setCurrentUser(user);
            try {
                TokenManager.saveToken(user.getEmail());
            } catch (Exception e) {
                e.printStackTrace();
            }
            NavigationManager.navigateTo("/fxml/Main.fxml");
        } else {
            showError("Email atau password salah.");
        }
    }

    @FXML
    private void goToRegister() {
        NavigationManager.navigateTo("/fxml/Register.fxml");
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }
}
