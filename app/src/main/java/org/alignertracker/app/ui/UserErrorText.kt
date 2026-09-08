package org.alignertracker.app.ui

import android.content.Context
import java.io.IOException
import org.alignertracker.app.R

/** Converts expected operation failures to safe, translatable user text. */
fun interface UserErrorText {
    fun message(error: Throwable): String
}

class ResourceUserErrorText(context: Context) : UserErrorText {
    private val context = context.applicationContext

    override fun message(error: Throwable): String {
        var current: Throwable? = error
        var storageFailure = false
        repeat(8) {
            val candidate = current ?: return@repeat
            storageFailure =
                storageFailure || candidate is IOException || candidate is SecurityException
            candidate.message?.let { detail ->
                messageResources[detail]?.let {
                    return context.getString(it)
                }
                categorized(detail)?.let {
                    return context.getString(it)
                }
            }
            current = candidate.cause.takeUnless { it === candidate }
        }
        val fallback =
            if (storageFailure) R.string.error_storage_access else R.string.error_unexpected
        return context.getString(fallback)
    }

    private fun categorized(detail: String): Int? =
        when {
            detail.startsWith("Invalid backup") ||
                detail.startsWith("Backup contains duplicate") ||
                detail.startsWith("Backup must be a JSON") ||
                detail.startsWith("Invalid event list") ||
                detail.startsWith("Invalid event fields") ||
                detail.startsWith("Invalid treatment object") ||
                detail.startsWith("Invalid treatment fields") ||
                detail.startsWith("Invalid backup structure") -> R.string.error_backup_invalid
            detail.startsWith("Invalid ") && detail.endsWith(" record.") ->
                R.string.error_backup_invalid
            detail.startsWith("Invalid ") && detail.endsWith(" fields.") ->
                R.string.error_backup_invalid
            detail.startsWith("Invalid ") && detail.endsWith(" value.") ->
                R.string.error_backup_invalid
            detail.endsWith(" IDs must be positive and unique.") -> R.string.error_data_invalid
            detail.startsWith("Note text must be between 1 and ") -> R.string.error_note_values
            else -> null
        }

