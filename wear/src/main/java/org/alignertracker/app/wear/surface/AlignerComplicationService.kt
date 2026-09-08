package org.alignertracker.app.wear.surface

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import org.alignertracker.app.wear.WearMainActivity
import org.alignertracker.app.wear.WearTrackerApplication
import org.alignertracker.app.wear.data.LocalCommandState

class AlignerComplicationService : SuspendingComplicationDataSourceService() {
    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        if (request.complicationType != ComplicationType.SHORT_TEXT) return null
        val view = (application as WearTrackerApplication).repository.currentView()
        val status = view.acknowledged
        val text =
            when {
                view.pending != null -> "Wait"
                view.latestResolved?.state == LocalCommandState.REJECTED -> "Check"
                view.syncError != null -> "Sync"
                status == null -> "Sync"
                !status.hasPlan -> "Setup"
                status.completed -> "Done"
                status.wearing -> "IN"
                else -> "OUT"
            }
        val description =
            when {
                view.pending != null -> "Waiting for phone acknowledgement"
                view.latestResolved?.state == LocalCommandState.REJECTED ->
                    "Watch change was not recorded"
                view.syncError != null -> "Could not sync with nearby phone"
                status == null -> "Aligner phone status unknown"
                status.wearing -> "Aligners acknowledged as in"
                else -> "Aligners acknowledged as out"
            }
        return shortText(text, description)
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        if (type == ComplicationType.SHORT_TEXT) shortText("IN", "Aligners acknowledged as in")
        else null

    private fun shortText(text: String, description: String): ShortTextComplicationData =
        ShortTextComplicationData.Builder(
                PlainComplicationText.Builder(text).build(),
                PlainComplicationText.Builder(description).build(),
            )
            .setTapAction(
                PendingIntent.getActivity(
                    this,
                    20,
                    Intent(this, WearMainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            )
            .build()
}
