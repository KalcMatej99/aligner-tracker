package org.alignertracker.app.data

import java.time.Instant
import java.time.ZoneId
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import org.alignertracker.app.domain.Appointment
import org.alignertracker.app.domain.PhotoMetadata
import org.alignertracker.app.domain.ScheduleRevision
import org.alignertracker.app.domain.TargetHistoryEntry
import org.alignertracker.app.domain.TrackingGap
import org.alignertracker.app.domain.TrackerSnapshot
import org.alignertracker.app.domain.TrackerValidation
import org.alignertracker.app.domain.TrayHistoryEntry
import org.alignertracker.app.domain.TrayInterval
import org.alignertracker.app.domain.TreatmentNote
import org.alignertracker.app.domain.TreatmentPhase
import org.alignertracker.app.domain.TreatmentPlan
import org.alignertracker.app.domain.WearEvent
import org.alignertracker.app.domain.WearMath

/** Plain UTF-8 JSON. Callers must explain that exported treatment history is not encrypted. */
object BackupCodec {
    private val codec = Json { encodeDefaults = true; ignoreUnknownKeys = false; isLenient = false; coerceInputValues = false }

    @Serializable private data class BackupV1(val schemaVersion: Int, val plan: TreatmentPlan?, val events: List<WearEvent>)

    @Serializable
    private data class BackupV2(
        val schemaVersion: Int,
        val plan: TreatmentPlan?,
        val events: List<WearEvent>,
        val phases: List<TreatmentPhase>,
        val scheduleRevisions: List<ScheduleRevision>,
        val trayIntervals: List<TrayInterval>,
        val trayHistory: List<TrayHistoryEntry>,
        val targetHistory: List<TargetHistoryEntry>,
        val notes: List<TreatmentNote>,
        val appointments: List<Appointment>,
        val photos: List<PortablePhoto>,
        val trackingGaps: List<TrackingGap>,
    )

    @Serializable
    private data class PortablePhoto(
        val id: Long,
        val capturedAt: Long,
        val mimeType: String,
        val byteSize: Long,
        val sha256: String,
        val caption: String,
        val phaseId: Long?,
        val trayHistoryId: Long?,
        val width: Int?,
        val height: Int?,
    ) {
        fun model() = PhotoMetadata(id, capturedAt, mimeType, byteSize, sha256, caption, phaseId, trayHistoryId, width, height, null)
        companion object { fun from(value: PhotoMetadata) = PortablePhoto(value.id, value.capturedAt, value.mimeType, value.byteSize, value.sha256, value.caption, value.phaseId, value.trayHistoryId, value.width, value.height) }
    }

    fun encode(snapshot: TrackerSnapshot): String {
        val expanded = TrackerValidation.expandLegacy(snapshot)
        val portable = expanded.copy(photos = expanded.photos.map { it.copy(ownedFileName = null) })
        TrackerValidation.snapshot(portable, Instant.now())
        val encoded =
            codec.encodeToString(
                BackupV2(
                    2, portable.plan, portable.events, portable.phases, portable.scheduleRevisions,
                    portable.trayIntervals, portable.trayHistory, portable.targetHistory, portable.notes,
                    portable.appointments, portable.photos.map(PortablePhoto::from),
                    portable.trackingGaps,
                )
            )
        require(encoded.toByteArray(Charsets.UTF_8).size <= TrackerValidation.MAX_BACKUP_BYTES) { "Backup exceeds the 5 MiB limit." }
        return encoded
    }

    fun decode(source: String): TrackerSnapshot {
        require(source.length <= TrackerValidation.MAX_BACKUP_BYTES && source.toByteArray(Charsets.UTF_8).size <= TrackerValidation.MAX_BACKUP_BYTES) {
            "Backup exceeds the 5 MiB limit."
        }
        rejectDuplicateKeysAndDeepNesting(source)
        val element =
            try { codec.parseToJsonElement(source) }
            catch (_: SerializationException) { throw IllegalArgumentException("Invalid backup JSON or unsupported fields.") }
        val root = element as? JsonObject ?: throw IllegalArgumentException("Backup must be a JSON object.")
        number(root["schemaVersion"])
        val version = (root["schemaVersion"] as JsonPrimitive).content.toIntOrNull()
            ?: throw IllegalArgumentException("Unsupported backup version.")
        val snapshot =
            try {
                when (version) {
                    1 -> decodeV1(root)
                    2 -> decodeV2(root)
                    else -> throw IllegalArgumentException("Unsupported backup version. This app accepts schema 1 and 2.")
                }
            } catch (_: SerializationException) {
                throw IllegalArgumentException("Invalid backup JSON or unsupported fields.")
            }
        TrackerValidation.snapshot(snapshot, Instant.now())
        return snapshot
    }

