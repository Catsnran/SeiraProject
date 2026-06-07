package com.seira.dao.impl;

import com.seira.dao.DBConnection;
import com.seira.dao.interfaces.ITransactionDAO;
import com.seira.models.PaymentMethod;
import com.seira.models.Transaction;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class TransactionDAO implements ITransactionDAO {

    private final PaymentMethodDAO pmDAO = new PaymentMethodDAO();

    @Override
    public boolean add(Transaction t) {
        try {
            PreparedStatement ps = DBConnection.getInstance().getConnection().prepareStatement(
                "INSERT INTO transactions (user_id, description, amount, type, date, " +
                "category_id, payment_method_id, reference, notes) VALUES (?,?,?,?,?,?,?,?,?)"
            );
            ps.setInt(1, t.getUserId());
            ps.setString(2, t.getDescription());
            ps.setDouble(3, t.getAmount().doubleValue());
            ps.setString(4, t.getType());
            ps.setString(5, t.getDate().toString());
            ps.setInt(6, t.getCategoryId());
            ps.setInt(7, t.getPaymentMethodId());
            ps.setString(8, t.getReference());
            ps.setString(9, t.getNotes());
            ps.executeUpdate();

            // Update saldo akun
            PaymentMethod pm = pmDAO.findById(t.getPaymentMethodId());
            if (pm != null) {
                BigDecimal newBal = t.isExpense()
                    ? pm.getBalance().subtract(t.getAmount())
                    : pm.getBalance().add(t.getAmount());
                pmDAO.updateBalance(pm.getId(), newBal);
            }
            return true;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    @Override
    public boolean update(Transaction t) {
        try {
            // Reverse saldo lama
            Transaction old = findById(t.getId());
            if (old != null) {
                PaymentMethod pm = pmDAO.findById(old.getPaymentMethodId());
                if (pm != null) {
                    BigDecimal reversed = old.isExpense()
                        ? pm.getBalance().add(old.getAmount())
                        : pm.getBalance().subtract(old.getAmount());
                    pmDAO.updateBalance(pm.getId(), reversed);
                }
            }

            PreparedStatement ps = DBConnection.getInstance().getConnection().prepareStatement(
                "UPDATE transactions SET description=?, amount=?, type=?, date=?, " +
                "category_id=?, payment_method_id=?, reference=?, notes=? WHERE id=?"
            );
            ps.setString(1, t.getDescription());
            ps.setDouble(2, t.getAmount().doubleValue());
            ps.setString(3, t.getType());
            ps.setString(4, t.getDate().toString());
            ps.setInt(5, t.getCategoryId());
            ps.setInt(6, t.getPaymentMethodId());
            ps.setString(7, t.getReference());
            ps.setString(8, t.getNotes());
            ps.setInt(9, t.getId());
            ps.executeUpdate();

            // Apply saldo baru
            PaymentMethod pm = pmDAO.findById(t.getPaymentMethodId());
            if (pm != null) {
                BigDecimal newBal = t.isExpense()
                    ? pm.getBalance().subtract(t.getAmount())
                    : pm.getBalance().add(t.getAmount());
                pmDAO.updateBalance(pm.getId(), newBal);
            }
            return true;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    @Override
    public boolean delete(int id) {
        try {
            // Reverse saldo
            Transaction t = findById(id);
            if (t != null) {
                PaymentMethod pm = pmDAO.findById(t.getPaymentMethodId());
                if (pm != null) {
                    BigDecimal reversed = t.isExpense()
                        ? pm.getBalance().add(t.getAmount())
                        : pm.getBalance().subtract(t.getAmount());
                    pmDAO.updateBalance(pm.getId(), reversed);
                }
            }
            PreparedStatement ps = DBConnection.getInstance().getConnection().prepareStatement(
                "DELETE FROM transactions WHERE id=?"
            );
            ps.setInt(1, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) { return false; }
    }

    @Override
    public Transaction findById(int id) {
        try {
            PreparedStatement ps = DBConnection.getInstance().getConnection().prepareStatement(
                "SELECT t.*, c.name as cat_name, pm.name as pm_name FROM transactions t " +
                "LEFT JOIN categories c ON t.category_id=c.id " +
                "LEFT JOIN payment_methods pm ON t.payment_method_id=pm.id WHERE t.id=?"
            );
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    @Override
    public List<Transaction> findAll(int userId, String type, LocalDate from, LocalDate to, String search, List<String> references) {
        List<Transaction> list = new ArrayList<>();
        try {
            StringBuilder sql = new StringBuilder(
                "SELECT t.*, c.name as cat_name, pm.name as pm_name FROM transactions t " +
                "LEFT JOIN categories c ON t.category_id=c.id " +
                "LEFT JOIN payment_methods pm ON t.payment_method_id=pm.id WHERE t.user_id=?"
            );
            List<Object> params = new ArrayList<>();
            params.add(userId);
            if (type != null && !type.isEmpty()) { sql.append(" AND t.type=?"); params.add(type); }
            if (from != null) { sql.append(" AND t.date>=?"); params.add(from.toString()); }
            if (to != null) { sql.append(" AND t.date<=?"); params.add(to.toString()); }
            if (search != null && !search.isEmpty()) {
                sql.append(" AND (t.description LIKE ? OR c.name LIKE ? OR pm.name LIKE ?)");
                params.add("%" + search + "%");
                params.add("%" + search + "%");
                params.add("%" + search + "%");
            }
            if (references != null && !references.isEmpty()) {
                StringJoiner refClause = new StringJoiner(" OR ", "(", ")");
                for (int i = 0; i < references.size(); i++)
                    refClause.add("t.reference==?");
                sql.append(" AND ").append(refClause);
                params.addAll(references);
            }
            sql.append(" ORDER BY t.date DESC, t.id DESC");

            PreparedStatement ps = DBConnection.getInstance().getConnection().prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Integer) ps.setInt(i + 1, (Integer) p);
                else ps.setString(i + 1, (String) p);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    private Transaction map(ResultSet rs) throws SQLException {
        Transaction t = new Transaction();
        t.setId(rs.getInt("id"));
        t.setUserId(rs.getInt("user_id"));
        t.setDescription(rs.getString("description"));
        t.setAmount(BigDecimal.valueOf(rs.getDouble("amount")));
        t.setType(rs.getString("type"));
        t.setDate(LocalDate.parse(rs.getString("date")));
        t.setCategoryId(rs.getInt("category_id"));
        t.setCategoryName(rs.getString("cat_name"));
        t.setPaymentMethodId(rs.getInt("payment_method_id"));
        t.setPaymentMethodName(rs.getString("pm_name"));
        t.setReference(rs.getString("reference"));
        t.setNotes(rs.getString("notes"));
        return t;
    }
}
