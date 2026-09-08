package org.alignertracker.app.wear

import android.app.Application
import org.alignertracker.app.wear.data.WearDatabase
import org.alignertracker.app.wear.data.WearRepository
import org.alignertracker.app.wear.sync.WearSyncClient

class WearTrackerApplication : Application() {
    internal val database: WearDatabase by lazy { WearDatabase.create(this) }
    internal val repository: WearRepository by lazy { WearRepository(database) }
    internal val syncClient: WearSyncClient by lazy { WearSyncClient(this, repository) }
}
