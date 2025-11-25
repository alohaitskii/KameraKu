📸 KameraKu – Aplikasi CameraX Jetpack Compose

Aplikasi praktikum untuk mengambil foto menggunakan CameraX, menyimpan ke MediaStore, dan menampilkan thumbnail foto terakhir.
Dibuat menggunakan Jetpack Compose, CameraX 1.3.4, dan MediaStore API Android modern (tanpa WRITE_EXTERNAL_STORAGE).

🧩 Fitur Aplikasi
✅ Level A (Wajib)

Preview kamera secara real-time

Tombol Ambil Foto

Penyimpanan hasil foto ke:

Pictures/KameraKu


Thumbnail foto terakhir ditampilkan di layar

❗ Tidak termasuk (opsional tugas)

Flash/Torch

Switch kamera depan/belakang

VideoCapture

🛠 Teknologi yang Digunakan

Kotlin

Jetpack Compose UI

CameraX API (Preview + ImageCapture)

MediaStore (Scoped Storage)

Activity Result API (permission kamera)

📂 Struktur Project
app/
└── src/main/java/com/example/kameraku/
├── MainActivity.kt
└── CameraScreen.kt   ← fitur utama CameraX

📷 Alur Kerja Aplikasi
1️⃣ Izin Kamera (Runtime Permission)

Menggunakan rememberLauncherForActivityResult:

val launcher = rememberLauncherForActivityResult(
ActivityResultContracts.RequestPermission()
) {}

LaunchedEffect(Unit) {
launcher.launch(Manifest.permission.CAMERA)
}


Saat pertama dijalankan, aplikasi meminta izin CAMERA.
Jika ditolak → preview tidak tampil.

2️⃣ Menampilkan Preview Kamera (CameraX + PreviewView + Compose)

Karena CameraX membutuhkan View klasik, digunakan AndroidView:

AndroidView(factory = {
PreviewView(it).apply {
scaleType = PreviewView.ScaleType.FILL_CENTER
}
})


PreviewView berfungsi untuk menampilkan live camera stream.

3️⃣ Menghubungkan Preview ke CameraX (bindToLifecycle)
provider.bindToLifecycle(
lifecycleOwner,
CameraSelector.DEFAULT_BACK_CAMERA,
preview,
imageCapture
)


bindToLifecycle memastikan:

Kamera aktif saat Activity foreground

Kamera berhenti otomatis saat background

4️⃣ Mengambil Foto (ImageCapture)

Menggunakan ImageCapture.takePicture():

ic.takePicture(
options,
ContextCompat.getMainExecutor(ctx),
object : ImageCapture.OnImageSavedCallback { ... }
)


Mode capture:

ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY


→ cepat, cocok untuk foto tanpa mode HDR.

5️⃣ Menyimpan Foto ke MediaStore

Tidak butuh WRITE_EXTERNAL_STORAGE karena Android modern pakai Scoped Storage.

ContentValues().apply {
put(DISPLAY_NAME, fileName)
put(MIME_TYPE, "image/jpeg")
put(RELATIVE_PATH, "Pictures/KameraKu")
}


Nama file:

IMG_<timestamp>.jpg


Folder otomatis dibuat jika belum ada.

6️⃣ Menampilkan Thumbnail Hasil Foto Terakhir
val bmp = BitmapFactory.decodeStream(
ctx.contentResolver.openInputStream(uri)
)


Ditampilkan dengan:

Image(bitmap = bmp.asImageBitmap(), ...)

7️⃣ MediaScanner Agar Foto Muncul di Galeri

Karena beberapa aplikasi galeri (termasuk Google Photos di emulator) lambat mengindex file, digunakan:

MediaScannerConnection.scanFile(
ctx,
arrayOf(uri.path),
arrayOf("image/jpeg"),
null
)


Ini memastikan foto muncul dalam daftar MediaStore.

Screenshot:
![Screenshot](https://github.com/alohaitskii/KameraKu/blob/master/screenshots/%7B1029078B-49B1-4305-8A1D-4AB5DBB814DE%7D.png)
![Screenshot](https://github.com/alohaitskii/KameraKu/blob/master/screenshots/%7BA4B7AFA6-057F-4D9D-A195-E05C25DF8500%7D.png)


🚀 Cara Menjalankan

Jalankan aplikasi di emulator atau device fisik

Berikan izin kamera

Tekan tombol Ambil Foto

Lihat thumbnail hasil foto di pojok kanan atas

File dapat ditemukan di:

Device File Explorer → sdcard/Pictures/KameraKu/


atau aplikasi Galeri (setelah MediaScanner)
