package com.seira.controllers;

import com.seira.dao.DAOFactory;
import com.seira.models.Budget;
import com.seira.models.Transaction;
import com.seira.models.StockAsset;
import com.seira.utils.FormatUtil;
import com.seira.utils.SessionManager;
import com.seira.utils.YahooFinanceService;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.application.Platform;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.math.BigDecimal;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.stage.Stage;
import com.seira.utils.StyledDialog;

public class DashboardControllers {

    @FXML
    private Label netWorthLabel;
    @FXML
    private Button monthlyReviewBtn;

    private LocalDate customStartDate = null;
    private LocalDate customEndDate = null;
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
    @FXML
    private ComboBox<String> stockCombo;
    @FXML
    private Label stockStatusLabel;
    @FXML
    private Pane stockChartPane;

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

        double income;
        double expense;
        double prevIncome;
        double prevExpense;

        if (customStartDate != null && customEndDate != null) {
            List<Transaction> incomeTxs = DAOFactory.getTransactionDAO().findAll(userId, "INCOME", customStartDate, customEndDate, null);
            income = incomeTxs.stream().mapToDouble(t -> t.getAmount().doubleValue()).sum();

            List<Transaction> expenseTxs = DAOFactory.getTransactionDAO().findAll(userId, "EXPENSE", customStartDate, customEndDate, null);
            expense = expenseTxs.stream().mapToDouble(t -> t.getAmount().doubleValue()).sum();

            long days = java.time.temporal.ChronoUnit.DAYS.between(customStartDate, customEndDate) + 1;
            LocalDate prevEndDate = customStartDate.minusDays(1);
            LocalDate prevStartDate = prevEndDate.minusDays(days - 1);

            List<Transaction> prevIncomeTxs = DAOFactory.getTransactionDAO().findAll(userId, "INCOME", prevStartDate, prevEndDate, null);
            prevIncome = prevIncomeTxs.stream().mapToDouble(t -> t.getAmount().doubleValue()).sum();

            List<Transaction> prevExpenseTxs = DAOFactory.getTransactionDAO().findAll(userId, "EXPENSE", prevStartDate, prevEndDate, null);
            prevExpense = prevExpenseTxs.stream().mapToDouble(t -> t.getAmount().doubleValue()).sum();
            
            if (monthlyReviewBtn != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
                monthlyReviewBtn.setText("Tinjauan: " + customStartDate.format(formatter) + " - " + customEndDate.format(formatter));
            }
        } else {
            income = DAOFactory.getReportDAO().getTotalIncome(userId, now);
            expense = DAOFactory.getReportDAO().getTotalExpense(userId, now);
            prevIncome = DAOFactory.getReportDAO().getTotalIncome(userId, prev);
            prevExpense = DAOFactory.getReportDAO().getTotalExpense(userId, prev);
            
            if (monthlyReviewBtn != null) {
                monthlyReviewBtn.setText("Tinjauan Bulanan");
            }
        }

        double liquidity = DAOFactory.getPaymentMethodDAO().getTotalLiquidity(userId);
        double net = income - expense;

        // Investasi = hanya akun bertipe INVESTMENT
        // Liquid assets = semua akun SELAIN INVESTMENT
        double investments = DAOFactory.getPaymentMethodDAO().getLiquidityByType(userId, "saham");
        double liquidAssets = DAOFactory.getPaymentMethodDAO().getLiquidityExcludingType(userId, "INVESTMENT");

        // Net Worth = total semua saldo akun
        netWorthLabel.setText(FormatUtil.formatIdr(liquidity));
        liquidAssetsLabel.setText(FormatUtil.formatIdr(liquidAssets));
        investmentsLabel.setText(FormatUtil.formatIdr(investments));

        double prevNet = prevIncome - prevExpense;
        if (prevNet != 0) {
            double chg = (net - prevNet) / Math.abs(prevNet) * 100;
            if (customStartDate != null && customEndDate != null) {
                netWorthChange.setText(String.format("%+.1f%% dari periode lalu", chg));
            } else {
                netWorthChange.setText(String.format("%+.1f%% dari bulan lalu", chg));
            }
            netWorthChange.getStyleClass().removeAll("stat-change-positive", "stat-change-negative");
            netWorthChange.getStyleClass().add(chg >= 0 ? "stat-change-positive" : "stat-change-negative");
        } else {
            netWorthChange.setText("Bulan pertama pencatatan");
        }

        // Monthly cards
        monthlyIncomeLabel.setText(FormatUtil.formatIdr(income));
        monthlyExpenseLabel.setText(FormatUtil.formatIdr(expense));

