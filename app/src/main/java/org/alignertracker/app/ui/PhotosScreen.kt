package org.alignertracker.app.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.alignertracker.app.R
import org.alignertracker.app.domain.PhotoMetadata
import org.alignertracker.app.domain.TrackerSnapshot

@Composable
fun PhotosScreen(snapshot: TrackerSnapshot, model: TrackerViewModel, busy: Boolean) {
    val zone = ZoneId.of(snapshot.plan?.zoneId ?: "UTC")
    var date by rememberSaveable { mutableStateOf(LocalDate.now(zone).toString()) }
    var caption by rememberSaveable { mutableStateOf("") }
    var deleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var comparing by rememberSaveable { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<Long>()) }
    val picked =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let {
                model.importPhoto(
                    it,
                    LocalDate.parse(date).atStartOfDay(zone).toInstant().toEpochMilli(),
                    caption,
                )
            }
        }
    val context = androidx.compose.ui.platform.LocalContext.current
    var captureUri by rememberSaveable { mutableStateOf<String?>(null) }
    val capture =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val uri = captureUri?.let(android.net.Uri::parse)
            captureUri = null
            if (uri != null) {
                if (success)
                    model.importCapture(
                        context.contentResolver,
                        uri,
                        LocalDate.parse(date).atStartOfDay(zone).toInstant().toEpochMilli(),
                        caption,
                    )
                else model.discardCapture(uri)
            }
        }
    val exporter =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/html")) { uri
            ->
            uri?.let { model.exportTimeLapse(context.contentResolver, it, selected) }
        }
    var exportPreview by remember { mutableStateOf(false) }
    val parsed = runCatching { LocalDate.parse(date) }.getOrNull()
    val validDate = parsed != null && parsed.year in 1970..2100 && parsed <= LocalDate.now(zone)
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        item {
            Text(
                stringResource(R.string.photos_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
        }
        item { Text(stringResource(R.string.photos_private)) }
        item {
            TrackerTextField(
                date,
                { date = it },
                label = { Text(stringResource(R.string.photo_date)) },
                isError = !validDate,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (!validDate) item { FormFeedback(R.string.photo_date_invalid) }
        item {
            TrackerTextField(
                caption,
                { caption = it.take(1000) },
                label = { Text(stringResource(R.string.photo_caption)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Button(
                onClick = {
                    runCatching {
                            picked.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        }
                        .onFailure {
                            model.reportError(context.getString(R.string.open_settings_failed))
                        }
                },
                enabled = !busy && validDate,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.photo_import))
            }
        }
        item {
            OutlinedButton(
                onClick = {
                    runCatching {
                            val uri = model.createCapture()
                            captureUri = uri.toString()
                            capture.launch(uri)
                        }
                        .onFailure {
                            captureUri?.let { uri ->
                                model.discardCapture(android.net.Uri.parse(uri))
                            }
                            captureUri = null
                            model.reportError(context.getString(R.string.capture_failed))
                        }
                },
                enabled = !busy && validDate,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.photo_capture))
            }
        }
        item { Text(stringResource(R.string.photo_selection, selected.size)) }
        item {
            Button(
                onClick = { comparing = !comparing },
                enabled = selected.size == 2,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.photo_compare))
            }
        }
        item {
            OutlinedButton(
                onClick = { exportPreview = true },
                enabled = !busy && selected.size in 2..100,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.photo_timelapse))
            }
        }
        if (snapshot.photos.isEmpty()) item { Text(stringResource(R.string.photos_empty)) }
        items(
            snapshot.photos
                .filter { !comparing || it.id in selected }
                .sortedByDescending { it.capturedAt },
            key = { it.id },
        ) { photo ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PhotoPreview(photo, model)
                    Text(
                        Instant.ofEpochMilli(photo.capturedAt)
                            .atZone(zone)
                            .toLocalDate()
                            .format(
                                java.time.format.DateTimeFormatter.ofLocalizedDate(
                                    java.time.format.FormatStyle.MEDIUM
                                )
                            )
                    )
                    if (photo.caption.isNotBlank()) Text(photo.caption)
                    FilterChip(
                        selected = photo.id in selected,
                        onClick = {
                            selected =
                                if (photo.id in selected) selected - photo.id
                                else selected + photo.id
                        },
                        label = { Text(stringResource(R.string.photo_select)) },
                    )
                    TextButton(onClick = { deleteId = photo.id }, enabled = !busy) {
                        Text(stringResource(R.string.photo_delete))
                    }
                }
            }
        }
    }
    if (exportPreview)
        TrackerDialog(
            onDismissRequest = { exportPreview = false },
            title = { Text(stringResource(R.string.photo_timelapse)) },
            text = { Text(stringResource(R.string.photo_export_notice, selected.size)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        exportPreview = false
                        exporter.launch("aligner-photo-timelapse.html")
                    }
                ) {
                    Text(stringResource(R.string.choose_location))
                }
            },
            dismissButton = {
                TextButton(onClick = { exportPreview = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    deleteId?.let { id ->
        TrackerDialog(
            onDismissRequest = { deleteId = null },
            title = { Text(stringResource(R.string.photo_delete)) },
            text = { Text(stringResource(R.string.photo_delete_notice)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        model.deletePhoto(id)
                        selected = selected - id
                        deleteId = null
                    },
                    enabled = !busy,
                ) {
                    Text(stringResource(R.string.confirm_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteId = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun PhotoPreview(photo: PhotoMetadata, model: TrackerViewModel) {
    val bitmap by
        produceState<android.graphics.Bitmap?>(null, photo.id, photo.ownedFileName) {
            value =
                withContext(Dispatchers.IO) {
                    photo.ownedFileName
                        ?.let { model.photoFile(it) }
                        ?.let {
                            BitmapFactory.decodeFile(
                                it.path,
                                BitmapFactory.Options().apply { inSampleSize = 2 },
                            )
                        }
                }
        }
    val image = bitmap
    if (image != null)
        Image(
            image.asImageBitmap(),
            contentDescription = photo.caption.ifBlank { stringResource(R.string.photo_image) },
            modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
        )
    else Text(stringResource(R.string.photo_loading))
}
