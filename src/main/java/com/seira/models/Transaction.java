package com.seira.models;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Model data satu entri transaksi keuangan.
 * Merepresentasikan satu baris pada tabel {@code transactions} di database.
 * Tipe transaksi: {@code "INCOME"} (pemasukan) atau {@code "EXPENSE"} (pengeluaran).
 */
public class Transaction {
    private int id;
    private int userId;
    private String description;
    private BigDecimal amount;
    /** Nilai: {@code "INCOME"} atau {@code "EXPENSE"}. */
    private String type;
    private LocalDate date;
    private int categoryId;
    /** Nama kategori — di-join dari tabel categories saat query. */
    private String categoryName;
    private int paymentMethodId;
    /** Nama akun — di-join dari tabel payment_methods saat query. */
    private String paymentMethodName;
    private String reference;
    private String notes;

    public Transaction() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public int getPaymentMethodId() { return paymentMethodId; }
    public void setPaymentMethodId(int paymentMethodId) { this.paymentMethodId = paymentMethodId; }

    public String getPaymentMethodName() { return paymentMethodName; }
    public void setPaymentMethodName(String paymentMethodName) { this.paymentMethodName = paymentMethodName; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    /** @return {@code true} jika tipe transaksi adalah pengeluaran. */
    public boolean isExpense() { return "EXPENSE".equalsIgnoreCase(type); }

    /** @return {@code true} jika tipe transaksi adalah pemasukan. */
    public boolean isIncome() { return "INCOME".equalsIgnoreCase(type); }
}
