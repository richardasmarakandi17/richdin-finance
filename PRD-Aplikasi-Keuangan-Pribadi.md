# Dokumen Awal (Initial PRD)
# Aplikasi Pencatatan Keuangan Pribadi — Working Title: "Duitku" / "Jatah"

Versi: 0.1 (Draft Awal)
Tanggal: 2 September 2026

---

## 1. Latar Belakang & Masalah

Pengguna menerima gaji setiap tanggal 2 dengan nominal **bervariasi** (dipengaruhi jumlah hari libur/tanggal merah). Tantangan yang dihadapi:

- Gaji harus cukup untuk kebutuhan sehari-hari (makan, bensin, parkir, dll) selama sebulan penuh sampai gajian berikutnya.
- Perlu alokasi untuk **dana darurat** dan **tabungan** setiap bulan.
- Tidak ada visibilitas real-time apakah pengeluaran harian masih "on track" atau sudah melebihi kapasitas.
- Akibatnya, di akhir bulan keuangan sering menipis drastis, bahkan sampai harus menekan pengeluaran makan secara ekstrem.

**Masalah inti:** kurangnya (1) alokasi otomatis di awal periode, dan (2) sistem peringatan dini harian yang memberi tahu apakah kecepatan belanja aman atau berbahaya.

---

## 2. Tujuan Aplikasi

1. Memastikan seluruh pendapatan bulanan (gaji + sumber lain seperti komisi/hadiah) teralokasi habis (zero-based) ke kategori: kebutuhan tetap, dana darurat, tabungan, dan jatah harian — **sejak hari gajian**, bukan menyisakan di akhir bulan.
2. Memberi pengguna angka **jatah belanja harian yang jelas dan dinamis**, yang otomatis menyesuaikan sisa hari dan sisa saldo.
3. Memberi **peringatan dini** ketika kecepatan pengeluaran berisiko membuat saldo habis sebelum gajian berikutnya.
4. Mempermudah pencatatan pengeluaran harian sekecil apa pun (makan, bensin, parkir) dengan gesekan sekecil mungkin (idealnya < 10 detik per entri).
5. Memberi gambaran tren bulan-ke-bulan agar pengguna bisa mengevaluasi pola kebiasaan belanja.
6. Memberi fleksibilitas darurat: mengizinkan "pinjam" dari kantong dana darurat/tabungan saat kondisi mendesak (sakit, dll), dengan mekanisme pelacakan agar tetap dikembalikan.
7. Melindungi data keuangan yang sensitif dengan PIN lock, dan memastikan data tidak pernah hilang meski ganti/hilang perangkat, tanpa biaya server.

---

## 3. Target Pengguna

**Persona utama:** Pekerja dengan gaji bulanan tetap-tanggal-tapi-variabel-nominal (karyawan shift/harian yang digaji bulanan, freelancer dengan retainer, dll), penghasilan menengah, mobile-first, terbiasa pakai e-wallet/QRIS untuk transaksi harian.

Kebutuhan persona ini: kesederhanaan pencatatan + kepastian angka "aman belanja berapa hari ini", bukan laporan akuntansi yang rumit.

---

## 4. Konsep Inti Sistem

### 4.1 Alokasi Zero-Based saat Gajian
Begitu gaji dicatat masuk (tanggal 2 atau kapan pun diterima), sistem membantu memecah gaji ke beberapa "kantong" (pockets):

| Kantong | Contoh Alokasi | Sifat |
|---|---|---|
| Kebutuhan Tetap/Wajib | Kos/cicilan, listrik, internet, dll | Nominal tetap tiap bulan |
| Dana Darurat | % dari sisa gaji atau nominal tetap sampai target tercapai | Terkunci, tidak masuk hitungan jatah harian |
| Tabungan/Goals | % dari sisa gaji, atau per goal (mis. beli laptop) | Terkunci |
| Jatah Harian (Daily Allowance) | Sisa setelah 3 kantong di atas, dibagi jumlah hari sampai gajian berikutnya | Cair, inilah yang dipakai sehari-hari |

Pengguna bisa set persentase default (misalnya 10% dana darurat, 10% tabungan) tapi tetap bisa mengubah nominal manual tiap bulan karena gaji variatif.

