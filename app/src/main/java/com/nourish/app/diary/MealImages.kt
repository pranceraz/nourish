package com.nourish.app.diary

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.oned.UPCEReader
import java.io.File
import java.io.IOException

class MealCameraProvider : FileProvider()

internal class MealImages(private val context: Context) {
    private val cameraDir get() = File(context.cacheDir, "meal-camera").apply { mkdirs() }
    private val draftDir get() = File(context.cacheDir, "meal-drafts").apply { mkdirs() }

    fun cameraFile() = File.createTempFile("capture-", ".jpg", cameraDir)
    fun cameraUri(file: File): Uri = FileProvider.getUriForFile(context, "${context.packageName}.camera", file)
    fun draftFile(name: String) = File(draftDir, name)
    fun prune() {
        val cutoff = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        listOf(cameraDir, draftDir).forEach { dir ->
            dir.listFiles()?.filter { it.lastModified() < cutoff }?.forEach(File::delete)
        }
    }

    private fun open(uri: Uri) = context.contentResolver.openInputStream(uri) ?: throw IOException("Image unavailable")

    fun decode(uri: Uri, longestSide: Int): Bitmap {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open(uri).use { BitmapFactory.decodeStream(it, null, options) }
        val longest = maxOf(options.outWidth, options.outHeight)
        if (longest <= 0) throw IOException("Unreadable image")
        options.inSampleSize = 1
        while (longest / options.inSampleSize > longestSide) options.inSampleSize *= 2
        options.inJustDecodeBounds = false
        val bitmap = open(uri).use { BitmapFactory.decodeStream(it, null, options) } ?: throw IOException("Unreadable image")
        val exif = open(uri).use { ExifInterface(it) }
        val matrix = Matrix().apply {
            if (exif.isFlipped) postScale(-1f, 1f)
            postRotate(exif.rotationDegrees.toFloat())
        }
        if (matrix.isIdentity) return bitmap
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
            if (it !== bitmap) bitmap.recycle()
        }
    }

    fun importPhoto(uri: Uri): String {
        val bitmap = decode(uri, 960)
        val file = File.createTempFile("meal-", ".jpg", draftDir)
        try {
            file.outputStream().use { if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 78, it)) throw IOException() }
            require(file.length() in 1..900_000)
            return file.name
        } catch (e: Exception) {
            file.delete()
            throw e
        } finally { bitmap.recycle() }
    }

    fun scan(uri: Uri): String? {
        val bitmap = decode(uri, 2048)
        try {
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            val gray = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
            val hints = mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.EAN_13, BarcodeFormat.EAN_8, BarcodeFormat.UPC_A, BarcodeFormat.UPC_E),
                DecodeHintType.TRY_HARDER to true,
            )
            fun read(source: LuminanceSource): String? = try {
                val result = MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(source)), hints)
                productCode(if (result.barcodeFormat == BarcodeFormat.UPC_E) UPCEReader.convertUPCEtoUPCA(result.text) else result.text)
            } catch (_: NotFoundException) { null }
            read(gray)?.let { return it }
            // Retail barcodes may be vertical in the camera frame.
            val rotated = IntArray(pixels.size)
            for (y in 0 until bitmap.height) for (x in 0 until bitmap.width) {
                rotated[x * bitmap.height + bitmap.height - 1 - y] = pixels[y * bitmap.width + x]
            }
            return read(RGBLuminanceSource(bitmap.height, bitmap.width, rotated))
        } finally { bitmap.recycle() }
    }
}
