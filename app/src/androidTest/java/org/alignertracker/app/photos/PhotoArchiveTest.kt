package org.alignertracker.app.photos

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.alignertracker.app.data.PortableArchive
import org.alignertracker.app.data.TrackerDatabase
import org.alignertracker.app.data.TrackerRepository
import org.alignertracker.app.domain.TreatmentPlan
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhotoArchiveTest {
    @Test
    fun rotatedImportCompleteArchiveReplacementAndDeletionPreserveOwnership() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, TrackerDatabase::class.java).build()
        val repository = TrackerRepository(database)
        val store = PhotoStore(context, repository)
        val now = Instant.now()
        repository.start(
            TreatmentPlan(
                startDate = now.atZone(java.time.ZoneOffset.UTC).toLocalDate().toString(),
                totalTrays = 5,
                currentTray = 1,
                daysPerTray = 7,
                currentTrayStartedOn =
                    now.atZone(java.time.ZoneOffset.UTC).toLocalDate().toString(),
                dailyGoalMinutes = 1200,
                zoneId = "UTC",
                trackingStartedAt = now.toEpochMilli(),
            ),
            true,
        )
        val source = File(context.cacheDir, "archive-photo-test.jpg")
        try {
            val bitmap = Bitmap.createBitmap(3200, 1600, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.GREEN)
            source.outputStream().use {
                assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it))
            }
            bitmap.recycle()
            ExifInterface(source).apply {
                setAttribute(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_ROTATE_90.toString(),
                )
                setAttribute(ExifInterface.TAG_GPS_LATITUDE, "45/1,0/1,0/1")
                saveAttributes()
            }
            store.import(Uri.fromFile(source), now.toEpochMilli(), "Synthetic")
            val original = repository.snapshot()
            val photo = original.photos.single()
            assertEquals(800, photo.width)
            assertEquals(1600, photo.height)
            val oldFile = store.file(photo.ownedFileName!!)
            assertNull(ExifInterface(oldFile).getAttribute(ExifInterface.TAG_GPS_LATITUDE))
            val archive = PortableArchive.encode(original, store)
            val inspected = PortableArchive.inspect(archive)
            assertNull(inspected.snapshot.photos.single().ownedFileName)
            assertTrue(PortableArchive.restore(inspected, repository, store))
            val restored = repository.snapshot()
            assertEquals(photo.sha256, restored.photos.single().sha256)
            assertNotEquals(original.stateVersion.generation, restored.stateVersion.generation)
            assertFalse(oldFile.exists())
            val restoredFile = store.file(restored.photos.single().ownedFileName!!)
            assertTrue(restoredFile.exists())
            // Failed unlink stays discoverable after the metadata transaction; recovery retries it.
            android.system.Os.chmod(restoredFile.parent!!, 0b101101101)
            try {
                assertTrue(runCatching { store.delete(restored.photos.single().id) }.isFailure)
                assertTrue(repository.snapshot().photos.isEmpty())
                assertTrue(restoredFile.exists())
            } finally {
                android.system.Os.chmod(restoredFile.parent!!, 0b111000000)
            }
            store.recover()
            assertFalse(restoredFile.exists())
            val capture = store.createCapture()
            val temporary = File(context.cacheDir, "capture/${capture.lastPathSegment}")
            store.recover()
            assertTrue(temporary.exists()) // Live external-camera result survives recreation.
            store.finishCapture(capture)
            assertFalse(temporary.exists())
            val abandoned = store.createCapture()
            val staleFile = File(context.cacheDir, "capture/${abandoned.lastPathSegment}")
            assertTrue(staleFile.setLastModified(System.currentTimeMillis() - 86_400_001))
            store.recover()
            assertFalse(staleFile.exists())
            val active = store.createCapture()
            store.recover(removeCaptures = true)
            assertFalse(File(context.cacheDir, "capture/${active.lastPathSegment}").exists())
        } finally {
            source.delete()
            repository.clearAll()
            store.recover()
            database.close()
        }
    }
}
