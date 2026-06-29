# Product Requirements Document (PRD) - EcoTech Waste Classifier

## 1. Ringkasan Produk
**EcoTech Waste Classifier** adalah aplikasi seluler *machine learning* (pembelajaran mesin) *on-device* dan *offline* yang secara instan mengklasifikasikan sampah organik, anorganik, dan berbahaya (B3). Aplikasi ini memberdayakan pengguna untuk membuat keputusan daur ulang yang cepat dan akurat dengan antarmuka yang bersih, cerah, dan kontras tinggi.

*   **Target Pengguna:** Individu peduli lingkungan, pekerja fasilitas pemilahan, dan pendukung gerakan *zero-waste*.
*   **Platform:** Android Mobile (Native Kotlin & XML).
*   **Konektivitas:** 100% Offline (tidak membutuhkan koneksi internet untuk pemindaian).

---

## 2. Fitur Utama & Kebutuhan Fungsional

### A. Klasifikasi Sampah Real-Time (On-Device ML)
*   Menggunakan kamera belakang perangkat secara real-time.
*   Mengklasifikasikan objek sampah ke dalam 3 kategori:
    1.  **ORGANIK:** Sisa makanan, dedaunan, bahan mudah terurai.
    2.  **ANORGANIK:** Plastik, botol, kertas, kardus, kaleng.
    3.  **BERBAHAYA (B3):** Baterai bekas, lampu bohlam, kemasan pestisida, limbah medis.
*   Tingkat kepercayaan (confidence rating) ditampilkan dalam persentase (%).

### B. Penyimpanan Riwayat Pemindaian (Room Database)
*   Pengguna dapat menyimpan hasil pemindaian yang sukses ke halaman "Catatan".
*   Data riwayat meliputi: gambar yang ditangkap, kategori sampah, tingkat kepercayaan, dan stempel waktu (*timestamp*).

### C. Basis Data Edukasi Pemilahan Sampah
*   Panduan pemilahan sampah offline yang dapat diakses dengan cepat melalui menu Accordion/Expandable List.
*   Membantu pengguna memahami tindakan penanganan khusus (misalnya: bilas botol plastik sebelum dibuang).

---

## 3. Spesifikasi Layar & Alur Pengguna (Flow)

```
[ Dasbor Utama ] --(Tombol "Mulai Pemindai")--> [ Pemindai Real-time ]
                                                        |
                                                  (Ambil Foto)
                                                        |
                                                        v
[ Catatan & Pelajari ] <--(Klik "Simpan")------- [ Hasil Analisis ]
```

1.  **Dasbor Utama (Dashboard):**
    *   Menampilkan ringkasan statistik harian (Total Pemindaian, Organik, Anorganik, Berbahaya).
    *   Menampilkan status sistem ML (`SYSTEM ACTIVE` atau `STANDBY`).
    *   Tombol pemicu utama untuk membuka pemindai kamera.
2.  **Pemindai Real-time (Scanner):**
    *   Tampilan penuh kamera dengan retikel pembingkai hijau neon di tengah.
    *   Tombol kontrol di bagian bawah (Galeri, Ambil Foto/Shutter, Flash).
3.  **Hasil Analisis (Analysis Result):**
    *   Menampilkan foto yang diambil, kategori klasifikasi (misal: "ORGANIK"), dan persentase akurasi.
    *   Instruksi penanganan detail (Tindakan yang disarankan dan pengecualian/pantangan).
    *   Tombol opsi: "Pindai Lainnya" (kembali ke kamera) atau "Simpan Ke Catatan".
4.  **Catatan & Pelajari (History & Learn):**
    *   Tab "Catatan": Daftar riwayat scan yang berhasil disimpan.
    *   Tab "Basis Data": Daftar accordion panduan jenis sampah yang rapi.
