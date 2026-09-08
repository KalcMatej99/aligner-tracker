package org.alignertracker.app.ui

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.ByteArrayOutputStream
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.alignertracker.app.data.BackupCodec
import org.alignertracker.app.data.EncryptedBackup
import org.alignertracker.app.data.PortableArchive
import org.alignertracker.app.data.TrackerRepository
import org.alignertracker.app.domain.TrackerSnapshot
import org.alignertracker.app.domain.TreatmentPlan
import org.alignertracker.app.photos.PhotoStore
import org.alignertracker.app.reminders.ReminderPreferences
import org.alignertracker.app.reminders.ReminderScheduler
import org.alignertracker.app.reminders.ReminderSettings

/** Keeps operations and an inspected restore independent of screen recomposition/rotation. */
class TrackerViewModel(
    private val repository: TrackerRepository,
    private val settings: ReminderSettings,
    private val scheduler: ReminderScheduler,
    private val photoStore: PhotoStore? = null,
    private val clockGuard: org.alignertracker.app.data.ClockGuard? = null,
    private val errorText: UserErrorText? = null,
) : ViewModel() {
    private val _snapshot = MutableStateFlow<TrackerSnapshot?>(null)
    val snapshot: StateFlow<TrackerSnapshot?> = _snapshot
    private val _preferences = MutableStateFlow(ReminderPreferences())
    val preferences: StateFlow<ReminderPreferences> = _preferences
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private val _notice = MutableStateFlow<Notice?>(null)
    val notice: StateFlow<Notice?> = _notice
    private val _pendingRestore = MutableStateFlow<TrackerSnapshot?>(null)
    val pendingRestore: StateFlow<TrackerSnapshot?> = _pendingRestore

    private var inspectedArchive: PortableArchive.Inspected? = null

    enum class Notice {
        SAVED,
        EXPORTED,
        RESTORED,
        DELETED,
        REMINDER_RETRY,
        CLEANUP_RETRY,
    }

    init {
        viewModelScope.launch {
            try {
                try {
                    photoStore?.recover()
                } catch (_: Exception) {
                    _notice.value = Notice.CLEANUP_RETRY
                }
                repository.snapshots.collect { _snapshot.value = it }
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                _error.value =
                    errorText?.message(exception)
                        ?: exception.message
                        ?: exception.javaClass.simpleName
            }
        }
        viewModelScope.launch {
            try {
                settings.preferences.collect { _preferences.value = it }
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                _error.value =
                    errorText?.message(exception)
                        ?: exception.message
                        ?: exception.javaClass.simpleName
            }
        }
    }

    fun dismissMessage() {
        _error.value = null
        _notice.value = null
    }

    fun reportError(detail: String) {
        _error.value = detail
    }

    private fun operate(notice: Notice? = Notice.SAVED, block: suspend () -> Unit) {
        if (_busy.value) return
        _busy.value = true
        _error.value = null
        _notice.value = null
        viewModelScope.launch {
            try {
                block()
                _snapshot.value = repository.snapshot()
                if (_notice.value == null) _notice.value = notice
                reconcileSafely()
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                _error.value =
                    errorText?.message(exception)
                        ?: exception.message
                        ?: exception.javaClass.simpleName
            } finally {
                _busy.value = false
            }
        }
    }

    private suspend fun reconcileSafely() {
        try {
            clockGuard?.observe()
            scheduler.reconcile()
        } catch (exception: Exception) {
            if (exception is CancellationException) throw exception
            // The data operation succeeded. Do not imply that retrying it is necessary.
            _notice.value = Notice.REMINDER_RETRY
        }
    }

    fun reconcile() {
        viewModelScope.launch { reconcileSafely() }
    }

    fun start(plan: TreatmentPlan, wearing: Boolean) = operate { repository.start(plan, wearing) }

    fun setWearing(wearing: Boolean) =
        operate(null) {
            clockGuard?.observe()
            repository.setWearing(wearing)
        }

    fun updateEvent(id: Long, at: Long, onSaved: () -> Unit = {}) = operate {
        repository.updateEvent(id, at)
        onSaved()
    }

    fun replaceSchedule(
        phaseId: Long,
        intervals: List<org.alignertracker.app.domain.TrayIntervalDraft>,
        reason: String,
    ) = operate { repository.replaceSchedule(phaseId, intervals, reason.ifBlank { null }) }

    fun beginPhase(
        kind: org.alignertracker.app.domain.TreatmentPhaseKind,
        name: String,
        count: Int,
        days: Int,
        wearing: Boolean,
    ) = operate {
        clockGuard?.observe()
        repository.beginPhase(kind, name, count, days, wearing)
    }

    fun insertMissingInterval(startAt: Long, endAt: Long, wearing: Boolean) = operate {
        repository.insertMissingInterval(startAt, endAt, wearing)
    }

    fun saveNote(note: org.alignertracker.app.domain.TreatmentNote, onSaved: () -> Unit = {}) =
        operate {
            if (note.id == 0L) repository.addNote(note) else repository.updateNote(note)
            onSaved()
        }

    fun deleteNote(id: Long) = operate { repository.deleteNote(id) }

    fun saveAppointment(
        appointment: org.alignertracker.app.domain.Appointment,
        onSaved: () -> Unit = {},
    ) = operate {
        if (appointment.id == 0L) repository.addAppointment(appointment)
        else repository.updateAppointment(appointment)
        onSaved()
    }

    fun deleteAppointment(id: Long) = operate { repository.deleteAppointment(id) }

    fun advance() = operate {
        clockGuard?.observe()
        repository.advanceTray()
    }

    fun complete() = operate {
        clockGuard?.observe()
        repository.completeTreatment()
    }

    fun updateGoal(minutes: Int) = operate { repository.updateGoal(minutes) }

    fun updateReminders(value: ReminderPreferences) = operate { settings.update(value) }

    fun clearAll() =
        operate(Notice.DELETED) {
            val oldPhotos = repository.snapshot().photos.mapNotNull { it.ownedFileName }
            repository.clearAll()
            withContext(Dispatchers.IO) {
                photoStore?.deleteOwned(oldPhotos)
                photoStore?.recover(removeCaptures = true)
            }
            inspectedArchive = null
            _pendingRestore.value = null
            try {
                settings.update(ReminderPreferences())
                scheduler.resetAfterRestore()
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                _notice.value = Notice.REMINDER_RETRY
            }
        }

    fun export(resolver: ContentResolver, uri: Uri, csv: Boolean, password: CharArray? = null) =
        operate(Notice.EXPORTED) {
            val current = repository.snapshot()
            withContext(Dispatchers.IO) {
                try {
                    val plaintext =
                        if (csv) BackupCodec.csv(current, Instant.now()).toByteArray()
                        else
                            photoStore?.let { PortableArchive.encode(current, it) }
                                ?: BackupCodec.encode(current).toByteArray()
                    val bytes =
                        if (password == null) plaintext
                        else EncryptedBackup.encrypt(plaintext, password)
                    val output =
                        resolver.openOutputStream(uri, "wt")
                            ?: error("Cannot open selected document")
                    output.use { it.write(bytes) }
                } finally {
                    password?.fill('\u0000')
                }
            }
        }

    fun inspectImport(resolver: ContentResolver, uri: Uri, password: CharArray? = null) =
        operate(null) {
            _pendingRestore.value = null
            inspectedArchive = null
            val candidate =
                withContext(Dispatchers.IO) {
                    val input =
                        resolver.openInputStream(uri) ?: error("Cannot open selected document")
                    val bytes =
                        input.use { stream ->
                            val output = ByteArrayOutputStream()
                            val buffer = ByteArray(8192)
                            var total = 0
                            while (true) {
                                val read = stream.read(buffer)
                                if (read < 0) break
                                total += read
                                require(total <= EncryptedBackup.MAX_FILE_BYTES) {
                                    "Backup exceeds the portable import limit"
                                }
                                output.write(buffer, 0, read)
                            }
                            output.toByteArray()
                        }
                    // Reject malformed UTF-8, rather than silently replacing bytes before JSON
                    // validation.
                    try {
                        val plaintext =
                            if (EncryptedBackup.isEncrypted(bytes)) {
                                require(password != null) {
                                    "Choose encrypted import and enter the backup password."
                                }
                                EncryptedBackup.decrypt(bytes, password)
                            } else bytes
                        PortableArchive.inspect(plaintext)
                    } finally {
                        password?.fill('\u0000')
                    }
                }
            inspectedArchive = candidate
            _pendingRestore.value = candidate.snapshot
        }

    fun exportTimeLapse(resolver: ContentResolver, uri: Uri, selected: Set<Long>) =
        operate(Notice.EXPORTED) {
            val current = repository.snapshot()
            withContext(Dispatchers.IO) {
                val html =
                    org.alignertracker.app.photos.PhotoTimeLapse.export(
                        current.photos.filter { it.id in selected },
                        requireNotNull(photoStore),
                        java.time.ZoneId.of(requireNotNull(current.plan).zoneId),
                    )
                resolver.openOutputStream(uri, "wt")?.use { it.write(html.toByteArray()) }
                    ?: error("Cannot open selected document")
            }
        }

    fun createCapture(): Uri = requireNotNull(photoStore).createCapture()

    fun discardCapture(uri: Uri) =
        operate(null) {
            withContext(Dispatchers.IO) { requireNotNull(photoStore).finishCapture(uri) }
        }

    fun importCapture(resolver: ContentResolver, uri: Uri, capturedAt: Long, caption: String) =
        operate {
            try {
                requireNotNull(photoStore).import(uri, capturedAt, caption)
            } finally {
                withContext(Dispatchers.IO) { requireNotNull(photoStore).finishCapture(uri) }
            }
        }

    fun importPhoto(uri: Uri, capturedAt: Long, caption: String) = operate {
        requireNotNull(photoStore).import(uri, capturedAt, caption)
    }

    fun deletePhoto(id: Long) = operate { requireNotNull(photoStore).delete(id) }

    fun photoFile(name: String) = photoStore?.file(name)

    fun cancelImport() {
        if (!_busy.value) {
            _pendingRestore.value = null
            inspectedArchive = null
        }
    }

    fun confirmImport() {
        val candidate = _pendingRestore.value ?: return
        operate(Notice.RESTORED) {
            val inspected = inspectedArchive
            if (inspected != null && photoStore != null) {
                val cleaned =
                    withContext(Dispatchers.IO) {
                        PortableArchive.restore(inspected, repository, photoStore)
                    }
                if (!cleaned) _notice.value = Notice.CLEANUP_RETRY
            } else repository.replaceFromBackup(candidate)
            inspectedArchive = null
            _pendingRestore.value = null
            try {
                scheduler.resetAfterRestore()
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                _notice.value = Notice.REMINDER_RETRY
            }
        }
    }
}
