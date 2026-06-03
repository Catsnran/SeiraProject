package com.seira.controllers;

import com.seira.dao.DAOFactory;
import com.seira.models.Category;
import com.seira.utils.SessionManager;
import com.seira.utils.StyledDialog;
import com.seira.utils.Toast;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.List;
import java.util.function.Consumer;

public class CategoryManagerDialog {

    private static final String[] ICON_OPTIONS = {
            "🍽", "🍔", "☕", "🛒", "🚗", "🚌", "✈️", "🎬", "🎮", "🎵",
            "🏠", "💊", "📚", "💆", "🛍", "💻", "💼", "💰", "📈", "🏢",
            "⚡", "💧", "📱", "🎁", "🏋️", "🐾", "🌿", "🔧", "🎨", "➕",
            "🍕", "🍜", "🥗", "🥤", "🎓", "🏥", "🏦", "🎪", "⛽", "🧹"
    };

    private final int userId;
    private Category resultCategory = null;
    private Consumer<Category> onCategoryAdded;

    public CategoryManagerDialog(int userId) {
        this.userId = userId;
    }

    public void setOnCategoryAdded(Consumer<Category> callback) {
        this.onCategoryAdded = callback;
    }

    public void showAddDialog(String defaultType) {
        buildFormDialog(null, defaultType);
    }

    public void showManageDialog() {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.initOwner(SessionManager.getPrimaryStage());

        VBox root = buildRootContainer();
        root.setPrefWidth(520);

        HBox header = buildHeader("📂 Kelola Kategori", "Edit atau hapus kategori buatanmu");
        enableDrag(header, stage);

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
        scroll.setPrefHeight(360);

        VBox listBox = new VBox(8);
        listBox.setPadding(new Insets(16, 20, 16, 20));
        listBox.setStyle("-fx-background-color: transparent;");
        scroll.setContent(listBox);

        refreshManageList(listBox, stage);

        HBox footer = new HBox(12);
        footer.setPadding(new Insets(16, 20, 20, 20));
        footer.setAlignment(Pos.CENTER_RIGHT);

        Button addNewBtn = new Button("＋ Tambah Kategori Baru");
        addNewBtn.setStyle(
                "-fx-background-color: #C87941; -fx-text-fill: white; -fx-font-size: 13; " +
                        "-fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;"
        );
        addNewBtn.setOnAction(e -> {
            // Buka form DI ATAS dialog ini — jangan close dulu, cegah crash owner hilang
            buildFormDialogWithOwner(stage, null, "EXPENSE", () -> refreshManageList(listBox, stage));
        });

        Button closeBtn2 = new Button("Tutup");
        closeBtn2.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #8B7355; -fx-font-size: 13; " +
                        "-fx-padding: 10 20; -fx-background-radius: 8; -fx-border-color: #E8DDD0; " +
                        "-fx-border-width: 1; -fx-border-radius: 8; -fx-cursor: hand;"
        );
        closeBtn2.setOnAction(e -> stage.close());

        footer.getChildren().addAll(closeBtn2, addNewBtn);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #E8DDD0;");

