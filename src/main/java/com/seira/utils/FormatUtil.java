package com.seira.utils;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class FormatUtil {
    public static String getCurrencyCode() {
        if (SessionManager.getCurrentUser() != null) {
            return SessionManager.getCurrentUser().getCurrency();
        }
        return "IDR";
    }

    public static String formatCurrency(double amount) {
        String currency = getCurrencyCode();
        if ("USD".equalsIgnoreCase(currency)) {
            NumberFormat usdFormat = NumberFormat.getInstance(Locale.US);
            usdFormat.setMinimumFractionDigits(2);
            usdFormat.setMaximumFractionDigits(2);
            return "$" + usdFormat.format(amount);
        } else {
            NumberFormat idrFormat = NumberFormat.getInstance(new Locale("id", "ID"));
            idrFormat.setMinimumFractionDigits(0);
            idrFormat.setMaximumFractionDigits(0);
            return "Rp " + idrFormat.format(amount);
        }
    }

    public static String formatCurrency(BigDecimal amount) {
        return formatCurrency(amount.doubleValue());
    }

    public static double convertIdrToUserCurrency(double amountInIdr) {
        String userCurrency = getCurrencyCode();
        return YahooFinanceService.convertPrice(amountInIdr, "IDR", userCurrency);
    }

    public static String formatIdr(double amountInIdr) {
        return formatCurrency(convertIdrToUserCurrency(amountInIdr));
    }

    public static String formatIdr(BigDecimal amountInIdr) {
        return formatIdr(amountInIdr.doubleValue());
    }

    public static String formatShort(double amount) {
        String currency = getCurrencyCode();
        if ("USD".equalsIgnoreCase(currency)) {
            if (amount >= 1_000_000_000) return String.format("$%.1fB", amount / 1_000_000_000);
            if (amount >= 1_000_000) return String.format("$%.1fM", amount / 1_000_000);
            if (amount >= 1_000) return String.format("$%.1fK", amount / 1_000);
            return formatCurrency(amount);
        } else {
            if (amount >= 1_000_000_000) return String.format("Rp %.1fM", amount / 1_000_000_000);
            if (amount >= 1_000_000) return String.format("Rp %.1fjt", amount / 1_000_000);
            if (amount >= 1_000) return String.format("Rp %.1frb", amount / 1_000);
            return formatCurrency(amount);
        }
    }
}
