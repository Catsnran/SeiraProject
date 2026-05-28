package com.seira.dao;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * Bertanggung jawab membuat tabel dan mengisi data default saat aplikasi pertama kali dijalankan.
 * Akses data tidak lagi dilakukan di sini — gunakan DAOFactory.
 */
public class DatabaseManager {

    private static DatabaseManager instance;

    private DatabaseManager() {}

    public static DatabaseManager getInstance() {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }

    public void initialize() {
        try {
            createTables();
            seedDefaultCategories();
        } catch (SQLException e) {
            throw new RuntimeException("Gagal inisialisasi database", e);
        }
    }

    private void createTables() throws SQLException {
        String[] sqls = {
                """
            CREATE TABLE IF NOT EXISTS users (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                username      TEXT NOT NULL,
                email         TEXT UNIQUE NOT NULL,
                password_hash TEXT NOT NULL,
                currency      TEXT DEFAULT 'IDR',
                created_at    TEXT DEFAULT (datetime('now'))
            )
            """,
                """
            CREATE TABLE IF NOT EXISTS categories (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id    INTEGER,
                name       TEXT NOT NULL,
                type       TEXT NOT NULL,
                color      TEXT DEFAULT '#C87941',
                icon       TEXT DEFAULT '📌',
                is_default INTEGER DEFAULT 0,
                FOREIGN KEY (user_id) REFERENCES users(id)
            )
            """,
                """
            CREATE TABLE IF NOT EXISTS payment_methods (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id     INTEGER NOT NULL,
                name        TEXT NOT NULL,
                type        TEXT DEFAULT 'CASH',
                balance     REAL DEFAULT 0,
                description TEXT,
                FOREIGN KEY (user_id) REFERENCES users(id)
            )
            """,
                """
            CREATE TABLE IF NOT EXISTS transactions (
                id                INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id           INTEGER NOT NULL,
                description       TEXT NOT NULL,
                amount            REAL NOT NULL,
                type              TEXT NOT NULL,
                date              TEXT NOT NULL,
                category_id       INTEGER,
                payment_method_id INTEGER,
                reference         TEXT,
                notes             TEXT,
                created_at        TEXT DEFAULT (datetime('now')),
                FOREIGN KEY (user_id)           REFERENCES users(id),
                FOREIGN KEY (category_id)       REFERENCES categories(id),
                FOREIGN KEY (payment_method_id) REFERENCES payment_methods(id)
            )
            """,
                """
            CREATE TABLE IF NOT EXISTS budgets (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id     INTEGER NOT NULL,
                category_id INTEGER NOT NULL,
                amount      REAL NOT NULL,
                period      TEXT NOT NULL,
                FOREIGN KEY (user_id)     REFERENCES users(id),
                FOREIGN KEY (category_id) REFERENCES categories(id)
            )
            """,
                """
            CREATE TABLE IF NOT EXISTS stock_assets (
                id           INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id      INTEGER NOT NULL,
                stock_symbol TEXT NOT NULL,
                stock_name   TEXT NOT NULL,
                total_lot    INTEGER NOT NULL,
                created_at   TEXT DEFAULT (datetime('now')),
                FOREIGN KEY (user_id) REFERENCES users(id)
            )
            """
        };
        Statement stmt = DBConnection.getInstance().getConnection().createStatement();
        for (String sql : sqls) stmt.execute(sql);
        stmt.close();
    }

    private void seedDefaultCategories() throws SQLException {
        var rs = DBConnection.getInstance().getConnection()
                .createStatement()
                .executeQuery("SELECT COUNT(*) FROM categories WHERE is_default=1");
        if (rs.next() && rs.getInt(1) > 0) return;

        String[][] defaults = {
                {"Dining",        "EXPENSE", "#E07B54", "🍽"},
                {"Transport",     "EXPENSE", "#C87941", "🚗"},
                {"Entertainment", "EXPENSE", "#4A90D9", "🎬"},
                {"Personal Care", "EXPENSE", "#D4A843", "💆"},
                {"Housing",       "EXPENSE", "#8B4513", "🏠"},
                {"Shopping",      "EXPENSE", "#9B59B6", "🛍"},
                {"Healthcare",    "EXPENSE", "#E74C3C", "💊"},
                {"Education",     "EXPENSE", "#3498DB", "📚"},
                {"Investing",     "EXPENSE", "#27AE60", "📈"},
                {"Other",         "EXPENSE", "#95A5A6", "📌"},
                {"Salary",        "INCOME",  "#27AE60", "💼"},
                {"Freelance",     "INCOME",  "#2ECC71", "💻"},
                {"Dividend",      "INCOME",  "#1ABC9C", "💰"},
                {"Business",      "INCOME",  "#16A085", "🏢"},
                {"Other Income",  "INCOME",  "#7F8C8D", "➕"}
        };

        var ps = DBConnection.getInstance().getConnection().prepareStatement(
                "INSERT INTO categories (name, type, color, icon, is_default) VALUES (?,?,?,?,1)"
        );
        for (String[] d : defaults) {
            ps.setString(1, d[0]); ps.setString(2, d[1]);
            ps.setString(3, d[2]); ps.setString(4, d[3]);
            ps.addBatch();
        }
        ps.executeBatch();
        ps.close();
    }
}
