package com.seira.controllers;

import com.seira.dao.DAOFactory;
import com.seira.models.Budget;
import com.seira.models.Category;
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

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public class BudgetControllers {

    @FXML private Label totalAllocatedLabel;
    @FXML private Label remainingLabel;
    @FXML private VBox categoryEnvelopesList;
    @FXML private Pane pacePaneContainer;
    @FXML private Label paceStatusLabel;
    @FXML private Label paceDescLabel;

    private int userId;
    private YearMonth currentPeriod = YearMonth.now();

    @FXML
    public void initialize() {
        userId = SessionManager.getCurrentUser().getId();
        loadData();
    }

    private void loadData() {
        List<Budget> budgets = DAOFactory.getBudgetDAO().findAll(userId, currentPeriod);

        double totalAlloc = budgets.stream().mapToDouble(b -> b.getAmount().doubleValue()).sum();
        double totalSpent = budgets.stream().mapToDouble(b -> b.getSpent().doubleValue()).sum();
        double liquidity = DAOFactory.getPaymentMethodDAO().getTotalLiquidity(userId);
        double remaining = Math.max(liquidity - totalAlloc, 0);

        totalAllocatedLabel.setText(FormatUtil.formatCurrency(totalAlloc));
        remainingLabel.setText(FormatUtil.formatCurrency(remaining));

        double overallPct = totalAlloc > 0 ? totalSpent / totalAlloc : 0;
        drawPaceChart(overallPct);
        paceDescLabel.setText(String.format("Kamu telah menggunakan %.0f%% dari rata-rata anggaran harian.", overallPct * 100));
        if (overallPct > 1.0) {
            paceStatusLabel.setText("OVER BUDGET");
            paceStatusLabel.getStyleClass().setAll("badge-red");
        } else if (overallPct >= 0.8) {
            paceStatusLabel.setText("NEAR LIMIT");
            paceStatusLabel.getStyleClass().setAll("badge-yellow");
        } else {
            paceStatusLabel.setText("ON TRACK");
            paceStatusLabel.getStyleClass().setAll("badge-green");
        }

        categoryEnvelopesList.getChildren().clear();
        if (budgets.isEmpty()) {
            VBox empty = new VBox(14);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(40));
            Label emptyIcon = new Label("🐷");
            emptyIcon.setStyle("-fx-font-size: 40;");
            Label emptyLbl = new Label("Belum ada anggaran.\nKlik '＋ Tambah Anggaran Baru' untuk memulai.");
            emptyLbl.setStyle("-fx-font-size: 13; -fx-text-fill: #8B7355;");
            emptyLbl.setWrapText(true);
            emptyLbl.setAlignment(Pos.CENTER);
            emptyLbl.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            empty.getChildren().addAll(emptyIcon, emptyLbl);
            categoryEnvelopesList.getChildren().add(empty);
            return;
        }

        for (Budget b : budgets) {
            categoryEnvelopesList.getChildren().add(buildEnvelopeCard(b));
        }
    }

    private VBox buildEnvelopeCard(Budget b) {
        // Card with color accent on left border based on status
        String accentColor = switch (b.getStatus()) {
            case "OVER BUDGET" -> "#C0392B";
            case "NEAR LIMIT" -> "#E67E22";
            default -> "#27AE60";
        };

        VBox card = new VBox(12);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setStyle(
                "-fx-background-color: #FDFAF5; -fx-background-radius: 12; " +
                        "-fx-border-color: #E8DDD0; " +
                        "-fx-border-width: 1; -fx-border-radius: 12; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 6, 0, 0, 2);"
        );

        // Top row: icon + name + status
        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // Icon in colored circle
        VBox iconWrap = new VBox();
        iconWrap.setAlignment(Pos.CENTER);
        iconWrap.setPrefWidth(44); iconWrap.setPrefHeight(44);
        iconWrap.setStyle("-fx-background-color: " + accentColor + "22; -fx-background-radius: 10;");
        Label iconLbl = new Label(b.getCategoryIcon() != null ? b.getCategoryIcon() : "📌");
        iconLbl.setStyle("-fx-font-size: 20;");
        iconWrap.getChildren().add(iconLbl);

        VBox nameBox = new VBox(2);
        HBox.setHgrow(nameBox, Priority.ALWAYS);
        Label nameLbl = new Label(b.getCategoryName());
        nameLbl.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #1A0F05;");
        Label descLbl = new Label(getCategoryDesc(b.getCategoryName()));
        descLbl.setStyle("-fx-font-size: 11; -fx-text-fill: #8B7355;");
        nameBox.getChildren().addAll(nameLbl, descLbl);

        // Status badge
        Label statusBadge = new Label(b.getStatus());
        statusBadge.setStyle(
                "-fx-background-color: " + accentColor + "22; -fx-text-fill: " + accentColor + "; " +
                        "-fx-font-size: 9; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4;"
        );

        topRow.getChildren().addAll(iconWrap, nameBox, statusBadge);

        // Amount row
        HBox amtRow = new HBox(0);
        amtRow.setAlignment(Pos.CENTER_LEFT);
        Label spentLbl = new Label(FormatUtil.formatCurrency(b.getSpent()));
        spentLbl.setStyle("-fx-font-size: 17; -fx-font-weight: bold; -fx-text-fill: #1A0F05;");
        Label sepLbl = new Label(" / " + FormatUtil.formatCurrency(b.getAmount()));
        sepLbl.setStyle("-fx-font-size: 13; -fx-text-fill: #8B7355;");
        amtRow.getChildren().addAll(spentLbl, sepLbl);

        // Progress bar (custom drawn for better look)
        double pct = Math.min(b.getPercentage() / 100.0, 1.0);
        VBox barWrap = new VBox();
        barWrap.setStyle("-fx-background-color: #E8DDD0; -fx-background-radius: 4;");
        barWrap.setPrefHeight(8); barWrap.setMaxWidth(Double.MAX_VALUE);
        Region fill = new Region();
        fill.setPrefHeight(8);
        fill.setMaxWidth(Double.MAX_VALUE);
        fill.setStyle("-fx-background-color: " + accentColor + "; -fx-background-radius: 4;");
        // We'll use a ProgressBar instead (simpler)
        ProgressBar pb = new ProgressBar(pct);
        pb.setMaxWidth(Double.MAX_VALUE);
        pb.setPrefHeight(8);
        pb.getStyleClass().clear();
        pb.getStyleClass().add("progress-bar");
        pb.setStyle("-fx-accent: " + accentColor + ";");

        // Actions + remaining info
        HBox bottomRow = new HBox(8);
        bottomRow.setAlignment(Pos.CENTER_LEFT);
        String remainingStr = b.getRemaining().compareTo(BigDecimal.ZERO) >= 0
                ? "Sisa " + FormatUtil.formatCurrency(b.getRemaining())
                : "Melebihi " + FormatUtil.formatCurrency(b.getRemaining().abs());
        Label remainLbl = new Label(remainingStr);
        remainLbl.setStyle("-fx-font-size: 11; -fx-text-fill: " + (b.getRemaining().compareTo(BigDecimal.ZERO) >= 0 ? "#27AE60" : "#C0392B") + ";");
        HBox.setHgrow(remainLbl, Priority.ALWAYS);

        Button editBtn = new Button("Edit");
        editBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8B7355; -fx-font-size: 11; " +
                "-fx-padding: 4 10; -fx-background-radius: 5; -fx-border-color: #DDD5C8; -fx-border-width: 1; " +
                "-fx-border-radius: 5; -fx-cursor: hand;");
        Button delBtn = new Button("Hapus");
        delBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #C0392B; -fx-font-size: 11; " +
                "-fx-padding: 4 10; -fx-background-radius: 5; -fx-border-color: #E8C5C0; -fx-border-width: 1; " +
                "-fx-border-radius: 5; -fx-cursor: hand;");
        editBtn.setOnAction(e -> showEditBudgetDialog(b));
        delBtn.setOnAction(e -> {
            DAOFactory.getBudgetDAO().delete(b.getId());
            loadData();
            Toast.showSuccess("Anggaran berhasil dihapus ✓");
        });
        bottomRow.getChildren().addAll(remainLbl, editBtn, delBtn);

        card.getChildren().addAll(topRow, amtRow, pb, bottomRow);
        return card;
    }

    private void drawPaceChart(double pct) {
        pacePaneContainer.getChildren().clear();
        double w = Math.max(pacePaneContainer.getPrefWidth(), 200);
        double h = Math.max(pacePaneContainer.getPrefHeight(), 60);
        Canvas canvas = new Canvas(w, h);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        int bars = 7;
        double barW = (w - (bars - 1) * 6) / bars;
        int filledBars = (int) Math.round(Math.min(pct, 1.0) * bars);

        for (int i = 0; i < bars; i++) {
            double x = i * (barW + 6);
            double barH = 16 + (i * 4.5);
            barH = Math.min(barH, h - 8);
            boolean filled = i < filledBars;
            gc.setFill(filled ? Color.web("#C87941") : Color.web("#E8DDD0"));
            gc.fillRoundRect(x, h - barH - 4, barW, barH, 4, 4);
        }
        pacePaneContainer.getChildren().add(canvas);
    }

    @FXML
    public void showAddBudgetDialog() {
        List<Category> cats = DAOFactory.getCategoryDAO().findAll(userId, "EXPENSE");
        ComboBox<Category> catCombo = StyledDialog.combo();
        catCombo.getItems().setAll(cats);
        if (!cats.isEmpty()) catCombo.setValue(cats.get(0));

        TextField amtField = StyledDialog.field("Jumlah anggaran (mis: 500000)");

        String periodDisplay = currentPeriod.getMonth()
                .getDisplayName(TextStyle.FULL, new Locale("id", "ID")) + " " + currentPeriod.getYear();

        Label periodBadge = new Label("📅  " + periodDisplay);
        periodBadge.setStyle("-fx-background-color: #FDF0E4; -fx-text-fill: #C87941; -fx-font-size: 12; " +
                "-fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 6;");

        Label errLbl = StyledDialog.errorLabel();

        Stage dialog = new StyledDialog.Builder()
                .title("Tambah Anggaran")
                .subtitle("Atur batas pengeluaran per kategori")
                .icon("💰")
                .confirmText("Simpan Anggaran")
                .content(
                        periodBadge,
                        StyledDialog.fieldGroup("KATEGORI", catCombo),
                        StyledDialog.fieldGroup("JUMLAH ANGGARAN (Rp)", amtField),
                        errLbl
                )
                .onConfirm(() -> {
                    if (catCombo.getValue() == null) { StyledDialog.showError(errLbl, "Pilih kategori."); return; }
                    String txt = amtField.getText().trim();
                    if (txt.isEmpty()) { StyledDialog.showError(errLbl, "Jumlah anggaran tidak boleh kosong."); return; }
                    try {
                        Budget b = new Budget();
                        b.setUserId(userId);
                        b.setCategoryId(catCombo.getValue().getId());
                        b.setAmount(new BigDecimal(txt));
                        b.setPeriod(currentPeriod);
                        DAOFactory.getBudgetDAO().save(b);
                        amtField.getScene().getWindow().hide();
                        loadData();
                        Toast.showSuccess("Anggaran berhasil disimpan ✓");
                    } catch (NumberFormatException e) {
                        StyledDialog.showError(errLbl, "Jumlah anggaran tidak valid.");
                    }
                })
                .build();
        dialog.showAndWait();
    }

    private void showEditBudgetDialog(Budget b) {
        TextField amtField = StyledDialog.field("Jumlah anggaran");
        amtField.setText(b.getAmount().toPlainString());

        Label infoBadge = new Label("📌  " + b.getCategoryName() + " — " +
                currentPeriod.getMonth().getDisplayName(TextStyle.SHORT, new Locale("id","ID")) + " " + currentPeriod.getYear());
        infoBadge.setStyle("-fx-background-color: #EDE7DC; -fx-text-fill: #6B5230; -fx-font-size: 12; " +
                "-fx-padding: 6 12; -fx-background-radius: 6;");

        Label errLbl = StyledDialog.errorLabel();

        Stage dialog = new StyledDialog.Builder()
                .title("Edit Anggaran")
                .subtitle(b.getCategoryName())
                .icon(b.getCategoryIcon() != null ? b.getCategoryIcon() : "💰")
                .confirmText("Simpan Perubahan")
                .content(
                        infoBadge,
                        StyledDialog.fieldGroup("JUMLAH ANGGARAN BARU (Rp)", amtField),
                        errLbl
                )
                .onConfirm(() -> {
                    String txt = amtField.getText().trim();
                    if (txt.isEmpty()) { StyledDialog.showError(errLbl, "Jumlah tidak boleh kosong."); return; }
                    try {
                        b.setAmount(new BigDecimal(txt));
                        DAOFactory.getBudgetDAO().save(b);
                        amtField.getScene().getWindow().hide();
                        loadData();
                        Toast.showSuccess("Anggaran berhasil diperbarui ✓");
                    } catch (NumberFormatException e) {
                        StyledDialog.showError(errLbl, "Jumlah tidak valid.");
                    }
                })
                .build();
        dialog.showAndWait();
    }

    @FXML
    private void openPreviousCycles() {
        currentPeriod = currentPeriod.minusMonths(1);
        loadData();
    }

    @FXML
    private void openNextCycles() {
        currentPeriod = currentPeriod.plusMonths(1);
        loadData();
    }

    private String getCategoryDesc(String name) {
        if (name == null) return "";
        return switch (name.toLowerCase()) {
            case "dining" -> "Makan malam & Acara sosial";
            case "transport" -> "BBM, Transit & Parkir";
            case "entertainment" -> "Film, Acara & Langganan";
            case "personal care" -> "Wellness & Perawatan";
            case "housing" -> "Sewa, Listrik & Perawatan";
            case "shopping" -> "Belanja Ritel & Online";
            case "healthcare" -> "Medis & Apotek";
            case "education" -> "Kursus & Buku";
            default -> "";
        };
    }
}