        double incPct = prevIncome > 0 ? income / (prevIncome * 1.2) : (income > 0 ? 0.75 : 0);
        incomeProgress.setProgress(Math.min(incPct, 1.0));
        if (customStartDate != null && customEndDate != null) {
            incomeProgressLabel.setText(String.format("%.0f%% dari target periode proyeksi", Math.min(incPct * 100, 100)));
        } else {
            incomeProgressLabel.setText(String.format("%.0f%% dari target bulanan proyeksi", Math.min(incPct * 100, 100)));
        }

        double expPct = prevExpense > 0 ? expense / prevExpense : (expense > 0 ? 0.5 : 0);
        expenseProgress.setProgress(Math.min(expPct, 1.0));
        double expChange = prevExpense > 0 ? (expense - prevExpense) / prevExpense * 100 : 0;
        if (customStartDate != null && customEndDate != null) {
            expenseChangeLabel.setText(String.format("%+.0f%% vs periode lalu", expChange));
        } else {
            expenseChangeLabel.setText(String.format("%+.0f%% vs bulan lalu", expChange));
        }

        if (expense <= prevExpense || prevExpense == 0) {
            burnLabel.setText("STABIL");
            burnLabel.getStyleClass().setAll("badge-green");
        } else {
            burnLabel.setText("MENINGKAT");
            burnLabel.getStyleClass().setAll("badge-red");
        }

        // Daily burn
        int daysPassed;
        if (customStartDate != null && customEndDate != null) {
            daysPassed = (int) (java.time.temporal.ChronoUnit.DAYS.between(customStartDate, customEndDate) + 1);
        } else {
            daysPassed = java.time.LocalDate.now().getDayOfMonth();
        }
        double dailyBurn = daysPassed > 0 ? expense / daysPassed : 0;
        
        double prevDailyBurn;
        if (customStartDate != null && customEndDate != null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(customStartDate, customEndDate) + 1;
            prevDailyBurn = days > 0 ? prevExpense / days : 0;
        } else {
            prevDailyBurn = prev.lengthOfMonth() > 0 ? prevExpense / prev.lengthOfMonth() : 0;
        }
        double diff = dailyBurn - prevDailyBurn;

        if (diff < 0) {
            if (customStartDate != null && customEndDate != null) {
                burnRateDesc.setText(String.format("Kamu menghemat %s per hari dibanding rata-rata periode lalu.",
                        FormatUtil.formatIdr(Math.abs(diff))));
            } else {
                burnRateDesc.setText(String.format("Kamu menghemat %s per hari dibanding rata-rata kuartal.",
                        FormatUtil.formatIdr(Math.abs(diff))));
            }
        } else {
            if (customStartDate != null && customEndDate != null) {
                burnRateDesc.setText(String.format("Pengeluaran harianmu %s lebih tinggi dari periode lalu.",
                        FormatUtil.formatIdr(diff)));
            } else {
                burnRateDesc.setText(String.format("Pengeluaran harianmu %s lebih tinggi dari bulan lalu.",
                        FormatUtil.formatIdr(diff)));
            }
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
            if (customStartDate != null && customEndDate != null) {
                investmentStrategyLabel.setText(String.format(
                        "Tingkat tabunganmu %.1f%% periode ini — excellent! Pertimbangkan untuk mengalokasikan surplus ke instrumen investasi jangka panjang.",
                        savingsRate));
            } else {
                investmentStrategyLabel.setText(String.format(
                        "Tingkat tabunganmu %.1f%% bulan ini — excellent! Pertimbangkan untuk mengalokasikan surplus ke instrumen investasi jangka panjang.",
                        savingsRate));
            }
        } else if (savingsRate > 0) {
            investmentStrategyLabel.setText(String.format(
                    "Tingkat tabunganmu %.1f%%. Tinjau pengeluaran terbesarmu untuk meningkatkan surplus.",
                    savingsRate));
        } else {
            if (customStartDate != null && customEndDate != null) {
                investmentStrategyLabel.setText(
                        "Pengeluaranmu melebihi pemasukan periode ini. Fokus pada pengurangan biaya diskresioner untuk kembali ke posisi surplus.");
            } else {
                investmentStrategyLabel.setText(
                        "Pengeluaranmu melebihi pemasukan bulan ini. Fokus pada pengurangan biaya diskresioner untuk kembali ke posisi surplus.");
            }
        }
        
