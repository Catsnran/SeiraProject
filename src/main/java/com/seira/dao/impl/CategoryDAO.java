package com.seira.dao.impl;

import com.seira.dao.DBConnection;
import com.seira.dao.interfaces.ICategoryDAO;
import com.seira.models.Category;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CategoryDAO implements ICategoryDAO {

    private Category mapRow(ResultSet rs) throws SQLException {
        Category c = new Category();
        c.setId(rs.getInt("id"));
        c.setUserId(rs.getInt("user_id"));
        c.setName(rs.getString("name"));
        c.setType(rs.getString("type"));
        c.setColor(rs.getString("color"));
        c.setIcon(rs.getString("icon"));
        return c;
    }

    @Override
    public List<Category> findAll(int userId, String type) {
        List<Category> list = new ArrayList<>();
        try {
            String sql = "SELECT * FROM categories WHERE (user_id=? OR is_default=1)";
            if (type != null) sql += " AND type=?";
            sql += " ORDER BY is_default DESC, name";
            PreparedStatement ps = DBConnection.getInstance().getConnection().prepareStatement(sql);
            ps.setInt(1, userId);
            if (type != null) ps.setString(2, type);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            System.out.println(list.size());
            Category newCategory = new Category();
            newCategory.setId(list.size() + 1);
            newCategory.setName("Lainnya");
            newCategory.setType(type);
            newCategory.setColor("#808080");
            newCategory.setIcon("...");
            list.add(newCategory);
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    @Override
    public List<Category> findAllByUser(int userId) {
        List<Category> list = new ArrayList<>();
        try {
            String sql = "SELECT * FROM categories WHERE user_id=? OR is_default=1 ORDER BY is_default DESC, type, name";
            PreparedStatement ps = DBConnection.getInstance().getConnection().prepareStatement(sql);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Category c = mapRow(rs);
                // Mark default categories (user_id will be 0 for is_default=1)
                c.setUserId(rs.getInt("user_id"));
                list.add(c);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    @Override
    public boolean add(Category c) {
        try {
            PreparedStatement ps = DBConnection.getInstance().getConnection().prepareStatement(
                    "INSERT INTO categories (user_id, name, type, color, icon) VALUES (?,?,?,?,?)"
            );
            ps.setInt(1, c.getUserId());
            ps.setString(2, c.getName());
            ps.setString(3, c.getType());
            ps.setString(4, c.getColor());
            ps.setString(5, c.getIcon());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) { return false; }
    }

    @Override
    public boolean update(Category c) {
        try {
            PreparedStatement ps = DBConnection.getInstance().getConnection().prepareStatement(
                    "UPDATE categories SET name=?, type=?, color=?, icon=? WHERE id=? AND is_default=0"
            );
            ps.setString(1, c.getName());
            ps.setString(2, c.getType());
            ps.setString(3, c.getColor());
            ps.setString(4, c.getIcon());
            ps.setInt(5, c.getId());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) { return false; }
    }

    @Override
    public boolean delete(int id) {
        try {
            PreparedStatement ps = DBConnection.getInstance().getConnection().prepareStatement(
                    "DELETE FROM categories WHERE id=? AND is_default=0"
            );
            ps.setInt(1, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) { return false; }
    }
}