package com.seira.models;
public class User {
    private int id;
    private String username;
    private String email;
    // BCrypt hash //
    private String passwordHash;
    // def idr //
    private String currency;
    // foto profil //
    private String profilePhoto;
    private int passwordLength;

    public User() {}

    public User(int id, String username, String email, int passwordLength, String passwordHash, String currency) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.passwordLength = passwordLength;
        this.currency = currency;
    }

    public User(int id, String username, String email, int passwordLength, String passwordHash, String currency, String profilePhoto) {
        this(id, username, email, passwordLength, passwordHash, currency);
        this.profilePhoto = profilePhoto;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public void setPasswordLength(int passwordLength) { this.passwordLength = passwordLength; }
    public int getPasswordLength() { return passwordLength; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    /** @return kode mata uang, fallback {@code "IDR"} jika null. */
    public String getCurrency() { return currency != null ? currency : "IDR"; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getProfilePhoto() { return profilePhoto; }
    public void setProfilePhoto(String profilePhoto) { this.profilePhoto = profilePhoto; }
}
