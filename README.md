# Richdin Finance (Duitku / Jatah) 🐮💰

**Richdin Finance** adalah aplikasi pencatatan keuangan pribadi Android *production-ready* berbasis **Zero-Based Budgeting** dan **Dynamic Daily Allowance Engine** dengan tema visual peternakan ceria dan maskot interaktif **"Si Sapi"**.

---

## 🌟 Fitur Utama (Sesuai PRD)

1. **Daily Allowance Engine (Jatah Harian Dinamis):**
   - Menghitung jatah belanja harian secara adaptif: `Sisa Saldo Kantong Harian / Sisa Hari sampai Gajian`.
   - Otomatis menyesuaikan sisa hari: hemat hari ini akan meningkatkan jatah hari berikutnya, boros hari ini akan otomatis menurunkan proyeksi jatah.
2. **Sistem Peringatan Dini & Maskot Sapi Reaktif (Early Warning):**
   - 🟢 **Aman (Happy Cow):** Pengeluaran on-track, jatah harian terjaga.
   - 🟡 **Waspada (Neutral Cow):** Proyeksi saldo habis H-3 s/d H-5 sebelum gajian.
   - 🔴 **Bahaya (Sad / Crying Cow):** Proyeksi saldo habis >5 hari sebelum gajian.
   - Memberi saran tindakan proaktif (contoh: *"Kurangi Rp 20.000/hari untuk 10 hari ke depan"*).
3. **Zero-Based Budgeting (4 Kantong Keuangan):**
   - Alokasi 100% pendapatan saat gajian ke: **Kebutuhan Tetap**, **Dana Darurat**, **Tabungan/Goals**, dan **Jatah Harian**.
   - Template 50/10/10/30 atau kustom.
4. **Pinjam Antar-Kantong Terkendali (Emergency Borrowing):**
   - Meminjam dari Dana Darurat/Tabungan ke Jatah Harian saat darurat.
   - **Batas Kuota Bulanan (Borrowing Cap):** Default 20–30% dari saldo Dana Darurat; otomatis menolak jika melampaui kuota.
   - Konfirmasi eksplisit & pelacakan status lunas/belum lunas dengan pengingat saat gajian berikutnya.
5. **Quick Expense Entry (<10 Detik):**
   - Input nominal cepat via keypad internal.
   - Pilihan kategori cepat (Makan, Bensin, Parkir, Transportasi, Belanja, Tagihan, dll).
6. **Keamanan Maksimal:**
   - Setup PIN 6-digit dengan hashing SHA-256 + salt via Keystore / Encrypted Preferences.
   - Dukungan sensor biometrik (sidik jari/wajah).
   - Auto-lock timeout saat aplikasi ditinggalkan ke background.
7. **Cadangan Data & Ekspor:**
   - Strategi cadangan hybrid ke Google Drive App Data Folder (Debounce 15 menit, on-background trigger, dan fallback harian 02:00).
   - Ekspor mandiri semua data ke format CSV melalui Android Share Sheet.
8. **AppWidget (Jetpack Glance):**
   - Widget layar beranda untuk melihat sisa jatah hari ini dan pintasan catat pengeluaran 1-tap.

---

## 🏗️ Tech Stack & Arsitektur

| Komponen | Teknologi | Alasan Pemilihan |
|---|---|---|
| **Bahasa & UI** | Kotlin 2.0 + Jetpack Compose + Material 3 | Reaktif, deklaratif, performa tinggi |
| **Arsitektur** | **Clean Architecture** (Presentation, Domain, Data) + **MVI / MVVM** | Pemisahan tanggung jawab (*separation of concerns*), mudah di-test, modular & scalable |
| **Dependency Injection** | Hilt (Dagger) | Standar industri Android untuk arsitektur skala besar |
| **Database Lokal** | Room SQLite (Offline-First) | Cepat, handal, tanpa latensi server, ACID-compliant |
| **Reactive Stream** | Kotlin Coroutines + StateFlow / Flow | Manajemen state reaktif tanpa memory leaks |
| **Background Tasks** | AndroidX WorkManager | Eksekusi reliabel untuk reminder harian dan cadangan data |
| **Keamanan** | AndroidX Security Crypto & BiometricPrompt | Enkripsi data sensitif & otentikasi hardware-backed |
| **AppWidget** | AndroidX Glance Material 3 | Pembuatan widget home screen modern berbasis Compose |

---

## 📁 Struktur Direktori

```
app/src/main/java/com/richdin/finance/
├── MainActivity.kt                      # Host Navigasi & Lifecycle Auto-Lock
├── RichdinApp.kt                        # Hilt Application Class & Worker Initialization
├── di/                                  # Hilt DI Modules (DatabaseModule, RepositoryModule)
├── core/
│   ├── model/                           # Domain Data Models & Enums
│   ├── database/                        # Room Database, Entities, DAOs, Converters
│   ├── repository/                      # Repository Interfaces & Implementations
│   ├── domain/                          # Business Engines & Use Cases
│   │   ├── DailyAllowanceEngine.kt      # Algoritma Jatah Harian & Status Sapi
│   │   ├── ZeroBasedAllocationUseCase.kt # Alokasi 100% 4 Kantong
│   │   ├── LoanManagerUseCase.kt        # Validasi Kuota Pinjam Darurat & Pelunasan
│   │   └── ExportCsvUseCase.kt          # Generator CSV File
│   ├── security/                        # PIN Hashing, Keystore, SessionLock & Biometric
│   ├── worker/                          # DailyReminderWorker, HybridBackupWorker, WorkScheduler
│   └── ui/
│       ├── theme/                       # Palet Peternakan (Meadow Green, Straw Yellow, Barn Red)
│       └── components/                  # SapiMascot Composable, FarmCards, NumberKeypad
├── feature/
│   ├── auth/                            # PIN Entry & Setup Biometric Screen
│   ├── dashboard/                       # Dashboard Hero Jatah Harian & Reaksi Sapi
│   ├── expense/                         # Quick Expense Input Screen (< 10s)
│   ├── income/                          # Input Gaji & Zero-Based Pocket Allocator
│   ├── loan/                            # Emergency Loan Request & Repay Screen
│   ├── pockets/                         # Rincian 4 Kantong & Goal Impian
│   ├── report/                          # Grafik & Evaluasi Pengeluaran
│   └── settings/                        # Pengaturan PIN, Biometrik, Kuota & Ekspor CSV
└── widget/                              # Jetpack Glance Home Screen Widget
```

---

## 🧪 Unit Testing

Semua engine kalkulasi penting dilengkapi unit test:
- `DailyAllowanceEngineTest`: Memvalidasi kalkulasi jatah dinamis, ambang batas *burn rate*, peringatan dini, dan perubahan ekspresi maskot sapi.
- `ZeroBasedAllocationUseCaseTest`: Memvalidasi pembagian 100% pendapatan dan pembuatan 4 kantong.
- `LoanManagerUseCaseTest`: Memvalidasi penolakan otomatis pinjaman jika melebihi kuota bulanan.

---

## 🚀 Cara Menjalankan Project

1. Buka folder `c:\Projects\Richdin_Finance` di **Android Studio** (Koala / Ladybug / versi terbaru).
2. Biarkan Gradle melakukan sync dependencies via Gradle Version Catalog (`gradle/libs.versions.toml`).
3. Hubungkan perangkat fisik Android atau jalankan Android Emulator (Android 8.0 / API 26+).
4. Klik tombol **Run 'app'** (`Shift + F10`).
