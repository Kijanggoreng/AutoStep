# AutoStep - Auto Clicker / Macro untuk Android

App Android (Kotlin, tanpa root) buat otomatisasi **step by step**: tap, tap random, hold (bisa selamanya),
swipe, input keyboard (huruf / angka / simbol, fixed atau random, bisa di-hold), semua dengan jeda
per step dan tampil **mengambang di atas app lain**.

> Dibuat buat auto-farm game. Cek aturan (ToS) game kamu dulu - banyak game melarang automation
> dan bisa banned.

---

## Fitur

| Fitur | Keterangan |
|---|---|
| Tap | Koordinat X,Y + optional random offset +-px |
| Random tap | Tap di titik acak dalam area (kotak) yang kamu gambar |
| Hold | Tahan jari di satu titik: durasi tetap, **random range**, atau **forever** (sampai Stop) |
| Swipe | Dari titik A ke B, durasi bisa diatur |
| Keyboard key | 1 tombol: fixed (`a`, `A`, `5`, `@`, `ENTER`, `LEFT`, `F5`...) atau **random** huruf / angka / simbol / any |
| Hold key | Contoh: step 68 = tahan `A` selama 38 detik (`38000` ms) atau forever |
| Type text | Ketik teks per karakter |
| Wait | Diam sekian ms (bisa random range) |
| Per step | Repeat xN, jeda setelah step (fixed / random range), enable/disable |
| Script | Banyak script, loop N kali atau 0 = infinite, start delay (waktu pindah ke game) |
| Floating panel | Tombol Run/Stop mengambang + rekam step dengan tap langsung di layar game |
| Picker | Layar transparan: tap titik / gambar kotak / drag swipe -> koordinat otomatis |

---

## Cara pakai (di HP)

### 1. Install APK
Lihat bagian **Cara dapat APK** di bawah.

Android 13+: kalau tombol Accessibility abu-abu ("Restricted setting"), buka
**Settings > Apps > AutoStep > titik tiga (kanan atas) > Allow restricted settings**, lalu ulangi.

### 2. Setup (kartu "Setup" di app)
1. **Accessibility service** -> Settings -> cari **AutoStep** -> ON. (Dipakai buat tap/hold/swipe + floating panel.)
2. **Keyboard** (hanya kalau pakai step Key/Text):
   - `Enable` -> nyalakan **AutoStep Keyboard**.
   - `Select` -> pilih **AutoStep Keyboard** jadi keyboard aktif.
3. Floating panel muncul otomatis setelah accessibility ON (`Show/Hide` di app).

### 3. Bikin script
Dua cara, boleh dicampur:

**A. Rekam langsung di game (paling gampang)**
1. Buka game. Di floating panel tekan `...` -> panel terbuka.
2. `+Tap` / `+Random` / `+Hold` / `+Swipe` -> layar jadi gelap transparan -> tap/gambar di posisi yang kamu mau.
3. Step otomatis masuk ke script. Edit detailnya (durasi, jeda, dll) di app AutoStep.

**B. Manual di app**
`+ Add step` -> pilih tipe -> isi angka, atau `Pick on screen (5s)` (kamu punya 5 detik pindah ke game,
lalu picker muncul).

Klik sebuah step buat edit. Tombol di kanan step: `^` naik, `v` turun, `++` duplikat, `x` hapus.

### 4. Jalankan
- Tekan **Run script** (di app) atau **Run** (floating). Setelah *Start delay* detik, script jalan.
- **Stop** kapan saja dari floating panel.
- `Loops = 0` -> ulang terus sampai Stop. Cocok buat farming.

### Contoh script farming
1. Tap (tombol serang) - jeda 200-400 ms (random)
2. Random tap di area musuh - repeat x5
3. Hold `A` 38000 ms (jalan ke kiri)
4. Swipe (kamera) 300 ms
5. Type text `/attack` + Key `ENTER`
Loops = 0.

---

## Cara dapat APK (karena kamu belum compile)

### Opsi 1 - GitHub Actions (tanpa install apa pun di PC)
1. Buat repo baru di github.com, upload **isi** folder project ini (termasuk folder `.github`).
2. Tab **Actions** -> workflow **Build APK** -> tunggu +-3-5 menit (atau `Run workflow`).
3. Buka run yang hijau -> **Artifacts** -> download `AutoStep-debug-apk` -> extract -> `app-debug.apk`.
4. Kirim ke HP, install (izinkan "install from unknown sources").

### Opsi 2 - Android Studio (disarankan kalau mau sering modif)
1. Install **Android Studio** (Koala / terbaru). Pastikan **JDK 17** (bawaan Studio sudah ok).
2. `File > Open` -> pilih folder `AutoStep`.
   Kalau Studio minta Gradle: pilih Gradle **8.7** (atau "Use default"), lalu **Sync**.
3. Colok HP (USB debugging ON) -> tombol **Run**; atau
   `Build > Build Bundle(s) / APK(s) > Build APK(s)`.
4. Hasil: `app/build/outputs/apk/debug/app-debug.apk`

### Opsi 3 - Command line
Prasyarat: JDK 17, Android SDK (platform 34, build-tools 34.0.0), env `ANDROID_HOME` diset,
Gradle 8.7+ terinstall.

