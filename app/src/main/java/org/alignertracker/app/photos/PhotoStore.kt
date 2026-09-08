package org.alignertracker.app.photos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.alignertracker.app.data.TrackerRepository
import org.alignertracker.app.domain.PhotoMetadata

/** Private normalized JPEG copies, with no original EXIF/location retained. */
class PhotoStore(context: Context, private val repository: TrackerRepository) {
    private val context = context.applicationContext
    private val directory = File(context.filesDir, "photos").apply { mkdirs() }

    private val captureDirectory = File(context.cacheDir, "capture").apply { mkdirs() }
    private val captureState = context.getSharedPreferences("photo-capture", Context.MODE_PRIVATE)

    fun createCapture(): Uri {
        val target = File(captureDirectory, "${UUID.randomUUID()}.jpg")
        check(target.createNewFile()) { "Cannot create a temporary photo." }
        captureState.edit().putString("active", target.name).commit()
        return androidx.core.content.FileProvider.getUriForFile(
            context,
            "org.alignertracker.app.photos",
            target,
        )
    }

    fun finishCapture(uri: Uri) {
        require(uri.authority == "org.alignertracker.app.photos")
        val name = requireNotNull(uri.lastPathSegment)
        require(name.matches(Regex("[0-9a-f-]{36}\\.jpg")))
        if (captureState.getString("active", null) == name)
            captureState.edit().remove("active").commit()
        val target = File(captureDirectory, name)
        check(!target.exists() || target.delete()) {
            "The record is saved, but the temporary photo still needs cleanup. Restart the app to retry."
        }
    }

    fun file(name: String): File {
        require(name.matches(Regex("[0-9a-f-]{36}\\.jpg"))) { "Invalid private photo reference." }
        return File(directory, name)
    }

    suspend fun import(uri: Uri, capturedAt: Long, caption: String = "") =
        withContext(Dispatchers.IO) {
            val temporary = File.createTempFile("photo-import-", ".tmp", context.cacheDir)
            var owned: File? = null
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    temporary.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var size = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            size += read
                            require(size <= 50L * 1024 * 1024) {
                                "Choose an image smaller than 50 MiB."
                            }
                            output.write(buffer, 0, read)
                        }
                    }
                } ?: error("Cannot open the selected photo.")
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(temporary.path, bounds)
                require(bounds.outWidth in 1..40000 && bounds.outHeight in 1..40000) {
                    "Unsupported image or dimensions."
                }
                var sample = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1600) sample *= 2
                val decoded =
                    BitmapFactory.decodeFile(
                        temporary.path,
                        BitmapFactory.Options().apply { inSampleSize = sample },
                    ) ?: error("Cannot decode this photo.")
                val exif = runCatching { ExifInterface(temporary) }.getOrNull()
                val matrix =
                    Matrix().apply {
                        if (exif?.isFlipped == true) postScale(-1f, 1f)
                        postRotate((exif?.rotationDegrees ?: 0).toFloat())
                    }
                val normalized =
                    Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
                owned = file("${UUID.randomUUID()}.jpg")
                try {
                    owned.outputStream().use {
                        require(normalized.compress(Bitmap.CompressFormat.JPEG, 90, it)) {
                            "Could not save the photo."
                        }
                    }
                    val photo =
                        PhotoMetadata(
                            capturedAt = capturedAt,
                            mimeType = "image/jpeg",
                            byteSize = owned.length(),
                            sha256 = sha256(owned.readBytes()),
                            caption = caption,
                            width = normalized.width,
                            height = normalized.height,
                            ownedFileName = owned.name,
                        )
                    repository.addPhoto(photo)
                    owned = null
                } finally {
                    if (normalized !== decoded) normalized.recycle()
                    decoded.recycle()
                }
            } finally {
                temporary.delete()
                owned?.delete()
            }
        }

    suspend fun delete(id: Long) =
        withContext(Dispatchers.IO) {
            repository.deletePhoto(id)
            recover()
        }

    fun deleteOwned(names: List<String>) {
        names.forEach { name ->
            val photo = file(name)
            check(!photo.exists() || photo.delete()) {
                "Records were updated, but a private photo could not be removed. Restart the app or retry Delete data to finish private-file cleanup."
            }
        }
    }

    /** Directory contents remain a durable retry inventory after process death or failed delete. */
    suspend fun recover(removeCaptures: Boolean = false) =
        withContext(Dispatchers.IO) {
            val referenced = repository.snapshot().photos.mapNotNull { it.ownedFileName }.toSet()
            val orphans =
                directory.listFiles()?.filter {
                    it.isFile &&
                        it.name !in referenced &&
                        it.name.matches(Regex("[0-9a-f-]{36}\\.jpg"))
                } ?: emptyList()
            deleteOwned(orphans.map { it.name })
            context.cacheDir
                .listFiles()
                ?.filter { it.name.startsWith("photo-import-") }
                ?.forEach {
                    check(it.delete()) { "Temporary photo cleanup needs a retry. Restart the app." }
                }
            val activeCapture = if (removeCaptures) null else captureState.getString("active", null)
            captureDirectory
                .listFiles()
                ?.filter {
                    it.name != activeCapture ||
                        System.currentTimeMillis() - it.lastModified() > 86_400_000
                }
                ?.forEach {
                    check(it.delete()) {
                        "Temporary camera photo cleanup needs a retry. Restart the app or retry Delete data."
                    }
                }
            if (removeCaptures) captureState.edit().clear().commit()
        }

    companion object {
        fun sha256(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {
                "%02x".format(it)
            }
    }
}