    fun csv(snapshot: TrackerSnapshot, now: Instant): String {
        TrackerValidation.snapshot(snapshot, now)
        val header = "date,treatment_timezone,worn_millis,removed_millis,tracked_millis,day_millis,current_goal_minutes,coverage\r\n"
        val plan = snapshot.plan ?: return header
        val zone = ZoneId.of(plan.zoneId)
        var date = Instant.ofEpochMilli(plan.trackingStartedAt).atZone(zone).toLocalDate()
        val last = Instant.ofEpochMilli(minOf(now.toEpochMilli(), plan.completedAt ?: Long.MAX_VALUE)).atZone(zone).toLocalDate()
        return buildString {
            append(header)
            while (date <= last) {
                val summary = WearMath.summarize(snapshot, date, now)
                val dayMillis = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - date.atStartOfDay(zone).toInstant().toEpochMilli()
                val coverage = when { summary.trackedMillis == 0L -> "untracked"; summary.trackedMillis == dayMillis -> "full"; else -> "partial" }
                append(date).append(',').append('"').append(plan.zoneId.replace("\"", "\"\"")).append('"').append(',')
                append(summary.wornMillis).append(',').append(summary.removedMillis).append(',')
                append(summary.trackedMillis).append(',').append(dayMillis).append(',')
                append(summary.goalMinutes).append(',').append(coverage).append("\r\n")
                date = date.plusDays(1)
            }
        }
    }

    private fun decodeV1(root: JsonObject): TrackerSnapshot {
        requireSchema1Types(root)
        val backup = codec.decodeFromJsonElement<BackupV1>(root)
        require(backup.schemaVersion == 1)
        return TrackerValidation.expandLegacy(TrackerSnapshot(backup.plan, backup.events))
    }

    private fun decodeV2(root: JsonObject): TrackerSnapshot {
        val expected = setOf("schemaVersion", "plan", "events", "phases", "scheduleRevisions", "trayIntervals", "trayHistory", "targetHistory", "notes", "appointments", "photos", "trackingGaps")
        require(root.keys == expected) { "Invalid backup fields." }
        expected.filterNot { it in setOf("schemaVersion", "plan") }.forEach {
            require(root[it] is JsonArray) { "Invalid backup list." }
        }
        require((root["events"] as JsonArray).size <= TrackerValidation.MAX_EVENTS) { "A backup can contain at most 50,000 events." }
        requirePlanTypes(root.getValue("plan"))
        requireObjects(root, "events", setOf("id", "at"), emptySet(), setOf("wearing"))
        requireObjects(root, "phases", setOf("id", "ordinal", "totalTrays"), setOf("kind", "name", "startedOn"), setOf("active"), nullableStrings = setOf("completedOn"))
        requireObjects(root, "scheduleRevisions", setOf("id", "phaseId", "createdAt"), emptySet(), emptySet(), nullableStrings = setOf("reason"))
        requireObjects(root, "trayIntervals", setOf("id", "scheduleRevisionId", "firstTray", "lastTray", "daysPerTray"), emptySet(), emptySet())
        requireObjects(root, "trayHistory", setOf("id", "phaseId", "trayNumber", "scheduleRevisionId", "prescribedDays"), setOf("startedOn"), emptySet(), setOf("startedAt", "endedAt"), setOf("endedOn"))
        requireObjects(root, "targetHistory", setOf("id", "goalMinutes"), setOf("effectiveFrom"), emptySet())
        requireObjects(root, "notes", setOf("id", "occurredAt", "createdAt", "updatedAt"), setOf("text"), emptySet(), setOf("phaseId", "trayHistoryId"))
        requireObjects(root, "appointments", setOf("id", "startsAt", "durationMinutes"), setOf("title", "note"), setOf("completed"), setOf("reminderMinutesBefore"))
        requireObjects(root, "photos", setOf("id", "capturedAt", "byteSize"), setOf("mimeType", "sha256", "caption"), emptySet(), setOf("phaseId", "trayHistoryId", "width", "height"))
        requireObjects(root, "trackingGaps", setOf("id", "startAt", "endAt"), setOf("reason"), emptySet())
        val backup = codec.decodeFromJsonElement<BackupV2>(root)
        require(backup.schemaVersion == 2)
        return TrackerSnapshot(
            plan = backup.plan,
            events = backup.events,
            phases = backup.phases,
            scheduleRevisions = backup.scheduleRevisions,
            trayIntervals = backup.trayIntervals,
            trayHistory = backup.trayHistory,
            targetHistory = backup.targetHistory,
            notes = backup.notes,
            appointments = backup.appointments,
            photos = backup.photos.map { it.model() },
            trackingGaps = backup.trackingGaps,
        )
    }

