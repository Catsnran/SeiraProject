package com.seira.utils;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class FormatUtil {
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getInstance(new Locale("id", "ID"));

    static {
        CURRENCY_FORMAT.setMinimumFractionDigits(0);
        CURRENCY_FORMAT.setMaximumFractionDigits(0);
    }

    public static String formatCurrency(double amount) {
        return "Rp " + CURRENCY_FORMAT.format(amount);
    }

    public static String formatCurrency(BigDecimal amount) {
        return formatCurrency(amount.doubleValue());
    }

    public static String formatShort(double amount) {
        if (amount >= 1_000_000_000) return String.format("Rp %.1fM", amount / 1_000_000_000);
        if (amount >= 1_000_000) return String.format("Rp %.1fjt", amount / 1_000_000);
        if (amount >= 1_000) return String.format("Rp %.1frb", amount / 1_000);
        return formatCurrency(amount);
    }
}
