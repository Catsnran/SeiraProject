package com.seira.dao.impl;

import com.seira.dao.DBConnection;
import com.seira.dao.interfaces.IUserDAO;
import com.seira.models.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO implements IUserDAO {

    @Override
    public boolean register(String username, String email, String password) {
        try {
            String hash = BCrypt.hashpw(password, BCrypt.gensalt());
            PreparedStatement ps = DBConnection.getInstance().getConnection().prepareStatement(
                "INSERT INTO users (username, email, password_hash) VALUES (?,?,?)"
            );
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, hash);
            ps.executeUpdate();
            ps.close();
            return true;
        } catch (SQLException e) {
            return false; // duplicate email
        }
    }

    @Override
    public User login(String email, String password) {
        try {
            PreparedStatement ps = DBConnection.getInstance().getConnection().prepareStatement(
                "SELECT * FROM users WHERE email = ?"
            );
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && BCrypt.checkpw(password, rs.getString("password_hash"))) {
                User u = new User();
                u.setId(rs.getInt("id"));
                u.setUsername(rs.getString("username"));
                u.setEmail(rs.getString("email"));
                u.setCurrency(rs.getString("currency"));
                return u;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    @Override
    public boolean emailExists(String email) {
        try {
            PreparedStatement ps = DBConnection.getInstance().getConnection().prepareStatement(
                "SELECT id FROM users WHERE email=?"
            );
            ps.setString(1, email);
            return ps.executeQuery().next();
        } catch (SQLException e) { return false; }
    }
}
