package com.seira.dao.impl;

import com.seira.dao.DBConnection;
import com.seira.dao.interfaces.IPaymentMethodDAO;
import com.seira.models.PaymentMethod;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PaymentMethodDAO implements IPaymentMethodDAO {

    @Override
    public List<PaymentMethod> findAll(int userId) {
        List<PaymentMethod> list = new ArrayList<>();
        try {
            PreparedStatement ps = DBConnection.getInstance().getConnection().prepareStatement(
                "SELECT * FROM payment_methods WHERE user_id=? ORDER BY name"
            );
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    @Override
    public PaymentMethod findById(int id) {
        try {
            PreparedStatement ps = DBConnection.getInstance().getConnection().prepareStatement(
                "SELECT * FROM payment_methods WHERE id=?"
            );
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    @Override
    public boolean add(PaymentMethod pm) {
        try {
            PreparedStatement ps = DBConnection.getInstance().getConnection().prepareStatement(
                "INSERT INTO payment_methods (user_id, name, type, balance, description) VALUES (?,?,?,?,?)"
            );
            ps.setInt(1, pm.getUserId());
            ps.setString(2, pm.getName());
            ps.setString(3, pm.getType());
            ps.setDouble(4, pm.getBalance().doubleValue());
            ps.setString(5, pm.getDescription());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) { return false; }
    }

    @Override
    public boolean updateBalance(int id, BigDecimal balance) {
        try {
            PreparedStatement ps = DBConnection.getInstance().getConnection().prepareStatement(
                "UPDATE payment_methods SET balance=? WHERE id=?"
            );
            ps.setDouble(1, balance.doubleValue());
            ps.setInt(2, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) { return false; }
    }

    @Override
    public boolean delete(int id) {
        try {
            PreparedStatement ps = DBConnection.getInstance().getConnection().prepareStatement(
                "DELETE FROM payment_methods WHERE id=?"
            );
            ps.setInt(1, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) { return false; }
    }

    @Override
    public double getTotalLiquidity(int userId) {
        return sumByFilter(userId, null, false);
    }

    @Override
    public double getLiquidityByType(int userId, String type) {
        return sumByFilter(userId, type, false);
    }

    @Override
    public double getLiquidityExcludingType(int userId, String excludeType) {
        return sumByFilter(userId, excludeType, true);
    }

    private double sumByFilter(int userId, String type, boolean exclude) {
        try {
            String sql = "SELECT COALESCE(SUM(balance),0) FROM payment_methods WHERE user_id=?";
            if (type != null) sql += exclude ? " AND type!=?" : " AND type=?";
            PreparedStatement ps = DBConnection.getInstance().getConnection().prepareStatement(sql);
            ps.setInt(1, userId);
            if (type != null) ps.setString(2, type);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getDouble(1) : 0;
        } catch (SQLException e) { return 0; }
    }

    private PaymentMethod map(ResultSet rs) throws SQLException {
        PaymentMethod pm = new PaymentMethod();
        pm.setId(rs.getInt("id"));
        pm.setUserId(rs.getInt("user_id"));
        pm.setName(rs.getString("name"));
        pm.setType(rs.getString("type"));
        pm.setBalance(BigDecimal.valueOf(rs.getDouble("balance")));
        pm.setDescription(rs.getString("description"));
        return pm;
    }
}
