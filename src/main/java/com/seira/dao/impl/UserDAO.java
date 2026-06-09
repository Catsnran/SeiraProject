package com.seira.dao.impl;

import com.seira.dao.DBConnection;
import com.seira.dao.interfaces.IUserDAO;
import com.seira.models.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO implements IUserDAO {
    int passwordLength;
    
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
                "SELECT * FROM users WHERE email = ? or username = ?"
            );
            ps.setString(1, email);
            ps.setString(2, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && BCrypt.checkpw(password, rs.getString("password_hash"))) {
                passwordLength = password.length();
                return mapUser(rs);
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

    @Override
    public User findByEmail(String email) {
        try {
            PreparedStatement ps = DBConnection.getInstance().getConnection().prepareStatement(
                "SELECT * FROM users WHERE email = ?"
            );
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapUser(rs);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    @Override
    public int updateProfile(int userId, String username, String email, String newPassword, String profilePhoto, String currency) {
        try {
            StringBuilder sql = new StringBuilder("UPDATE users SET username=?, email=?, profile_photo=?, currency=?");
            if (newPassword != null && !newPassword.isEmpty()) {
                sql.append(", password_hash=?");
            }
            sql.append(" WHERE id=?");

            PreparedStatement ps = DBConnection.getInstance().getConnection().prepareStatement(sql.toString());
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, profilePhoto);
            ps.setString(4, currency);

            int idx = 5;
            if (newPassword != null && !newPassword.isEmpty()) {
                ps.setString(idx++, BCrypt.hashpw(newPassword, BCrypt.gensalt()));
                this.passwordLength = newPassword.length();
            }
            ps.setInt(idx, userId);

            int rows = ps.executeUpdate();
            ps.close();
            return rows > 0 ? 1 : 0; // 1 = berhasil, 0 = tidak ada perubahan
        } catch (SQLException e) {
            e.printStackTrace();
            return -1; // gagal
        }
    }

    @Override
    public User findById(int userId) {
        try {
            PreparedStatement ps = DBConnection.getInstance().getConnection().prepareStatement(
                "SELECT * FROM users WHERE id = ?"
            );
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapUser(rs);
            }
            
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setEmail(rs.getString("email"));
        u.setPasswordLength(passwordLength);
        u.setPasswordHash(rs.getString("password_hash"));
        u.setCurrency(rs.getString("currency"));
        try {
            u.setProfilePhoto(rs.getString("profile_photo"));
        } catch (SQLException ignored) {
            // kolom belum ada di database lama
        }
        return u;
    }
}
