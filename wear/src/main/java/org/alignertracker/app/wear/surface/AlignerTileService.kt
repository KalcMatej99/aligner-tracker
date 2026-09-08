package org.alignertracker.app.wear.surface

import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.alignertracker.app.wear.EXTRA_RECORD_NEXT_CHANGE
import org.alignertracker.app.wear.WearMainActivity
import org.alignertracker.app.wear.WearTrackerApplication
import org.alignertracker.app.wear.data.LocalCommandState

class AlignerTileService : TileService() {
    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest
    ): ListenableFuture<TileBuilders.Tile> {
        val view =
            runBlocking(Dispatchers.IO) {
                (application as WearTrackerApplication).repository.currentView()
            }
        val status = view.acknowledged
        val headline =
            when {
                view.pending != null -> if (view.pending.wearing) "Pending: IN" else "Pending: OUT"
                view.latestResolved?.state == LocalCommandState.REJECTED -> "Change not recorded"
                view.syncError != null -> "Phone not nearby"
                status == null -> "Phone status unknown"
                !status.hasPlan -> "Set up on phone"
                status.completed -> "Treatment done"
                status.wearing -> "Acknowledged: IN"
                else -> "Acknowledged: OUT"
            }
        val action =
            when {
                view.pending != null -> "Retry sync"
                view.latestResolved?.state == LocalCommandState.REJECTED -> "Review"
                view.syncError != null -> "Sync phone"
                status == null || !status.hasPlan || status.completed -> "Open Aligner Tracker"
                status.wearing -> "Record OUT"
                else -> "Record IN"
            }
        val launch =
            ActionBuilders.LaunchAction.Builder()
                .setAndroidActivity(
                    ActionBuilders.AndroidActivity.Builder()
                        .setPackageName(packageName)
                        .setClassName(WearMainActivity::class.java.name)
                        .addKeyToExtraMapping(
                            EXTRA_RECORD_NEXT_CHANGE,
                            ActionBuilders.AndroidBooleanExtra.Builder().setValue(true).build(),
                        )
                        .build()
                )
                .build()
        val clickable =
            ModifiersBuilders.Clickable.Builder().setId("record_next").setOnClick(launch).build()
        val button =
            LayoutElementBuilders.Box.Builder()
                .setWidth(DimensionBuilders.expand())
                .setHeight(DimensionBuilders.dp(48f))
                .setModifiers(
                    ModifiersBuilders.Modifiers.Builder()
                        .setClickable(clickable)
                        .setBackground(
                            ModifiersBuilders.Background.Builder()
                                .setColor(ColorBuilders.argb(0xff4f635f.toInt()))
                                .setCorner(
                                    ModifiersBuilders.Corner.Builder()
                                        .setRadius(DimensionBuilders.dp(24f))
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
                .addContent(
                    LayoutElementBuilders.Text.Builder()
                        .setText(action)
                        .setFontStyle(
                            LayoutElementBuilders.FontStyle.Builder()
                                .setColor(ColorBuilders.argb(0xffffffff.toInt()))
                                .build()
                        )
                        .build()
                )
                .build()
        val content =
            LayoutElementBuilders.Column.Builder()
                .setWidth(DimensionBuilders.expand())
                .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                .addContent(LayoutElementBuilders.Text.Builder().setText(headline).build())
                .addContent(
                    LayoutElementBuilders.Spacer.Builder()
                        .setHeight(DimensionBuilders.dp(12f))
                        .build()
                )
                .addContent(button)
                .build()
        val root =
            LayoutElementBuilders.Box.Builder()
                .setWidth(DimensionBuilders.expand())
                .setHeight(DimensionBuilders.expand())
                .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                .addContent(content)
                .build()
        val layout = LayoutElementBuilders.Layout.Builder().setRoot(root).build()
        val timeline =
            TimelineBuilders.Timeline.Builder()
                .addTimelineEntry(
                    TimelineBuilders.TimelineEntry.Builder().setLayout(layout).build()
                )
                .build()
        return immediate(
            TileBuilders.Tile.Builder()
                .setResourcesVersion(RESOURCES_VERSION)
                .setTileTimeline(timeline)
                .build()
        )
    }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ): ListenableFuture<ResourceBuilders.Resources> =
        immediate(ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION).build())

    private fun <T> immediate(value: T): ListenableFuture<T> =
        CallbackToFutureAdapter.getFuture { completer ->
            completer.set(value)
            "AlignerTileService immediate result"
        }

    private companion object {
        const val RESOURCES_VERSION = "1"
    }
}
