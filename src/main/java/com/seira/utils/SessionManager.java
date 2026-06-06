package com.seira.utils;

import com.seira.models.User;
import javafx.stage.Stage;

public class SessionManager {
    private static User currentUser;
    private static Stage primaryStage;
    private static String searchQuery;

    public static User getCurrentUser() { return currentUser; }
    public static void setCurrentUser(User user) { currentUser = user; }

    public static Stage getPrimaryStage() { return primaryStage; }
    public static void setPrimaryStage(Stage stage) { primaryStage = stage; }

    public static String getSearchQuery() {
        return searchQuery;
    }
    public static void setSearchQuery(String query) {
        searchQuery = query;
    }

    public static void logout() {
        currentUser = null;
        searchQuery = null;
    }

    public static boolean isLoggedIn() { return currentUser != null; }
}