    // Schema 1 stays byte-for-byte strict because old decoders accepted some quoted primitives.
    private fun requireSchema1Types(root: JsonObject) {
        require(root.keys == setOf("schemaVersion", "plan", "events")) { "Invalid backup fields." }
        requirePlanTypes(root.getValue("plan"))
        val events = root["events"] as? JsonArray ?: throw IllegalArgumentException("Invalid event list.")
        require(events.size <= TrackerValidation.MAX_EVENTS) { "A backup can contain at most 50,000 events." }
        for (event in events) {
            require(event is JsonObject && event.keys == setOf("id", "at", "wearing")) { "Invalid event fields." }
            number(event["id"]); number(event["at"]); boolean(event["wearing"])
        }
    }

    private fun requirePlanTypes(plan: JsonElement) {
        if (plan != JsonNull) {
            require(plan is JsonObject) { "Invalid treatment object." }
            val numeric = setOf("id", "totalTrays", "currentTray", "daysPerTray", "dailyGoalMinutes", "trackingStartedAt")
            val strings = setOf("startDate", "currentTrayStartedOn", "zoneId")
            require(plan.keys == numeric + strings + setOf("completed", "completedAt")) { "Invalid treatment fields." }
            numeric.forEach { number(plan[it]) }
            strings.forEach { require((plan[it] as? JsonPrimitive)?.isString == true) { "Invalid treatment text field." } }
            boolean(plan["completed"])
            if (plan["completedAt"] != JsonNull) number(plan["completedAt"])
        }
    }

    private fun requireObjects(
        root: JsonObject,
        field: String,
        numbers: Set<String>,
        strings: Set<String>,
        booleans: Set<String>,
        nullableNumbers: Set<String> = emptySet(),
        nullableStrings: Set<String> = emptySet(),
    ) {
        (root[field] as JsonArray).forEach { element ->
            require(element is JsonObject) { "Invalid $field record." }
            require(element.keys == numbers + strings + booleans + nullableNumbers + nullableStrings) {
                "Invalid $field fields."
            }
            numbers.forEach { number(element[it]) }
            strings.forEach {
                require((element[it] as? JsonPrimitive)?.isString == true) { "Invalid $field text field." }
            }
            booleans.forEach { boolean(element[it]) }
            nullableNumbers.forEach { if (element[it] != JsonNull) number(element[it]) }
            nullableStrings.forEach {
                if (element[it] != JsonNull) {
                    require((element[it] as? JsonPrimitive)?.isString == true) {
                        "Invalid $field text field."
                    }
                }
            }
        }
    }

    private fun number(element: JsonElement?) {
        require(element is JsonPrimitive && !element.isString && element.content.toLongOrNull() != null) {
            "Backup numbers must be whole JSON numbers within range."
        }
    }

    private fun boolean(element: JsonElement?) {
        require(element is JsonPrimitive && !element.isString && element.content in setOf("true", "false")) {
            "Backup state must be a JSON boolean."
        }
    }

    private class ObjectFrame(val objectType: Boolean, var expectsKey: Boolean = objectType) { val keys = HashSet<String>() }

    private fun rejectDuplicateKeysAndDeepNesting(source: String) {
        val stack = ArrayDeque<ObjectFrame>()
        var i = 0
        while (i < source.length) {
            when (source[i]) {
                '{', '[' -> { require(stack.size < 5) { "Invalid backup structure: too deeply nested." }; stack.addLast(ObjectFrame(source[i] == '{')) }
                '}', ']' -> { require(stack.isNotEmpty()) { "Invalid backup JSON." }; stack.removeLast() }
                ',' -> stack.lastOrNull()?.let { it.expectsKey = it.objectType }
                ':' -> stack.lastOrNull()?.let { it.expectsKey = false }
                '"' -> {
                    val start = i; i++; var closed = false
                    while (i < source.length) {
                        if (source[i] == '\\') { i += 2; continue }
                        if (source[i] == '"') { closed = true; break }
                        i++
                    }
                    require(closed) { "Invalid backup JSON." }
                    val frame = stack.lastOrNull()
                    if (frame?.expectsKey == true) {
                        val key = try { codec.decodeFromString<String>(source.substring(start, i + 1)) }
                        catch (_: SerializationException) { throw IllegalArgumentException("Invalid backup JSON.") }
                        require(frame.keys.add(key)) { "Backup contains duplicate fields." }
                        frame.expectsKey = false
                    }
                }
            }
            i++
        }
        require(stack.isEmpty()) { "Invalid backup JSON." }
    }
}
