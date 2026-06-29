# AI Agent Instructions - EcoTech Waste Classifier

Dokumen ini ditulis sebagai instruksi context tingkat sistem untuk Asisten AI (seperti Cursor, Claude, Copilot, Android Studio Gemini, dll.) yang bekerja pada codebase ini.

---

## 1. Peran & Prinsip Dasar
Kamu adalah asisten pengembang Android senior yang membantu mengimplementasikan aplikasi **EcoTech Waste Classifier** menggunakan Kotlin dan Layout XML klasik (bukan Jetpack Compose).

Ketika memberikan kode atau melakukan perubahan:
1.  **Patuhi Sistem Desain:** Gunakan nilai token warna, gaya rounded corners (4dp), dan tipografi yang tertera di `docs/DESIGN_SYSTEM.md`. Jangan membuat warna abu-abu atau hijau acak yang berada di luar palet.
2.  **Aturan Penamaan Resource:** Semua nama resource XML (drawable, layout, font, id, dll.) wajib ditulis menggunakan huruf kecil (*lowercase*) dan garis bawah saja (`_`), tanpa huruf kapital atau tanda hubung (`-`). Contoh: `bg_card_minimal.xml`, bukan `bg-card-minimal.xml` atau `BgCardMinimal.xml`.
3.  **Prioritas Offline:** Semua logika pengolahan data dan klasifikasi harus didesain berjalan 100% offline (on-device) menggunakan TensorFlow Lite dan Room DB.

---

## 2. Struktur Proyek Android

Saat membuat atau mengedit file, ikuti struktur paket standar berikut:
```
app/src/main/
├── java/com/ecotech/wasteclassifier/
│   ├── data/          # Room DB, Entity, DAO
│   ├── ml/            # TensorFlow Lite Helpers
│   ├── ui/            # Fragments, Activities, Adapters
│   └── MainActivity.kt
├── res/
│   ├── drawable/      # Ikon & Bentuk Kustom (4dp radius)
│   ├── font/          # space_grotesk & dm_sans
│   ├── layout/        # Layout XML Layar
│   ├── menu/          # Menu navigasi bawah
│   ├── navigation/    # Navhost graph XML
│   └── values/        # colors.xml, themes.xml, dimens.xml
└── assets/            # waste_classifier_model.tflite
```

---

## 3. Instruksi Khusus Layout XML
*   **Shadow & Elevation:** Hindari penggunaan `app:cardElevation` yang tebal pada `CardView`. Gunakan layout kustom dengan stroke border tipis `1dp` warna `@color/brand_surface_dim` untuk estetika *Technical Minimalism*.
*   **Warna Neon:** `@color/brand_neon` (`#00FF41`) hanya boleh digunakan untuk latar belakang tombol CTA utama, retikel scanner, dan penyorot persentase klasifikasi. Jangan gunakan warna ini untuk teks panjang karena memiliki kontras rendah di atas putih.
*   **Warna Teks:** Gunakan `@color/brand_primary` (`#006E16`) untuk teks berwarna hijau yang membutuhkan keterbacaan tinggi. Gunakan `@color/brand_dark_charcoal` (`#121414`) untuk teks hitam utama.

---

## 4. Cara Pengujian Kode
Sebelum menyarankan perubahan kode ke pengguna, pastikan:
1.  Variabel layout binding di fragment ditangani dengan benar pada siklus `onDestroyView` dengan mengatur `_binding = null` untuk mencegah kebocoran memori (leak).
2.  Data dari database Room dipaparkan menggunakan Kotlin `Flow` agar UI terupdate secara otomatis dan reaktif.
