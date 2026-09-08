package org.alignertracker.app

import android.content.Context
import org.alignertracker.app.data.TrackerDatabase
import org.alignertracker.app.data.TrackerRepository
import org.alignertracker.app.reminders.ReminderScheduler
import org.alignertracker.app.reminders.ReminderSettings

class AppContainer(context: Context) {
    val repository = TrackerRepository(TrackerDatabase.create(context))
    val reminderSettings = ReminderSettings(context)
    val reminderScheduler = ReminderScheduler(context, repository, reminderSettings)
}
