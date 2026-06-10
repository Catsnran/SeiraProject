package com.seira.controllers;

import com.seira.dao.DAOFactory;
import com.seira.models.Category;
import com.seira.models.PaymentMethod;
import com.seira.models.Transaction;
import com.seira.utils.SessionManager;
import com.seira.utils.Toast;
import com.seira.utils.FormatUtil;
import com.seira.utils.YahooFinanceService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class AddTransactionControllers {

    // Sentinel object untuk opsi "Tambah Kategori Baru"
    private static final Category ADD_NEW_CATEGORY = new Category(-1, "+ Tambah Kategori Baru", null, "#C87941", "➕");

    @FXML private TextField amountDisplay;
    @FXML private Button tabExpense, tabIncome, tabTransfer;
    @FXML private ComboBox<PaymentMethod> accountCombo;
    @FXML private ComboBox<Category> categoryCombo;
    @FXML private ComboBox<PaymentMethod> transferToCombo;
    @FXML private VBox categoryRow;
    @FXML private VBox transferToRow;
    @FXML private DatePicker datePicker;
    @FXML private TextField referenceField;
    @FXML private TextArea notesArea;
    @FXML private Label errorLabel;

    private StringBuilder amountStr = new StringBuilder("0");
    private boolean isUpdatingAmount = false;
    private String type = "EXPENSE";
    private int userId;
    private Transaction editTarget = null;
    private String returnPage = "transactions";
    private MainControllers mainController;

    @FXML
    public void initialize() {
        userId = SessionManager.getCurrentUser().getId();
        datePicker.setValue(LocalDate.now());
        loadCombos();

        amountDisplay.textProperty().addListener((observable, oldValue, newValue) -> {
            if (isUpdatingAmount) return;
            String digits = newValue.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) {
                digits = "0";
            }
            if (digits.length() > 13) {
                digits = digits.substring(0, 13);
            }
            amountStr = new StringBuilder(digits);
            updateAmountDisplay();
        });

        updateAmountDisplay();
        setActiveTab(tabExpense);
        setupCategoryComboListener();
    }

    public void setMainController(MainControllers mc) { this.mainController = mc; }
    public void setCurrentPage(String page) { this.returnPage = page; }

    public void setEditMode(Transaction t) {
        this.editTarget = t;
        this.type = t.getType();
        double amtUser = t.getAmount().doubleValue();
        String userCurrency = FormatUtil.getCurrencyCode();
        if ("USD".equalsIgnoreCase(userCurrency)) {
            amtUser = YahooFinanceService.convertPrice(amtUser, "IDR", "USD");
        }
        amountStr = new StringBuilder(String.format(java.util.Locale.US, "%.0f", amtUser));
        updateAmountDisplay();
        datePicker.setValue(t.getDate());
        if (t.getReference() != null) referenceField.setText(t.getReference());
        if (t.getNotes() != null) notesArea.setText(t.getNotes());

        switch (type) {
            case "INCOME" -> { setActiveTab(tabIncome); showCategoryMode(); }
            case "TRANSFER" -> { setActiveTab(tabTransfer); showTransferMode(); }
            default -> { setActiveTab(tabExpense); showCategoryMode(); }
        }

        for (PaymentMethod pm : accountCombo.getItems())
            if (pm.getId() == t.getPaymentMethodId()) { accountCombo.setValue(pm); break; }
        for (Category c : categoryCombo.getItems())
            if (c.getId() == t.getCategoryId()) { categoryCombo.setValue(c); break; }
    }

    private void loadCombos() {
        List<PaymentMethod> methods = DAOFactory.getPaymentMethodDAO().findAll(userId);
        accountCombo.getItems().setAll(methods);
        if (!methods.isEmpty()) accountCombo.setValue(methods.get(0));

        transferToCombo.getItems().setAll(methods);
        if (methods.size() > 1) transferToCombo.setValue(methods.get(1));
        else if (!methods.isEmpty()) transferToCombo.setValue(methods.get(0));

        loadCategoryCombo();
    }

    private void loadCategoryCombo() {
        String catType = "INCOME".equals(type) ? "INCOME" : "EXPENSE";
        List<Category> cats = DAOFactory.getCategoryDAO().findAll(userId, catType);

        // Custom cell factory untuk render "+ Tambah Kategori Baru" secara khusus
        categoryCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                if (item == ADD_NEW_CATEGORY) {
                    setText("  ➕  Tambah Kategori Baru");
                    setStyle("-fx-text-fill: #C87941; -fx-font-weight: bold; -fx-font-size: 13;");
                } else {
                    String icon = item.getIcon() != null ? item.getIcon() : "📌";
                    setText("  " + icon + "  " + item.getName());
                    setStyle("-fx-text-fill: #1A0F05;");
                }
            }
        });

        categoryCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                if (item == ADD_NEW_CATEGORY) {
                    setText("➕  Tambah Kategori Baru");
                    setStyle("-fx-text-fill: #C87941; -fx-font-weight: bold;");
                } else {
                    String icon = item.getIcon() != null ? item.getIcon() : "📌";
                    setText(icon + "  " + item.getName());
                    setStyle("-fx-text-fill: #1A0F05;");
                }
            }
        });

        categoryCombo.getItems().clear();
        categoryCombo.getItems().addAll(cats);
        categoryCombo.getItems().add(ADD_NEW_CATEGORY); // opsi terakhir
        if (!cats.isEmpty()) categoryCombo.setValue(cats.get(0));
    }

    /**
     * Listener: saat user pilih "Tambah Kategori Baru", buka dialog.
     * Setelah kategori dibuat, langsung set sebagai nilai terpilih.
     */
    private void setupCategoryComboListener() {
        categoryCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == ADD_NEW_CATEGORY) {
                // Kembalikan nilai sebelumnya dulu agar tidak stuck di sentinel
                categoryCombo.setValue(oldVal);

                CategoryManagerDialog dialog = new CategoryManagerDialog(userId);
                dialog.setOnCategoryAdded(newCat -> {
                    // Tambahkan ke combo sebelum sentinel, lalu pilih
                    if (newCat.getType().equals("INCOME".equals(type) ? "INCOME" : "EXPENSE")) {
                        int insertIdx = categoryCombo.getItems().size() - 1; // sebelum sentinel
                        categoryCombo.getItems().add(insertIdx, newCat);
                        categoryCombo.setValue(newCat);
                    } else {
                        // Tipe berbeda — reload setelah switch
                        loadCategoryCombo();
                    }
                });
                dialog.showAddDialog("INCOME".equals(type) ? "INCOME" : "EXPENSE");
            }
        });
    }

    @FXML private void selectExpense() {
        type = "EXPENSE";
        setActiveTab(tabExpense);
        showCategoryMode();
        loadCategoryCombo();
        setupCategoryComboListener();
    }

    @FXML private void selectIncome() {
        type = "INCOME";
        setActiveTab(tabIncome);
        showCategoryMode();
        loadCategoryCombo();
        setupCategoryComboListener();
    }

    @FXML private void selectTransfer() {
        type = "TRANSFER";
        setActiveTab(tabTransfer);
        showTransferMode();
    }

    /** Tampilkan row kategori, sembunyikan row transfer-to */
    private void showCategoryMode() {
        categoryRow.setVisible(true);
        categoryRow.setManaged(true);
        transferToRow.setVisible(false);
        transferToRow.setManaged(false);
    }

    /** Tampilkan row transfer-to, sembunyikan kategori */
    private void showTransferMode() {
        categoryRow.setVisible(false);
        categoryRow.setManaged(false);
        transferToRow.setVisible(true);
        transferToRow.setManaged(true);
    }

    private void setActiveTab(Button active) {
        for (Button b : new Button[]{tabExpense, tabIncome, tabTransfer}) {
            b.getStyleClass().remove("type-tab-active");
            b.getStyleClass().remove("type-tab");
            b.getStyleClass().add(b == active ? "type-tab-active" : "type-tab");
        }
    }

    // === Numpad ===
    @FXML private void pressKey1() { appendDigit("1"); }
    @FXML private void pressKey2() { appendDigit("2"); }
    @FXML private void pressKey3() { appendDigit("3"); }
    @FXML private void pressKey4() { appendDigit("4"); }
    @FXML private void pressKey5() { appendDigit("5"); }
    @FXML private void pressKey6() { appendDigit("6"); }
    @FXML private void pressKey7() { appendDigit("7"); }
    @FXML private void pressKey8() { appendDigit("8"); }
    @FXML private void pressKey9() { appendDigit("9"); }
    @FXML private void pressKey0() { appendDigit("0"); }
    @FXML private void pressDot() {
        // Rupiah tidak pakai desimal
        updateAmountDisplay();
    }
    @FXML private void pressBackspace() {
        if (amountStr.length() > 1) amountStr.deleteCharAt(amountStr.length() - 1);
        else amountStr = new StringBuilder("0");
        updateAmountDisplay();
    }

    private void appendDigit(String d) {
        if (amountStr.toString().equals("0")) amountStr = new StringBuilder();
        if (amountStr.length() >= 13) return; // max 9.999.999.999.999
        amountStr.append(d);
        updateAmountDisplay();
    }

    private void updateAmountDisplay() {
        isUpdatingAmount = true;
        try {
            long val = Long.parseLong(amountStr.toString());
            String currency = FormatUtil.getCurrencyCode();
            String formatted;
            if ("USD".equalsIgnoreCase(currency)) {
                formatted = "$ " + String.format(java.util.Locale.US, "%,d", val);
            } else {
                formatted = "Rp " + String.format("%,d", val).replace(',', '.');
            }
            amountDisplay.setText(formatted);
            amountDisplay.positionCaret(formatted.length());
        } catch (NumberFormatException e) {
            String currency = FormatUtil.getCurrencyCode();
            String prefix = "USD".equalsIgnoreCase(currency) ? "$ " : "Rp ";
            amountDisplay.setText(prefix + "0");
            amountDisplay.positionCaret(prefix.length() + 1);
        } finally {
            isUpdatingAmount = false;
        }
    }

    @FXML
    private void handleSave() {
        errorLabel.setVisible(false);

        long amount;
        try {
            amount = Long.parseLong(amountStr.toString());
            if (amount <= 0) { showError("Jumlah harus lebih dari nol."); return; }
        } catch (NumberFormatException e) { showError("Jumlah tidak valid."); return; }

        if (accountCombo.getValue() == null) { showError("Pilih akun terlebih dahulu."); return; }

        BigDecimal amountInIdr;
        String userCurrency = FormatUtil.getCurrencyCode();
        if ("USD".equalsIgnoreCase(userCurrency)) {
            double converted = YahooFinanceService.convertPrice(amount, "USD", "IDR");
            amountInIdr = BigDecimal.valueOf(converted);
        } else {
            amountInIdr = BigDecimal.valueOf(amount);
        }

        if ("TRANSFER".equals(type)) {
            // Transfer: kurangi dari akun asal, tambah ke akun tujuan
            if (transferToCombo.getValue() == null) { showError("Pilih akun tujuan."); return; }
            PaymentMethod from = accountCombo.getValue();
            PaymentMethod to = transferToCombo.getValue();
            if (from.getId() == to.getId()) { showError("Akun asal dan tujuan tidak boleh sama."); return; }

            String desc = notesArea.getText().trim();
            if (desc.isEmpty()) desc = "Transfer ke " + to.getName();

            // Dapatkan atau buat kategori khusus "Transfer" agar tidak salah masuk ke "Transport"
            int catId = getOrCreateTransferCategory();

            Transaction out = new Transaction();
            out.setUserId(userId); out.setDescription(desc);
            out.setAmount(amountInIdr); out.setType("EXPENSE");
            out.setDate(datePicker.getValue()); out.setCategoryId(catId);
            out.setPaymentMethodId(from.getId());
            out.setReference("TRANSFER"); out.setNotes("Transfer ke " + to.getName());

            Transaction in = new Transaction();
            in.setUserId(userId); in.setDescription("Transfer dari " + from.getName());
            in.setAmount(amountInIdr); in.setType("INCOME");
            in.setDate(datePicker.getValue()); in.setCategoryId(catId);
            in.setPaymentMethodId(to.getId());
            in.setReference("TRANSFER"); in.setNotes("Dari " + from.getName());

            boolean ok1 = DAOFactory.getTransactionDAO().add(out);
            boolean ok2 = DAOFactory.getTransactionDAO().add(in);
            if (ok1 && ok2) {
                Toast.showSuccess("Transfer berhasil disimpan ✓");
                navigateBack();
            }
            else showError("Gagal menyimpan transfer.");
            return;
        }

        // EXPENSE / INCOME
        if (categoryCombo.getValue() == null || categoryCombo.getValue() == ADD_NEW_CATEGORY) {
            showError("Pilih kategori terlebih dahulu.");
            return;
        }

        String desc = notesArea.getText().trim();
        if (desc.isEmpty()) desc = categoryCombo.getValue().getName();

        Transaction t = editTarget != null ? editTarget : new Transaction();
        t.setUserId(userId);
        t.setDescription(desc);
        t.setAmount(amountInIdr);
        t.setType(type);
        t.setDate(datePicker.getValue());
        t.setCategoryId(categoryCombo.getValue().getId());
        t.setPaymentMethodId(accountCombo.getValue().getId());
        t.setReference(referenceField.getText().trim());
        t.setNotes(notesArea.getText().trim());

        boolean ok = editTarget != null ? DAOFactory.getTransactionDAO().update(t) : DAOFactory.getTransactionDAO().add(t);
        if (ok) {
            if (editTarget != null) {
                Toast.showSuccess("Transaksi berhasil diperbarui ✓");
            } else {
                Toast.showSuccess("Transaksi berhasil disimpan ✓");
            }
            navigateBack();
        }
        else showError("Gagal menyimpan transaksi.");
    }

    private void navigateBack() {
        if (mainController != null) mainController.loadPage(returnPage);
    }

    @FXML
    private void handleCancel() { navigateBack(); }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }

    private int getOrCreateTransferCategory() {
        List<Category> cats = DAOFactory.getCategoryDAO().findAll(userId, null);
        for (Category c : cats) {
            if ("Transfer".equalsIgnoreCase(c.getName())) {
                return c.getId();
            }
        }
        Category transferCat = new Category();
        transferCat.setUserId(userId);
        transferCat.setName("Transfer");
        transferCat.setType("EXPENSE");
        transferCat.setColor("#4A90D9");
        transferCat.setIcon("🔄");
        boolean added = DAOFactory.getCategoryDAO().add(transferCat);
        if (added) {
            List<Category> updatedCats = DAOFactory.getCategoryDAO().findAll(userId, null);
            for (Category c : updatedCats) {
                if ("Transfer".equalsIgnoreCase(c.getName())) {
                    return c.getId();
                }
            }
        }
        if (!cats.isEmpty()) return cats.get(0).getId();
        return 1;
    }
}