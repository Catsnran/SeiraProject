package com.seira.controllers;

import com.seira.dao.DAOFactory;
import com.seira.models.PaymentMethod;
import com.seira.utils.FormatUtil;
import com.seira.utils.SessionManager;
import com.seira.utils.StyledDialog;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.util.List;

public class AccountsControllers {

    @FXML private Label totalLiquidityLabel, liquidityChangeLabel;
    @FXML private HBox accountCardsBox;
    @FXML private VBox recentAllocationsList;
    @FXML private Pane diversificationBar;

    private int userId;

    @FXML
    public void initialize() {
        userId = SessionManager.getCurrentUser().getId();
        loadData();
    }

    private void loadData() {
        List<PaymentMethod> methods = DAOFactory.getPaymentMethodDAO().findAll(userId);
        double total = methods.stream().mapToDouble(pm -> pm.getBalance().doubleValue()).sum();

        totalLiquidityLabel.setText(FormatUtil.formatCurrency(total));
        liquidityChangeLabel.setText("+2.4% dari bulan lalu");

        accountCardsBox.getChildren().clear();
        String[] typeColors = {"#C87941", "#4A90D9", "#27AE60", "#9B59B6"};
        for (int i = 0; i < methods.size(); i++) {
            accountCardsBox.getChildren().add(buildAccountCard(methods.get(i), typeColors[i % typeColors.length]));
        }
        if (methods.isEmpty()) {
            Label empty = new Label("Belum ada akun. Tambah akun baru untuk memulai.");
            empty.getStyleClass().add("mini-label");
            empty.setPadding(new Insets(20));
            accountCardsBox.getChildren().add(empty);
        }

        drawDiversificationBar(methods, total);
        buildRecentAllocations(methods);
    }

    private VBox buildAccountCard(PaymentMethod pm, String color) {
        VBox card = new VBox(12);
        card.getStyleClass().add("account-card");
        card.setPadding(new Insets(20));
        card.setPrefWidth(210);
        card.setStyle("-fx-background-color: #FDFAF5; -fx-background-radius: 14; -fx-border-color: #E8DDD0; -fx-border-radius: 14; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);");

        // Top: icon + type badge
        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label iconLbl = new Label(getTypeIcon(pm.getType()));
        iconLbl.setStyle("-fx-font-size: 22;");
        HBox.setHgrow(iconLbl, Priority.ALWAYS);
        Label typeLbl = new Label(pm.getType());
        typeLbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 9; -fx-font-weight: bold; " +
                "-fx-background-color: " + color + "22; -fx-background-radius: 4; -fx-padding: 2 7;");
        topRow.getChildren().addAll(iconLbl, typeLbl);

        // Name + desc
        Label nameLbl = new Label(pm.getName());
        nameLbl.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #1A0F05;");
        Label descLbl = new Label(pm.getDescription() != null ? pm.getDescription() : "");
        descLbl.setStyle("-fx-font-size: 11; -fx-text-fill: #8B7355;");

        // Balance with colored accent
        Label balLbl = new Label(FormatUtil.formatCurrency(pm.getBalance()));
        balLbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 19; -fx-font-weight: bold;");

