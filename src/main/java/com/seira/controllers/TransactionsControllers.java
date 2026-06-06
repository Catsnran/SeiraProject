package com.seira.controllers;

import com.seira.dao.DAOFactory;
import com.seira.models.Transaction;
import com.seira.utils.*;
import com.opencsv.CSVWriter;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class TransactionsControllers {

    @FXML private VBox transactionList;
    @FXML private Label narrativeText, searchQuery;
    @FXML private HBox searchInfo;
    @FXML private Button showMoreBtn;
    @FXML private Button btnFilterAll, btnFilterExpenses, btnFilterIncome;

    private int userId;
    private String currentFilter = null;
    private int pageSize = 20;
    private int currentPage = 0;
    private List<Transaction> allTransactions;
    private com.seira.controllers.MainControllers mainControllers;

    @FXML
    public void initialize() {
        userId = SessionManager.getCurrentUser().getId();
        buildNarrative();
    }

    public void setMainController(com.seira.controllers.MainControllers mc) {
        this.mainControllers = mc;
        loadTransactions();
    }

    private void loadTransactions() {
        if (!mainControllers.getSearchQuery().isEmpty())
            allTransactions = DAOFactory.getTransactionDAO()
                .findAll(userId, currentFilter, null, null, mainControllers.getSearchQuery());
        else
            allTransactions = DAOFactory.getTransactionDAO().findAll(userId, currentFilter, null, null, null);
        currentPage = 0;
        renderTransactions();
    }

    private void renderTransactions() {
        transactionList.getChildren().clear();
        int limit = (currentPage + 1) * pageSize;
        List<Transaction> visible = allTransactions.subList(0, Math.min(limit, allTransactions.size()));

        for (Transaction t : visible) {
            transactionList.getChildren().add(buildTransactionRow(t));
        }

        if (showMoreBtn != null)
            showMoreBtn.setVisible(allTransactions.size() > limit);

        boolean isSearching = !mainControllers.getSearchQuery().isEmpty();
        if (visible.isEmpty()) {
            Label empty = new Label(
                isSearching
                ? "Tidak ada transaksi dengan kata kunci seperti ini."
                : "Belum ada transaksi. Tekan '+ Tambah Transaksi' untuk memulai."
            );
            empty.getStyleClass().add("mini-label");
            empty.setPadding(new Insets(24, 20, 24, 20));
            transactionList.getChildren().add(empty);
        }

        searchInfo.setVisible(isSearching);
        searchQuery.setText(mainControllers.getSearchQuery());
    }

    private HBox buildTransactionRow(Transaction t) {
        HBox row = new HBox(16);
        row.getStyleClass().add("tx-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(13, 20, 13, 20));

        // Date
        Label dateLbl = new Label(t.getDate().toString());
        dateLbl.getStyleClass().add("tx-date");
        dateLbl.setPrefWidth(100);

        // Icon + Description
        HBox descWrap = new HBox(10);
        descWrap.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(descWrap, Priority.ALWAYS);
        Label iconLbl = new Label(getIcon(t.getCategoryName(), t.isIncome()));
        iconLbl.getStyleClass().add("tx-icon");
        Label descLbl = new Label(t.getDescription());
        descLbl.getStyleClass().add("tx-desc");
        descWrap.getChildren().addAll(iconLbl, descLbl);

        // Category badge
        Label catLbl = new Label(t.getCategoryName() != null ? t.getCategoryName() : "—");
        catLbl.getStyleClass().add("category-badge");
        catLbl.setPrefWidth(120);

        // Amount
        Label amtLbl = new Label((t.isExpense() ? "-" : "+") + FormatUtil.formatCurrency(t.getAmount()));
        amtLbl.getStyleClass().add(t.isExpense() ? "amount-expense" : "amount-income");
        amtLbl.setPrefWidth(150);
        amtLbl.setAlignment(Pos.CENTER_RIGHT);

        // Actions
        HBox actions = new HBox(4);
        actions.setPrefWidth(70);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button editBtn = new Button("✏");
        editBtn.getStyleClass().add("btn-icon");
        editBtn.setOnAction(e -> editTransaction(t));
        Button delBtn = new Button("✕");
        delBtn.getStyleClass().add("btn-icon-danger");
        delBtn.setOnAction(e -> deleteTransaction(t));
        actions.getChildren().addAll(editBtn, delBtn);

        row.getChildren().addAll(dateLbl, descWrap, catLbl, amtLbl, actions);
        return row;
    }

    private void editTransaction(Transaction t) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Transaksi");
        dialog.setHeaderText("Edit: " + t.getDescription());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        VBox form = new VBox(12);
        form.setPadding(new Insets(20));
        TextField descField = new TextField(t.getDescription());
        TextField amtField = new TextField(t.getAmount().toPlainString());
        form.getChildren().addAll(new Label("Deskripsi:"), descField, new Label("Jumlah:"), amtField);
        dialog.getDialogPane().setContent(form);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    t.setDescription(descField.getText().trim());
                    t.setAmount(new BigDecimal(amtField.getText().trim()));
                    DAOFactory.getTransactionDAO().update(t);
                    loadTransactions();
                    Toast.showSuccess("Transaksi berhasil diperbarui ✓");
                } catch (Exception ex) {
                    Toast.showError("Jumlah tidak valid.");
                }
            }
        });
    }

    private void deleteTransaction(Transaction t) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Hapus Transaksi");
        confirm.setHeaderText("Hapus \"" + t.getDescription() + "\"?");
        confirm.setContentText("Tindakan ini tidak dapat dibatalkan.");
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                DAOFactory.getTransactionDAO().delete(t.getId());
                loadTransactions();
                Toast.showSuccess("Transaksi berhasil dihapus ✓");
            }
        });
    }

    @FXML private void filterAll() {
        currentFilter = null;
        setActiveFilter(btnFilterAll);
        loadTransactions();
    }

    @FXML private void filterExpenses() {
        currentFilter = "EXPENSE";
        setActiveFilter(btnFilterExpenses);
        loadTransactions();
    }

    @FXML private void filterIncome() {
        currentFilter = "INCOME";
        setActiveFilter(btnFilterIncome);
        loadTransactions();
    }

    private void setActiveFilter(Button active) {
        for (Button b : new Button[]{btnFilterAll, btnFilterExpenses, btnFilterIncome}) {
            if (b == null) continue;
            b.getStyleClass().remove("filter-btn-active");
            b.getStyleClass().remove("filter-btn");
            b.getStyleClass().add(b == active ? "filter-btn-active" : "filter-btn");
        }
    }

    @FXML
    private void showMore() {
        currentPage++;
        renderTransactions();
    }

    @FXML
    private void exportCsv() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export Transaksi");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fc.setInitialFileName("seira_transaksi.csv");
        File file = fc.showSaveDialog(transactionList.getScene().getWindow());
        if (file == null) return;
        try (CSVWriter writer = new CSVWriter(new FileWriter(file))) {
            writer.writeNext(new String[]{"Tanggal","Deskripsi","Kategori","Akun","Tipe","Jumlah","Catatan"});
            for (Transaction t : allTransactions) {
                writer.writeNext(new String[]{
                        t.getDate().toString(), t.getDescription(),
                        t.getCategoryName() != null ? t.getCategoryName() : "",
                        t.getPaymentMethodName() != null ? t.getPaymentMethodName() : "",
                        t.getType(), t.getAmount().toPlainString(),
                        t.getNotes() != null ? t.getNotes() : ""
                });
            }
            Toast.showSuccess("Berhasil diekspor ke: " + file.getName() + " ✓");
        } catch (Exception e) {
            Toast.showError("Gagal ekspor: " + e.getMessage());
        }
    }

    @FXML
    private void showDatePicker() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Pilih Rentang Tanggal");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        VBox content = new VBox(12);
        content.setPadding(new Insets(16));
        DatePicker fromPicker = new DatePicker();
        DatePicker toPicker = new DatePicker();
        fromPicker.setValue(LocalDate.now().withDayOfMonth(1));
        toPicker.setValue(LocalDate.now());
        content.getChildren().addAll(new Label("Dari:"), fromPicker, new Label("Sampai:"), toPicker);
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                allTransactions = DAOFactory.getTransactionDAO().findAll(userId, currentFilter, fromPicker.getValue(), toPicker.getValue(), null);
                currentPage = 0;
                renderTransactions();
            }
        });
    }

    private void buildNarrative() {
        double income = DAOFactory.getReportDAO().getTotalIncome(userId, java.time.YearMonth.now());
        double expense = DAOFactory.getReportDAO().getTotalExpense(userId, java.time.YearMonth.now());
        double savings = income - expense;
        double rate = income > 0 ? savings / income * 100 : 0;

        String text;
        if (income == 0 && expense == 0) {
            text = "Mulai tambahkan transaksi untuk melihat narasi keuangan personalmu. Setiap entri menceritakan kisah finansialmu.";
        } else if (savings > 0) {
            text = String.format("Posisi keuanganmu bulan ini menunjukkan surplus sebesar %s dengan tingkat tabungan %.1f%%. Pola pengeluaranmu mencerminkan disiplin finansial yang sehat.", FormatUtil.formatCurrency(savings), rate);
        } else {
            text = String.format("Pengeluaranmu sebesar %s melebihi pemasukan %s bulan ini. Tinjau anggaran kategorimu untuk mencari peluang penghematan.", FormatUtil.formatCurrency(expense), FormatUtil.formatCurrency(income));
        }
        narrativeText.setText(text);
    }

    private String getIcon(String cat, boolean isIncome) {
        if (isIncome) return "💰";
        if (cat == null) return "📌";
        return switch (cat.toLowerCase()) {
            case "dining" -> "🍽";
            case "transport" -> "🚗";
            case "housing" -> "🏠";
            case "entertainment" -> "🎬";
            case "shopping" -> "🛍";
            case "healthcare" -> "💊";
            case "salary" -> "💼";
            default -> "📌";
        };
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    @FXML
    private void deleteSearch() {
        mainControllers.clearSearchField();
        mainControllers.loadPage("transactions"); // is this how you refresh?
    }
}
