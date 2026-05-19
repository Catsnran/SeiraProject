package com.seira.dao.impl;

import com.seira.dao.DBConnection;
import com.seira.dao.interfaces.IReportDAO;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class ReportDAO implements IReportDAO {

    @Override
    public double getTotalIncome(int userId, YearMonth period) {
        return sum(userId, "INCOME", period);
    }

    @Override
    public double getTotalExpense(int userId, YearMonth period) {
        return sum(userId, "EXPENSE", period);
    }

    private double sum(int userId, String type, YearMonth period) {
        try {
            String sql = "SELECT COALESCE(SUM(amount),0) FROM transactions WHERE user_id=? AND type=?";
            if (period != null) sql += " AND strftime('%Y-%m', date)=?";
            PreparedStatement ps = DBConnection.getInstance().getConnection().prepareStatement(sql);
            ps.setInt(1, userId);
            ps.setString(2, type);
            if (period != null) ps.setString(3, period.toString());
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getDouble(1) : 0;
        } catch (SQLException e) { e.printStackTrace(); return 0; }
    }

    @Override
    public List<double[]> getMonthlyTrend(int userId, int months) {
        List<double[]> list = new ArrayList<>();
        for (int i = months - 1; i >= 0; i--) {
            YearMonth ym = YearMonth.now().minusMonths(i);
            list.add(new double[]{getTotalIncome(userId, ym), getTotalExpense(userId, ym)});
        }
        return list;
    }

    @Override
    public List<double[]> getNetWorthTrend(int userId, int months) {
        List<double[]> list = new ArrayList<>();
        double running = 0;
        for (int i = months - 1; i >= 0; i--) {
            YearMonth ym = YearMonth.now().minusMonths(i);
            running += getTotalIncome(userId, ym) - getTotalExpense(userId, ym);
            list.add(new double[]{running});
        }
        return list;
    }

    @Override
    public List<Object[]> getCategoryBreakdown(int userId, YearMonth period) {
        List<Object[]> list = new ArrayList<>();
        try {
            PreparedStatement ps = DBConnection.getInstance().getConnection().prepareStatement(
                "SELECT c.name, c.color, SUM(t.amount) as total FROM transactions t " +
                "JOIN categories c ON t.category_id=c.id " +
                "WHERE t.user_id=? AND t.type='EXPENSE' AND strftime('%Y-%m', t.date)=? " +
                "GROUP BY c.id ORDER BY total DESC"
            );
            ps.setInt(1, userId);
            ps.setString(2, period.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Object[]{rs.getString("name"), rs.getString("color"), rs.getDouble("total")});
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }
}