```bash
# sekali saja: bikin gradle wrapper
gradle wrapper --gradle-version 8.7

# build APK debug
./gradlew assembleDebug
# hasil: app/build/outputs/apk/debug/app-debug.apk

# install ke HP (adb)
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
Build release (tetap ditandatangani debug key biar bisa langsung diinstall): `./gradlew assembleRelease`.

### Compile ulang setelah modif
Ubah kode di `app/src/main/java/com/autostep/app/` -> jalankan lagi salah satu opsi di atas.
Naikkan `versionCode` di `app/build.gradle.kts` kalau mau update tanpa uninstall.
APK debug dengan debug key yang sama bisa di-install menimpa versi lama.

---

## Struktur project

```
AutoStep/
  README.md   CLAUDE.md
  settings.gradle.kts  build.gradle.kts  gradle.properties
  .github/workflows/build.yml          # build APK otomatis di GitHub
  app/
    build.gradle.kts                   # minSdk 26, targetSdk 34, Kotlin 1.9.24, AGP 8.5.2
    src/main/AndroidManifest.xml
    src/main/res/xml/accessibility_config.xml   # canPerformGestures=true
    src/main/res/xml/method.xml                 # deklarasi IME
    src/main/java/com/autostep/app/
      MainActivity.kt            # UI utama + editor step (dibuat programatik, tanpa XML layout)
      AutoAccessibilityService.kt# dispatchGesture: tap, hold (chained stroke), swipe
      FloatingUi.kt              # panel mengambang + PickerView (pilih koordinat)
      AutoKeyboardService.kt     # IME: inject key event & teks via InputConnection
      Keys.kt                    # KeyInjector: random key, hold key, shift/simbol
      Engine.kt                  # eksekusi script (thread), loop, jeda, random
      Model.kt                   # Step, Script, enum, JSON
      Store.kt                   # simpan script ke filesDir/scripts.json
      Ui.kt                      # warna & helper view
```

## Cara kerja (singkat)
- **Tap/Hold/Swipe**: `AccessibilityService.dispatchGesture`. Hold panjang/infinite dikirim sebagai
  rantai stroke 1 detik dengan `willContinue=true` jadi jari tidak "lepas" sampai selesai / Stop.
- **Floating panel**: window `TYPE_ACCESSIBILITY_OVERLAY` (tidak perlu izin "Display over other apps").
- **Keyboard**: `InputMethodService` sendiri. `InputConnection.sendKeyEvent` kirim `ACTION_DOWN`
  (diulang dengan `repeatCount++` selama hold) lalu `ACTION_UP`. Huruf besar/simbol dipetakan lewat
  `KeyCharacterMap` (otomatis tambah SHIFT).

---

## Batasan penting (jujur)
1. **Key/Text hanya masuk ke app yang punya input field fokus** dan sedang memakai AutoStep Keyboard.
   Game yang tidak punya text field / tidak baca key event (kebanyakan game touch) **tidak akan
   merespon** tombol "A" hold. Kalau game kamu pakai kontrol touch (joystick virtual), pakai step **Hold**
   di posisi joystick / **Swipe** - bukan Key. Key cocok untuk: chat/ketik command, game dengan dukungan
   keyboard (mis. di emulator / Chromebook / HP + keyboard).
2. Tap di titik yang tertutup floating panel akan kena panel - geser panel (drag `::`).
3. Game dengan anti-cheat bisa mendeteksi accessibility/auto-click.
4. Beberapa app (bank, layar secure) memblokir gesture/overlay.
5. Hemat baterai/OEM (Xiaomi, Oppo, Vivo, Samsung) kadang mematikan service: set Battery -> *No restrictions*
   dan kunci app di Recents.
6. Layar tetap menyala selama floating panel tampil (flag keep-screen-on).

## Troubleshooting
| Masalah | Solusi |
|---|---|
| Accessibility tidak bisa di-ON | Allow restricted settings (lihat atas) |
| Tap tidak kena | Resolusi/rotasi beda dari saat rekam; rekam ulang. Coordinate = piksel layar penuh |
| Key tidak jalan | Pastikan "Keyboard selected" ON dan field fokus (keyboard bar AutoStep muncul) |
| Setelah Pick, app tidak balik sendiri | Android blok buka app dari background: tekan `Open app` di floating panel |
| Script berhenti sendiri | Lihat toast error; atau service dimatikan OEM |

## Referensi yang dipakai
- eci99/Clicker - overlay + `dispatchGesture` (github.com/eci99/Clicker)
- Kiyoscripts/CosmoClicker - interval/loop/preset (github.com/Kiyoscripts/CosmoClicker)
- ryanuo/mdot - multi-point floating clicker (github.com/ryanuo/mdot)
- kemw/android-mouse-cursor - accessibility + overlay (github.com/kemw/android-mouse-cursor)
- Android docs "Create an input method" - `InputConnection.sendKeyEvent`
- rustdesk PR #6097 - contoh kirim `KeyEvent` lewat IME `InputConnection`

### Claude skills untuk Android (buat dipakai di Claude Code)
- Drjacky/claude-android-ninja - Kotlin + Compose
- dpconde/claude-android-skill - arsitektur ala NowInAndroid
- chrisbanes/skills - skill Kotlin/Compose/Android
- android/skills (resmi Google) lewat Android CLI, atau `claude mcp add android-skills -- npx -y android-skills-mcp`
- Lightrion/skills - skill AOSP/Android app development

Pasang (contoh): `git clone https://github.com/Drjacky/claude-android-ninja ~/.claude/skills/claude-android-ninja`
(atau taruh di `.claude/skills/` di dalam project ini).
