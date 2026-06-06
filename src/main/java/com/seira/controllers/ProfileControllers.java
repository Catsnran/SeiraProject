package com.seira.controllers;

import com.seira.dao.DAOFactory;
import com.seira.models.User;
import com.seira.utils.SessionManager;
import com.seira.utils.Toast;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class ProfileControllers {

    @FXML private Label avatarInitial;
    @FXML private ImageView profileImageView;
    @FXML private StackPane photoContainer;
    @FXML private Button btnChangePhoto;
    @FXML private Label photoHint;

    @FXML private Label usernameDisplay;
    @FXML private TextField usernameField;

    @FXML private Label emailDisplay;
    @FXML private TextField emailField;

    @FXML private Label passwordDisplay;
    @FXML private PasswordField passwordField;
    @FXML private Label passwordHint;

    @FXML private Label statusLabel;
    @FXML private Button btnEditProfile;
    @FXML private Button btnCancel;
    @FXML private Button btnBack;

    private MainControllers mainController;
    private boolean editMode = false;

    /** Path foto profil yang dipilih saat edit (null = tidak diubah). */
    private String selectedPhotoPath;

    @FXML
    public void initialize() {
        User user = SessionManager.getCurrentUser();

        // Clip foto profil menjadi lingkaran
        Circle clip = new Circle(50, 50, 50);
        profileImageView.setClip(clip);

        loadProfileData(user);
    }

    /** Memuat data profil ke tampilan read-only. */
    private void loadProfileData(User user) {
        usernameDisplay.setText(user.getUsername());
        emailDisplay.setText(user.getEmail());
        int len = user.getPasswordLength();
        if (len <= 0) len = 8; // fallback default
        passwordDisplay.setText("•".repeat(len));

        // Avatar initial
        String initial = user.getUsername().substring(0, 1).toUpperCase();
        avatarInitial.setText(initial);

        // Load foto profil jika ada
        String photo = user.getProfilePhoto();
        if (photo != null && !photo.isEmpty()) {
            File photoFile = new File(photo);
            if (photoFile.exists()) {
                try (var is = Files.newInputStream(photoFile.toPath())) {
                    profileImageView.setImage(new Image(is));
                    profileImageView.setVisible(true);
                    avatarInitial.setVisible(false);
                } catch (IOException e) {
                    e.printStackTrace();
                    profileImageView.setVisible(false);
                    avatarInitial.setVisible(true);
                }
            } else {
                profileImageView.setVisible(false);
                avatarInitial.setVisible(true);
            }
        } else {
            profileImageView.setVisible(false);
            avatarInitial.setVisible(true);
        }

        selectedPhotoPath = photo;
    }

    /** Toggle antara mode read-only dan mode edit. */
    @FXML
    private void toggleEditMode() {
        if (!editMode) {
            // Masuk ke mode edit
            enterEditMode();
        } else {
            // Submit perubahan
            submitChanges();
        }
    }

    private void enterEditMode() {
        editMode = true;
        User user = SessionManager.getCurrentUser();

        // Isi field dengan data saat ini
        usernameField.setText(user.getUsername());
        emailField.setText(user.getEmail());
        passwordField.setText("");

        // Sembunyikan display, tampilkan field
        usernameDisplay.setVisible(false); usernameDisplay.setManaged(false);
        emailDisplay.setVisible(false);    emailDisplay.setManaged(false);
        passwordDisplay.setVisible(false); passwordDisplay.setManaged(false);

        usernameField.setVisible(true);  usernameField.setManaged(true);
        emailField.setVisible(true);     emailField.setManaged(true);
        passwordField.setVisible(true);  passwordField.setManaged(true);
        passwordHint.setVisible(true);   passwordHint.setManaged(true);

        // Tampilkan tombol ubah foto dan cancel
        btnChangePhoto.setVisible(true);  btnChangePhoto.setManaged(true);
        photoHint.setText("Klik untuk mengganti foto profil");
        photoHint.setVisible(true);       photoHint.setManaged(true);
        btnCancel.setVisible(true);       btnCancel.setManaged(true);

        // Ubah teks tombol
        btnEditProfile.setText("Simpan Perubahan");
        btnEditProfile.getStyleClass().removeAll("btn-primary");
        btnEditProfile.getStyleClass().add("btn-primary-full");

        // Sembunyikan status
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
    }

    @FXML
    private void cancelEdit() {
        exitEditMode();
        loadProfileData(SessionManager.getCurrentUser());
    }

    private void exitEditMode() {
        editMode = false;

        // Kembalikan display, sembunyikan field
        usernameDisplay.setVisible(true);  usernameDisplay.setManaged(true);
        emailDisplay.setVisible(true);     emailDisplay.setManaged(true);
        passwordDisplay.setVisible(true);  passwordDisplay.setManaged(true);

        usernameField.setVisible(false);  usernameField.setManaged(false);
        emailField.setVisible(false);     emailField.setManaged(false);
        passwordField.setVisible(false);  passwordField.setManaged(false);
        passwordHint.setVisible(false);   passwordHint.setManaged(false);

        // Sembunyikan tombol ubah foto dan cancel
        btnChangePhoto.setVisible(false); btnChangePhoto.setManaged(false);
        photoHint.setVisible(false);      photoHint.setManaged(false);
        btnCancel.setVisible(false);      btnCancel.setManaged(false);

        // Kembalikan teks tombol
        btnEditProfile.setText("Edit Profil");
        btnEditProfile.getStyleClass().removeAll("btn-primary-full");
        btnEditProfile.getStyleClass().add("btn-primary");
    }

    /** Pilih foto profil dari FileChooser. */
    @FXML
    private void changePhoto() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Pilih Foto Profil");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Gambar", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );

        File file = fc.showOpenDialog(photoContainer.getScene().getWindow());
        if (file != null) {
            // Salin ke folder data/profiles
            try {
                Path profileDir = Path.of("data", "profiles");
                Files.createDirectories(profileDir);
                String ext = file.getName().substring(file.getName().lastIndexOf('.'));
                Path dest = profileDir.resolve(SessionManager.getCurrentUser().getId() + ext);
                Files.copy(file.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);

                selectedPhotoPath = dest.toAbsolutePath().toString();

                // Tampilkan preview tanpa caching
                try (var is = Files.newInputStream(dest)) {
                    profileImageView.setImage(new Image(is));
                    profileImageView.setVisible(true);
                    avatarInitial.setVisible(false);
                }
                photoHint.setText("Foto dipilih: " + file.getName());
            } catch (IOException e) {
                e.printStackTrace();
                photoHint.setText("Gagal menyalin foto.");
            }
        }
    }

    /** Kirim perubahan ke database. */
    private void submitChanges() {
        String newUsername = usernameField.getText().trim();
        String newEmail = emailField.getText().trim();
        String newPassword = passwordField.getText(); // biarkan kosong jika tidak ubah

        // Validasi sederhana
        if (newUsername.isEmpty()) {
            showStatus("Username tidak boleh kosong.", "error");
            return;
        }
        if (newUsername.length() < 4) {
            showStatus("Username minimal harus 4 karakter.", "error");
            return;
        }
        if (!newEmail.contains("@") || !newEmail.matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$")) {
            showStatus("Format email tidak valid (harus mengandung '@' dan domain).", "error");
            return;
        }
        if (newPassword != null && !newPassword.isEmpty()) {
            if (newPassword.length() < 8) {
                showStatus("Password baru minimal harus 8 karakter.", "error");
                return;
            }
            if (!newPassword.matches("(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).+")) {
                showStatus("Password baru harus mengandung setidaknya satu huruf besar, satu huruf kecil, dan satu angka.", "error");
                return;
            }
        }

        User current = SessionManager.getCurrentUser();

        // Cek apakah email berubah dan sudah digunakan user lain
        if (!newEmail.equalsIgnoreCase(current.getEmail())) {
            if (DAOFactory.getUserDAO().emailExists(newEmail)) {
                showStatus("Email sudah digunakan pengguna lain.", "error");
                return;
            }
        }

        // Cek apakah ada perubahan
        boolean usernameChanged = !newUsername.equals(current.getUsername());
        boolean emailChanged = !newEmail.equalsIgnoreCase(current.getEmail());
        boolean passwordChanged = newPassword != null && !newPassword.isEmpty();
        boolean photoChanged = (selectedPhotoPath == null && current.getProfilePhoto() != null)
                || (selectedPhotoPath != null && !selectedPhotoPath.equals(current.getProfilePhoto()));

        if (!usernameChanged && !emailChanged && !passwordChanged && !photoChanged) {
            // Tidak ada data diperbaharui
            showStatus("Tidak ada data diperbaharui.", "info");
            Toast.show("Tidak ada data diperbaharui");
            exitEditMode();
            loadProfileData(current);
            return;
        }

        // Lakukan update
        int result = DAOFactory.getUserDAO().updateProfile(
                current.getId(),
                newUsername,
                newEmail,
                passwordChanged ? newPassword : null,
                selectedPhotoPath
        );

        if (result == 1) {
            // Data berhasil di update — refresh session dan kembali ke dashboard
            User refreshed = DAOFactory.getUserDAO().findById(current.getId());
            if (refreshed != null) {
                SessionManager.setCurrentUser(refreshed);
            }

            Toast.showSuccess("Data berhasil di update ✓");

            // Kembali ke dashboard dan update navbar state
            if (mainController != null) {
                mainController.refreshUserState();
                mainController.loadPage("dashboard");
            }
        } else if (result == 0) {
            // Tidak ada data diperbaharui (server level)
            showStatus("Tidak ada data diperbaharui.", "info");
            Toast.show("Tidak ada data diperbaharui");
            exitEditMode();
            loadProfileData(current);
        } else {
            // Data gagal di update
            showStatus("Data gagal di update. Silakan coba lagi.", "error");
            Toast.showError("Data gagal di update");
        }
    }

    private void showStatus(String message, String type) {
        statusLabel.setText(message);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
        statusLabel.getStyleClass().removeAll("error-label", "success-label", "info-label");
        switch (type) {
            case "error"   -> statusLabel.getStyleClass().add("error-label");
            case "success" -> statusLabel.getStyleClass().add("success-label");
            default        -> statusLabel.getStyleClass().add("info-label");
        }
    }

    @FXML
    private void goBackToDashboard() {
        if (mainController != null) {
            mainController.loadPage("dashboard");
        }
    }

    public void setMainController(MainControllers mc) {
        this.mainController = mc;
    }
}
