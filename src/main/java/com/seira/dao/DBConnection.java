package com.seira.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Mengelola koneksi tunggal ke database SQLite.
 * Diakses oleh semua DAO implementation.
 */
public class DBConnection {
    private static DBConnection instance;
    private Connection connection;
    private static final String DB_URL = "jdbc:sqlite:seira.db";

    private DBConnection() {}

    public static DBConnection getInstance() {
        if (instance == null) instance = new DBConnection();
        return instance;
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL);
        }
        return connection;
    }
}
