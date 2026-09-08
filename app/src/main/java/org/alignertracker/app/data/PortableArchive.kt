package org.alignertracker.app.data

import android.graphics.BitmapFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.alignertracker.app.domain.TrackerSnapshot
import org.alignertracker.app.photos.PhotoStore

/** Bounded logical archive. File names from a zip are never used as filesystem paths. */
object PortableArchive {
    data class Inspected(val snapshot: TrackerSnapshot, val photos: Map<Long, ByteArray>)

    fun encode(snapshot: TrackerSnapshot, store: PhotoStore): ByteArray {
        val output = ByteArrayOutputStream()
        var size = 0L
        ZipOutputStream(output).use { zip ->
            fun entry(name: String, bytes: ByteArray) {
                size += bytes.size
                require(size <= EncryptedBackup.MAX_PLAINTEXT_BYTES) {
                    "Backup content exceeds 32 MiB. Export or remove photos before retrying."
                }
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
            entry("records.json", BackupCodec.encode(snapshot).toByteArray())
            snapshot.photos.forEach { photo ->
                val bytes =
                    store
                        .file(
                            requireNotNull(photo.ownedFileName) {
                                "A photo is missing. Restore its complete archive or delete its entry before exporting."
                            }
                        )
                        .readBytes()
                require(
                    bytes.size.toLong() == photo.byteSize &&
                        PhotoStore.sha256(bytes) == photo.sha256
                ) {
                    "A private photo is missing or damaged. Existing records are unchanged."
                }
                entry("photos/${photo.id}.jpg", bytes)
            }
        }
        return output.toByteArray().also {
            require(it.size <= EncryptedBackup.MAX_PLAINTEXT_BYTES) { "Backup exceeds 32 MiB." }
        }
    }

    fun inspect(bytes: ByteArray): Inspected {
        require(bytes.size <= EncryptedBackup.MAX_PLAINTEXT_BYTES) { "Backup exceeds 32 MiB." }
        if (bytes.size < 2 || bytes[0] != 0x50.toByte() || bytes[1] != 0x4b.toByte()) {
            val text =
                Charsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString()
            val snapshot = BackupCodec.decode(text)
            require(snapshot.photos.isEmpty()) {
                "This JSON references photos. Import the complete portable archive instead."
            }
            return Inspected(snapshot, emptyMap())
        }
        val entries = linkedMapOf<String, ByteArray>()
        var total = 0L
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(
                    !entry.isDirectory &&
                        (entry.name == "records.json" ||
                            entry.name.matches(Regex("photos/[1-9][0-9]{0,18}\\.jpg")))
                ) {
                    "Unexpected archive entry."
                }
                require(entries.size < 1001 && entry.name !in entries) {
                    "Duplicate or excessive archive entries."
                }
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = zip.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= EncryptedBackup.MAX_PLAINTEXT_BYTES) {
                        "Expanded archive exceeds 32 MiB."
                    }
                    require(
                        output.size() + count <=
                            if (entry.name == "records.json") 5 * 1024 * 1024 else 10 * 1024 * 1024
                    ) {
                        "Archive entry is too large."
                    }
                    output.write(buffer, 0, count)
                }
                entries[entry.name] = output.toByteArray()
                zip.closeEntry()
            }
        }
        val records = requireNotNull(entries.remove("records.json")) { "Archive has no records." }
        val snapshot =
            BackupCodec.decode(
                Charsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(records)).toString()
            )
        val photos =
            snapshot.photos.associate { photo ->
                val content =
                    requireNotNull(entries.remove("photos/${photo.id}.jpg")) {
                        "Archive is missing a photo."
                    }
                require(
                    content.size.toLong() == photo.byteSize &&
                        PhotoStore.sha256(content) == photo.sha256
                ) {
                    "A backup photo is damaged."
                }
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(content, 0, content.size, bounds)
                require(
                    bounds.outMimeType == "image/jpeg" &&
                        bounds.outWidth in 1..1600 &&
                        bounds.outHeight in 1..1600 &&
                        bounds.outWidth == photo.width &&
                        bounds.outHeight == photo.height
                ) {
                    "Invalid backup photo dimensions or format."
                }
                val decoded = BitmapFactory.decodeByteArray(content, 0, content.size)
                require(decoded != null) { "A backup photo cannot be decoded." }
                decoded.recycle()
                photo.id to content
            }
        require(entries.isEmpty()) { "Archive contains unreferenced photos." }
        return Inspected(snapshot, photos)
    }

    suspend fun restore(
        candidate: Inspected,
        repository: TrackerRepository,
        store: PhotoStore,
    ): Boolean {
        val staged = linkedMapOf<Long, String>()
        var committed = false
        try {
            candidate.photos.forEach { (id, bytes) ->
                val name = "${UUID.randomUUID()}.jpg"
                staged[id] = name
                store.file(name).outputStream().use { it.write(bytes) }
            }
            val obsolete = repository.replaceFromBackupWithCleanup(candidate.snapshot, staged)
            committed = true
            return runCatching { store.deleteOwned(obsolete) }.isSuccess
        } finally {
            if (!committed) store.deleteOwned(staged.values.toList())
        }
    }
}