**Multi-Sumber Pendapatan:** Dalam satu periode gajian, pengguna bisa mencatat lebih dari satu pemasukan — gaji utama, komisi, hadiah, atau pendapatan lain-lain. Setiap sumber dicatat terpisah (untuk keperluan riwayat/analisis), namun totalnya digabung sebagai dasar alokasi zero-based ke 4 kantong. Pendapatan tambahan yang masuk **di tengah bulan** (bukan di awal periode) bisa langsung ditambahkan ke kantong pilihan pengguna (misal: komisi mendadak → langsung ke tabungan, atau ke jatah harian jika ingin dipakai).

### 4.4 Mekanisme Pinjam Antar-Kantong (Emergency Borrowing)
Karena kondisi darurat (sakit, dll) bisa muncul di tengah bulan, pengguna diizinkan **meminjam** dari kantong Dana Darurat atau Tabungan ke kantong Jatah Harian — namun **tidak otomatis**, harus konfirmasi eksplisit agar tetap disiplin. Setiap pinjaman:

- Dicatat sebagai transaksi "Pinjaman" (bukan pengeluaran biasa), dengan kantong asal, nominal, dan tanggal.
- Menampilkan status "Belum Lunas" pada kantong asal (dana darurat/tabungan akan menampilkan saldo tersedia vs. saldo dipinjam, agar target tidak menyesatkan).
- Sistem memberi **reminder berkala** (misal tiap gajian berikutnya) untuk mengembalikan pinjaman ke kantong asal sebelum melanjutkan alokasi normal.
- Dashboard menampilkan indikator jika ada pinjaman aktif yang belum dikembalikan.

**Batas Maksimal Pinjaman per Bulan (Borrowing Cap):**
- Pengguna menentukan batas pinjam bulanan saat setup awal (bisa berupa **nominal tetap** atau **persentase dari saldo kantong asal**, mis. maksimal 30% dari saldo Dana Darurat yang tersedia). Default disarankan: 20–30% agar dana darurat tidak habis sekali pakai.
- Batas ini dihitung ulang tiap periode gajian (kuota "reset" tiap bulan setelah alokasi baru).
- Saat pengguna mencoba meminjam melebihi sisa kuota bulan ini, sistem **menolak** transaksi dan menampilkan sisa kuota yang masih tersedia — bukan sekadar warning, agar benar-benar menahan godaan "mengakali" sistem.
- Dashboard menampilkan indikator "Sisa Kuota Pinjam Bulan Ini: Rp X" agar pengguna sadar batasannya sebelum kepepet.
- Batas ini bisa diubah pengguna kapan saja lewat Pengaturan (tidak dikunci permanen), tapi perubahan hanya berlaku untuk periode berikutnya — supaya tidak bisa "menaikkan limit" di tengah kondisi darurat demi alasan psikologis, harus direncanakan di awal periode.

### 4.2 Jatah Harian Dinamis (Daily Allowance Engine)
Ini fitur pembeda utama. Rumus dasar:

```
Jatah Harian Hari Ini = Sisa Saldo Kantong Harian / Sisa Hari sampai Gajian Berikutnya
```

- Dihitung ulang **setiap hari**, bukan statis di awal bulan.
- Jika hari ini belanja lebih hemat dari jatah → sisa otomatis menambah jatah rata-rata hari-hari berikutnya (naik sedikit).
- Jika hari ini belanja lebih boros dari jatah → jatah harian rata-rata untuk sisa hari otomatis turun, dan sistem memberi notifikasi.

### 4.3 Sistem Peringatan Dini (Early Warning)
Level status ditampilkan tiap hari, misalnya:
- 🟢 **Aman** — pengeluaran sesuai/di bawah proyeksi.
- 🟡 **Waspada** — proyeksi saldo akan habis 3-5 hari sebelum gajian berikutnya jika tren berlanjut.
- 🔴 **Bahaya** — proyeksi saldo habis lebih dari 5 hari sebelum gajian berikutnya.

Notifikasi push harian ringkas: *"Jatah hari ini: Rp X. Sisa 14 hari lagi menuju gajian."*

---

## 5. Fitur Utama (MVP)

1. **Input Pendapatan (Multi-Sumber)**
   - Catat tanggal & nominal gaji utama (manual, karena bervariasi tiap bulan).
   - Tambahkan sumber pendapatan lain kapan pun dalam periode berjalan: komisi, hadiah, lain-lain — masing-masing dengan kategori & catatan.
   - Opsi set tanggal gajian berikutnya (default tanggal 2, bisa disesuaikan jika weekend/libur).

2. **Alokasi Otomatis ke Kantong**
   - Setup kantong: Kebutuhan Tetap, Dana Darurat, Tabungan, Jatah Harian.
   - Template alokasi persen (bisa diedit tiap bulan).
   - Progress dana darurat & tabungan (target vs. terkumpul).

