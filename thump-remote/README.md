# Thump Remote — remote IR untuk head unit Thump TP‑6911 (52W×4)

Aplikasi Android yang mengubah HP ber‑**IR blaster** menjadi remote untuk head unit
Thump TP‑6911 (double‑DIN MP5 6,95"). Head unit ini punya jendela penerima IR di
pojok kiri atas layar (tulisan "IR"), jadi aplikasi mengirim sinyal inframerah persis
seperti remote bawaannya.

> **Syarat:** HP harus punya pemancar inframerah (IR blaster), misalnya Xiaomi / Redmi /
> POCO, Huawei / Honor, dan sebagian Oppo / Realme. HP tanpa IR blaster tidak bisa
> dipakai — aplikasi akan memberi tahu di layar utama.

## Pasang

1. Unduh `apk/ThumpRemote-1.0-release.apk` dari folder ini (atau artefak
   *ThumpRemote-apk* dari tab **Actions** di GitHub untuk build terbaru).
2. Buka file APK di HP → izinkan "pasang dari sumber tidak dikenal" bila diminta.
3. Buka **Thump Remote**, arahkan bagian atas HP ke jendela IR head unit.

## Cara kerja kode

Head unit MP5 generik seperti ini memakai protokol **NEC** (38 kHz). Aplikasi datang
dengan preset *Generik MP5 (NEC alamat 0x00)* — kode remote 21 tombol yang paling
umum dipakai pabrikan unit MP3/MP5 mobil murah. Kode ini **belum tentu cocok** dengan
TP‑6911; tidak ada daftar kode resminya yang dipublikasikan.

Kalau tombol tidak merespons, pakai menu **Cari Kode**:

1. Pilih alamat NEC (mulai dari `0x00`, lalu `0x80`, `0xFF`, …).
2. Opsional: pilih tombol tujuan (mis. *Vol +*) supaya hasil langsung tersimpan.
3. Tekan **Mulai Scan**. Aplikasi mengirim kode `00`…`FF` satu per satu (jeda bisa diatur).
4. Begitu head unit bereaksi (volume berubah, mute, ganti lagu, dll.) tekan **ITU DIA!**
5. Empat kode terakhir muncul sebagai kandidat. Tekan **Kirim** untuk memastikan mana
   yang benar, lalu **Pakai** untuk menyimpannya ke tombol.
6. Setelah satu kode ketemu, alamatnya sudah pasti benar — ulangi scan pada alamat yang
   sama untuk tombol lain (kolom *Mulai dari kode* otomatis lanjut dari kode terakhir).

Semua kode bisa dilihat/diubah di **Pengaturan**, termasuk:

- alamat bawaan untuk seluruh tombol,
- kode per tombol (NEC alamat + perintah, atau pola RAW dalam mikrodetik),
- **Salin semua kode** → teks di clipboard yang bisa disimpan/dibagikan,
- **Tempel & impor** → memasukkan kembali teks tersebut (format `TOMBOL=nec:AAAA:CC`).

Alamat 4 digit hex berarti NEC *extended*; byte yang dikirim lebih dulu ditulis di
belakang (contoh dari Flipper `address: 86 6B` → tulis `6B86`).

## Build sendiri

Project Gradle biasa, Java murni tanpa dependensi AndroidX.

```bash
cd thump-remote
# butuh Android SDK (platform 34, build-tools 34) dan JDK 17+
gradle assembleRelease
# hasil: app/build/outputs/apk/release/ThumpRemote-1.0-release.apk
```

Keystore `keystore/thump-remote.jks` (sandi `thumpremote`, alias `thump`) sengaja ikut
di repo agar build lokal dan GitHub Actions menghasilkan tanda tangan yang sama,
sehingga update bisa dipasang tanpa uninstall. Aplikasi tidak menyimpan data sensitif.

## Struktur

```
app/src/main/java/id/linktv/thumpremote/
  MainActivity.java      layar remote (tata letak dibangun lewat kode)
  ScanActivity.java      pencari kode NEC
  SettingsActivity.java  daftar & editor kode, ekspor/impor
  IrSender.java          pembungkus ConsumerIrManager
  Code.java              representasi kode (NEC / RAW) + pembuat pola NEC
  Keys.java / KeyDef.java daftar tombol & kode bawaan preset
  CodeStore.java         penyimpanan SharedPreferences
```
