package com.seira.controllers;

import com.seira.dao.DAOFactory;
import com.seira.models.Budget;
import com.seira.models.Transaction;
import com.seira.utils.FormatUtil;
import com.seira.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public class DashboardControllers {

    @FXML
    private Label netWorthLabel;
    @FXML
    private Label netWorthChange;
    @FXML
    private Label liquidAssetsLabel;
    @FXML
    private Label investmentsLabel;
    @FXML
    private Label monthlyIncomeLabel;
    @FXML
    private Label monthlyExpenseLabel;
    @FXML
    private ProgressBar incomeProgress;
    @FXML
    private ProgressBar expenseProgress;
    @FXML
    private Label incomeProgressLabel;
    @FXML
    private Label expenseChangeLabel;
    @FXML
    private Label burnLabel;
    @FXML
    private Pane trendChartPane;
    @FXML
    private Pane burnRatePane;
    @FXML
    private Label burnRateDesc;
    @FXML
    private VBox budgetMiniList;
    @FXML
    private VBox recentLedgerList;
    @FXML
    private Label investmentStrategyLabel;

    private int userId;
    private MainControllers mainController;

    @FXML
    public void initialize() {
        userId = SessionManager.getCurrentUser().getId();
        loadData();
    }

    private void loadData() {
        YearMonth now = YearMonth.now();
        YearMonth prev = now.minusMonths(1);

        double income = DAOFactory.getReportDAO().getTotalIncome(userId, now);
        double expense = DAOFactory.getReportDAO().getTotalExpense(userId, now);
        double prevIncome = DAOFactory.getReportDAO().getTotalIncome(userId, prev);
        double prevExpense = DAOFactory.getReportDAO().getTotalExpense(userId, prev);
        double liquidity = DAOFactory.getPaymentMethodDAO().getTotalLiquidity(userId);
        double net = income - expense;

        // Investasi = hanya akun bertipe INVESTMENT
        // Liquid assets = semua akun SELAIN INVESTMENT
        double investments = DAOFactory.getPaymentMethodDAO().getLiquidityByType(userId, "INVESTMENT");
        double liquidAssets = DAOFactory.getPaymentMethodDAO().getLiquidityExcludingType(userId, "INVESTMENT");

        // Net Worth = total semua saldo akun
        netWorthLabel.setText(FormatUtil.formatCurrency(liquidity));
        liquidAssetsLabel.setText(FormatUtil.formatCurrency(liquidAssets));
        investmentsLabel.setText(FormatUtil.formatCurrency(investments));

        double prevNet = prevIncome - prevExpense;
        if (prevNet != 0) {
            double chg = (net - prevNet) / Math.abs(prevNet) * 100;
            netWorthChange.setText(String.format("%+.1f%% dari bulan lalu", chg));
            netWorthChange.getStyleClass().removeAll("stat-change-positive", "stat-change-negative");
            netWorthChange.getStyleClass().add(chg >= 0 ? "stat-change-positive" : "stat-change-negative");
        } else {
            netWorthChange.setText("Bulan pertama pencatatan");
        }

        // Monthly cards
        monthlyIncomeLabel.setText(FormatUtil.formatCurrency(income));
        monthlyExpenseLabel.setText(FormatUtil.formatCurrency(expense));

        double incPct = prevIncome > 0 ? income / (prevIncome * 1.2) : (income > 0 ? 0.75 : 0);
        incomeProgress.setProgress(Math.min(incPct, 1.0));
        incomeProgressLabel.setText(String.format("%.0f%% dari target bulanan proyeksi", Math.min(incPct * 100, 100)));

        double expPct = prevExpense > 0 ? expense / prevExpense : (expense > 0 ? 0.5 : 0);
        expenseProgress.setProgress(Math.min(expPct, 1.0));
        double expChange = prevExpense > 0 ? (expense - prevExpense) / prevExpense * 100 : 0;
        expenseChangeLabel.setText(String.format("%+.0f%% vs bulan lalu", expChange));

        if (expense <= prevExpense || prevExpense == 0) {
            burnLabel.setText("STABIL");
            burnLabel.getStyleClass().setAll("badge-green");
        } else {
            burnLabel.setText("MENINGKAT");
            burnLabel.getStyleClass().setAll("badge-red");
        }

        // Daily burn
        int daysPassed = java.time.LocalDate.now().getDayOfMonth();
        double dailyBurn = daysPassed > 0 ? expense / daysPassed : 0;
        double prevDailyBurn = prev.lengthOfMonth() > 0 ? prevExpense / prev.lengthOfMonth() : 0;
        double diff = dailyBurn - prevDailyBurn;

        if (diff < 0) {
            burnRateDesc.setText(String.format("Kamu menghemat %s per hari dibanding rata-rata kuartal.",
                    FormatUtil.formatCurrency(Math.abs(diff))));
        } else {
            burnRateDesc.setText(String.format("Pengeluaran harianmu %s lebih tinggi dari bulan lalu.",
                    FormatUtil.formatCurrency(diff)));
        }

        // Draw charts after layout (bind to width)
        trendChartPane.widthProperty().addListener((obs, ov, nv) -> drawTrendChart(now));
        trendChartPane.heightProperty().addListener((obs, ov, nv) -> drawTrendChart(now));
        drawTrendChart(now);

        burnRatePane.widthProperty().addListener((obs, ov, nv) -> drawBurnRateDonut(dailyBurn, expPct));
        burnRatePane.heightProperty().addListener((obs, ov, nv) -> drawBurnRateDonut(dailyBurn, expPct));
        drawBurnRateDonut(dailyBurn, expPct);

        // Budget mini list
        loadBudgetMini(now);

        // Recent ledger
        loadRecentLedger();

        // Strategy
        double savingsRate = income > 0 ? net / income * 100 : 0;
        if (income == 0) {
            investmentStrategyLabel.setText(
                    "Tambahkan pemasukan dan pengeluaran pertamamu untuk mulai mendapatkan analisis strategi keuangan.");
        } else if (savingsRate > 20) {
            investmentStrategyLabel.setText(String.format(
                    "Tingkat tabunganmu %.1f%% bulan ini — excellent! Pertimbangkan untuk mengalokasikan surplus ke instrumen investasi jangka panjang.",
                    savingsRate));
        } else if (savingsRate > 0) {
            investmentStrategyLabel.setText(String.format(
                    "Tingkat tabunganmu %.1f%%. Tinjau pengeluaran terbesarmu untuk meningkatkan surplus.",
                    savingsRate));
        } else {
            investmentStrategyLabel.setText(
                    "Pengeluaranmu melebihi pemasukan bulan ini. Fokus pada pengurangan biaya diskresioner untuk kembali ke posisi surplus.");
        }
    }

    private void drawTrendChart(YearMonth now) {
        double w = trendChartPane.getWidth();
        double h = trendChartPane.getHeight();
        if (w < 10 || h < 10)
            return;

        trendChartPane.getChildren().clear();
        Canvas canvas = new Canvas(w, h);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        List<double[]> trend = DAOFactory.getReportDAO().getMonthlyTrend(userId, 6);
        double maxVal = 1;
        for (double[] d : trend)
            maxVal = Math.max(maxVal, Math.max(d[0], d[1]));

        int n = trend.size();
        double slotW = (w - 40) / n;
        double barW = slotW * 0.35;
        double chartH = h - 30;

        for (int i = 0; i < n; i++) {
            double x = 20 + i * slotW + slotW * 0.1;
            double incH = maxVal > 0 ? (trend.get(i)[0] / maxVal) * chartH : 0;
            double expH = maxVal > 0 ? (trend.get(i)[1] / maxVal) * chartH : 0;

            // Income bar
            gc.setFill(Color.web("#C87941"));
            gc.fillRoundRect(x, h - 20 - incH, barW, incH, 3, 3);

            // Expense bar
            gc.setFill(Color.web("#E8DDD0"));
            gc.fillRoundRect(x + barW + 3, h - 20 - expH, barW, expH, 3, 3);

            // Month label
            String month = now.minusMonths(n - 1 - i).getMonth()
                    .getDisplayName(TextStyle.SHORT, Locale.US).toUpperCase();
            gc.setFill(Color.web("#8B7355"));
            gc.setFont(Font.font("System", 9));
            gc.fillText(month, x + barW * 0.3, h - 6);
        }

        trendChartPane.getChildren().add(canvas);
    }

    private void drawBurnRateDonut(double dailyBurn, double pct) {
        double size = Math.min(
                Math.max(burnRatePane.getWidth(), 100),
                Math.max(burnRatePane.getHeight(), 100));
        if (size < 40)
            size = 140;

        burnRatePane.getChildren().clear();
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        double cx = size / 2, cy = size / 2;
        double r = size * 0.40;
        double sw = size * 0.13;

        // Background ring
        gc.setStroke(Color.web("#E8DDD0"));
        gc.setLineWidth(sw);
        gc.strokeArc(cx - r, cy - r, r * 2, r * 2, 0, 360, ArcType.OPEN);

        // Progress ring
        gc.setStroke(Color.web("#C87941"));
        gc.setLineWidth(sw);
        double angle = Math.min(pct, 1.0) * 360;
        gc.strokeArc(cx - r, cy - r, r * 2, r * 2, 90, -angle, ArcType.OPEN);

        // Center: amount
        String amtText = FormatUtil.formatCurrency(dailyBurn);
        gc.setFill(Color.web("#2C1A0E"));
        gc.setFont(Font.font("System", FontWeight.BOLD, size * 0.13));
        // Approximate centering
        double tw = amtText.length() * size * 0.07;
        gc.fillText(amtText, cx - tw / 2, cy + size * 0.04);

        gc.setFont(Font.font("System", FontWeight.NORMAL, size * 0.085));
        gc.setFill(Color.web("#8B7355"));
        gc.fillText("AVG. / HARI", cx - size * 0.21, cy + size * 0.17);

        burnRatePane.getChildren().add(canvas);
    }

    private void loadBudgetMini(YearMonth now) {
        budgetMiniList.getChildren().clear();
        List<Budget> budgets = DAOFactory.getBudgetDAO().findAll(userId, now);
        int shown = 0;
        for (Budget b : budgets) {
            if (shown >= 3)
                break;

            VBox item = new VBox(5);

            HBox topRow = new HBox();
            topRow.setAlignment(Pos.CENTER_LEFT);
            Label name = new Label(b.getCategoryName());
            name.getStyleClass().add("budget-mini-name");
            HBox.setHgrow(name, Priority.ALWAYS);
            Label pctLbl = new Label(String.format("%.0f%%", b.getPercentage()));
            pctLbl.getStyleClass().add("budget-mini-pct");
            topRow.getChildren().addAll(name, pctLbl);

            Label amtLbl = new Label(
                    FormatUtil.formatCurrency(b.getSpent()) + " / " + FormatUtil.formatCurrency(b.getAmount()));
            amtLbl.getStyleClass().add("budget-mini-amt");

            ProgressBar pb = new ProgressBar(Math.min(b.getPercentage() / 100.0, 1.0));
            pb.setMaxWidth(Double.MAX_VALUE);
            pb.getStyleClass().clear();
            pb.getStyleClass().add("progress-bar");
            pb.getStyleClass().add(b.getPercentage() > 100 ? "progress-red"
                    : b.getPercentage() > 80 ? "progress-yellow" : "progress-green");

            item.getChildren().addAll(topRow, amtLbl, pb);
            budgetMiniList.getChildren().add(item);
            shown++;
        }
        if (shown == 0) {
            Label empty = new Label("Belum ada anggaran aktif");
            empty.getStyleClass().add("mini-label");
            budgetMiniList.getChildren().add(empty);
        }
    }

    private void loadRecentLedger() {
        recentLedgerList.getChildren().clear();
        List<Transaction> txs = DAOFactory.getTransactionDAO().findAll(userId, null, null, null, null);
        int shown = 0;
        for (Transaction t : txs) {
            if (shown >= 3)
                break;

            HBox row = new HBox(8);
            row.getStyleClass().add("ledger-row");
            row.setAlignment(Pos.CENTER_LEFT);

            Label iconLbl = new Label(t.isIncome() ? "💰" : getCategoryIcon(t.getCategoryName()));
            iconLbl.getStyleClass().add("ledger-icon");

            VBox info = new VBox(2);
            HBox.setHgrow(info, Priority.ALWAYS);
            Label descLbl = new Label(t.getDescription());
            descLbl.getStyleClass().add("ledger-desc");
            Label dateLbl = new Label(t.getDate().toString());
            dateLbl.getStyleClass().add("ledger-date");
            info.getChildren().addAll(descLbl, dateLbl);

            Label amtLbl = new Label((t.isExpense() ? "-" : "+") + FormatUtil.formatCurrency(t.getAmount()));
            amtLbl.getStyleClass().add(t.isExpense() ? "amount-expense" : "amount-income");

            row.getChildren().addAll(iconLbl, info, amtLbl);
            recentLedgerList.getChildren().add(row);
            shown++;
        }
        if (shown == 0) {
            Label empty = new Label("Belum ada transaksi terkini");
            empty.getStyleClass().add("mini-label");
            recentLedgerList.getChildren().add(empty);
        }
    }

    private String getCategoryIcon(String cat) {
        if (cat == null)
            return "📌";
        return switch (cat.toLowerCase()) {
            case "dining" -> "🍽";
            case "transport" -> "🚗";
            case "housing" -> "🏠";
            case "entertainment" -> "🎬";
            case "healthcare" -> "💊";
            case "shopping" -> "🛍";
            case "education" -> "📚";
            default -> "📌";
        };
    }

    public void setMainController(MainControllers mc) {
        this.mainController = mc;
    }

    @FXML
    private void openMonthlyReview() {
    }

    @FXML
    private void exportLedger() {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Export Ledger");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("CSV", "*.csv"));
        fc.setInitialFileName("seira_ledger.csv");
        java.io.File file = fc.showSaveDialog(netWorthLabel.getScene().getWindow());
        if (file == null)
            return;
        try (java.io.PrintWriter pw = new java.io.PrintWriter(file)) {
            pw.println("Tanggal,Deskripsi,Kategori,Akun,Tipe,Jumlah");
            java.util.List<com.seira.models.Transaction> txs = DAOFactory.getTransactionDAO().findAll(userId, null,
                    null, null, null);
            for (com.seira.models.Transaction t : txs) {
                pw.printf("%s,%s,%s,%s,%s,%s%n",
                        t.getDate(), t.getDescription(),
                        t.getCategoryName() != null ? t.getCategoryName() : "",
                        t.getPaymentMethodName() != null ? t.getPaymentMethodName() : "",
                        t.getType(), t.getAmount().toPlainString());
            }
        } catch (Exception e) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR,
                    "Gagal export: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void openBudgets() {
        if (mainController != null)
            mainController.loadPage("budget");
    }
}
