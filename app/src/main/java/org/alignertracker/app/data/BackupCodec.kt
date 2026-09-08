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
import org.alignertracker.app.domain.TrackerSnapshot
import org.alignertracker.app.domain.TrackerValidation
import org.alignertracker.app.domain.TreatmentPlan
import org.alignertracker.app.domain.WearEvent
import org.alignertracker.app.domain.WearMath

/** Plain UTF-8 JSON. Callers must explain that exported treatment history is not encrypted. */
object BackupCodec {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = false
        isLenient = false
        coerceInputValues = false
    }

    @Serializable
    private data class Backup(
        val schemaVersion: Int,
        val plan: TreatmentPlan?,
        val events: List<WearEvent>,
    )

    fun encode(snapshot: TrackerSnapshot): String {
        TrackerValidation.snapshot(snapshot, Instant.now())
        val encoded = json.encodeToString(Backup(1, snapshot.plan, snapshot.events))
        require(encoded.toByteArray(Charsets.UTF_8).size <= TrackerValidation.MAX_BACKUP_BYTES) {
            "Backup exceeds the 5 MiB limit."
        }
        return encoded
    }

    fun decode(json: String): TrackerSnapshot {
        require(
            json.length <= TrackerValidation.MAX_BACKUP_BYTES &&
                json.toByteArray(Charsets.UTF_8).size <= TrackerValidation.MAX_BACKUP_BYTES
        ) {
            "Backup exceeds the 5 MiB limit."
        }
        rejectDuplicateKeysAndDeepNesting(json)
        val backup =
            try {
                val element = this.json.parseToJsonElement(json)
                requireSchemaTypes(element)
                this.json.decodeFromJsonElement<Backup>(element)
            } catch (_: SerializationException) {
                // Decoder messages can contain the source history. Expose only a safe summary.
                throw IllegalArgumentException("Invalid backup JSON or unsupported fields.")
            }
        require(backup.schemaVersion == 1) {
            "Unsupported backup version. This app accepts schema 1."
        }
        return TrackerSnapshot(backup.plan, backup.events).also {
            TrackerValidation.snapshot(it, Instant.now())
        }
    }

    fun csv(snapshot: TrackerSnapshot, now: Instant): String {
        TrackerValidation.snapshot(snapshot, now)
        val header =
            "date,treatment_timezone,worn_millis,removed_millis,tracked_millis,day_millis,current_goal_minutes,coverage\r\n"
        val plan = snapshot.plan ?: return header
        val zone = ZoneId.of(plan.zoneId)
        var date = Instant.ofEpochMilli(plan.trackingStartedAt).atZone(zone).toLocalDate()
        val last =
            Instant.ofEpochMilli(minOf(now.toEpochMilli(), plan.completedAt ?: Long.MAX_VALUE))
                .atZone(zone)
                .toLocalDate()
        return buildString {
            append(header)
            while (date <= last) {
                val summary = WearMath.summarize(snapshot, date, now)
                val dayMillis =
                    date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() -
                        date.atStartOfDay(zone).toInstant().toEpochMilli()
                val coverage =
                    when {
                        summary.trackedMillis == 0L -> "untracked"
                        summary.trackedMillis == dayMillis -> "full"
                        else -> "partial"
                    }
                append(date)
                    .append(',')
                    .append('"')
                    .append(plan.zoneId.replace("\"", "\"\""))
                    .append('"')
                    .append(',')
                append(summary.wornMillis).append(',').append(summary.removedMillis).append(',')
                append(summary.trackedMillis).append(',').append(dayMillis).append(',')
                append(plan.dailyGoalMinutes).append(',').append(coverage).append("\r\n")
                date = date.plusDays(1)
            }
        }
    }

    private class ObjectFrame(val objectType: Boolean, var expectsKey: Boolean = objectType) {
        val keys = HashSet<String>()
    }

    // JSON primitive decoders can accept quoted numbers/booleans. Schema 1 is deliberately typed.
    private fun requireSchemaTypes(element: JsonElement) {
        val root =
            element as? JsonObject
                ?: throw IllegalArgumentException("Backup must be a JSON object.")
        require(root.keys == setOf("schemaVersion", "plan", "events")) { "Invalid backup fields." }
        number(root["schemaVersion"])
        val plan = root.getValue("plan")
        if (plan != JsonNull) {
            require(plan is JsonObject) { "Invalid treatment object." }
            val numeric =
                setOf(
                    "id",
                    "totalTrays",
                    "currentTray",
                    "daysPerTray",
                    "dailyGoalMinutes",
                    "trackingStartedAt",
                )
            val strings = setOf("startDate", "currentTrayStartedOn", "zoneId")
            require(plan.keys == numeric + strings + setOf("completed", "completedAt")) {
                "Invalid treatment fields."
            }
            numeric.forEach { number(plan[it]) }
            strings.forEach {
                require((plan[it] as? JsonPrimitive)?.isString == true) {
                    "Invalid treatment text field."
                }
            }
            boolean(plan["completed"])
            if (plan["completedAt"] != JsonNull) number(plan["completedAt"])
        }
        val events =
            root["events"] as? JsonArray ?: throw IllegalArgumentException("Invalid event list.")
        require(events.size <= TrackerValidation.MAX_EVENTS) {
            "A backup can contain at most 50,000 events."
        }
        for (event in events) {
            require(event is JsonObject && event.keys == setOf("id", "at", "wearing")) {
                "Invalid event fields."
            }
            number(event["id"])
            number(event["at"])
            boolean(event["wearing"])
        }
    }

    private fun number(element: JsonElement?) {
        require(
            element is JsonPrimitive && !element.isString && element.content.toLongOrNull() != null
        ) {
            "Backup numbers must be whole JSON numbers within range."
        }
    }

    private fun boolean(element: JsonElement?) {
        require(
            element is JsonPrimitive &&
                !element.isString &&
                element.content in setOf("true", "false")
        ) {
            "Backup state must be a JSON boolean."
        }
    }

    /**
     * The JSON decoder handles syntax; this bounded pass rejects ambiguous keys and nesting bombs.
     */
    private fun rejectDuplicateKeysAndDeepNesting(source: String) {
        val stack = ArrayDeque<ObjectFrame>()
        var i = 0
        while (i < source.length) {
            when (source[i]) {
                '{',
                '[' -> {
                    require(stack.size < 4) { "Invalid backup structure: too deeply nested." }
                    stack.addLast(ObjectFrame(source[i] == '{'))
                }
                '}',
                ']' -> {
                    require(stack.isNotEmpty()) { "Invalid backup JSON." }
                    stack.removeLast()
                }
                ',' -> stack.lastOrNull()?.let { it.expectsKey = it.objectType }
                ':' -> stack.lastOrNull()?.let { it.expectsKey = false }
                '"' -> {
                    val start = i
                    i++
                    var closed = false
                    while (i < source.length) {
                        if (source[i] == '\\') {
                            i += 2
                            continue
                        }
                        if (source[i] == '"') {
                            closed = true
                            break
                        }
                        i++
                    }
                    require(closed) { "Invalid backup JSON." }
                    val frame = stack.lastOrNull()
                    if (frame?.expectsKey == true) {
                        val key =
                            try {
                                json.decodeFromString<String>(source.substring(start, i + 1))
                            } catch (_: SerializationException) {
                                throw IllegalArgumentException("Invalid backup JSON.")
                            }
                        require(frame.keys.add(key)) { "Backup contains duplicate fields." }
                        frame.expectsKey = false
                    }
                }
            }
            i++
        }
    }
}
