package org.alignertracker.app

import android.app.Application

class TrackerApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
