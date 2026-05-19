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

    @Override
    public List<Category> findAll(int userId, String type) {
        List<Category> list = new ArrayList<>();
        try {
            String sql = "SELECT * FROM categories WHERE (user_id=? OR is_default=1)";
            if (type != null) sql += " AND type=?";
            sql += " ORDER BY name";
            PreparedStatement ps = DBConnection.getInstance().getConnection().prepareStatement(sql);
            ps.setInt(1, userId);
            if (type != null) ps.setString(2, type);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Category c = new Category();
                c.setId(rs.getInt("id"));
                c.setName(rs.getString("name"));
                c.setType(rs.getString("type"));
                c.setColor(rs.getString("color"));
                c.setIcon(rs.getString("icon"));
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
