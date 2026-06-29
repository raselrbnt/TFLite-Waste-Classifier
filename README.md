# ♻️ EcoTech Waste Classifier

[![Kotlin Version](https://img.shields.io/badge/kotlin-1.9.0-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Platform](https://img.shields.io/badge/platform-Android-green.svg?logo=android)](https://developer.android.com)
[![TensorFlow Lite](https://img.shields.io/badge/ML-TensorFlow%20Lite-orange.svg?logo=tensorflow)](https://www.tensorflow.org/lite)
[![Room Database](https://img.shields.io/badge/Database-Room-blue.svg?logo=sqlite)](https://developer.android.com/training/data-storage/room)

**EcoTech Waste Classifier** adalah aplikasi mobile berbasis Android yang dirancang untuk mengklasifikasikan jenis sampah secara otomatis dan *offline* (on-device) menggunakan model Machine Learning TensorFlow Lite. Aplikasi ini mendukung gerakan *go-green* dengan membantu pengguna memilah sampah secara tepat ke dalam kategori **Organik** dan **Anorganik**.

---

## ✨ Fitur Utama

*   📷 **Smart Camera Scanner**: Integrasi kamera menggunakan **CameraX** untuk pengambilan gambar objek sampah secara langsung yang cepat, stabil, dan hemat daya.
*   🖼️ **Galeri Import**: Memungkinkan klasifikasi objek sampah dengan mengambil gambar yang ada dari penyimpanan galeri perangkat.
*   🧠 **On-Device ML Inference**: Menggunakan **TensorFlow Lite** interpreter dengan optimasi Memory Mapping (`mmap`) dan caching instansi model (Singleton) untuk klasifikasi offline instan tanpa beban jaringan internet.
*   ⚠️ **70% Confidence Safety Guard**: Logika pembatas tingkat kepercayaan (confidence threshold) sebesar **70%** untuk menghindari misklasifikasi objek. Hasil di bawah 70% secara dinamis dikategorikan sebagai **TIDAK TERDETEKSI** disertai instruksi pemindaian ulang.
*   📊 **Dynamic System Status**: Indikator status sistem ML yang aktif secara dinamis pada Dashboard utama berdasarkan status inisialisasi model.
*   📂 **History Log (Catatan)**: Menyimpan riwayat pemindaian ke dalam database lokal **Room DB** lengkap dengan detail kategori, tingkat kepercayaan, waktu pemindaian, dan gambar objek sampah.
*   🎨 **Premium UI/UX Design**:
    *   Navigasi transisi halaman yang mulus (*slide horizontal*).
    *   Warna tema dinamis (Hijau Neon untuk sukses, Merah Peringatan untuk status Tidak Terdeteksi).
    *   Penanganan layar kosong (*Empty State Handling*) yang estetis.
    *   Bar navigasi bawah (*Bottom Navigation*) responsif.

---

## 🛠️ Tech Stack & Library

*   **Bahasa Utama**: Kotlin
*   **Arsitektur**: Model-View-ViewModel (MVVM) / Single Activity & Multi-Fragment
*   **Minimum SDK**: API 24 (Android 7.0 Nougat)
*   **Target SDK**: API 35 (Android 15)
*   **Build System**: Gradle 8.7 & Android Gradle Plugin (AGP) 8.4.0
*   **Database**: Room Database (SQLite ORM)
*   **Kamera**: Android CameraX (Core, Camera2, Lifecycle, View)
*   **Machine Learning**: TensorFlow Lite (`2.4.0`) & TensorFlow Lite Support (`0.1.0`)
*   **UI/UX Components**: Google Material Components, ViewBinding, Jetpack Navigation Component

---

## 📂 Struktur Proyek

```text
app/src/main/java/com/example/sampahclassifierapp/
├── data/                    # Manajemen Data & Room Database
│   ├── AppDatabase.kt       # Inisialisasi Database Room
│   ├── ScanDao.kt           # Data Access Object untuk Query
│   ├── ScanEntity.kt        # Skema Entitas Riwayat Pemindaian
│   └── ScanRepository.kt    # Pola Repositori untuk Abstraksi Data
├── ml/                      # Komponen Machine Learning
│   └── WasteClassifier.kt   # Classifier TFLite (Direct ByteBuffer & Caching)
└── ui/                      # Presentasi & UI/UX Fragments
    ├── DashboardFragment.kt # Statistik & Status Sistem ML
    ├── HistoryFragment.kt   # Tampilan Riwayat Pemindaian (Catatan)
    ├── ResultFragment.kt    # Analisis Klasifikasi & Instruksi Dinamis
    ├── ScanHistoryAdapter.kt# Adapter RecyclerView untuk Catatan
    ├── ScannerFragment.kt   # Antarmuka Pengambilan Gambar (Kamera/Galeri)
    └── MainActivity.kt      # Kontainer Utama Bottom Navigation
```

---

## ⚡ Cara Instalasi & Menjalankan Proyek

### 1. Prasyarat
*   **Android Studio Jellyfish / Koala** atau versi terbaru.
*   **JDK 11 atau JDK 17** terpasang dan terkonfigurasi.
*   HP Fisik Android dengan fitur *USB Debugging* aktif (untuk performa optimal saat penangkapan kamera).

### 2. Kloning Repositori
```bash
git clone https://github.com/username/Nama-Repo-Anda.git
cd Nama-Repo-Anda
```

### 3. Build & Run
1. Buka proyek menggunakan Android Studio.
2. Tunggu proses **Gradle Sync** selesai secara otomatis.
3. Hubungkan perangkat Android fisik Anda via kabel USB / Wi-Fi Debugging.
4. Klik tombol **Run** (ikon segitiga hijau) atau gunakan shortcut `Shift + F10`.

---

## 🛡️ Optimasi Kinerja & Memori

*   **Direct ByteBuffer Preprocessing**: Proses konversi citra masukan `Bitmap` ke array byte langsung (`ByteBuffer`) dialokasikan pada memori native untuk memangkas overhead Garbage Collector (GC).
*   **Memory-Mapped Model**: Menggunakan `FileChannel.map` untuk memetakan file `.tflite` langsung ke ruang memori virtual sistem operasi secara instan.
*   **Bitmap Garbage Recovery**: Objek bitmap jepretan kamera langsung dibersihkan menggunakan metode `.recycle()` setelah inferensi selesai untuk menjamin pemakaian memori perangkat fisik tetap rendah dan mencegah crash *Out of Memory* (OOM).
*   **R8 Minification**: Pengurangan ukuran berkas rilis APK diaktifkan dengan konfigurasi *minifikasi* dan *resource shrinking* yang terlindungi oleh aturan Proguard kustom ([proguard-rules.pro](app/proguard-rules.pro)).