        loadStockData();
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
        String amtText = FormatUtil.formatIdr(dailyBurn);
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
                    FormatUtil.formatIdr(b.getSpent()) + " / " + FormatUtil.formatIdr(b.getAmount()));
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

            Label amtLbl = new Label((t.isExpense() ? "-" : "+") + FormatUtil.formatIdr(t.getAmount()));
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
        DatePicker startPicker = new DatePicker();
        DatePicker endPicker = new DatePicker();

        // Style standard DatePickers to match the dialog
        String pickerStyle = "-fx-background-color: #EDE7DC; -fx-border-color: transparent; " +
                             "-fx-border-radius: 8; -fx-background-radius: 8; -fx-pref-height: 42; " +
                             "-fx-font-size: 13; -fx-text-fill: #1A0F05; -fx-padding: 0 14;";
        startPicker.setStyle(pickerStyle);
        endPicker.setStyle(pickerStyle);
        startPicker.setMaxWidth(Double.MAX_VALUE);
        endPicker.setMaxWidth(Double.MAX_VALUE);

        if (customStartDate != null) {
            startPicker.setValue(customStartDate);
        } else {
            startPicker.setValue(LocalDate.now().withDayOfMonth(1));
        }

        if (customEndDate != null) {
            endPicker.setValue(customEndDate);
        } else {
            endPicker.setValue(LocalDate.now());
        }

        Label errLabel = StyledDialog.errorLabel();

        VBox content = new VBox(14);
        content.getChildren().addAll(
            StyledDialog.fieldGroup("MULAI TANGGAL", startPicker),
            StyledDialog.fieldGroup("SAMPAI TANGGAL", endPicker),
            errLabel
        );

        final Stage[] stageRef = new Stage[1];

        Button resetBtn = new Button("Reset ke Bulan Ini");
        resetBtn.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #C0392B; -fx-font-size: 12; " +
            "-fx-padding: 6 12; -fx-background-radius: 6; -fx-border-color: #E8C5C0; " +
            "-fx-border-width: 1; -fx-border-radius: 6; -fx-cursor: hand;"
        );
        resetBtn.setMaxWidth(Double.MAX_VALUE);
        resetBtn.setOnAction(e -> {
            customStartDate = null;
            customEndDate = null;
            loadData();
            if (stageRef[0] != null) {
                stageRef[0].close();
            }
        });
        content.getChildren().add(resetBtn);

        StyledDialog.Builder builder = new StyledDialog.Builder()
            .title("Tinjauan Bulanan")
            .subtitle("Pilih rentang tanggal untuk memfilter ringkasan keuangan")
            .icon("📅")
            .confirmText("Terapkan")
            .cancelText("Batal")
            .content(content);

        builder.onConfirm(() -> {
            LocalDate start = startPicker.getValue();
            LocalDate end = endPicker.getValue();

            if (start == null || end == null) {
                StyledDialog.showError(errLabel, "Tanggal mulai dan sampai harus diisi.");
                return;
            }
            if (end.isBefore(start)) {
                StyledDialog.showError(errLabel, "Tanggal sampai tidak boleh sebelum tanggal mulai.");
                return;
            }

            customStartDate = start;
            customEndDate = end;

            loadData();

            if (stageRef[0] != null) {
                stageRef[0].close();
            }
        });

        stageRef[0] = builder.build();
        stageRef[0].showAndWait();
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

    private void loadStockData() {
        List<StockAsset> stocks = DAOFactory.getStockAssetDAO().findAll(userId);
        if (stocks.isEmpty()) {
            stockCombo.setVisible(false);
            stockCombo.setManaged(false);
            stockStatusLabel.setText("Belum ada saham terdaftar. Tambah aset saham di menu Akun.");
            stockChartPane.getChildren().clear();
            return;
        }

        stockCombo.setVisible(true);
        stockCombo.setManaged(true);
        stockCombo.getItems().clear();
        for (StockAsset sa : stocks) {
            stockCombo.getItems().add(sa.getStockSymbol());
        }

        stockCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                fetchAndDrawStock(newVal);
            }
        });

        // Select first stock by default
        stockCombo.setValue(stocks.get(0).getStockSymbol());
    }

    private void fetchAndDrawStock(String symbol) {
        System.out.println("[DEBUG] fetchAndDrawStock called for: " + symbol);
        stockStatusLabel.setText("Memuat data saham " + symbol + "...");
        stockStatusLabel.setStyle("-fx-text-fill: #8B7355;");
        
        CompletableFuture.supplyAsync(() -> {
            try {
                return YahooFinanceService.getChartData(symbol);
            } catch (Exception e) {
                System.out.println("[DEBUG] Error fetching chart data: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        })
            .thenAcceptAsync(data -> {
                if (data == null) {
                    stockStatusLabel.setText("Gagal koneksi API saham " + symbol + ".");
                    return;
                }
                List<YahooFinanceService.StockPricePoint> prices = data.getPrices();
                System.out.println("[DEBUG] prices size: " + (prices != null ? prices.size() : "null"));
                
                if (prices == null || prices.isEmpty()) {
                    stockStatusLabel.setText("Gagal memuat data harga untuk " + symbol + ".");
                    stockStatusLabel.setStyle("-fx-text-fill: #C0392B;");
                    stockChartPane.getChildren().clear();
                    return;
                }

                double latest = prices.get(prices.size() - 1).getPrice();
                double first = prices.get(0).getPrice();
                
                String stockCurrency = data.getCurrency();
                String userCurrency = SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getCurrency() : "IDR";
                double latestUser = YahooFinanceService.convertPrice(latest, stockCurrency, userCurrency);
                double firstUser = YahooFinanceService.convertPrice(first, stockCurrency, userCurrency);
                
                double pctChange = ((latestUser - firstUser) / firstUser) * 100;
                
                String formattedPrice = FormatUtil.formatCurrency(latestUser);
                String changeText = String.format("%s — %s (%+.1f%% bulan ini)", 
                    symbol, formattedPrice, pctChange);
                stockStatusLabel.setText(changeText);
                if (pctChange >= 0) {
                    stockStatusLabel.setStyle("-fx-text-fill: #27AE60; -fx-font-weight: bold;");
                } else {
                    stockStatusLabel.setStyle("-fx-text-fill: #C0392B; -fx-font-weight: bold;");
                }

                // Add size listeners to redraw on resize
                stockChartPane.widthProperty().addListener((obs, ov, nv) -> {
                    System.out.println("[DEBUG] Width listener fired. New width: " + nv);
                    drawStockLineChart(prices, pctChange >= 0);
                });
                stockChartPane.heightProperty().addListener((obs, ov, nv) -> {
                    System.out.println("[DEBUG] Height listener fired. New height: " + nv);
                    drawStockLineChart(prices, pctChange >= 0);
                });
                
                drawStockLineChart(prices, pctChange >= 0);
            }, Platform::runLater);
    }

    private void drawStockLineChart(List<YahooFinanceService.StockPricePoint> prices, boolean isPositive) {
        double w = stockChartPane.getWidth();
        double h = stockChartPane.getHeight();
        System.out.println("[DEBUG] drawStockLineChart. Pane dimensions: " + w + "x" + h);
        if (w < 10 || h < 10) return;

        stockChartPane.getChildren().clear();
        Canvas canvas = new Canvas(w, h);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        double minVal = Double.MAX_VALUE;
        double maxVal = Double.MIN_VALUE;
        for (YahooFinanceService.StockPricePoint p : prices) {
            minVal = Math.min(minVal, p.getPrice());
            maxVal = Math.max(maxVal, p.getPrice());
        }

        // Add 5% padding to top and bottom of chart values
        double diff = maxVal - minVal;
        if (diff == 0) diff = 1;
        minVal = Math.max(0, minVal - diff * 0.05);
        maxVal = maxVal + diff * 0.05;
        diff = maxVal - minVal;

        int n = prices.size();
        if (n < 2) return;
        double stepX = w / (n - 1);

        // Path for fill
        double[] xPoints = new double[n + 2];
        double[] yPoints = new double[n + 2];

        // Draw line and populate fill path
        gc.setLineWidth(2.0);
        gc.setStroke(isPositive ? Color.web("#27AE60") : Color.web("#C0392B"));
        
        gc.beginPath();
        for (int i = 0; i < n; i++) {
            double x = i * stepX;
            double y = h - ((prices.get(i).getPrice() - minVal) / diff) * h;
            System.out.println("[DEBUG] Drawing point " + i + ": x=" + x + ", y=" + y + ", price=" + prices.get(i).getPrice());
            if (i == 0) {
                gc.moveTo(x, y);
            } else {
                gc.lineTo(x, y);
            }
            xPoints[i] = x;
            yPoints[i] = y;
        }
        gc.stroke();

        // Close path for gradient fill
        xPoints[n] = (n - 1) * stepX;
        yPoints[n] = h;
        xPoints[n + 1] = 0;
        yPoints[n + 1] = h;

        // Draw smooth gradient below the line
        javafx.scene.paint.LinearGradient fillGrad = new javafx.scene.paint.LinearGradient(
            0, 0, 0, h, false, javafx.scene.paint.CycleMethod.NO_CYCLE,
            new javafx.scene.paint.Stop(0, isPositive ? Color.web("#27AE60", 0.15) : Color.web("#C0392B", 0.15)),
            new javafx.scene.paint.Stop(1, Color.TRANSPARENT)
        );
        gc.setFill(fillGrad);
        gc.fillPolygon(xPoints, yPoints, n + 2);

        stockChartPane.getChildren().add(canvas);
    }
}
