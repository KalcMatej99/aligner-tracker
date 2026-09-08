package org.alignertracker.app.data

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import kotlin.math.abs

/** Detects same-boot wall-clock jumps using Android's monotonic elapsed time. */
class ClockGuard(context: Context, private val repository: TrackerRepository) {
    private val context = context.applicationContext
    private val preferences =
        context.getSharedPreferences("clock-observation", Context.MODE_PRIVATE)
    private val mutex = kotlinx.coroutines.sync.Mutex()

    suspend fun observe() {
        mutex.lock()
        try {
            val snapshot = repository.snapshot()
            if (snapshot.plan == null) {
                preferences.edit().clear().apply()
                return
            }
            val now = System.currentTimeMillis()
            val elapsed = SystemClock.elapsedRealtime()
            val boot =
                Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
            val sameGeneration =
                preferences.getString("generation", null) == snapshot.stateVersion.generation
            val previousWall = preferences.getLong("wall", now)
            val previousElapsed = preferences.getLong("elapsed", elapsed)
            val sameBoot =
                boot >= 0 && preferences.getInt("boot", -2) == boot && elapsed >= previousElapsed
            if (snapshot.plan != null && !snapshot.plan.completed && sameGeneration && sameBoot) {
                val difference = (now - previousWall) - (elapsed - previousElapsed)
                if (abs(difference) > 120_000) {
                    if (now < previousWall) {
                        preferences.edit().putBoolean("warning", true).apply()
                        error(
                            "The device clock moved backwards. Correct the device clock before recording a new action. Existing records are unchanged."
                        )
                    }
                    val start = maxOf(previousWall, snapshot.plan.trackingStartedAt)
                    if (now > start) repository.recordTrackingGap(start, now)
                    preferences.edit().putBoolean("warning", true).apply()
                }
            }
            preferences
                .edit()
                .putString("generation", snapshot.stateVersion.generation)
                .putLong("wall", now)
                .putLong("elapsed", elapsed)
                .putInt("boot", boot)
                .apply()
        } finally {
            mutex.unlock()
        }
    }
}
