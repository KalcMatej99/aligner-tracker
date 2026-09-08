package org.alignertracker.app.ui

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetTime
import java.time.ZoneId

/** Reject DST gaps; repeated local times require an explicit valid UTC offset. */
internal fun parseTreatmentTime(date: LocalDate?, text: String, zone: ZoneId): Long? =
    runCatching {
            val day = requireNotNull(date)
            val offsetTime = runCatching { OffsetTime.parse(text.trim()) }.getOrNull()
            val local =
                LocalDateTime.of(day, offsetTime?.toLocalTime() ?: LocalTime.parse(text.trim()))
            val offsets = zone.rules.getValidOffsets(local)
            val offset = offsetTime?.offset?.also { require(it in offsets) } ?: offsets.single()
            local.toInstant(offset).toEpochMilli()
        }
        .getOrNull()