        root.getChildren().addAll(header, scroll, sep, footer);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.sizeToScene();
        centerOnOwner(stage);
        stage.showAndWait();
    }

    private void buildFormDialog(Category editTarget, String defaultType) {
        buildFormDialogWithOwner(SessionManager.getPrimaryStage(), editTarget, defaultType, null);
    }

    private void buildFormDialogWithOwner(Stage ownerStage, Category editTarget, String defaultType, Runnable onDone) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.initOwner(ownerStage != null ? ownerStage : SessionManager.getPrimaryStage());

        boolean isEdit = editTarget != null;

        VBox root = buildRootContainer();
        root.setPrefWidth(480);

        String headerTitle = isEdit ? "✏️ Edit Kategori" : "＋ Kategori Baru";
        String headerSub   = isEdit ? "Ubah detail kategori kustom" : "Tambahkan kategori ke daftarmu";
        HBox header = buildHeader(headerTitle, headerSub);
        enableDrag(header, stage);

        VBox body = new VBox(16);
        body.setPadding(new Insets(24, 24, 16, 24));

        HBox previewRow = new HBox(14);
        previewRow.setAlignment(Pos.CENTER_LEFT);
        previewRow.setPadding(new Insets(12, 16, 12, 16));
        previewRow.setStyle("-fx-background-color: #FDF0E4; -fx-background-radius: 10;");

        Label previewIcon = new Label(isEdit && editTarget.getIcon() != null ? editTarget.getIcon() : "📌");
        previewIcon.setStyle("-fx-font-size: 28;");
        VBox previewText = new VBox(2);
        Label previewName = new Label(isEdit ? editTarget.getName() : "Nama Kategori");
        previewName.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #1A0F05;");
        Label previewType = new Label(isEdit ? typeLabelOf(editTarget.getType()) : "Tipe belum dipilih");
        previewType.setStyle("-fx-font-size: 11; -fx-text-fill: #8B7355;");
        previewText.getChildren().addAll(previewName, previewType);
        previewRow.getChildren().addAll(previewIcon, previewText);

        TextField nameField = StyledDialog.field("cth: Makan Siang, Gaji, Spotify...");
        if (isEdit) nameField.setText(editTarget.getName());
        nameField.textProperty().addListener((obs, o, n) ->
                previewName.setText(n.isEmpty() ? "Nama Kategori" : n));

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("EXPENSE", "INCOME");
        typeCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Pilih tipe..." : typeLabelOf(item));
            }
        });
        typeCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                setText(typeLabelOf(item));
                setStyle(item.equals("INCOME")
                        ? "-fx-text-fill: #27AE60; -fx-font-weight: bold;"
                        : "-fx-text-fill: #C0392B; -fx-font-weight: bold;");
            }
        });
        if (isEdit) typeCombo.setValue(editTarget.getType());
        else typeCombo.setValue(defaultType != null ? defaultType : "EXPENSE");

        typeCombo.setMaxWidth(Double.MAX_VALUE);
        typeCombo.setStyle(
                "-fx-background-color: #EDE7DC; -fx-border-color: transparent; " +
                        "-fx-border-radius: 8; -fx-background-radius: 8; -fx-pref-height: 42;"
        );
        typeCombo.valueProperty().addListener((obs, o, n) ->
                previewType.setText(n == null ? "Tipe belum dipilih" : typeLabelOf(n)));

        Label iconPickerLabel = StyledDialog.fieldLabel("PILIH IKON");
        ScrollPane iconScroll = new ScrollPane();
        iconScroll.setFitToWidth(true);
        iconScroll.setPrefHeight(130);
        iconScroll.setStyle("-fx-background: #EDE7DC; -fx-background-color: #EDE7DC; " +
                "-fx-border-color: transparent; -fx-background-radius: 8; -fx-border-radius: 8;");

        String[] selectedIcon = { isEdit && editTarget.getIcon() != null ? editTarget.getIcon() : "📌" };

        FlowPane iconGrid = new FlowPane(8, 8);
        iconGrid.setPadding(new Insets(10));
        iconGrid.setStyle("-fx-background-color: transparent;");

        for (String emoji : ICON_OPTIONS) {
            Button iconBtn = new Button(emoji);
            iconBtn.setPrefSize(44, 44);
            boolean isSelected = emoji.equals(selectedIcon[0]);
            iconBtn.setStyle(buildIconBtnStyle(isSelected));
            iconBtn.setOnAction(e -> {
                selectedIcon[0] = emoji;
                previewIcon.setText(emoji);
                iconGrid.getChildren().forEach(node -> {
                    if (node instanceof Button b) b.setStyle(buildIconBtnStyle(b.getText().equals(emoji)));
                });
            });
            iconGrid.getChildren().add(iconBtn);
        }
        iconScroll.setContent(iconGrid);

        Label errLbl = StyledDialog.errorLabel();

        body.getChildren().addAll(
                previewRow,
                StyledDialog.fieldGroup("NAMA KATEGORI", nameField),
                StyledDialog.fieldGroup("TIPE KATEGORI", typeCombo),
                new VBox(6, iconPickerLabel, iconScroll),
                errLbl
        );

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #E8DDD0;");

        HBox footer = new HBox(12);
        footer.setPadding(new Insets(16, 24, 24, 24));
        footer.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = buildCancelBtn("Batal");
        cancelBtn.setOnAction(e -> stage.close());

        String confirmLabel = isEdit ? "Simpan Perubahan" : "Buat Kategori";
        Button confirmBtn = buildConfirmBtn(confirmLabel);
        confirmBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            String type = typeCombo.getValue();

            if (name.isEmpty()) { StyledDialog.showError(errLbl, "Nama kategori tidak boleh kosong."); return; }
            if (name.length() > 40) { StyledDialog.showError(errLbl, "Nama terlalu panjang (maks 40 karakter)."); return; }
            if (type == null) { StyledDialog.showError(errLbl, "Pilih tipe kategori."); return; }

            if (isEdit) {
                editTarget.setName(name);
                editTarget.setType(type);
                editTarget.setIcon(selectedIcon[0]);
                boolean ok = DAOFactory.getCategoryDAO().update(editTarget);
                if (ok) {
                    Toast.showSuccess("Kategori berhasil diperbarui ✓");
                    stage.close();
                    if (onDone != null) onDone.run();
                    if (onCategoryAdded != null) onCategoryAdded.accept(editTarget);
                } else {
                    StyledDialog.showError(errLbl, "Gagal memperbarui. Kategori default tidak bisa diedit.");
                }
            } else {
                Category newCat = new Category();
                newCat.setUserId(userId);
                newCat.setName(name);
                newCat.setType(type);
                newCat.setColor(typeColorOf(type));
                newCat.setIcon(selectedIcon[0]);

                boolean ok = DAOFactory.getCategoryDAO().add(newCat);
                if (ok) {
                    List<Category> allCats = DAOFactory.getCategoryDAO().findAllByUser(userId);
                    for (Category c : allCats) {
                        if (c.getName().equals(name) && c.getUserId() == userId) {
                            newCat.setId(c.getId());
                            break;
                        }
                    }
                    Toast.showSuccess("Kategori \"" + name + "\" berhasil dibuat ✓");
                    resultCategory = newCat;
                    stage.close();
                    if (onDone != null) onDone.run();
                    if (onCategoryAdded != null) onCategoryAdded.accept(newCat);
                } else {
                    StyledDialog.showError(errLbl, "Gagal menyimpan kategori.");
                }
            }
        });

        footer.getChildren().addAll(cancelBtn, confirmBtn);
        root.getChildren().addAll(header, body, sep, footer);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.sizeToScene();
        centerOnOwner(stage);
        stage.showAndWait();
    }

    private void refreshManageList(VBox listBox, Stage parentStage) {
        listBox.getChildren().clear();
        List<Category> all = DAOFactory.getCategoryDAO().findAllByUser(userId);
        List<Category> custom = all.stream().filter(c -> c.getUserId() == userId).toList();

        if (custom.isEmpty()) {
            VBox empty = new VBox(10);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(32));
            Label ico = new Label("📂");
            ico.setStyle("-fx-font-size: 36;");
            Label lbl = new Label("Belum ada kategori kustom.\nKlik '＋ Tambah Kategori Baru' untuk mulai.");
            lbl.setStyle("-fx-font-size: 13; -fx-text-fill: #8B7355; -fx-text-alignment: CENTER;");
            lbl.setWrapText(true);
            lbl.setAlignment(Pos.CENTER);
            empty.getChildren().addAll(ico, lbl);
            listBox.getChildren().add(empty);
            return;
        }

        Label hdrLabel = new Label("KATEGORI KUSTOM (" + custom.size() + ")");
        hdrLabel.setStyle("-fx-font-size: 10; -fx-font-weight: bold; -fx-text-fill: #8B7355; -fx-letter-spacing: 0.8;");
        listBox.getChildren().add(hdrLabel);

        for (Category c : custom) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 14, 10, 14));
            row.setStyle("-fx-background-color: #FDFAF5; -fx-background-radius: 10; " +
                    "-fx-border-color: #E8DDD0; -fx-border-radius: 10; -fx-border-width: 1;");

            Label iconLbl = new Label(c.getIcon() != null ? c.getIcon() : "📌");
            iconLbl.setStyle("-fx-font-size: 22;");

            VBox nameBox = new VBox(2);
            HBox.setHgrow(nameBox, Priority.ALWAYS);
            Label nameLbl = new Label(c.getName());
            nameLbl.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #1A0F05;");
            Label typeLbl = new Label(typeLabelOf(c.getType()));
            String typeColor = "INCOME".equals(c.getType()) ? "#27AE60" : "#C0392B";
            typeLbl.setStyle("-fx-font-size: 10; -fx-text-fill: " + typeColor + "; -fx-font-weight: bold;");
            nameBox.getChildren().addAll(nameLbl, typeLbl);

            Button editBtn = new Button("Edit");
            editBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8B7355; -fx-font-size: 11; " +
                    "-fx-padding: 5 12; -fx-background-radius: 6; -fx-border-color: #DDD5C8; " +
                    "-fx-border-width: 1; -fx-border-radius: 6; -fx-cursor: hand;");
            editBtn.setOnAction(ev -> {
                // Buka form di atas manage dialog, refresh setelah selesai
                buildFormDialogWithOwner(parentStage, c, c.getType(), () -> refreshManageList(listBox, parentStage));
            });

            Button delBtn = new Button("🗑");
            delBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #C0392B; -fx-font-size: 13; " +
                    "-fx-padding: 5 10; -fx-background-radius: 6; -fx-border-color: #E8C5C0; " +
                    "-fx-border-width: 1; -fx-border-radius: 6; -fx-cursor: hand;");
            delBtn.setOnAction(ev -> {
                boolean ok = DAOFactory.getCategoryDAO().delete(c.getId());
                if (ok) {
                    Toast.showSuccess("Kategori \"" + c.getName() + "\" dihapus ✓");
                    refreshManageList(listBox, parentStage);
                } else {
                    Toast.showSuccess("Gagal menghapus. Cek apakah masih dipakai transaksi.");
                }
            });

            row.getChildren().addAll(iconLbl, nameBox, editBtn, delBtn);
            listBox.getChildren().add(row);
        }
    }

    private String typeLabelOf(String type) {
        return "INCOME".equals(type) ? "💚 Pemasukan" : "🔴 Pengeluaran";
    }

    private String typeColorOf(String type) {
        return "INCOME".equals(type) ? "#27AE60" : "#E74C3C";
    }

    private String buildIconBtnStyle(boolean selected) {
        if (selected) {
            return "-fx-background-color: #C87941; -fx-background-radius: 8; " +
                    "-fx-font-size: 18; -fx-cursor: hand; -fx-border-color: #A8622E; " +
                    "-fx-border-width: 2; -fx-border-radius: 8;";
        }
        return "-fx-background-color: transparent; -fx-background-radius: 8; " +
                "-fx-font-size: 18; -fx-cursor: hand; -fx-border-color: transparent; -fx-border-width: 2;";
    }

    private VBox buildRootContainer() {
        VBox root = new VBox(0);
        root.setStyle(
                "-fx-background-color: #FDFAF5; -fx-background-radius: 16; " +
                        "-fx-border-color: #E8DDD0; -fx-border-radius: 16; -fx-border-width: 1; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 24, 0, 0, 6);"
        );
        return root;
    }

    private HBox buildHeader(String title, String subtitle) {
        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(22, 24, 18, 24));
        header.setStyle("-fx-background-color: linear-gradient(to right, #8B5E1A, #C87941); -fx-background-radius: 14 14 0 0;");

        VBox titleBox = new VBox(3);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: white;");
        Label subLbl = new Label(subtitle);
        subLbl.setStyle("-fx-font-size: 11; -fx-text-fill: rgba(255,255,255,0.75);");
        titleBox.getChildren().addAll(titleLbl, subLbl);

        header.getChildren().add(titleBox);
        return header;
    }

    private void enableDrag(HBox header, Stage stage) {
        final double[] offset = new double[2];
        header.setOnMousePressed((MouseEvent e) -> { offset[0] = e.getSceneX(); offset[1] = e.getSceneY(); });
        header.setOnMouseDragged((MouseEvent e) -> {
            stage.setX(e.getScreenX() - offset[0]);
            stage.setY(e.getScreenY() - offset[1]);
        });
    }

    private Button buildCancelBtn(String text) {
        Button btn = new Button(text);
        btn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #8B7355; -fx-font-size: 13; " +
                        "-fx-padding: 10 20; -fx-background-radius: 8; -fx-border-color: #E8DDD0; " +
                        "-fx-border-width: 1; -fx-border-radius: 8; -fx-cursor: hand;"
        );
        return btn;
    }

    private Button buildConfirmBtn(String text) {
        Button btn = new Button(text);
        btn.setStyle(
                "-fx-background-color: #C87941; -fx-text-fill: white; -fx-font-size: 13; " +
                        "-fx-font-weight: bold; -fx-padding: 10 24; -fx-background-radius: 8; " +
                        "-fx-border-color: transparent; -fx-cursor: hand;"
        );
        return btn;
    }

    private void centerOnOwner(Stage stage) {
        stage.setOnShowing(e -> {
            Stage owner = SessionManager.getPrimaryStage();
            if (owner != null) {
                stage.setX(owner.getX() + owner.getWidth() / 2 - stage.getWidth() / 2);
                stage.setY(owner.getY() + owner.getHeight() / 2 - stage.getHeight() / 2);
            }
        });
    }

    public Category getResultCategory() { return resultCategory; }
}