package org.alignertracker.app.reminders

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.reminderDataStore by preferencesDataStore(name = "reminders")

data class ReminderPreferences(
    val enabled: Boolean = false,
    val breakMinutes: Int = 30,
    val trayEnabled: Boolean = false,
    val precise: Boolean = false,
    val streaksEnabled: Boolean = false,
)

class ReminderSettings(context: Context) {
    private val store = context.applicationContext.reminderDataStore
    private val enabledKey = booleanPreferencesKey("break_enabled")
    private val minutesKey = intPreferencesKey("break_minutes")
    private val trayKey = booleanPreferencesKey("tray_enabled")
    private val preciseKey = booleanPreferencesKey("precise")
    val preferences: Flow<ReminderPreferences> =
        store.data.map {
            ReminderPreferences(
                it[enabledKey] ?: false,
                it[minutesKey] ?: 30,
                it[trayKey] ?: false,
                it[preciseKey] ?: false,
                it[booleanPreferencesKey("streaks_enabled")] ?: false,
            )
        }

    suspend fun update(preferences: ReminderPreferences) {
        require(preferences.breakMinutes in 1..240) {
            "Break reminder must be between 1 and 240 minutes."
        }
        store.edit {
            it[enabledKey] = preferences.enabled
            it[minutesKey] = preferences.breakMinutes
            it[trayKey] = preferences.trayEnabled
            it[preciseKey] = preferences.precise
            it[booleanPreferencesKey("streaks_enabled")] = preferences.streaksEnabled
        }
    }

    internal suspend fun snooze(kind: String, baseKey: String, until: Long) {
        store.edit { it[longPreferencesKey("snooze_until:$kind:$baseKey")] = until }
    }

    internal suspend fun snoozed(candidate: ReminderCandidate): ReminderCandidate {
        val data = store.data.first()
        val until = data[longPreferencesKey("snooze_until:${candidate.kind}:${candidate.key}")]
        return if (until != null)
            candidate.copy(key = "${candidate.key}:snooze:$until", dueAt = until)
        else candidate
    }

    internal suspend fun delivered(): Set<String> {
        val data = store.data.first()
        return listOfNotNull(
                data[stringPreferencesKey("delivered_break")],
                data[stringPreferencesKey("delivered_tray")],
            )
            .toSet() + (data[stringSetPreferencesKey("delivered_appointments")] ?: emptySet())
    }

    internal suspend fun markDelivered(candidate: ReminderCandidate) {
        store.edit {
            if (candidate.kind == "appointment")
                it[stringSetPreferencesKey("delivered_appointments")] =
                    (it[stringSetPreferencesKey("delivered_appointments")] ?: emptySet()) +
                        candidate.key
            else it[stringPreferencesKey("delivered_${candidate.kind}")] = candidate.key
        }
    }

    internal suspend fun clearDelivered() {
        store.edit {
            it.remove(stringSetPreferencesKey("delivered_appointments"))
            it.remove(stringPreferencesKey("delivered_break"))
            it.remove(stringPreferencesKey("delivered_tray"))
            it.asMap()
                .keys
                .filter { key -> key.name.startsWith("snooze_until:") }
                .forEach { key -> it.remove(key) }
            for (kind in listOf("break", "tray", "appointment")) {
                it.remove(stringPreferencesKey("snooze_key_$kind"))
                it.remove(longPreferencesKey("snooze_until_$kind"))
            }
        }
    }
}
