# Design System - Lumina Tech-Eco (Light Mode)

Dokumen ini mendefinisikan panduan gaya visual aplikasi dan cara mengimplementasikannya ke dalam file konfigurasi XML Android Studio (`colors.xml`, `themes.xml`, `font/`, dan drawable resource).

---

## 1. Palet Warna (`res/values/colors.xml`)

| Nama Desain | Nilai Hex | Penggunaan di Android |
| :--- | :--- | :--- |
| **Primary/Accessible Green** | `#006E16` | Warna teks status, ikon label, status aktif. Memiliki kontras tinggi. |
| **Neon Green** | `#00FF41` | Tombol CTA utama, retikel pemindai, batas indikator aktif. |
| **Deep Charcoal** | `#121414` | Warna teks utama, ikon sekunder, warna teks di tombol neon. |
| **Light Gray Background** | `#F8F9FB` | Latar belakang layar aplikasi (`windowBackground`). |
| **Pure White** | `#FFFFFF` | Latar belakang Card, kontainer layout, panel dialog. |
| **Surface Dim** | `#F3F4F6` | Batas tipis (stroke) 1px pada kartu dan latar kartu non-aktif. |
| **Warning Red** | `#FF3366` | Aksen kategori sampah berbahaya (B3) dan ikon penolakan. |
| **Glass Translucent** | `#B3FFFFFF` | Latar belakang transparan 70% dengan efek blur pada panel scanner. |

### Kode Resource:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="brand_primary">#006E16</color>
    <color name="brand_neon">#00FF41</color>
    <color name="brand_dark_charcoal">#121414</color>
    <color name="brand_bg_light">#F8F9FB</color>
    <color name="brand_surface_white">#FFFFFF</color>
    <color name="brand_surface_dim">#F3F4F6</color>
    <color name="brand_text_secondary">#6B7280</color>
    <color name="brand_warning_red">#FF3366</color>
    <color name="glass_bottom_bar">#B3FFFFFF</color>
</resources>
```

---

## 2. Tipografi & Font (`res/font/`)

Aplikasi ini menggunakan **dual-font** untuk menyeimbangkan karakter eco-tech yang geometris dengan keterbacaan artikel.

1.  **Space Grotesk** (Geometris, Monospace-ish): Digunakan untuk Headline, Judul Toolbar, Judul Kartu, Label Tombol, dan Status Data.
2.  **DM Sans** (Sederhana, Netral): Digunakan untuk deskripsi panjang, poin instruksi, isi tabel, dan teks sekunder.

### Pengaturan Font Family di Android XML:
Untuk menerapkan font, panggil lewat atribut `android:fontFamily` di TextView:
```xml
<!-- Contoh Judul -->
<TextView
    android:fontFamily="@font/space_grotesk_bold"
    android:textSize="24sp"
    android:textColor="@color/brand_dark_charcoal" />

<!-- Contoh Deskripsi -->
<TextView
    android:fontFamily="@font/dm_sans_regular"
    android:textSize="14sp"
    android:textColor="@color/brand_text_secondary" />
```

---

## 3. Sudut Melengkung (Shapes) & Outline
Karakter brand adalah **Technical Minimalism** (minimalis presisi teknik). 
*   **Batas Lengkung (Corner Radius):** Seluruh tombol, kartu (CardView), dan input field wajib menggunakan radius kelengkungan yang konsisten sebesar **`4dp`**. Hindari penggunaan sudut bulat penuh (pill shape/capsule) agar tetap terlihat kokoh seperti alat instrumen laboratorium.
*   **Bayangan (Elevation):** Hindari penggunaan bayangan tebal (shadowless). Gunakan garis batas (**outline/stroke**) setebal **`1dp`** dengan warna `@color/brand_surface_dim` untuk memisahkan kartu dari latar belakang.

---

## 4. Gaya Komponen Kustom (Styles)

Gunakan tema Material Components berikut di `res/values/themes.xml`:

```xml
<resources>
    <!-- Gaya Tombol Utama Neon -->
    <style name="Widget.EcoTech.Button.Neon" parent="Widget.MaterialComponents.Button">
        <item name="backgroundTint">@color/brand_neon</item>
        <item name="android:textColor">@color/brand_dark_charcoal</item>
        <item name="fontFamily">@font/space_grotesk_bold</item>
        <item name="cornerRadius">4dp</item>
        <item name="android:paddingTop">12dp</item>
        <item name="android:paddingBottom">12dp</item>
    </style>

    <!-- Gaya Tombol Sekunder/Outlined -->
    <style name="Widget.EcoTech.Button.Outlined" parent="Widget.MaterialComponents.Button.OutlinedButton">
        <item name="strokeColor">@color/brand_dark_charcoal</item>
        <item name="strokeWidth">1dp</item>
        <item name="android:textColor">@color/brand_dark_charcoal</item>
        <item name="fontFamily">@font/space_grotesk_medium</item>
        <item name="cornerRadius">4dp</item>
        <item name="android:paddingTop">12dp</item>
        <item name="android:paddingBottom">12dp</item>
    </style>
</resources>
```
