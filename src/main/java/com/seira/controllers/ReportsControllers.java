package com.seira.controllers;

import com.seira.dao.DAOFactory;
import com.seira.utils.FormatUtil;
import com.seira.utils.SessionManager;
import com.seira.utils.Toast;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public class ReportsControllers {

    @FXML private Pane netWorthChartPane;
    @FXML private Pane donutChartPane;
    @FXML private VBox donutLegend;
    @FXML private VBox monthlyBreakdownList;
    @FXML private ToggleButton btn12Months, btnAllTime;
    @FXML private Label totalSpendingLabel;

    private int userId;
    private int monthsToShow = 12;

    @FXML
    public void initialize() {
        userId = SessionManager.getCurrentUser().getId();
        loadData();
        netWorthChartPane.widthProperty().addListener((o, v, n) -> drawNetWorthChart());
        netWorthChartPane.heightProperty().addListener((o, v, n) -> drawNetWorthChart());
        donutChartPane.widthProperty().addListener((o, v, n) -> drawDonutChart());
        donutChartPane.heightProperty().addListener((o, v, n) -> drawDonutChart());
    }

    private void loadData() {
        drawNetWorthChart();
        drawDonutChart();
        loadMonthlyBreakdown();
    }

    @FXML private void show12Months() { monthsToShow = 12; btn12Months.setSelected(true); btnAllTime.setSelected(false); drawNetWorthChart(); }
    @FXML private void showAllTime() { monthsToShow = 24; btnAllTime.setSelected(true); btn12Months.setSelected(false); drawNetWorthChart(); }

    private void drawNetWorthChart() {
        double w = netWorthChartPane.getWidth();
        double h = netWorthChartPane.getHeight();
        if (w < 20 || h < 20) return;

        netWorthChartPane.getChildren().clear();
        Canvas canvas = new Canvas(w, h);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        List<double[]> trend = DAOFactory.getReportDAO().getNetWorthTrend(userId, monthsToShow);
        if (trend.isEmpty()) {
            gc.setFill(Color.web("#8B7355"));
            gc.fillText("Belum ada data. Tambahkan transaksi untuk melihat trenmu.", 20, h / 2);
            netWorthChartPane.getChildren().add(canvas);
            return;
        }

        double maxVal = trend.stream().mapToDouble(d -> Math.abs(d[0])).max().orElse(1);
        double minVal = trend.stream().mapToDouble(d -> d[0]).min().orElse(0);
        double range = maxVal - minVal;
        if (range == 0) range = 1;

        double padLeft = 10, padRight = 10, padTop = 20, padBottom = 30;
        double chartW = w - padLeft - padRight;
        double chartH = h - padTop - padBottom;

        // Draw grid lines
        gc.setStroke(Color.web("#E8DDD0"));
        gc.setLineWidth(1);
        for (int i = 0; i <= 4; i++) {
            double y = padTop + chartH * i / 4;
            gc.strokeLine(padLeft, y, w - padRight, y);
        }

        // Build points
        int n = trend.size();
        double[] xs = new double[n];
        double[] ys = new double[n];
        for (int i = 0; i < n; i++) {
            xs[i] = padLeft + (double) i / (n - 1) * chartW;
            double normVal = (trend.get(i)[0] - minVal) / range;
            ys[i] = padTop + (1 - normVal) * chartH;
        }

        // Draw filled area
        double[] fillXs = new double[n + 2];
        double[] fillYs = new double[n + 2];
        System.arraycopy(xs, 0, fillXs, 0, n);
        System.arraycopy(ys, 0, fillYs, 0, n);
        fillXs[n] = xs[n - 1]; fillYs[n] = padTop + chartH;
        fillXs[n + 1] = xs[0]; fillYs[n + 1] = padTop + chartH;
        gc.setFill(Color.web("#C87941", 0.15));
        gc.fillPolygon(fillXs, fillYs, n + 2);

        // Draw line (smooth approximation via cubic)
        gc.setStroke(Color.web("#C87941"));
        gc.setLineWidth(2.5);
        gc.beginPath();
        gc.moveTo(xs[0], ys[0]);
        for (int i = 1; i < n; i++) {
            double cpx1 = xs[i - 1] + (xs[i] - xs[i - 1]) * 0.5;
            double cpx2 = xs[i] - (xs[i] - xs[i - 1]) * 0.5;
            gc.bezierCurveTo(cpx1, ys[i - 1], cpx2, ys[i], xs[i], ys[i]);
        }
        gc.stroke();

        // Draw tooltip dot on last point
        gc.setFill(Color.web("#C87941"));
        gc.fillOval(xs[n - 1] - 5, ys[n - 1] - 5, 10, 10);

        // Label on peak
        int peakIdx = 0;
        double peakVal = trend.get(0)[0];
        for (int i = 1; i < n; i++) {
            if (trend.get(i)[0] > peakVal) { peakVal = trend.get(i)[0]; peakIdx = i; }
        }
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(Color.web("#2C1A0E"));
        gc.setFont(Font.font("System", FontWeight.BOLD, 11));
        gc.fillText(FormatUtil.formatCurrency(peakVal), xs[peakIdx], ys[peakIdx] - 10);

        // Month labels
        gc.setFont(Font.font("System", 10));
        gc.setFill(Color.web("#8B7355"));
        int labelEvery = Math.max(1, n / 6);
        for (int i = 0; i < n; i += labelEvery) {
            String label = YearMonth.now().minusMonths(n - 1 - i)
                    .getMonth().getDisplayName(TextStyle.SHORT, new Locale("id", "ID"));
            gc.fillText(label, xs[i], h - 6);
        }

        netWorthChartPane.getChildren().add(canvas);
    }

    private void drawDonutChart() {
        double size = Math.min(donutChartPane.getWidth(), donutChartPane.getHeight());
        if (size < 20) size = 200;
        donutChartPane.getChildren().clear();

        List<Object[]> breakdown = DAOFactory.getReportDAO()
                .getCategoryBreakdown(userId, YearMonth.now());
        double total = breakdown.stream().mapToDouble(d -> (double) d[2]).sum();

        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double cx = size / 2, cy = size / 2;
        double r = size * 0.38;
        double sw = size * 0.16;
        double gap = 2;

        if (breakdown.isEmpty()) {
            gc.setFill(Color.web("#E8DDD0"));
            gc.setLineWidth(sw);
            gc.strokeArc(cx - r, cy - r, r * 2, r * 2, 0, 360, ArcType.OPEN);
        } else {
            double startAngle = 90;
            for (Object[] entry : breakdown) {
                String color = (String) entry[1];
                double val = (double) entry[2];
                double sweep = (val / total) * 360 - gap;
                gc.setStroke(Color.web(color != null ? color : "#C87941"));
                gc.setLineWidth(sw);
                gc.strokeArc(cx - r, cy - r, r * 2, r * 2, startAngle, -sweep, ArcType.OPEN);
                startAngle -= (val / total) * 360;
            }
        }

        // Center label
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(Color.web("#2C1A0E"));
        gc.setFont(Font.font("System", FontWeight.NORMAL, size * 0.07));
        String totalLabel = "TOTAL";
        gc.fillText(totalLabel, cx, cy - size * 0.04);

        String totalAmt = FormatUtil.formatCurrency(total);
        double fontSize = size * 0.105; // 21pt for size=200
        if (totalAmt.length() > 12) {
            fontSize = size * 0.075; // 15pt for large numbers (e.g. Rp 10.000.000)
        } else if (totalAmt.length() > 9) {
            fontSize = size * 0.085; // 17pt (e.g. Rp 100.000)
        }
        gc.setFont(Font.font("System", FontWeight.BOLD, fontSize));
        gc.fillText(totalAmt, cx, cy + size * 0.08);

        donutChartPane.getChildren().add(canvas);
        totalSpendingLabel.setText(FormatUtil.formatCurrency(total));

        // Legend
        donutLegend.getChildren().clear();
        int shown = 0;
        for (Object[] entry : breakdown) {
            if (shown++ >= 4) break;
            String name = (String) entry[0];
            String color = (String) entry[1];
            double val = (double) entry[2];
            HBox row = new HBox(8);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            Label dot = new Label("●");
            dot.setStyle("-fx-text-fill: " + (color != null ? color : "#C87941") + "; -fx-font-size: 14;");
            Label nameLbl = new Label(name);
            nameLbl.getStyleClass().add("legend-name");
            HBox.setHgrow(nameLbl, Priority.ALWAYS);
            Label valLbl = new Label(FormatUtil.formatCurrency(val));
            valLbl.getStyleClass().add("legend-value");
            row.getChildren().addAll(dot, nameLbl, valLbl);
            donutLegend.getChildren().add(row);
        }
    }

    private void loadMonthlyBreakdown() {
        monthlyBreakdownList.getChildren().clear();
        for (int i = 2; i >= 0; i--) {
            YearMonth ym = YearMonth.now().minusMonths(i);
            double income = DAOFactory.getReportDAO().getTotalIncome(userId, ym);
            double expense = DAOFactory.getReportDAO().getTotalExpense(userId, ym);
            double savings = income - expense;
            double savingsRate = income > 0 ? savings / income * 100 : 0;
            boolean surplus = savings >= 0;

            HBox row = new HBox(16);
            row.getStyleClass().add("breakdown-row");
            row.setPadding(new javafx.geometry.Insets(14, 20, 14, 20));

            Label monthLbl = new Label(ym.getMonth().getDisplayName(TextStyle.FULL, new Locale("id", "ID")));
            monthLbl.getStyleClass().add("breakdown-month");
            monthLbl.setPrefWidth(120);

            Label inLbl = new Label(FormatUtil.formatCurrency(income));
            inLbl.getStyleClass().add("breakdown-income");
            inLbl.setPrefWidth(140);

            Label outLbl = new Label(FormatUtil.formatCurrency(expense));
            outLbl.getStyleClass().add("breakdown-expense");
            outLbl.setPrefWidth(140);

            Label rateLbl = new Label(String.format("%.1f%%", savingsRate));
            rateLbl.getStyleClass().add(surplus ? "savings-positive" : "savings-negative");
            rateLbl.setPrefWidth(120);

            Label statusLbl = new Label(surplus ? "SURPLUS" : "DEFISIT");
            statusLbl.getStyleClass().add(surplus ? "badge-surplus" : "badge-deficit");

            row.getChildren().addAll(monthLbl, inLbl, outLbl, rateLbl, statusLbl);
            monthlyBreakdownList.getChildren().add(row);
        }
    }

    @FXML
    private void exportCsv() {
        // Reuse CSV logic stub
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Ekspor Laporan");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("CSV", "*.csv"));
        fc.setInitialFileName("seira_laporan.csv");
        java.io.File file = fc.showSaveDialog(monthlyBreakdownList.getScene().getWindow());
        if (file == null) return;
        try (java.io.PrintWriter pw = new java.io.PrintWriter(file)) {
            pw.println("Bulan,Pemasukan,Pengeluaran,Rasio Tabungan,Status");
            for (int i = 2; i >= 0; i--) {
                YearMonth ym = YearMonth.now().minusMonths(i);
                double inc = DAOFactory.getReportDAO().getTotalIncome(userId, ym);
                double exp = DAOFactory.getReportDAO().getTotalExpense(userId, ym);
                double rate = inc > 0 ? (inc - exp) / inc * 100 : 0;
                pw.printf("%s,%.2f,%.2f,%.1f%%,%s%n",
                        ym, inc, exp, rate, inc >= exp ? "SURPLUS" : "DEFISIT");
            }
            Toast.showSuccess("Berhasil diekspor ke: " + file.getName() + " ✓");
        } catch (Exception e) {
            Toast.showError("Gagal ekspor: " + e.getMessage());
        }
    }
}
