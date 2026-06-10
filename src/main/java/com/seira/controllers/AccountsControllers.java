package com.seira.controllers;

import com.seira.dao.DAOFactory;
import com.seira.models.PaymentMethod;
import com.seira.utils.FormatUtil;
import com.seira.utils.SessionManager;
import com.seira.utils.StyledDialog;
import com.seira.utils.Toast;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import javafx.application.Platform;
import javafx.scene.control.ListView;
import java.util.concurrent.CompletableFuture;
import com.seira.models.StockAsset;
import com.seira.utils.YahooFinanceService;

import java.math.BigDecimal;
import java.util.List;

public class AccountsControllers {

    @FXML
    private Label totalLiquidityLabel, liquidityChangeLabel;
    @FXML
    private HBox accountCardsBox;
    @FXML
    private VBox recentAllocationsList;
    @FXML
    private Pane diversificationBar;

    private int userId;

    @FXML
    public void initialize() {
        userId = SessionManager.getCurrentUser().getId();
        loadData();
    }

    private void loadData() {
        List<PaymentMethod> methods = DAOFactory.getPaymentMethodDAO().findAll(userId);
        double total = methods.stream().mapToDouble(pm -> pm.getBalance().doubleValue()).sum();

        totalLiquidityLabel.setText(FormatUtil.formatIdr(total));
        liquidityChangeLabel.setText("+2.4% dari bulan lalu");

        accountCardsBox.getChildren().clear();
        String[] typeColors = { "#C87941", "#4A90D9", "#27AE60", "#9B59B6", "#E07B54" };
        for (int i = 0; i < methods.size(); i++) {
            accountCardsBox.getChildren().add(buildAccountCard(methods.get(i), typeColors[i % typeColors.length]));
        }

        // Draw stock assets cards
        List<StockAsset> stocks = DAOFactory.getStockAssetDAO().findAll(userId);
        for (int i = 0; i < stocks.size(); i++) {
            accountCardsBox.getChildren()
                    .add(buildStockCard(stocks.get(i), typeColors[(methods.size() + i) % typeColors.length]));
        }

        if (methods.isEmpty() && stocks.isEmpty()) {
            Label empty = new Label("Belum ada akun atau aset saham. Tambah baru untuk memulai.");
            empty.getStyleClass().add("mini-label");
            empty.setPadding(new Insets(20));
            accountCardsBox.getChildren().add(empty);
        }

        drawDiversificationBar(methods, total);
        buildRecentAllocations(methods);
        updateTotalLiquidity();
    }

