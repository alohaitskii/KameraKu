package com.example.kameraku

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import android.view.Surface



// ==========================================================
// PREVIEW VIEW (Compose)
// ==========================================================
@Composable
fun CameraPreview(onReady: (PreviewView) -> Unit) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                post { onReady(this) }
            }
        }
    )
}


// ==========================================================
// BIND PREVIEW
// ==========================================================
suspend fun bindPreview(
    ctx: Context,
    owner: LifecycleOwner,
    view: PreviewView
): Triple<Preview, Camera, ProcessCameraProvider> {

    val provider = suspendCancellableCoroutine<ProcessCameraProvider> { cont ->
        val future = ProcessCameraProvider.getInstance(ctx)
        future.addListener(
            { cont.resume(future.get()) },
            ContextCompat.getMainExecutor(ctx)
        )
    }

    val preview = Preview.Builder()
        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
        .build()
        .also { it.setSurfaceProvider(view.surfaceProvider) }

    val selector = CameraSelector.DEFAULT_BACK_CAMERA

    provider.unbindAll()
    val camera = provider.bindToLifecycle(owner, selector, preview)

    return Triple(preview, camera, provider)
}


// ==========================================================
// OUTPUT MediaStore
// ==========================================================
fun outputOptions(ctx: Context, name: String): ImageCapture.OutputFileOptions {
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/KameraKu")
    }

    val resolver = ctx.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!

    return ImageCapture.OutputFileOptions.Builder(resolver, uri, values).build()
}


// ==========================================================
// MAIN CAMERA SCREEN (LEVEL A) — FIXED
// ==========================================================
@Composable
fun CameraScreen() {

    val ctx = LocalContext.current
    val owner = LocalLifecycleOwner.current

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var lastUri by remember { mutableStateOf<Uri?>(null) }

    // -----------------------------
    // Permission Kamera
    // -----------------------------
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.CAMERA)
    }

    // -----------------------------
    // UI Preview Kamera
    // -----------------------------
    Box(modifier = Modifier.fillMaxSize()) {

        CameraPreview { pv -> previewView = pv }

        // -----------------------------
        // Bind CameraX saat preview siap
        // -----------------------------
        LaunchedEffect(previewView) {
            val pv = previewView ?: return@LaunchedEffect

            val (preview, camera, provider) = bindPreview(ctx, owner, pv)

            val ic = ImageCapture.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            provider.unbindAll()
            provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, ic)

            // orientasi supaya foto tidak miring
            ic.targetRotation = pv.display?.rotation ?: Surface.ROTATION_0

            imageCapture = ic
        }

        // -----------------------------
        // Tombol Ambil Foto
        // -----------------------------
        Button(
            onClick = {
                val ic = imageCapture ?: return@Button
                val opt = outputOptions(ctx, "IMG_${System.currentTimeMillis()}")

                ic.takePicture(
                    opt,
                    ContextCompat.getMainExecutor(ctx),
                    object : ImageCapture.OnImageSavedCallback {

                        override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                            val savedUri = result.savedUri ?: return

                            lastUri = savedUri

                            // -----------------------------------
                            // FIX: Scan file agar muncul di Galeri
                            // -----------------------------------
                            MediaScannerConnection.scanFile(
                                ctx,
                                arrayOf(savedUri.path),
                                arrayOf("image/jpeg"),
                                null
                            )
                        }

                        override fun onError(error: ImageCaptureException) {
                            error.printStackTrace()
                        }
                    }
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
        ) {
            Text("Ambil Foto")
        }

        // -----------------------------
        // Thumbnail Foto Terakhir
        // -----------------------------
        lastUri?.let { uri ->
            val bmp = remember(uri) {
                val stream = ctx.contentResolver.openInputStream(uri)
                android.graphics.BitmapFactory.decodeStream(stream)
            }
            bmp?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                )
            }
        }
    }
}
