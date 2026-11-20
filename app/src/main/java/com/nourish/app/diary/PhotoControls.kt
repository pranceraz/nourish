package com.nourish.app.diary

import android.graphics.BitmapFactory
import androidx.core.net.toUri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import com.nourish.app.ui.Sticker
import com.nourish.app.ui.StickerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.nourish.app.ui.theme.*
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun PhotoControls(
    images: MealImages, draft: String?, saved: ByteArray?, picking: Boolean,
    onPhoto: (String?) -> Unit, onBarcode: (String) -> Unit,
    onWorking: (Boolean) -> Unit,
) {
    var cameraPath by rememberSaveable { mutableStateOf<String?>(null) }
    var scanning by rememberSaveable { mutableStateOf(false) }
    var pendingUri by rememberSaveable { mutableStateOf<String?>(null) }
    var message by rememberSaveable { mutableStateOf("") }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null || uri.scheme == "content") pendingUri = uri?.toString()
        else message = "Choose an image from your photo library."
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val capture = cameraPath?.let(::File)
        if (ok && capture != null) pendingUri = images.cameraUri(capture).toString()
        else { capture?.delete(); cameraPath = null }
    }
    fun launchCamera(scan: Boolean) {
        message = ""
        scanning = scan
        try {
            val file = images.cameraFile()
            cameraPath = file.absolutePath
            camera.launch(images.cameraUri(file))
        } catch (_: Exception) {
            cameraPath?.let(::File)?.delete()
            cameraPath = null
            message = "Camera unavailable. You can choose a photo or enter the label manually."
        }
    }
    LaunchedEffect(pendingUri) {
        val input = pendingUri ?: return@LaunchedEffect
        onWorking(true)
        try {
            val uri = input.toUri()
            if (scanning) {
                val code = withContext(Dispatchers.IO) { images.scan(uri) }
                if (code == null) message = "Couldn’t read the barcode. Try again in good light or type its digits."
                else onBarcode(code)
            } else {
                val name = withContext(Dispatchers.IO) { images.importPhoto(uri) }
                onPhoto(name)
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            message = "Couldn’t open that image. Choose another or continue manually."
        }
        cameraPath?.let(::File)?.delete()
        cameraPath = null
        pendingUri = null
        onWorking(false)
    }
    val preview by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, draft, saved) {
        value = withContext(Dispatchers.IO) {
            when {
                draft != null -> BitmapFactory.decodeFile(images.draftFile(draft).absolutePath)
                saved != null -> BitmapFactory.decodeByteArray(saved, 0, saved.size)
                else -> null
            }?.asImageBitmap()
        }
    }
    val ink = nourishColors.ink
    val buttons = ButtonDefaults.outlinedButtonColors(contentColor = ink)
    Column(Modifier.fillMaxWidth().clip(NourishShape.panel).background(nourishColors.blue).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StickerShape(Sticker.Petal, ink, Modifier.size(30.dp))
            Text("Meal photo", style = NourishType.heading, color = ink)
        }
        preview?.let { Image(it, "Meal photo", Modifier.fillMaxWidth().height(154.dp).clip(NourishShape.panel),
            contentScale = ContentScale.Fit) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(colors = buttons, enabled = pendingUri == null, onClick = { launchCamera(false) }) { Text("Take photo") }
            OutlinedButton(colors = buttons, enabled = pendingUri == null, onClick = {
                scanning = false
                message = ""
                try { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                catch (_: Exception) { message = "Photo picker unavailable. You can take a photo or continue manually." }
            }) { Text("Choose photo") }
        }
        if (saved != null || draft != null) TextButton(colors = buttons, onClick = { onPhoto(null) }) { Text("Remove photo") }
        if (picking) TextButton(colors = buttons, enabled = pendingUri == null, onClick = { launchCamera(true) }) { Text("Scan package barcode") }
        if (pendingUri != null) Text(if (scanning) "Reading barcode…" else "Preparing photo…", color = ink)
        if (message.isNotEmpty()) Text(message, color = ink)
    }
}