    private VBox buildAccountCard(PaymentMethod pm, String color) {
        VBox card = new VBox(12);
        card.getStyleClass().add("account-card");
        card.setPadding(new Insets(20));
        card.setPrefWidth(210);
        card.setStyle(
                "-fx-background-color: #FDFAF5; -fx-background-radius: 14; -fx-border-color: #E8DDD0; -fx-border-radius: 14; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);");

        // Top: icon + type badge
        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label iconLbl = new Label(getTypeIcon(pm.getType()));
        iconLbl.setStyle("-fx-font-size: 18; -fx-background-color: " + color + "15; -fx-background-radius: 50; -fx-min-width: 38; -fx-min-height: 38; -fx-max-width: 38; -fx-max-height: 38; -fx-alignment: center;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label typeLbl = new Label(pm.getType());
        typeLbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 9; -fx-font-weight: bold; " +
                "-fx-background-color: " + color + "22; -fx-background-radius: 4; -fx-padding: 2 7;");
        topRow.getChildren().addAll(iconLbl, spacer, typeLbl);

        // Name + desc
        Label nameLbl = new Label(pm.getName());
        nameLbl.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #1A0F05;");
        Label descLbl = new Label(pm.getDescription() != null ? pm.getDescription() : "");
        descLbl.setStyle("-fx-font-size: 11; -fx-text-fill: #8B7355;");

        // Balance with colored accent
        Label balLbl = new Label(FormatUtil.formatIdr(pm.getBalance()));
        balLbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 19; -fx-font-weight: bold;");

        // Subtle accent bar at bottom
        Region accentBar = new Region();
        accentBar.setPrefHeight(3);
        accentBar.setMaxWidth(Double.MAX_VALUE);
        accentBar.setStyle("-fx-background-color: linear-gradient(to right, " + color + ", " + color
                + "44); -fx-background-radius: 2;");

        // Actions
        HBox actions = new HBox(6);
        Button editBtn = new Button("Edit");
        editBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8B7355; -fx-font-size: 11; " +
                "-fx-padding: 4 10; -fx-background-radius: 5; -fx-border-color: #DDD5C8; -fx-border-width: 1; " +
                "-fx-border-radius: 5; -fx-cursor: hand;");
        Button delBtn = new Button("Hapus");
        delBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #C0392B; -fx-font-size: 11; " +
                "-fx-padding: 4 10; -fx-background-radius: 5; -fx-border-color: #E8C5C0; -fx-border-width: 1; " +
                "-fx-border-radius: 5; -fx-cursor: hand;");
        editBtn.setOnAction(e -> showEditDialog(pm));
        delBtn.setOnAction(e -> confirmDelete(pm));
        actions.getChildren().addAll(editBtn, delBtn);

        card.getChildren().addAll(topRow, nameLbl, descLbl, balLbl, accentBar, actions);
        return card;
    }

    private void drawDiversificationBar(List<PaymentMethod> methods, double total) {
        diversificationBar.getChildren().clear();
        diversificationBar.widthProperty().addListener((o, v, n) -> drawBar(methods, total));
        drawBar(methods, total);
    }

    private void drawBar(List<PaymentMethod> methods, double total) {
        diversificationBar.getChildren().clear();
        if (total == 0 || methods.isEmpty())
            return;
        double w = diversificationBar.getWidth();
        if (w < 10)
            w = 400;
        double h = diversificationBar.getHeight();
        if (h < 4)
            h = 10;

        Canvas c = new Canvas(w, h);
        GraphicsContext gc = c.getGraphicsContext2D();
        String[] colors = { "#8B4513", "#4A90D9", "#27AE60", "#9B59B6", "#E07B54" };
        double x = 0;
        for (int i = 0; i < methods.size(); i++) {
            double pct = methods.get(i).getBalance().doubleValue() / total;
            double barW = Math.max(pct * w - 2, 0);
            gc.setFill(Color.web(colors[i % colors.length]));
            gc.fillRoundRect(x, 0, barW, h, 4, 4);
            x += pct * w;
        }
        diversificationBar.getChildren().add(c);
    }

    private void buildRecentAllocations(List<PaymentMethod> methods) {
        recentAllocationsList.getChildren().clear();
        String[] times = { "2 menit lalu", "1 jam lalu", "Baru saja", "5 menit lalu", "30 menit lalu" };
        for (int i = 0; i < methods.size(); i++) {
            PaymentMethod pm = methods.get(i);
            HBox row = new HBox(16);
            row.setStyle("-fx-background-color: #FDFAF5; -fx-border-color: #F0EBE0; -fx-border-width: 0 0 1 0;");
            row.setPadding(new Insets(14, 20, 14, 20));
            row.setAlignment(Pos.CENTER_LEFT);

            Label iconLbl = new Label(getTypeIcon(pm.getType()));
            iconLbl.getStyleClass().add("alloc-icon");

            Label nameLbl = new Label(pm.getName());
            nameLbl.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #1A0F05;");
            HBox.setHgrow(nameLbl, Priority.ALWAYS);

            // boolean syncing = i % 4 == 100;
            Label statusLbl = new Label("● Active");
            statusLbl.setStyle("-fx-text-fill: " + ("#27AE60") + "; -fx-font-size: 12;");
            statusLbl.setPrefWidth(100);

            // Label syncLbl = new Label(times[i % times.length]);
            // syncLbl.setStyle("-fx-font-size: 12; -fx-text-fill: #8B7355;");
            // syncLbl.setPrefWidth(130);

            Label balLbl = new Label(FormatUtil.formatIdr(pm.getBalance()));
            balLbl.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #2C1A0E;");

            row.getChildren().addAll(iconLbl, nameLbl, statusLbl,balLbl);
            recentAllocationsList.getChildren().add(row);
        }
        if (methods.isEmpty()) {
            Label empty = new Label("Belum ada akun terdaftar.");
            empty.setStyle("-fx-font-size: 12; -fx-text-fill: #8B7355;");
            empty.setPadding(new Insets(16, 20, 16, 20));
            recentAllocationsList.getChildren().add(empty);
        }
    }

    @FXML
    private void openAddAccount() {
        TextField nameField = StyledDialog.field("Nama akun (mis: BCA Utama)");
        ComboBox<String> typeCombo = StyledDialog.combo();
        typeCombo.getItems().addAll("CASH", "M-BANKING", "SAVINGS", "E-WALLET", "Saham");
        typeCombo.setValue("M-BANKING");
        String currencyCode = FormatUtil.getCurrencyCode();
        String currencyLabel = "USD".equalsIgnoreCase(currencyCode) ? "USD" : "Rp";
        String promptMsg = "USD".equalsIgnoreCase(currencyCode) ? "Saldo awal (mis: 350)" : "Saldo awal (mis: 5000000)";
        TextField balanceField = StyledDialog.field(promptMsg);
        TextField descField = StyledDialog.field("Deskripsi (mis: Rekening utama)");
        Label errLbl = StyledDialog.errorLabel();

        VBox nameGroup = StyledDialog.fieldGroup("NAMA AKUN", nameField);
        VBox typeGroup = StyledDialog.fieldGroup("TIPE AKUN", typeCombo);
        VBox balanceGroup = StyledDialog.fieldGroup("SALDO AWAL (" + currencyLabel + ")", balanceField);
        VBox descGroup = StyledDialog.fieldGroup("DESKRIPSI", descField);

        // Saham specific fields
        TextField searchField = StyledDialog.field("Cari saham (mis: TSLA, BBCA.JK)");
        VBox stockSearchGroup = StyledDialog.fieldGroup("CARI SAHAM", searchField);

        ListView<YahooFinanceService.StockSearchResult> suggestionList = new ListView<>();
        suggestionList.setPrefHeight(100);
        suggestionList.setVisible(false);
        suggestionList.setManaged(false);

        TextField lotField = StyledDialog.field("Jumlah Lot (minimal 1)");
        VBox lotGroup = StyledDialog.fieldGroup("JUMLAH LOT", lotField);

        stockSearchGroup.setVisible(false);
        stockSearchGroup.setManaged(false);
        lotGroup.setVisible(false);
        lotGroup.setManaged(false);

        final String[] selectedSymbol = { null };
        final String[] selectedName = { null };

        typeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isStock = "Saham".equalsIgnoreCase(newVal);
            nameGroup.setVisible(!isStock);
            nameGroup.setManaged(!isStock);
            balanceGroup.setVisible(!isStock);
            balanceGroup.setManaged(!isStock);
            descGroup.setVisible(!isStock);
            descGroup.setManaged(!isStock);

            stockSearchGroup.setVisible(isStock);
            stockSearchGroup.setManaged(isStock);
            lotGroup.setVisible(isStock);
            lotGroup.setManaged(isStock);
            if (!isStock) {
                suggestionList.setVisible(false);
                suggestionList.setManaged(false);
            }
        });

        // Search Autocomplete with Debounce logic
        final Timeline[] searchDebounce = { null };
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (searchDebounce[0] != null) {
                searchDebounce[0].stop();
            }
            String query = newVal.trim();
            // Don't search if user just selected a suggestion
            if (selectedSymbol[0] != null && newVal.startsWith(selectedSymbol[0])) {
                return;
            }
            selectedSymbol[0] = null;
            selectedName[0] = null;

            if (query.length() < 2) {
                suggestionList.setVisible(false);
                suggestionList.setManaged(false);
                return;
            }

            searchDebounce[0] = new Timeline(new KeyFrame(Duration.millis(350), event -> {
                CompletableFuture.supplyAsync(() -> YahooFinanceService.searchStocks(query))
                        .thenAcceptAsync(results -> {
                            if (results.isEmpty()) {
                                suggestionList.setVisible(false);
                                suggestionList.setManaged(false);
                            } else {
                                suggestionList.getItems().setAll(results);
                                suggestionList.setVisible(true);
                                suggestionList.setManaged(true);
                            }
                        }, Platform::runLater);
            }));
            searchDebounce[0].play();
        });

        suggestionList.setOnMouseClicked(event -> {
            YahooFinanceService.StockSearchResult selected = suggestionList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                selectedSymbol[0] = selected.getSymbol();
                selectedName[0] = selected.getName();
                searchField.setText(selected.getSymbol() + " — " + selected.getName());
                suggestionList.setVisible(false);
                suggestionList.setManaged(false);
            }
        });

        Stage dialog = new StyledDialog.Builder()
                .title("Tambah Akun Baru")
                .subtitle("Daftarkan sumber dana atau aset saham")
                .icon("🏦")
                .confirmText("Tambah")
                .content(
                        typeGroup,
                        nameGroup,
                        balanceGroup,
                        descGroup,
                        stockSearchGroup,
                        suggestionList,
                        lotGroup,
                        errLbl)
                .onConfirm(() -> {
                    String type = typeCombo.getValue();
                    if ("Saham".equalsIgnoreCase(type)) {
                        if (selectedSymbol[0] == null) {
                            StyledDialog.showError(errLbl, "Silakan pilih saham valid dari daftar pencarian.");
                            return;
                        }
                        String lotText = lotField.getText().trim();
                        if (lotText.isEmpty()) {
                            StyledDialog.showError(errLbl, "Jumlah lot tidak boleh kosong.");
                            return;
                        }
                        try {
                            int lot = Integer.parseInt(lotText);
                            if (lot < 1) {
                                StyledDialog.showError(errLbl, "Jumlah lot minimal 1.");
                                return;
                            }
                            StockAsset sa = new StockAsset();
                            sa.setUserId(userId);
                            sa.setStockSymbol(selectedSymbol[0]);
                            sa.setStockName(selectedName[0]);
                            sa.setTotalLot(lot);

                            DAOFactory.getStockAssetDAO().add(sa);

                            searchField.getScene().getWindow().hide();
                            loadData();
                            Toast.showSuccess("Aset saham berhasil ditambahkan ✓");
                        } catch (NumberFormatException e) {
                            StyledDialog.showError(errLbl, "Jumlah lot harus berupa angka bulat.");
                        }
                    } else {
                        String name = nameField.getText().trim();
                        String balText = balanceField.getText().trim();
                        if (name.isEmpty()) {
                            StyledDialog.showError(errLbl, "Nama akun tidak boleh kosong.");
                            return;
                        }
                        try {
                            BigDecimal bal;
                            String userCurrency = FormatUtil.getCurrencyCode();
                            if ("USD".equalsIgnoreCase(userCurrency)) {
                                double idrVal = YahooFinanceService.convertPrice(Double.parseDouble(balText.isEmpty() ? "0" : balText), "USD", "IDR");
                                bal = BigDecimal.valueOf(idrVal);
                            } else {
                                bal = new BigDecimal(balText.isEmpty() ? "0" : balText);
                            }
                            if(bal.signum() == -1){
                                StyledDialog.showError(errLbl, "Bro, akun lu kok bisa kurang dari 0 jir");
                                return;
                            }
                            PaymentMethod pm = new PaymentMethod();
                            pm.setUserId(userId);
                            pm.setName(name);
                            pm.setType(type);
                            pm.setBalance(bal);
                            pm.setDescription(descField.getText().trim());
                            DAOFactory.getPaymentMethodDAO().add(pm);
                            nameField.getScene().getWindow().hide();
                            loadData();
                            Toast.showSuccess("Akun berhasil ditambahkan ✓");
                        } catch (NumberFormatException e) {
                            StyledDialog.showError(errLbl, "Saldo harus berupa angka.");
                        }
                    }
                })
                .build();
        dialog.showAndWait();
    }

    private void showEditDialog(PaymentMethod pm) {
        TextField nameField = StyledDialog.field("Nama akun");
        nameField.setText(pm.getName());
        TextField descField = StyledDialog.field("Deskripsi");
        descField.setText(pm.getDescription() != null ? pm.getDescription() : "");

        double balanceInUserCurrency = pm.getBalance().doubleValue();
        String currencyCode = FormatUtil.getCurrencyCode();
        if ("USD".equalsIgnoreCase(currencyCode)) {
            balanceInUserCurrency = YahooFinanceService.convertPrice(balanceInUserCurrency, "IDR", "USD");
        }

        TextField balField = StyledDialog.field("Saldo");
        balField.setText("USD".equalsIgnoreCase(currencyCode)
            ? String.format(java.util.Locale.US, "%.2f", balanceInUserCurrency)
            : String.format(java.util.Locale.US, "%.0f", balanceInUserCurrency));

        Label errLbl = StyledDialog.errorLabel();
        String currencyLabel = "USD".equalsIgnoreCase(currencyCode) ? "USD" : "Rp";

        Stage dialog = new StyledDialog.Builder()
                .title("Edit Akun")
                .subtitle(pm.getName())
                .icon(getTypeIcon(pm.getType()))
                .confirmText("Simpan Perubahan")
                .content(
                        StyledDialog.fieldGroup("NAMA AKUN", nameField),
                        StyledDialog.fieldGroup("DESKRIPSI", descField),
                        StyledDialog.fieldGroup("SALDO (" + currencyLabel + ")", balField),
                        errLbl)
                .onConfirm(() -> {
                    try {
                        BigDecimal bal;
                        if ("USD".equalsIgnoreCase(currencyCode)) {
                            double idrVal = YahooFinanceService.convertPrice(Double.parseDouble(balField.getText().trim()), "USD", "IDR");
                            bal = BigDecimal.valueOf(idrVal);
                        } else {
                            bal = new BigDecimal(balField.getText().trim());
                        }
                        if(bal.signum() == -1){
                            StyledDialog.showError(errLbl, "Bro, akun lu kok bisa kurang dari 0 jir");
                            return;
                        }
                        pm.setName(nameField.getText().trim());
                        pm.setDescription(descField.getText().trim());
                        pm.setBalance(bal);
                        DAOFactory.getPaymentMethodDAO().update(pm);
                        nameField.getScene().getWindow().hide();
                        loadData();
                        Toast.showSuccess("Akun berhasil diperbarui ✓");
                    } catch (NumberFormatException e) {
                        StyledDialog.showError(errLbl, "Saldo tidak valid.");
                    }
                })
                .build();
        dialog.showAndWait();
    }

    private void confirmDelete(PaymentMethod pm) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Hapus Akun");
        alert.setHeaderText("Hapus \"" + pm.getName() + "\"?");
        alert.setContentText("Tindakan ini tidak dapat dibatalkan.");
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                DAOFactory.getPaymentMethodDAO().delete(pm.getId());
                loadData();
                Toast.showSuccess("Akun berhasil dihapus ✓");
            }
        });
    }

    private String getTypeIcon(String type) {
        if (type == null)
            return "💳";
        return switch (type.toUpperCase()) {
            case "CASH" -> "💵";
            case "M-BANKING" -> "🏦";
            case "SAVINGS" -> "🐷";
            case "E-WALLET" -> "📱";
            case "INVESTMENT" -> "📈";
            default -> "💳";
        };
    }

    private VBox buildStockCard(StockAsset sa, String color) {
        VBox card = new VBox(12);
        card.getStyleClass().add("account-card");
        card.setPadding(new Insets(20));
        card.setPrefWidth(210);
        card.setStyle(
                "-fx-background-color: #FDFAF5; -fx-background-radius: 14; -fx-border-color: #E8DDD0; -fx-border-radius: 14; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);");

        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label iconLbl = new Label("📈");
        iconLbl.setStyle("-fx-font-size: 18; -fx-background-color: " + color + "15; -fx-background-radius: 50; -fx-min-width: 38; -fx-min-height: 38; -fx-max-width: 38; -fx-max-height: 38; -fx-alignment: center;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label typeLbl = new Label("SAHAM");
        typeLbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 9; -fx-font-weight: bold; " +
                "-fx-background-color: " + color + "22; -fx-background-radius: 4; -fx-padding: 2 7;");
        topRow.getChildren().addAll(iconLbl, spacer, typeLbl);

        Label nameLbl = new Label(sa.getStockSymbol());
        nameLbl.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #1A0F05;");
        Label descLbl = new Label(sa.getStockName() + "\n(" + sa.getTotalLot() + " Lot)");
        descLbl.setStyle("-fx-font-size: 11; -fx-text-fill: #8B7355;");
        descLbl.setMinHeight(30);

        Label valLbl = new Label("Memuat...");
        valLbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 17; -fx-font-weight: bold;");

        Region accentBar = new Region();
        accentBar.setPrefHeight(3);
        accentBar.setMaxWidth(Double.MAX_VALUE);
        accentBar.setStyle("-fx-background-color: linear-gradient(to right, " + color + ", " + color
                + "44); -fx-background-radius: 2;");

        HBox actions = new HBox(6);
        Button delBtn = new Button("Hapus");
        delBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #C0392B; -fx-font-size: 11; " +
                "-fx-padding: 4 10; -fx-background-radius: 5; -fx-border-color: #E8C5C0; -fx-border-width: 1; " +
                "-fx-border-radius: 5; -fx-cursor: hand;");
        delBtn.setOnAction(e -> confirmDeleteStock(sa));
        actions.getChildren().add(delBtn);

        card.getChildren().addAll(topRow, nameLbl, descLbl, valLbl, accentBar, actions);

        CompletableFuture.supplyAsync(() -> {
            YahooFinanceService.StockChartData data = YahooFinanceService.getChartData(sa.getStockSymbol());
            List<YahooFinanceService.StockPricePoint> prices = data.getPrices();
            if (!prices.isEmpty()) {
                double rawPrice = prices.get(prices.size() - 1).getPrice();
                String stockCurrency = data.getCurrency();
                String userCurrency = SessionManager.getCurrentUser() != null
                        ? SessionManager.getCurrentUser().getCurrency()
                        : "IDR";
                return YahooFinanceService.convertPrice(rawPrice, stockCurrency, userCurrency);
            }
            return 0.0;
        }).thenAcceptAsync(price -> {
            double totalVal = price * sa.getTotalLot() * 100;
            valLbl.setText(FormatUtil.formatCurrency(totalVal));
            updateTotalLiquidity();
        }, Platform::runLater);

        return card;
    }

    private void confirmDeleteStock(StockAsset sa) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Hapus Aset Saham");
        alert.setHeaderText("Hapus \"" + sa.getStockSymbol() + "\"?");
        alert.setContentText("Tindakan ini tidak dapat dibatalkan.");
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                DAOFactory.getStockAssetDAO().delete(sa.getId());
                loadData();
                Toast.showSuccess("Aset saham berhasil dihapus ✓");
            }
        });
    }

    private void updateTotalLiquidity() {
        double pmTotal = DAOFactory.getPaymentMethodDAO().findAll(userId).stream()
                .mapToDouble(pm -> pm.getBalance().doubleValue()).sum();

        List<StockAsset> stocks = DAOFactory.getStockAssetDAO().findAll(userId);

        CompletableFuture.supplyAsync(() -> {
            double stockTotal = 0;
            String userCurrency = SessionManager.getCurrentUser() != null
                    ? SessionManager.getCurrentUser().getCurrency()
                    : "IDR";
            for (StockAsset sa : stocks) {
                YahooFinanceService.StockChartData data = YahooFinanceService.getChartData(sa.getStockSymbol());
                List<YahooFinanceService.StockPricePoint> prices = data.getPrices();
                if (!prices.isEmpty()) {
                    double rawPrice = prices.get(prices.size() - 1).getPrice();
                    String stockCurrency = data.getCurrency();
                    double price = YahooFinanceService.convertPrice(rawPrice, stockCurrency, userCurrency);
                    stockTotal += price * sa.getTotalLot() * 100;
                }
            }
            return stockTotal;
        }).thenAcceptAsync(stockTotal -> {
            double pmTotalConverted = FormatUtil.convertIdrToUserCurrency(pmTotal);
            totalLiquidityLabel.setText(FormatUtil.formatCurrency(pmTotalConverted + stockTotal));
        }, Platform::runLater);
    }
}
