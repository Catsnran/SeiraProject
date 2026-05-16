# Seira — Fiscal Curator
> Aplikasi manajemen keuangan pribadi berbasis JavaFX  
> Project Mata Kuliah Praktikum RPLBO

---

## Cara Menjalankan

**Persyaratan:** Java 17+ dan Maven 3.8+

```bash
cd seira
mvn clean package -DskipTests
mvn javafx:run
```

Database SQLite (`seira.db`) dibuat otomatis saat pertama kali dijalankan.

---

## Fitur

| Fitur | Keterangan |
|---|---|
| 🔐 Login & Registrasi | Autentikasi dengan enkripsi BCrypt |
| 📊 Dashboard | Net worth, tren income vs expense, daily burn rate |
| 📋 Transaksi | CRUD transaksi, filter, export CSV |
| 💸 Transfer | Pindah dana antar akun, saldo otomatis terupdate |
| 💰 Budget | Anggaran per kategori (Healthy / Near Limit / Over Budget) |
| 📈 Laporan | Tren kekayaan, donut chart, rincian bulanan |
| 🏦 Akun | Kelola sumber dana (Cash, M-Banking, Savings, E-Wallet) |

---

## Struktur Proyek

```
seira/
├── pom.xml
└── src/main/
    ├── java/com/seira/
    │   ├── Main.java
    │   ├── controller/
    │   │   ├── LoginController.java
    │   │   ├── RegisterController.java
    │   │   ├── MainController.java
    │   │   ├── DashboardController.java
    │   │   ├── TransactionsController.java
    │   │   ├── AddTransactionController.java
    │   │   ├── BudgetController.java
    │   │   ├── ReportsController.java
    │   │   └── AccountsController.java
    │   ├── dao/
    │   │   └── DatabaseManager.java
    │   ├── model/
    │   │   ├── User.java
    │   │   ├── Transaction.java
    │   │   ├── Category.java
    │   │   ├── PaymentMethod.java
    │   │   └── Budget.java
    │   └── util/
    │       ├── FormatUtil.java
    │       ├── SessionManager.java
    │       ├── NavigationManager.java
    │       ├── NavigationUtil.java
    │       └── StyledDialog.java
    └── resources/
        ├── css/styles.css
        └── fxml/
            ├── Login.fxml
            ├── Register.fxml
            ├── Main.fxml
            └── pages/
                ├── Dashboard.fxml
                ├── Transactions.fxml
                ├── AddTransaction.fxml
                ├── Budget.fxml
                ├── Reports.fxml
                └── Accounts.fxml
```

---

## Penjelasan Class

### `Main.java`
Entry point JavaFX. Inisialisasi database, load halaman Login, set ukuran window minimum.

---

### Controller

> Penghubung antara tampilan FXML dan logika bisnis.

| Class | Fungsi |
|---|---|
| `LoginController` | Validasi email & password, simpan sesi ke `SessionManager`, navigasi ke `Main.fxml` |
| `RegisterController` | Validasi input (email regex, password min 6 karakter, konfirmasi cocok), daftar akun baru, redirect ke Login |
| `MainController` | Mengelola sidebar navigasi & `StackPane` konten. Method `loadPage()` memuat FXML halaman, inject referensi diri ke controller yang membutuhkan |
| `DashboardController` | Hitung net worth, pemasukan/pengeluaran bulan ini, daily burn rate. Gambar bar chart tren 6 bulan & ring chart burn rate menggunakan Canvas |
| `TransactionsController` | Render daftar transaksi dinamis, filter tipe/tanggal, pagination, edit inline, export CSV, generate Wealth Narrative |
| `AddTransactionController` | Numpad input Rupiah, 3 mode (Pengeluaran/Pemasukan/Transfer). Mode Transfer buat 2 transaksi sekaligus (EXPENSE + INCOME) agar saldo akun terupdate |
| `BudgetController` | Load anggaran bulan aktif + pengeluaran aktual, render envelope card berwarna per status, pace chart Canvas, navigasi siklus bulan sebelumnya |
| `ReportsController` | Gambar line chart net worth trend (Bezier curve) & donut chart pengeluaran per kategori menggunakan Canvas, tabel rincian komparatif 3 bulan |
| `AccountsController` | Render kartu akun berwarna, diversification bar Canvas, tabel alokasi terkini, dialog tambah/edit akun |

---

### DAO

| Class | Fungsi |
|---|---|
| `DatabaseManager` | Singleton. Satu-satunya class yang akses SQLite langsung via JDBC. Buat tabel & seed 15 kategori default saat init. Semua operasi CRUD user, transaksi, kategori, akun, dan anggaran ada di sini. Setiap transaksi ditambah/diedit/dihapus, saldo `PaymentMethod` diupdate otomatis |

---

### Model

> POJO — representasi data murni, tidak ada logika bisnis.

| Class | Atribut Utama |
|---|---|
| `User` | `id`, `username`, `email`, `passwordHash`, `currency` (default: IDR) |
| `Transaction` | `id`, `userId`, `description`, `amount`, `type` (INCOME/EXPENSE), `date`, `categoryId`, `paymentMethodId`, `reference`, `notes` |
| `Category` | `id`, `userId`, `name`, `type`, `color` (hex), `icon` (emoji) |
| `PaymentMethod` | `id`, `userId`, `name`, `type` (CASH/M-BANKING/SAVINGS/E-WALLET/INVESTMENT), `balance`, `description` |
| `Budget` | `id`, `categoryId`, `amount`, `spent`, `period` (YearMonth). Computed: `getRemaining()`, `getPercentage()`, `getStatus()` |

---

### Utility

| Class | Fungsi |
|---|---|
| `FormatUtil` | Format angka ke Rupiah: `formatCurrency()` → `Rp 1.250.000`, `formatShort()` → `Rp 1,5jt` / `Rp 500rb` |
| `SessionManager` | Simpan `User` yang sedang login dan referensi `primaryStage` secara static. Data hilang saat app ditutup |
| `NavigationManager` | Ganti root scene di `primaryStage` tanpa buat Stage baru (ukuran window terjaga). Path lengkap: `/fxml/Login.fxml` |
| `NavigationUtil` | Sama dengan `NavigationManager` tapi path otomatis diawali `/fxml/`. Duplikat dari masa pengembangan — yang aktif dipakai adalah `NavigationManager` |
| `StyledDialog` | Builder untuk popup modal bertema Seira (header gradient coklat-oranye, field styled, tombol branded). Menggantikan `Dialog` bawaan JavaFX yang tampilannya flat. Dipakai di `AccountsController` dan `BudgetController` |

---

## Arsitektur

```
FXML (View)  ──@FXML──►  Controller  ──calls──►  DatabaseManager  ──JDBC──►  seira.db
```

---

## Dependencies

| Library | Versi | Fungsi |
|---|---|---|
| `javafx-controls` | 21.0.2 | Komponen UI |
| `javafx-fxml` | 21.0.2 | Load FXML + @FXML injection |
| `javafx-graphics` | 21.0.2 | Canvas, Scene, Stage |
| `sqlite-jdbc` | 3.45.1.0 | Koneksi SQLite |
| `opencsv` | 5.9 | Export CSV |
| `pdfbox` | 3.0.1 | Export PDF (disiapkan) |
| `jbcrypt` | 0.4 | Hash password BCrypt |