        // Subtle accent bar at bottom
        Region accentBar = new Region();
        accentBar.setPrefHeight(3);
        accentBar.setMaxWidth(Double.MAX_VALUE);
        accentBar.setStyle("-fx-background-color: linear-gradient(to right, " + color + ", " + color + "44); -fx-background-radius: 2;");

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
        diversificationBar.widthProperty().addListener((o,v,n) -> drawBar(methods, total));
        drawBar(methods, total);
    }

    private void drawBar(List<PaymentMethod> methods, double total) {
        diversificationBar.getChildren().clear();
        if (total == 0 || methods.isEmpty()) return;
        double w = diversificationBar.getWidth();
        if (w < 10) w = 400;
        double h = diversificationBar.getHeight();
        if (h < 4) h = 10;

        Canvas c = new Canvas(w, h);
        GraphicsContext gc = c.getGraphicsContext2D();
        String[] colors = {"#8B4513", "#4A90D9", "#27AE60", "#9B59B6", "#E07B54"};
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
        String[] times = {"2 menit lalu", "1 jam lalu", "Baru saja", "5 menit lalu", "30 menit lalu"};
        for (int i = 0; i < methods.size(); i++) {
            PaymentMethod pm = methods.get(i);
            HBox row = new HBox(0);
            row.setStyle("-fx-background-color: #FDFAF5; -fx-border-color: #F0EBE0; -fx-border-width: 0 0 1 0;");
            row.setPadding(new Insets(14, 20, 14, 20));
            row.setAlignment(Pos.CENTER_LEFT);

            Label iconLbl = new Label(getTypeIcon(pm.getType()));
            iconLbl.setPrefWidth(36);

            Label nameLbl = new Label(pm.getName());
            nameLbl.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #1A0F05;");
            HBox.setHgrow(nameLbl, Priority.ALWAYS);

            boolean syncing = i % 4 == 2;
            Label statusLbl = new Label(syncing ? "● Syncing" : "● Active");
            statusLbl.setStyle("-fx-text-fill: " + (syncing ? "#E07B54" : "#27AE60") + "; -fx-font-size: 12;");
            statusLbl.setPrefWidth(100);

            Label syncLbl = new Label(times[i % times.length]);
            syncLbl.setStyle("-fx-font-size: 12; -fx-text-fill: #8B7355;");
            syncLbl.setPrefWidth(130);

            Label balLbl = new Label(FormatUtil.formatCurrency(pm.getBalance()));
            balLbl.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #2C1A0E;");

            row.getChildren().addAll(iconLbl, nameLbl, statusLbl, syncLbl, balLbl);
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
        typeCombo.getItems().addAll("CASH", "M-BANKING", "SAVINGS", "E-WALLET", "INVESTMENT");
        typeCombo.setValue("M-BANKING");
        TextField balanceField = StyledDialog.field("Saldo awal (mis: 5000000)");
        TextField descField = StyledDialog.field("Deskripsi (mis: Rekening utama)");
        Label errLbl = StyledDialog.errorLabel();

        Stage dialog = new StyledDialog.Builder()
                .title("Tambah Akun Baru")
                .subtitle("Daftarkan sumber danamu")
                .icon("🏦")
                .confirmText("Tambah Akun")
                .content(
                        StyledDialog.fieldGroup("NAMA AKUN", nameField),
                        StyledDialog.fieldGroup("TIPE AKUN", typeCombo),
                        StyledDialog.fieldGroup("SALDO AWAL (Rp)", balanceField),
                        StyledDialog.fieldGroup("DESKRIPSI", descField),
                        errLbl
                )
                .onConfirm(() -> {
                    String name = nameField.getText().trim();
                    String balText = balanceField.getText().trim();
                    if (name.isEmpty()) { StyledDialog.showError(errLbl, "Nama akun tidak boleh kosong."); return; }
                    try {
                        BigDecimal bal = new BigDecimal(balText.isEmpty() ? "0" : balText);
                        PaymentMethod pm = new PaymentMethod();
                        pm.setUserId(userId); pm.setName(name);
                        pm.setType(typeCombo.getValue()); pm.setBalance(bal);
                        pm.setDescription(descField.getText().trim());
                        DAOFactory.getPaymentMethodDAO().add(pm);
                        // Close and reload
                        nameField.getScene().getWindow().hide();
                        loadData();
                    } catch (NumberFormatException e) {
                        StyledDialog.showError(errLbl, "Saldo harus berupa angka (tanpa titik/koma).");
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
        TextField balField = StyledDialog.field("Saldo");
        balField.setText(pm.getBalance().toPlainString());
        Label errLbl = StyledDialog.errorLabel();

        Stage dialog = new StyledDialog.Builder()
                .title("Edit Akun")
                .subtitle(pm.getName())
                .icon(getTypeIcon(pm.getType()))
                .confirmText("Simpan Perubahan")
                .content(
                        StyledDialog.fieldGroup("NAMA AKUN", nameField),
                        StyledDialog.fieldGroup("DESKRIPSI", descField),
                        StyledDialog.fieldGroup("SALDO (Rp)", balField),
                        errLbl
                )
                .onConfirm(() -> {
                    try {
                        pm.setName(nameField.getText().trim());
                        pm.setDescription(descField.getText().trim());
                        pm.setBalance(new BigDecimal(balField.getText().trim()));
                        DAOFactory.getPaymentMethodDAO().updateBalance(pm.getId(), pm.getBalance());
                        nameField.getScene().getWindow().hide();
                        loadData();
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
            }
        });
    }

    private String getTypeIcon(String type) {
        if (type == null) return "💳";
        return switch (type.toUpperCase()) {
            case "CASH" -> "💵";
            case "M-BANKING" -> "🏦";
            case "SAVINGS" -> "🐷";
            case "E-WALLET" -> "📱";
            case "INVESTMENT" -> "📈";
            default -> "💳";
        };
    }
}
