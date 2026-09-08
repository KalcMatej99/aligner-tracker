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
import org.alignertracker.app.data.TrackerRepository
import org.alignertracker.app.domain.TrackerSnapshot
import org.alignertracker.app.domain.TreatmentPlan
import org.alignertracker.app.reminders.ReminderPreferences
import org.alignertracker.app.reminders.ReminderScheduler
import org.alignertracker.app.reminders.ReminderSettings

/** Keeps operations and an inspected restore independent of screen recomposition/rotation. */
class TrackerViewModel(
    private val repository: TrackerRepository,
    private val settings: ReminderSettings,
    private val scheduler: ReminderScheduler,
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

    enum class Notice { SAVED, EXPORTED, RESTORED, DELETED, REMINDER_RETRY }

    init {
        viewModelScope.launch {
            try {
                repository.snapshots.collect { _snapshot.value = it }
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                _error.value = exception.message ?: exception.javaClass.simpleName
            }
        }
        viewModelScope.launch {
            try {
                settings.preferences.collect { _preferences.value = it }
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                _error.value = exception.message ?: exception.javaClass.simpleName
            }
        }
    }

    fun dismissMessage() { _error.value = null; _notice.value = null }
    fun reportError(detail: String) { _error.value = detail }

    private fun operate(notice: Notice? = Notice.SAVED, block: suspend () -> Unit) {
        if (_busy.value) return
        _busy.value = true
        _error.value = null
        _notice.value = null
        viewModelScope.launch {
            try {
                block()
                _snapshot.value = repository.snapshot()
                if (_notice.value != Notice.REMINDER_RETRY) _notice.value = notice
                reconcileSafely()
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                _error.value = exception.message ?: exception.javaClass.simpleName
            } finally {
                _busy.value = false
            }
        }
    }

    private suspend fun reconcileSafely() {
        try {
            scheduler.reconcile()
        } catch (exception: Exception) {
            if (exception is CancellationException) throw exception
            // The data operation succeeded. Do not imply that retrying it is necessary.
            _notice.value = Notice.REMINDER_RETRY
        }
    }

    fun reconcile() { viewModelScope.launch { reconcileSafely() } }
    fun start(plan: TreatmentPlan, wearing: Boolean) = operate { repository.start(plan, wearing) }
    fun setWearing(wearing: Boolean) = operate(null) { repository.setWearing(wearing) }
    fun updateEvent(id: Long, at: Long, onSaved: () -> Unit = {}) = operate { repository.updateEvent(id, at); onSaved() }
    fun advance() = operate { repository.advanceTray() }
    fun complete() = operate { repository.completeTreatment() }
    fun updateGoal(minutes: Int) = operate { repository.updateGoal(minutes) }
    fun updateReminders(value: ReminderPreferences) = operate { settings.update(value) }
    fun clearAll() = operate(Notice.DELETED) {
        repository.clearAll()
        _pendingRestore.value = null
        try {
            settings.update(ReminderPreferences())
            scheduler.resetAfterRestore()
        } catch (exception: Exception) {
            if (exception is CancellationException) throw exception
            _notice.value = Notice.REMINDER_RETRY
        }
    }

    fun export(resolver: ContentResolver, uri: Uri, csv: Boolean) = operate(Notice.EXPORTED) {
        val current = repository.snapshot()
        withContext(Dispatchers.IO) {
            val text = if (csv) BackupCodec.csv(current, Instant.now()) else BackupCodec.encode(current)
            val output = resolver.openOutputStream(uri, "wt") ?: error("Cannot open selected document")
            output.use { it.write(text.toByteArray(Charsets.UTF_8)) }
        }
    }

    fun inspectImport(resolver: ContentResolver, uri: Uri) = operate(null) {
        _pendingRestore.value = null
        val candidate = withContext(Dispatchers.IO) {
            val input = resolver.openInputStream(uri) ?: error("Cannot open selected document")
            val bytes = input.use { stream ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                var total = 0
                while (true) {
                    val read = stream.read(buffer)
                    if (read < 0) break
                    total += read
                    require(total <= 5 * 1024 * 1024) { "Backup exceeds the 5 MiB import limit" }
                    output.write(buffer, 0, read)
                }
                output.toByteArray()
            }
            // Reject malformed UTF-8, rather than silently replacing bytes before JSON validation.
            val text = Charsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString()
            BackupCodec.decode(text)
        }
        _pendingRestore.value = candidate
    }

    fun cancelImport() { if (!_busy.value) _pendingRestore.value = null }
    fun confirmImport() {
        val candidate = _pendingRestore.value ?: return
        operate(Notice.RESTORED) {
            repository.replaceFromBackup(candidate)
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