3. **Pencatatan Pengeluaran Harian (Quick Entry)**
   - Kategori cepat: Makan, Bensin, Parkir, Transportasi, Lainnya (bisa custom).
   - Input minim tap: nominal + kategori + (opsional) catatan/foto struk.
   - Widget/shortcut home screen untuk catat pengeluaran dalam 1-2 tap.

4. **Dashboard Harian**
   - Jatah harian hari ini (angka besar, jelas).
   - Sisa hari menuju gajian.
   - Status early warning (hijau/kuning/merah).
   - Sisa saldo kantong harian bulan ini.

5. **Notifikasi**
   - Reminder harian (jam yang bisa diatur, misal jam 8 malam) berisi ringkasan jatah & sisa hari.
   - Alert otomatis saat status berubah ke Waspada/Bahaya.

6. **Riwayat & Laporan Sederhana**
   - Grafik pengeluaran harian dalam bulan berjalan.
   - Perbandingan bulan ini vs bulan lalu per kategori.

7. **Pinjam Antar-Kantong (Emergency Borrowing)**
   - Form konfirmasi eksplisit untuk meminjam dari Dana Darurat/Tabungan ke Jatah Harian.
   - Setup batas maksimal pinjam per bulan (nominal atau persentase saldo), dengan penolakan otomatis jika kuota terlampaui.
   - Indikator "Sisa Kuota Pinjam Bulan Ini" di dashboard.
   - Tracking status lunas/belum lunas per pinjaman.
   - Reminder pengembalian otomatis di periode gajian berikutnya.

8. **PIN Lock (Keamanan)**
   - Setup PIN 6 digit saat pertama kali buka app.
   - App terkunci otomatis saat dibuka kembali dari background (timeout bisa diatur).
   - Opsi tambahan: unlock via biometric (sidik jari/wajah) sebagai alternatif PIN.

9. **Backup & Restore ke Google Drive**
   - Login dengan akun Google (Google Sign-In).
   - Backup otomatis (lihat strategi timing hybrid di bagian 9) ke Google Drive App Data Folder — gratis, tersembunyi, terikat akun Google pengguna.
   - Saat install ulang/ganti perangkat: deteksi backup yang ada, tawarkan restore.

10. **Export Manual (CSV)**
    - Export data (per periode/bulan atau seluruh histori) ke file CSV, disimpan ke penyimpanan lokal/Downloads.
    - Bisa langsung dibagikan (share intent Android) ke email, cloud lain, atau aplikasi spreadsheet — sebagai cadangan tambahan di luar ekosistem Google, sesuai permintaan Anda.
    - Terpisah dari backup otomatis: ini murni untuk kontrol penuh pengguna atas datanya sendiri.

---

## 6. Fitur Lanjutan (Fase 2+, bukan MVP)

- Proyeksi otomatis alokasi ideal berdasarkan rata-rata pengeluaran 3-6 bulan terakhir.
- Multiple dana darurat/goals dengan target nominal & tanggal.
- Import otomatis transaksi dari SMS/notifikasi e-wallet (opsional, perlu izin akses notifikasi Android).
- Mode "amplop" (envelope) per kategori kebutuhan tetap.
- Backup/sync ke cloud (Google Drive) atau akun.
- Export laporan bulanan (PDF/Excel).
- Insight/rekomendasi otomatis (mis. "Pengeluaran bensin naik 30% dibanding bulan lalu").

---

## 7. Alur Pengguna Utama (User Flow)

**Alur A — Awal Bulan (Hari Gajian)**
1. Notifikasi/reminder: "Sudah gajian? Catat gaji bulan ini."
2. Input nominal gaji.
3. Review/adjust alokasi ke 4 kantong (sistem sarankan berdasarkan template/bulan lalu).
4. Sistem tampilkan: jatah harian awal, target dana darurat, target tabungan.

**Alur B — Harian**
1. Buka app / widget → lihat jatah hari ini & status warna.
2. Belanja → catat pengeluaran (kategori + nominal), idealnya lewat widget tanpa buka app penuh.
3. Dashboard update otomatis: sisa jatah hari ini, proyeksi sisa bulan.

**Alur C — Saat Status Kuning/Merah**
1. Notifikasi alert muncul.
2. App tampilkan saran singkat (mis. "Kurangi Rp20.000/hari untuk 10 hari ke depan agar tetap aman") — bisa juga opsi menarik sedikit dari kantong tabungan (dengan konfirmasi eksplisit, bukan otomatis, agar tetap disiplin).

