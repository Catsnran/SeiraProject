package com.seira.models;

/**
 * Model data pengguna aplikasi Seira.
 * Merepresentasikan satu baris pada tabel {@code users} di database.
 */
public class User {
    private int id;
    private String username;
    private String email;
    /** BCrypt hash dari password asli — tidak pernah disimpan plaintext. */
    private String passwordHash;
    /** Kode mata uang, default {@code IDR}. */
    private String currency;

    public User() {}

    public User(int id, String username, String email, String passwordHash, String currency) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.currency = currency;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    /** @return kode mata uang, fallback {@code "IDR"} jika null. */
    public String getCurrency() { return currency != null ? currency : "IDR"; }
    public void setCurrency(String currency) { this.currency = currency; }
}
