package com.seira.models;

import java.math.BigDecimal;
import java.time.YearMonth;

public class Budget {
    private int id;
    private int userId;
    private int categoryId;
    private String categoryName;
    private String categoryIcon;
    private BigDecimal amount;
    private BigDecimal spent;
    private YearMonth period;

    public Budget() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getCategoryIcon() { return categoryIcon; }
    public void setCategoryIcon(String icon) { this.categoryIcon = icon; }

    public BigDecimal getAmount() { return amount != null ? amount : BigDecimal.ZERO; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getSpent() { return spent != null ? spent : BigDecimal.ZERO; }
    public void setSpent(BigDecimal spent) { this.spent = spent; }

    public YearMonth getPeriod() { return period; }
    public void setPeriod(YearMonth period) { this.period = period; }

    public BigDecimal getRemaining() {
        return getAmount().subtract(getSpent());
    }

    public double getPercentage() {
        if (getAmount().doubleValue() == 0.0 && getSpent().doubleValue() > 0.0) return Double.POSITIVE_INFINITY;
        if (getAmount().compareTo(BigDecimal.ZERO) == 0) return 0;
        return getSpent().doubleValue() / getAmount().doubleValue() * 100.0;
    }

    public String getStatus() {
        double pct = getPercentage();
        if (pct > 100) return "OVER BUDGET";
        if (pct >= 80) return "NEAR LIMIT";
        return "HEALTHY";
    }
}