**Alur D — Kondisi Darurat (Meminjam dari Dana Darurat/Tabungan)**
1. Pengguna buka menu "Pinjam Dana" dari dashboard.
2. Pilih kantong asal (Dana Darurat/Tabungan), input nominal, input alasan (opsional, mis. "sakit").
3. Konfirmasi eksplisit (bukan sekali tap, ada dialog peringatan agar tidak asal pakai).
4. Saldo kantong harian bertambah, status "Belum Lunas" muncul di kantong asal.
5. Di gajian berikutnya, sebelum alokasi normal, app ingatkan untuk melunasi pinjaman terlebih dahulu.

**Alur E — Buka Aplikasi (Keamanan)**
1. App diluncurkan/dibuka dari background setelah timeout → layar PIN muncul.
2. Input PIN atau biometric → masuk ke dashboard.

---

## 8. Struktur Data Awal (Data Model Sederhana)

```
User
 ├─ pin_hash, biometric_enabled, google_account_id (untuk backup)
 ├─ LoanLimitConfig
 │   ├─ tipe_batas [nominal_tetap|persentase_saldo], nilai_batas
 │   ├─ berlaku_mulai_periode (agar perubahan hanya efektif periode berikutnya)
 └─ SalaryPeriod (per bulan)
     ├─ id, tanggal_mulai, tanggal_gajian_berikutnya
     ├─ IncomeSource (bisa lebih dari satu per periode)
     │   ├─ id, salary_period_id, jenis [gaji_utama|komisi|hadiah|lainnya]
     │   ├─ nominal, tanggal_diterima, catatan
     ├─ Pocket (kantong): id, nama, tipe [tetap|darurat|tabungan|harian], nominal_alokasi, saldo_berjalan
     ├─ Expense (pengeluaran harian)
     │   ├─ id, pocket_id, tanggal, kategori, nominal, catatan, foto_opsional
     └─ Loan (pinjaman antar kantong)
         ├─ id, pocket_asal_id, pocket_tujuan_id, nominal
         ├─ tanggal_pinjam, alasan, status [belum_lunas|lunas], tanggal_lunas
         ├─ kuota_terpakai_periode_ini (untuk validasi terhadap LoanLimitConfig)

Category (kategori pengeluaran): id, nama, icon, is_custom

Goal (untuk dana darurat/tabungan bertarget): id, nama, target_nominal, saldo_terkumpul, target_tanggal

BackupMeta: id, last_backup_timestamp, drive_file_id, status, pending_changes_flag
```

**Catatan:** `pin_hash` **tidak** disimpan sebagai teks biasa — harus di-hash (mis. dengan salt, algoritma seperti BCrypt/Argon2) dan disimpan lewat Android Keystore/Jetpack Security, bukan di kolom database biasa yang ikut ter-backup ke Drive.

---

## 9. Rekomendasi Tech Stack (Android)

| Layer | Rekomendasi | Alasan |
|---|---|---|
| Bahasa & UI | Kotlin + Jetpack Compose | Standar modern Android, reaktif, cocok untuk dashboard yang sering update |
| Arsitektur | **Clean Architecture** (Presentation → Domain → Data) + **MVVM** di layer presentation | Domain layer (use case, entity) murni Kotlin tanpa dependency Android → mudah di-test, mudah berkembang jangka panjang tanpa "rewrite" |
| Modularisasi | Multi-module Gradle: `app`, `core-domain`, `core-data`, `core-security`, `feature-income`, `feature-expense`, `feature-pockets`, `feature-loan`, `feature-auth`, `feature-report` | Build lebih cepat, tiap fitur terisolasi, mudah ditambah/diubah tanpa mengganggu modul lain — ini kunci "scalable" jangka panjang |
| Dependency Injection | Hilt | Standar DI untuk Android, wajib untuk arsitektur modular |
| Reactive Data | Kotlin Coroutines + Flow | Untuk observe perubahan saldo/jatah harian secara real-time di UI |
| Local Database (primer) | Room (SQLite) | Sumber data utama, cepat, offline-first, gratis, tanpa server |
| Backup/Sync Jangka Panjang | **Google Drive REST API — App Data Folder**, via Google Sign-In (Credential Manager API) | Gratis (bagian kuota Drive 15GB user), tersembunyi dari file manager biasa, data terikat identitas Google pengguna → sesuai kebutuhan "tidak hilang selamanya" tanpa biaya server. **Bukan** Google Sheets — Sheets API tidak cocok jadi database aktif (lambat, rate limit, rawan corrupt) |
| Keamanan (PIN Lock) | Jetpack Security Crypto (EncryptedSharedPreferences) + Android Keystore untuk simpan PIN hash; BiometricPrompt API untuk opsi sidik jari/wajah | PIN tidak boleh tersimpan sebagai plain text maupun ikut ter-backup mentah ke Drive |
| Background/Notifikasi | WorkManager + Notification API | Reminder harian, recalculation jatah harian, reminder pelunasan pinjaman, backup terjadwal |
| Widget | Jetpack Glance (App Widget) | Quick-entry pengeluaran dari home screen |
| Charting | Vico atau MPAndroidChart | Grafik tren pengeluaran |

