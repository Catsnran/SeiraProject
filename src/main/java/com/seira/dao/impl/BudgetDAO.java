package com.seira.dao.impl;

import com.seira.dao.DBConnection;
import com.seira.dao.interfaces.IBudgetDAO;
import com.seira.models.Budget;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class BudgetDAO implements IBudgetDAO {

    @Override
    public List<Budget> findAll(int userId, YearMonth period) {
        List<Budget> list = new ArrayList<>();
        try {
            String periodStr = period.toString();
            PreparedStatement ps = DBConnection.getInstance().getConnection().prepareStatement(
                "SELECT b.*, c.name as cat_name, c.icon as cat_icon, " +
                "COALESCE((SELECT SUM(t.amount) FROM transactions t " +
                "  WHERE t.user_id=b.user_id AND t.category_id=b.category_id " +
                "  AND t.type='EXPENSE' AND strftime('%Y-%m', t.date)=?), 0) as spent " +
                "FROM budgets b LEFT JOIN categories c ON b.category_id=c.id " +
                "WHERE b.user_id=? AND b.period=? ORDER BY c.name"
            );
            ps.setString(1, periodStr);
            ps.setInt(2, userId);
            ps.setString(3, periodStr);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Budget b = new Budget();
                b.setId(rs.getInt("id"));
                b.setUserId(rs.getInt("user_id"));
                b.setCategoryId(rs.getInt("category_id"));
                b.setCategoryName(rs.getString("cat_name"));
                b.setCategoryIcon(rs.getString("cat_icon"));
                b.setAmount(BigDecimal.valueOf(rs.getDouble("amount")));
                b.setSpent(BigDecimal.valueOf(rs.getDouble("spent")));
                b.setPeriod(period);
                list.add(b);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    @Override
    public boolean save(Budget b) {
        try {
            // Check if budget already exists for this user, category, and period
            PreparedStatement psCheck = DBConnection.getInstance().getConnection().prepareStatement(
                "SELECT id FROM budgets WHERE user_id = ? AND category_id = ? AND period = ?"
            );
            psCheck.setInt(1, b.getUserId());
            psCheck.setInt(2, b.getCategoryId());
            psCheck.setString(3, b.getPeriod().toString());
            ResultSet rs = psCheck.executeQuery();

            if (rs.next()) {
                // If it exists, update it
                PreparedStatement psUpdate = DBConnection.getInstance().getConnection().prepareStatement(
                    "UPDATE budgets SET amount = ? WHERE user_id = ? AND category_id = ? AND period = ?"
                );
                psUpdate.setDouble(1, b.getAmount().doubleValue());
                psUpdate.setInt(2, b.getUserId());
                psUpdate.setInt(3, b.getCategoryId());
                psUpdate.setString(4, b.getPeriod().toString());
                psUpdate.executeUpdate();
            } else {
                // If it doesn't exist, insert it
                PreparedStatement psInsert = DBConnection.getInstance().getConnection().prepareStatement(
                    "INSERT INTO budgets (user_id, category_id, amount, period) VALUES (?,?,?,?)"
                );
                psInsert.setInt(1, b.getUserId());
                psInsert.setInt(2, b.getCategoryId());
                psInsert.setDouble(3, b.getAmount().doubleValue());
                psInsert.setString(4, b.getPeriod().toString());
                psInsert.executeUpdate();
            }
            return true;
        } catch (SQLException e) { 
            e.printStackTrace();
            return false; 
        }
    }

    @Override
    public boolean delete(int id) {
        try {
            PreparedStatement ps = DBConnection.getInstance().getConnection().prepareStatement(
                "DELETE FROM budgets WHERE id=?"
            );
            ps.setInt(1, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) { return false; }
    }
}
