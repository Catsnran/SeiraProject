package com.seira.controllers;


import com.seira.utils.NavigationManager;
import com.seira.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import com.seira.models.User;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import java.io.File;
import java.nio.file.Files;

public class MainControllers {

    @FXML private StackPane pageContainer;
    @FXML private Label userInitial;
    @FXML private TextField searchField;
    @FXML private Button navDashboard;
    @FXML private Button navTransactions;
    @FXML private Button navBudget;
    @FXML private Button navReports;
    @FXML private Button navAccounts;
    @FXML private Button navLogout;

    private String currentPage = "";

    @FXML
    public void initialize() {
        updateTopBarAvatar();
        userInitial.setOnMouseClicked(this::navAccount);
        loadPage("dashboard");
    }

    //  navigate to profile when cliccked
    private void navAccount(MouseEvent event) {
        loadPage("profile");
    }

    @FXML private void navDashboard()     { loadPage("dashboard"); }
    @FXML private void navTransactions()  { loadPage("transactions"); }
    @FXML private void navBudget()        { loadPage("budget"); }
    @FXML private void navReports()       { loadPage("reports"); }
    @FXML private void navAccounts()      { loadPage("accounts"); }
    @FXML private void openSettings()     {}
    @FXML private void openSupport()      {}
    @FXML private void navLogout()      {
        SessionManager.logout();
        NavigationManager.navigateTo("/fxml/Login.fxml");
    }

    public void loadPage(String page) {
        currentPage = page;
        updateNavActive(page);
        try {
            String fxml = switch (page) {
                case "dashboard"    -> "/fxml/pages/Dashboard.fxml";
                case "transactions" -> "/fxml/pages/Transactions.fxml";
                case "budget"       -> "/fxml/pages/Budget.fxml";
                case "reports"      -> "/fxml/pages/Reports.fxml";
                case "accounts"     -> "/fxml/pages/Accounts.fxml";
                case "profile"      -> "/fxml/pages/Profile.fxml";
                case "logout"       -> "/fxml/Login.fxml";
                default             -> "/fxml/pages/Dashboard.fxml";
            };
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Node node = loader.load();
            pageContainer.getChildren().setAll(node);

            // Inject MainController reference into every page controller
            Object ctrl = loader.getController();
            if (ctrl instanceof com.seira.controllers.DashboardControllers dc)     dc.setMainController(this);
            if (ctrl instanceof TransactionsControllers tc)  tc.setMainController(this);
            if (ctrl instanceof com.seira.controllers.BudgetControllers bc)        {} // no nav needed
            if (ctrl instanceof ReportsControllers rc)       {} // no nav needed
            if (ctrl instanceof AccountsControllers ac)      {} // no nav needed
            if (ctrl instanceof ProfileControllers pc)       pc.setMainController(this);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void openAddTransaction() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/pages/AddTransaction.fxml"));
            Node node = loader.load();
            com.seira.controllers.AddTransactionControllers ctrl = loader.getController();
            ctrl.setMainController(this);
            ctrl.setCurrentPage(currentPage);
            pageContainer.getChildren().setAll(node);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSearch(KeyEvent ev) {
        // if (ev.getCode() != KeyCode.ENTER) return; // commenting this might be a bad idea
        loadPage("transactions");
    }
    public void clearSearchField() {
        searchField.setText("");
    }
    public String getSearchQuery() {
        return searchField.getText();
    }

    // refresh on profile change
    public void refreshUserState() {
        updateTopBarAvatar();
    }

    // update top bar avatar
    private void updateTopBarAvatar() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        String photo = user.getProfilePhoto();
        if (photo != null && !photo.isEmpty()) {
            File photoFile = new File(photo);
            if (photoFile.exists()) {
                try (var is = Files.newInputStream(photoFile.toPath())) {
                    ImageView imgView = new ImageView(new Image(is));
                    imgView.setFitWidth(34);
                    imgView.setFitHeight(34);

                    // Clip to circle matching the userInitial styling diameter (34px)
                    Circle clip = new Circle(17, 17, 17);
                    imgView.setClip(clip);

                    userInitial.setGraphic(imgView);
                    userInitial.setText("");
                    userInitial.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // Fallback to letter initial
        userInitial.setGraphic(null);
        String name = user.getUsername();
        if (name != null && !name.isEmpty()) {
            userInitial.setText(name.substring(0, 1).toUpperCase());
        } else {
            userInitial.setText("U");
        }
        userInitial.setStyle(""); // Restore default styling from CSS
    }

    private void updateNavActive(String page) {
        for (Button b : new Button[]{navDashboard, navTransactions, navBudget, navReports, navAccounts}) {
            b.getStyleClass().remove("nav-item-active");
        }
        switch (page) {
            case "dashboard"    -> navDashboard.getStyleClass().add("nav-item-active");
            case "transactions" -> navTransactions.getStyleClass().add("nav-item-active");
            case "budget"       -> navBudget.getStyleClass().add("nav-item-active");
            case "reports"      -> navReports.getStyleClass().add("nav-item-active");
            case "accounts"     -> navAccounts.getStyleClass().add("nav-item-active");
        }
    }
}