**Strategi penyimpanan (ringkas):** Room tetap jadi sumber kebenaran utama (cepat & selalu bisa dipakai offline). Google Drive App Data Folder berfungsi sebagai **backup otomatis berkala** (bukan database aktif) — jadi performa app tidak bergantung koneksi internet, tapi data tetap aman selama akun Google pengguna ada. Ini menghindari biaya server (MySQL/PostgreSQL/Firebase berbayar) sekaligus menghindari risiko kehilangan data karena hanya tersimpan di satu device.

**Timing Backup (Hybrid — 3 pemicu, agar tidak terlalu sering tapi juga tidak terlalu jarang):**

1. **Debounced backup setelah perubahan data** — setiap ada transaksi baru (expense/income/loan), jadwalkan backup via `WorkManager` dengan delay 15 menit sejak perubahan terakhir (pakai `ExistingWorkPolicy.REPLACE`, jadi kalau ada input baru lagi dalam 15 menit itu, timer reset). Hasilnya: backup jalan otomatis begitu aktivitas pencatatan "mereda", tidak membakar baterai tiap 1 transaksi.
2. **Trigger saat app ke background** — begitu pengguna menutup/minimize app dan ada perubahan yang belum ter-backup, langsung jadwalkan backup segera (expedited WorkManager job). Ini penting untuk skenario "tiba-tiba install ulang/ganti HP" — jadi data yang baru dicatat tidak menunggu lama.
3. **Fallback terjadwal harian** — backup wajib dijalankan minimal 1x/hari (misal jam 02:00 dini hari, saat device idle & biasanya charging) sebagai jaring pengaman kalau pemicu #1 dan #2 gagal (mis. tidak ada koneksi internet saat itu).

Dengan kombinasi ini, **worst-case kehilangan data** hanya sebatas transaksi dalam sesi terakhir yang belum sempat tersinkron (biasanya menit ke jam, bukan hari) — sudah cukup aman untuk skala data personal seperti ini, tanpa perlu backup real-time yang boros resource.

---

## 10. Metrik Keberhasilan (Success Metrics)

- Pengguna mencatat pengeluaran minimal 5x/minggu (indikasi kebiasaan terbentuk).
- Saldo di kantong harian tidak minus/habis sebelum H-3 gajian berikutnya (masalah utama yang ingin diselesaikan).
- Dana darurat & tabungan konsisten terisi tiap bulan (tidak "dimakan" duluan).

---

## 11. Roadmap Pengembangan

**Fase 1 — MVP (fokus core problem + keamanan & keamanan data dasar)**
- Setup arsitektur modular (Clean Architecture + Hilt) sejak awal, agar fase berikutnya tidak perlu refactor besar
- PIN Lock (wajib, karena data sensitif)
- Input pendapatan multi-sumber & alokasi kantong
- Daily Allowance Engine + dashboard
- Pencatatan pengeluaran cepat
- Notifikasi status harian
- Backup otomatis hybrid ke Google Drive App Data Folder (dasar, agar dari awal data sudah aman)

**Fase 2 — Penguatan Kebiasaan & Fleksibilitas**
- Widget home screen
- Laporan & grafik tren
- Goals untuk tabungan/dana darurat
- Mekanisme Pinjam Antar-Kantong + batas kuota bulanan + reminder pelunasan
- Restore dari backup (flow ganti device)
- Export manual ke CSV

**Fase 3 — Otomatisasi & Insight**
- Rekomendasi alokasi berbasis histori
- Import transaksi otomatis
- Biometric unlock sebagai pelengkap PIN

---

## 12. Tema Visual & Maskot (Branding)