    private companion object {
        val messageResources: Map<String, Int> = buildMap {
            messages(R.string.error_no_treatment, "Set up a treatment first.")
            messages(
                R.string.error_treatment_exists,
                "A treatment already exists. Delete it or explicitly replace a backup first.",
            )
            messages(
                R.string.error_treatment_read_only,
                "Completed treatment is read-only.",
                "A new treatment cannot already be completed.",
            )
            messages(
                R.string.error_treatment_values,
                "A backup must contain one treatment with ID 1.",
                "Total trays must be between 1 and 1000.",
                "Current tray must be within the treatment.",
                "Days per tray must be between 1 and 365.",
                "Tray start must be between treatment start and today.",
                "Tracking start cannot be in the future.",
                "Treatment cannot start after tracking begins.",
                "Completed treatment needs a completion timestamp.",
                "Completion must be within recorded treatment time.",
                "Completion cannot precede the current tray.",
                "Choose a valid treatment time zone.",
            )
            messages(
                R.string.error_clock_backwards,
                "The device clock moved backwards. Correct the device clock before recording a new action. Existing records are unchanged.",
            )
            messages(
                R.string.error_clock_after_switch,
                "The clock is at or before the previous switch. Correct the clock or previous switch time, then try again.",
                "The current clock must follow the previous switch.",
            )
            messages(
                R.string.error_clock_before_tray,
                "The clock precedes the current tray start. Correct it before advancing.",
                "The clock precedes the last switch. Correct it before advancing.",
            )
            messages(
                R.string.error_clock_before_phase,
                "The clock precedes existing treatment history. Correct it before starting a phase.",
                "The clock precedes the last switch. Correct it before completing treatment.",
            )
            messages(
                R.string.error_event_limit,
                "A backup can contain at most 50,000 events.",
                "Event limit reached.",
                "Event limit reached. Export your history before starting another treatment.",
                "Event limit reached. Export your history before starting another phase.",
            )
            messages(R.string.error_switch_missing, "This switch no longer exists.")
            messages(
                R.string.error_initial_switch_fixed,
                "The initial tracking event cannot be moved.",
            )
            messages(
                R.string.error_switch_time,
                "A switch cannot be in the future or after completion.",
                "Choose a time strictly between the adjacent switches.",
                "A switch cannot precede tracking.",
                "Treatment needs an initial tracking event.",
                "Initial event must match tracking start.",
                "An event is outside the tracked period or in the future.",
                "Event timestamps must increase strictly.",
                "Events must alternate between in and out.",
            )
            messages(
                R.string.error_missing_interval_order,
                "Missing interval end must follow its start.",
            )
            messages(
                R.string.error_missing_interval_bounds,
                "Missing interval must start inside tracked history.",
                "Missing interval must stay strictly inside one existing interval.",
            )
            messages(
                R.string.error_missing_interval_state,
                "Missing interval must differ from the surrounding state.",
            )
            messages(
                R.string.error_missing_interval_gap,
                "Missing interval cannot overlap untracked coverage.",
            )
            messages(
                R.string.error_tracking_gap,
                "Tracking gap must be a positive interval within recorded treatment time.",
                "Tracking gap is outside treatment history.",
                "Tracking gaps cannot overlap.",
            )
            messages(
                R.string.error_last_tray,
                "This is the last tray. Complete or begin a new phase explicitly when appropriate.",
            )
            messages(
                R.string.error_schedule_phase,
                "Phase does not exist.",
                "Only the active phase schedule can be changed.",
            )
            messages(
                R.string.error_schedule_values,
                "A schedule must contain at least one interval.",
                "Schedule intervals must have no gaps or overlaps.",
                "Schedule interval is outside the phase.",
                "Tray interval must be between 1 and 365 days.",
                "Schedule must cover every tray in the phase.",
                "Tray interval is invalid.",
            )
            messages(R.string.error_schedule_reason, "Schedule reason is too long.")
            messages(
                R.string.error_phase_state_required,
                "Choose the current IN or OUT state when resuming a completed treatment.",
            )
            messages(
                R.string.error_phase_values,
                "Phase name is required.",
                "Phase name is invalid.",
                "Phase tray count is invalid.",
            )
            messages(
                R.string.error_target_values,
                "Prescribed target must be 1 to 1440 minutes.",
                "Historical target is invalid.",
            )
            messages(
                R.string.error_target_history,
                "A target change cannot rewrite past target history.",
            )
            messages(R.string.error_note_missing, "Note does not exist.")
            messages(R.string.error_note_values, "Note time cannot be in the future.")
            messages(R.string.error_appointment_missing, "Appointment does not exist.")
            messages(
                R.string.error_appointment_values,
                "Appointment time is invalid.",
                "Appointment duration is invalid.",
                "Appointment title is required.",
                "Appointment note is too long.",
                "Appointment reminder is invalid.",
            )
            messages(R.string.error_photo_missing, "Photo does not exist.")
            messages(
                R.string.error_photo_values,
                "Photo time cannot be in the future.",
                "Photo MIME type is invalid.",
                "Photo size is invalid.",
                "Photo checksum is invalid.",
                "Photo caption is too long.",
                "Photo width is invalid.",
                "Photo height is invalid.",
                "Choose an image smaller than 50 MiB.",
                "Unsupported image or dimensions.",
            )
            messages(
                R.string.error_photo_decode,
                "Cannot open the selected photo.",
                "Cannot decode this photo.",
            )
            messages(
                R.string.error_photo_save,
                "Cannot create a temporary photo.",
                "Could not save the photo.",
            )
            messages(
                R.string.error_storage_cleanup,
                "The record is saved, but the temporary photo still needs cleanup. Restart the app to retry.",
                "Records were updated, but a private photo could not be removed. Restart the app or retry Delete data to finish private-file cleanup.",
                "Temporary photo cleanup needs a retry. Restart the app.",
                "Temporary camera photo cleanup needs a retry. Restart the app or retry Delete data.",
            )
            messages(
                R.string.error_timelapse_selection,
                "Choose between 2 and 100 photos for a time-lapse.",
            )
            messages(
                R.string.error_timelapse_size,
                "Time-lapse is larger than 16 MiB. Select fewer photos.",
            )
            messages(
                R.string.error_reminder_minutes,
                "Break reminder must be between 1 and 240 minutes.",
            )
            messages(R.string.error_backup_password_new, "Use a password of 12 to 1024 characters.")
            messages(
                R.string.error_backup_password_enter,
                "Enter the backup password (at most 1024 characters).",
                "Choose encrypted import and enter the backup password.",
            )
            messages(
                R.string.error_backup_password_or_damage,
                "The password is incorrect or the backup is damaged. Your existing data is unchanged.",
            )
            messages(
                R.string.error_backup_too_large,
                "Backup is too large.",
                "Backup exceeds the 5 MiB limit.",
                "Backup content exceeds 32 MiB. Export or remove photos before retrying.",
                "Backup exceeds 32 MiB.",
                "Expanded archive exceeds 32 MiB.",
                "Archive entry is too large.",
                "Invalid or oversized encrypted backup.",
                "Backup exceeds the portable import limit",
            )
            messages(
                R.string.error_backup_version,
                "Unsupported backup version.",
                "Unsupported backup version. This app accepts schema 1 and 2.",
                "Unsupported encrypted backup version.",
                "Unsupported backup key derivation settings.",
            )
            messages(
                R.string.error_backup_invalid,
                "Invalid backup JSON or unsupported fields.",
                "Unexpected archive entry.",
                "Duplicate or excessive archive entries.",
                "Archive has no records.",
                "Archive contains unreferenced photos.",
            )
            messages(
                R.string.error_backup_needs_archive,
                "This JSON references photos. Import the complete portable archive instead.",
            )
            messages(
                R.string.error_backup_photo_missing,
                "Archive is missing a photo.",
                "A backup photo is damaged.",
                "Invalid backup photo dimensions or format.",
                "A backup photo cannot be decoded.",
                "A photo is missing or damaged.",
            )
            messages(
                R.string.error_backup_local_photo,
                "A photo is missing. Restore its complete archive or delete its entry before exporting.",
                "A private photo is missing or damaged. Existing records are unchanged.",
            )
            messages(
                R.string.error_data_changed,
                "Referenced phase does not exist.",
                "Referenced tray does not exist.",
            )
            messages(R.string.error_storage_access, "Cannot open selected document")
            messages(
                R.string.error_data_invalid,
                "Treatment records require a treatment plan.",
                "Event IDs must be positive and unique.",
                "Expanded treatment needs a phase.",
                "Treatment must have exactly one active phase.",
                "Phase order must be contiguous.",
                "Phase completion precedes its start.",
                "Active phase must match the treatment tray count.",
                "Schedule revision is invalid.",
                "Tray interval references a missing schedule revision.",
                "Treatment must have exactly one open tray history record.",
                "Tray history references a missing phase.",
                "Tray history number is invalid.",
                "Tray history references the wrong schedule.",
                "Tray history interval is invalid.",
                "Tray history ends before it starts.",
                "Tray history start time is invalid.",
                "Tray history end time is invalid.",
                "Tray history end precedes its start.",
                "Expanded treatment needs target history.",
                "Target dates must increase without duplicates.",
                "Too many treatment records.",
                "Note references a missing phase.",
                "Note references a missing tray.",
                "Photo references a missing phase.",
                "Photo references a missing tray.",
                "State revision is invalid.",
                "State generation is invalid.",
                "Portable photo metadata cannot claim ownership of a local file.",
                "Photo file ownership name is invalid.",
                "Portable restore cannot claim local photo ownership.",
                "Trusted photo files must identify every restored photo exactly once.",
                "Trusted photo filename is invalid.",
                "Invalid private photo reference.",
                "Invalid command idempotency ID.",
                "Dates must use YYYY-MM-DD format.",
                "Dates must use YYYY-MM-DD between 1970 and 2100.",
            )
        }

        private fun MutableMap<String, Int>.messages(resource: Int, vararg messages: String) {
            messages.forEach { put(it, resource) }
        }
    }
}
