package org.alignertracker.app.wear.surface

import android.content.ComponentName
import android.content.Context
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester

internal object WearSurfaceUpdates {
    fun request(context: Context) {
        TileService.getUpdater(context).requestUpdate(AlignerTileService::class.java)
        ComplicationDataSourceUpdateRequester.create(
                context,
                ComponentName(context, AlignerComplicationService::class.java),
            )
            .requestUpdateAll()
    }
}