### 12.1 Konsep Umum
Aplikasi mengusung tema **peternakan**, dengan **maskot sapi** yang berperan seperti "admin" penjaga keuangan pengguna — bukan sekadar ikon dekoratif, tapi elemen yang bereaksi terhadap kondisi keuangan pengguna secara real-time agar aplikasi terasa hidup dan interaktif.

### 12.2 Ekspresi Maskot Sesuai Status
Selaras dengan sistem Early Warning (bagian 4.3):

| Status | Ekspresi Sapi |
|---|---|
| 🟢 Aman | Tersenyum bahagia |
| 🟡 Waspada | Ekspresi datar/netral |
| 🔴 Bahaya | Menangis |

### 12.3 Momen Interaksi Maskot (di luar early warning)
Agar maskot tidak hanya muncul di satu tempat, sapi sebaiknya bereaksi di berbagai momen:
- Anggukan/senyum kecil saat pencatatan pengeluaran masih di bawah jatah harian
- Ekspresi was-was saat mendekati batas kuota Pinjam Antar-Kantong
- Reaksi gembira/merayakan saat target tabungan atau dana darurat tercapai
- Ekspresi prihatin (tanpa menghakimi) saat kategori pengeluaran tertentu naik signifikan dibanding bulan lalu
- Sapaan harian bergaya personal, bukan notifikasi datar

### 12.4 Rekomendasi Teknis Animasi
Gunakan **Rive** (bukan Lottie) untuk animasi maskot:
- Rive mendukung *State Machine* — satu file animasi berisi banyak state (senang/datar/sedih) yang bisa di-*blend*/transisi mulus dan dikendalikan langsung oleh data aplikasi secara real-time (status keuangan sebagai input yang menggerakkan animasi).
- Lottie lebih cocok untuk animasi linear sekali putar (mis. splash screen), kurang fleksibel untuk karakter yang harus terus merespons perubahan state.
- Integrasi: `rive-android` SDK, dengan State Machine Input yang di-bind ke enum status (`aman`/`waspada`/`bahaya`) dari Daily Allowance Engine.

### 12.5 Palet Warna Tema Peternakan
Warna status keuangan diselaraskan dengan elemen visual peternakan, sehingga warna terasa menyatu dengan tema, bukan sekadar warna sistem generik:

| Elemen Tema | Peran Warna |
|---|---|
| Hijau padang rumput | Status Aman / elemen positif |
| Kuning jerami/gandum | Status Waspada / elemen perhatian |
| Merah gudang/lumbung | Status Bahaya / elemen peringatan |
| Coklat kayu kandang & krem | Warna netral untuk background/card |

### 12.6 Catatan Pengembangan
- Fase 1 (MVP): maskot bisa mulai dengan ilustrasi statis (3 ekspresi: senang/datar/sedih) untuk mempercepat rilis awal.
- Fase 2+: upgrade ke animasi Rive dengan State Machine agar transisi ekspresi lebih hidup, plus tambahan momen interaksi di luar dashboard utama (poin 12.3).
- Nama maskot sebaiknya ditentukan lebih dulu (mis. nama yang catchy/related ke uang atau peternakan) karena akan sering muncul dalam copywriting notifikasi dan sapaan aplikasi.

---

## 13. Keputusan yang Sudah Diambil

1. **Multi-akun:** tidak diperlukan untuk saat ini.
2. **Multi-sumber pendapatan:** diperlukan (gaji utama, komisi, hadiah, lainnya) — lihat bagian 4.3 dan 8.
3. **Pinjam antar-kantong:** diizinkan dengan konfirmasi eksplisit + reminder pelunasan + **batas maksimal pinjam per bulan** (nominal/persentase, ditolak otomatis jika melebihi kuota) — lihat bagian 4.4.
4. **Platform:** native Android (Kotlin) saja, dengan arsitektur Clean Architecture + modular untuk skalabilitas jangka panjang — lihat bagian 9.
5. **Keamanan:** wajib PIN lock, dengan opsi biometric di fase lanjutan.
6. **Penyimpanan jangka panjang:** Room (lokal, primer) + backup otomatis **hybrid** (debounce 15 menit + trigger saat app background + fallback harian jam 02:00) ke Google Drive App Data Folder — lihat bagian 9.
7. **Cadangan tambahan:** disediakan fitur export manual ke CSV, terpisah dari backup otomatis Google Drive — lihat bagian 5 (fitur #10).

Semua pertanyaan terbuka pada draft sebelumnya sudah terjawab. Dokumen ini sudah cukup lengkap untuk mulai masuk ke tahap desain UI (wireframe) dan setup skeleton project.
