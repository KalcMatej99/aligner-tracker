package org.alignertracker.app

import android.content.Context
import org.alignertracker.app.data.TrackerDatabase
import org.alignertracker.app.data.TrackerRepository
import org.alignertracker.app.reminders.ReminderScheduler
import org.alignertracker.app.reminders.ReminderSettings

class AppContainer(context: Context) {
    val repository = TrackerRepository(TrackerDatabase.create(context))
    val clockGuard = org.alignertracker.app.data.ClockGuard(context, repository)
    val photoStore = org.alignertracker.app.photos.PhotoStore(context, repository)
    val wearBridge = org.alignertracker.app.wear.WearBridge(context, repository, clockGuard)
    val reminderSettings = ReminderSettings(context)
    val reminderScheduler = ReminderScheduler(context, repository, reminderSettings)
}
