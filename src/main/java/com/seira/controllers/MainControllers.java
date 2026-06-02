package com.seira.controllers;


import com.seira.utils.NavigationManager;
import com.seira.utils.SessionManager;
import com.seira.utils.TokenManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

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
        String name = SessionManager.getCurrentUser().getUsername();
        userInitial.setText(name.substring(0, 1).toUpperCase());
        loadPage("dashboard");
    }

    @FXML private void navDashboard()     { loadPage("dashboard"); }
    @FXML private void navTransactions()  { loadPage("transactions"); }
    @FXML private void navBudget()        { loadPage("budget"); }
    @FXML private void navReports()       { loadPage("reports"); }
    @FXML private void navAccounts()      { loadPage("accounts"); }
    @FXML private void openSettings()     {}
    @FXML private void openSupport()      {}
    @FXML private void navLogout()      {
        TokenManager.deleteToken();
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
                case "logout"     -> "/fxml/Login.fxml";
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
    private void handleSearch() {
        // future: delegate to page controller
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
