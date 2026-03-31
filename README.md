# Dual Screen POS Application

Aplikasi Android untuk mesin POS C9 dengan dua layar yang menampilkan URL aplikasi POS dan Customer Display secara bersamaan.

## Fitur

- **Dual Screen Support**: Menampilkan konten berbeda di dua layar (utama dan sekunder)
- **Konfigurasi Dinamis**: URL dapat dikonfigurasi melalui aplikasi tanpa perlu rebuild
- **Local IP Support**: Mendukung akses ke server lokal (HTTP)
- **Hide Keyboard Option**: Opsi untuk menyembunyikan native keyboard Android
- **Bluetooth Support**: Mendukung printer Bluetooth untuk printing

## Spesifikasi

### Layar Utama (Primary Display)
- Menampilkan aplikasi POS (URL dikonfigurasi)
- Default: `http://192.168.5.21/esb-fnb-pos/en/login`

### Layar Kedua (Secondary Display)  
- Menampilkan Customer Display (URL dikonfigurasi)
- Default: `http://192.168.5.21/esb-fnb-pos/en/customer-display`

## Cara Penggunaan

### Pertama Kali Install
1. Buka aplikasi
2. Masukkan URL untuk aplikasi POS (layar utama)
3. Masukkan URL untuk Customer Display (layar kedua)
4. Centang "Sembunyikan Native Keyboard" jika ingin menyembunyikan keyboard
5. Klik "Simpan & Mulai"
6. Izinkan permissions Bluetooth jika diminta

### Print via Bluetooth
- Aplikasi sudah memiliki permissions Bluetooth yang diperlukan
- Printer Bluetooth dapat terhubung melalui sistem Android
- Pastikan Bluetooth printer sudah paired dengan device

### Mengubah Konfigurasi
- Hapus data aplikasi untuk reset konfigurasi
- Atau modify kode untuk menambahkan menu konfigurasi

## Konfigurasi IP Address

Jika menggunakan IP address lokal, sudah support:
- 192.168.0.x
- 192.168.1.x
- 192.168.5.x
- 10.0.0.x
- localhost

Untuk menambah range IP lain, edit file:
`app/src/main/res/xml/network_security_config.xml`

## Teknologi

- Android (Kotlin)
- WebView untuk menampilkan web app
- Presentation API untuk dual screen
- SharedPreferences untuk storage konfigurasi
- Bluetooth API untuk printing

## Build

```
bash
./gradlew assembleDebug
```

## Lisensi

MIT License
