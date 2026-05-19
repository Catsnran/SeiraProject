package com.seira.utils;

import com.seira.model.User;
import javafx.stage.Stage;

public class SessionManager {
    private static User currentUser;
    private static Stage primaryStage;

    public static User getCurrentUser() { return currentUser; }
    public static void setCurrentUser(User user) { currentUser = user; }

    public static Stage getPrimaryStage() { return primaryStage; }
    public static void setPrimaryStage(Stage stage) { primaryStage = stage; }

    public static void logout() { currentUser = null; }

    public static boolean isLoggedIn() { return currentUser != null; }
}
