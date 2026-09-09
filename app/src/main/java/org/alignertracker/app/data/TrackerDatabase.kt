package org.alignertracker.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID
import org.alignertracker.app.domain.Appointment
import org.alignertracker.app.domain.CommandOutcome
import org.alignertracker.app.domain.CommandRejection
import org.alignertracker.app.domain.CommandStatus
import org.alignertracker.app.domain.PhotoMetadata
import org.alignertracker.app.domain.ScheduleRevision
import org.alignertracker.app.domain.StateVersion
import org.alignertracker.app.domain.TargetHistoryEntry
import org.alignertracker.app.domain.TrackingGap
import org.alignertracker.app.domain.TrackingGapReason
import org.alignertracker.app.domain.TrayHistoryEntry
import org.alignertracker.app.domain.TrayInterval
import org.alignertracker.app.domain.TreatmentNote
import org.alignertracker.app.domain.TreatmentPhase
import org.alignertracker.app.domain.TreatmentPhaseKind
import org.alignertracker.app.domain.TreatmentPlan
import org.alignertracker.app.domain.WearEvent

@Entity(tableName = "treatment")
data class PlanEntity(
    @PrimaryKey val id: Long,
    val startDate: String?,
    val totalTrays: Int?,
    val currentTray: Int?,
    val daysPerTray: Int?,
    val currentTrayStartedOn: String?,
    val dailyGoalMinutes: Int?,
    val zoneId: String,
    val trackingStartedAt: Long,
    val completed: Boolean,
    val completedAt: Long?,
) {
    fun model() =
        TreatmentPlan(
            id,
            startDate,
            totalTrays,
            currentTray,
            daysPerTray,
            currentTrayStartedOn,
            dailyGoalMinutes,
            zoneId,
            trackingStartedAt,
            completed,
            completedAt,
        )

    companion object {
        fun from(plan: TreatmentPlan) =
            PlanEntity(
                plan.id,
                plan.startDate,
                plan.totalTrays,
                plan.currentTray,
                plan.daysPerTray,
                plan.currentTrayStartedOn,
                plan.dailyGoalMinutes,
                plan.zoneId,
                plan.trackingStartedAt,
                plan.completed,
                plan.completedAt,
            )
    }
}

@Entity(tableName = "wear_events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val at: Long,
    val wearing: Boolean,
) {
    fun model() = WearEvent(id, at, wearing)
}

@Entity(tableName = "tracking_gaps")
data class TrackingGapEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startAt: Long,
    val endAt: Long,
    val reason: String,
) {
    fun model() = TrackingGap(id, startAt, endAt, TrackingGapReason.valueOf(reason))

    companion object {
        fun from(value: TrackingGap) =
            TrackingGapEntity(value.id, value.startAt, value.endAt, value.reason.name)
    }
}

@Entity(tableName = "treatment_phases")
data class PhaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String,
    val ordinal: Int,
    val name: String,
    val totalTrays: Int,
    val startedOn: String?,
    val completedOn: String?,
    val active: Boolean,
) {
    fun model() =
        TreatmentPhase(
            id,
            TreatmentPhaseKind.valueOf(kind),
            ordinal,
            name,
            totalTrays,
            startedOn,
            completedOn,
            active,
        )

    companion object {
        fun from(value: TreatmentPhase) =
            PhaseEntity(
                value.id,
                value.kind.name,
                value.ordinal,
                value.name,
                value.totalTrays,
                value.startedOn,
                value.completedOn,
                value.active,
            )
    }
}

@Entity(tableName = "schedule_revisions")
data class ScheduleRevisionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phaseId: Long,
    val createdAt: Long,
    val reason: String?,
) {
    fun model() = ScheduleRevision(id, phaseId, createdAt, reason)

    companion object {
        fun from(value: ScheduleRevision) =
            ScheduleRevisionEntity(value.id, value.phaseId, value.createdAt, value.reason)
    }
}

@Entity(tableName = "tray_intervals")
data class TrayIntervalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scheduleRevisionId: Long,
    val firstTray: Int,
    val lastTray: Int,
    val daysPerTray: Int,
) {
    fun model() = TrayInterval(id, scheduleRevisionId, firstTray, lastTray, daysPerTray)

    companion object {
        fun from(value: TrayInterval) =
            TrayIntervalEntity(
                value.id,
                value.scheduleRevisionId,
                value.firstTray,
                value.lastTray,
                value.daysPerTray,
            )
    }
}

@Entity(tableName = "tray_history")
data class TrayHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phaseId: Long,
    val trayNumber: Int,
    val startedOn: String,
    val endedOn: String?,
    val scheduleRevisionId: Long,
    val prescribedDays: Int,
    val startedAt: Long?,
    val endedAt: Long?,
) {
    fun model() =
        TrayHistoryEntry(
            id,
            phaseId,
            trayNumber,
            startedOn,
            endedOn,
            scheduleRevisionId,
            prescribedDays,
            startedAt,
            endedAt,
        )

    companion object {
        fun from(value: TrayHistoryEntry) =
            TrayHistoryEntity(
                value.id,
                value.phaseId,
                value.trayNumber,
                value.startedOn,
                value.endedOn,
                value.scheduleRevisionId,
                value.prescribedDays,
                value.startedAt,
                value.endedAt,
            )
    }
}

@Entity(tableName = "target_history")
data class TargetHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val effectiveFrom: String,
    val goalMinutes: Int,
    val effectiveAt: Long? = null,
) {
    fun model() = TargetHistoryEntry(id, effectiveFrom, goalMinutes, effectiveAt)

    companion object {
        fun from(value: TargetHistoryEntry) =
            TargetHistoryEntity(value.id, value.effectiveFrom, value.goalMinutes, value.effectiveAt)
    }
}

@Entity(tableName = "treatment_notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val occurredAt: Long,
    val text: String,
    val phaseId: Long?,
    val trayHistoryId: Long?,
    val createdAt: Long,
    val updatedAt: Long,
) {
    fun model() = TreatmentNote(id, occurredAt, text, phaseId, trayHistoryId, createdAt, updatedAt)

    companion object {
        fun from(value: TreatmentNote) =
            NoteEntity(
                value.id,
                value.occurredAt,
                value.text,
                value.phaseId,
                value.trayHistoryId,
                value.createdAt,
                value.updatedAt,
            )
    }
}

@Entity(tableName = "appointments")
data class AppointmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startsAt: Long,
    val durationMinutes: Int,
    val title: String,
    val note: String,
    val reminderMinutesBefore: Int?,
    val completed: Boolean,
) {
    fun model() =
        Appointment(id, startsAt, durationMinutes, title, note, reminderMinutesBefore, completed)

    companion object {
        fun from(value: Appointment) =
            AppointmentEntity(
                value.id,
                value.startsAt,
                value.durationMinutes,
                value.title,
                value.note,
                value.reminderMinutesBefore,
                value.completed,
            )
    }
}

@Entity(tableName = "photo_metadata")
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val capturedAt: Long,
    val mimeType: String,
    val byteSize: Long,
    val sha256: String,
    val caption: String,
    val phaseId: Long?,
    val trayHistoryId: Long?,
    val width: Int?,
    val height: Int?,
    val ownedFileName: String?,
) {
    fun model() =
        PhotoMetadata(
            id,
            capturedAt,
            mimeType,
            byteSize,
            sha256,
            caption,
            phaseId,
            trayHistoryId,
            width,
            height,
            ownedFileName,
        )

    companion object {
        fun from(value: PhotoMetadata) =
            PhotoEntity(
                value.id,
                value.capturedAt,
                value.mimeType,
                value.byteSize,
                value.sha256,
                value.caption,
                value.phaseId,
                value.trayHistoryId,
                value.width,
                value.height,
                value.ownedFileName,
            )
    }
}

@Entity(tableName = "tracker_state")
data class StateEntity(@PrimaryKey val id: Long = 1, val generation: String, val revision: Long) {
    fun model() = StateVersion(generation, revision)
}

@Entity(tableName = "command_outcomes")
data class CommandOutcomeEntity(
    @PrimaryKey val idempotencyId: String,
    val status: String,
    val rejection: String?,
    val receivedAt: Long,
    val beforeGeneration: String,
    val beforeRevision: Long,
    val afterGeneration: String,
    val afterRevision: Long,
) {
    fun model() =
        CommandOutcome(
            idempotencyId,
            CommandStatus.valueOf(status),
            rejection?.let(CommandRejection::valueOf),
            receivedAt,
            StateVersion(beforeGeneration, beforeRevision),
            StateVersion(afterGeneration, afterRevision),
        )

    companion object {
        fun from(value: CommandOutcome) =
            CommandOutcomeEntity(
                value.idempotencyId,
                value.status.name,
                value.rejection?.name,
                value.receivedAt,
                value.stateBefore.generation,
                value.stateBefore.revision,
                value.stateAfter.generation,
                value.stateAfter.revision,
            )
    }
}

@Dao
interface TrackerDao {
    @Query("SELECT * FROM treatment WHERE id = 1") suspend fun plan(): PlanEntity?

    @Query("SELECT * FROM wear_events ORDER BY at, id") suspend fun events(): List<EventEntity>

    @Query("SELECT * FROM tracking_gaps ORDER BY startAt, id")
    suspend fun trackingGaps(): List<TrackingGapEntity>

    @Query("SELECT * FROM treatment_phases ORDER BY ordinal, id")
    suspend fun phases(): List<PhaseEntity>

    @Query("SELECT * FROM schedule_revisions ORDER BY id")
    suspend fun scheduleRevisions(): List<ScheduleRevisionEntity>

    @Query("SELECT * FROM tray_intervals ORDER BY scheduleRevisionId, firstTray, id")
    suspend fun trayIntervals(): List<TrayIntervalEntity>

    @Query("SELECT * FROM tray_history ORDER BY id")
    suspend fun trayHistory(): List<TrayHistoryEntity>

    @Query("SELECT * FROM target_history ORDER BY effectiveFrom, id")
    suspend fun targetHistory(): List<TargetHistoryEntity>

    @Query("SELECT * FROM treatment_notes ORDER BY occurredAt, id")
    suspend fun notes(): List<NoteEntity>

    @Query("SELECT * FROM appointments ORDER BY startsAt, id")
    suspend fun appointments(): List<AppointmentEntity>

    @Query("SELECT * FROM photo_metadata ORDER BY capturedAt, id")
    suspend fun photos(): List<PhotoEntity>

    @Query("SELECT * FROM tracker_state WHERE id = 1") suspend fun state(): StateEntity?

    @Query("SELECT * FROM command_outcomes WHERE idempotencyId = :id")
    suspend fun commandOutcome(id: String): CommandOutcomeEntity?

    @Insert suspend fun insertPlan(value: PlanEntity)

    @Update suspend fun updatePlan(value: PlanEntity)

    @Insert suspend fun insertEvent(value: EventEntity): Long

    @Insert suspend fun insertEvents(values: List<EventEntity>)

    @Insert suspend fun insertTrackingGap(value: TrackingGapEntity): Long

    @Insert suspend fun insertTrackingGaps(values: List<TrackingGapEntity>)

    @Update suspend fun updateEvent(value: EventEntity)

    @Insert suspend fun insertPhase(value: PhaseEntity): Long

    @Insert suspend fun insertPhases(values: List<PhaseEntity>)

    @Update suspend fun updatePhase(value: PhaseEntity)

    @Insert suspend fun insertScheduleRevision(value: ScheduleRevisionEntity): Long

    @Insert suspend fun insertScheduleRevisions(values: List<ScheduleRevisionEntity>)

    @Insert suspend fun insertTrayInterval(value: TrayIntervalEntity): Long

    @Insert suspend fun insertTrayIntervals(values: List<TrayIntervalEntity>)

    @Insert suspend fun insertTrayHistory(value: TrayHistoryEntity): Long

    @Insert suspend fun insertTrayHistories(values: List<TrayHistoryEntity>)

    @Update suspend fun updateTrayHistory(value: TrayHistoryEntity)

    @Insert suspend fun insertTarget(value: TargetHistoryEntity): Long

    @Insert suspend fun insertTargets(values: List<TargetHistoryEntity>)

    @Update suspend fun updateTarget(value: TargetHistoryEntity)

    @Insert suspend fun insertNote(value: NoteEntity): Long

    @Insert suspend fun insertNotes(values: List<NoteEntity>)

    @Update suspend fun updateNote(value: NoteEntity)

    @Insert suspend fun insertAppointment(value: AppointmentEntity): Long

    @Insert suspend fun insertAppointments(values: List<AppointmentEntity>)

    @Update suspend fun updateAppointment(value: AppointmentEntity)

    @Insert suspend fun insertPhoto(value: PhotoEntity): Long

    @Insert suspend fun insertPhotos(values: List<PhotoEntity>)

    @Update suspend fun updatePhoto(value: PhotoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putState(value: StateEntity)

    @Insert suspend fun insertCommandOutcome(value: CommandOutcomeEntity)

    @Query("SELECT * FROM treatment_notes WHERE id = :id") suspend fun note(id: Long): NoteEntity?

    @Query("SELECT * FROM appointments WHERE id = :id")
    suspend fun appointment(id: Long): AppointmentEntity?

    @Query("SELECT * FROM photo_metadata WHERE id = :id") suspend fun photo(id: Long): PhotoEntity?

    @Query("DELETE FROM treatment_notes WHERE id = :id") suspend fun deleteNote(id: Long): Int

    @Query("DELETE FROM appointments WHERE id = :id") suspend fun deleteAppointment(id: Long): Int

    @Query("DELETE FROM photo_metadata WHERE id = :id") suspend fun deletePhoto(id: Long): Int

    @Query("DELETE FROM command_outcomes WHERE receivedAt < :before")
    suspend fun clearCommandLedger(before: Long): Int

    @Query("DELETE FROM command_outcomes") suspend fun deleteCommandOutcomes()

    @Query("DELETE FROM tracker_state") suspend fun deleteState()

    @Query("DELETE FROM tracking_gaps") suspend fun deleteTrackingGaps()

    @Query("DELETE FROM photo_metadata") suspend fun deletePhotos()

    @Query("DELETE FROM appointments") suspend fun deleteAppointments()

    @Query("DELETE FROM treatment_notes") suspend fun deleteNotes()

    @Query("DELETE FROM target_history") suspend fun deleteTargets()

    @Query("DELETE FROM tray_history") suspend fun deleteTrayHistory()

    @Query("DELETE FROM tray_intervals") suspend fun deleteTrayIntervals()

    @Query("DELETE FROM schedule_revisions") suspend fun deleteScheduleRevisions()

    @Query("DELETE FROM treatment_phases") suspend fun deletePhases()

    @Query("DELETE FROM wear_events") suspend fun deleteEvents()

    @Query("DELETE FROM treatment") suspend fun deletePlan()
}

@Database(
    entities =
        [
            PlanEntity::class,
            EventEntity::class,
            TrackingGapEntity::class,
            PhaseEntity::class,
            ScheduleRevisionEntity::class,
            TrayIntervalEntity::class,
            TrayHistoryEntity::class,
            TargetHistoryEntity::class,
            NoteEntity::class,
            AppointmentEntity::class,
            PhotoEntity::class,
            StateEntity::class,
            CommandOutcomeEntity::class,
        ],
    version = 3,
    exportSchema = true,
)
abstract class TrackerDatabase : RoomDatabase() {
    abstract fun trackerDao(): TrackerDao

    companion object {
        val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `treatment_phases` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `kind` TEXT NOT NULL, `ordinal` INTEGER NOT NULL, `name` TEXT NOT NULL, `totalTrays` INTEGER NOT NULL, `startedOn` TEXT NOT NULL, `completedOn` TEXT, `active` INTEGER NOT NULL)"
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `tracking_gaps` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `startAt` INTEGER NOT NULL, `endAt` INTEGER NOT NULL, `reason` TEXT NOT NULL)"
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `schedule_revisions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `phaseId` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `reason` TEXT)"
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `tray_intervals` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `scheduleRevisionId` INTEGER NOT NULL, `firstTray` INTEGER NOT NULL, `lastTray` INTEGER NOT NULL, `daysPerTray` INTEGER NOT NULL)"
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `tray_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `phaseId` INTEGER NOT NULL, `trayNumber` INTEGER NOT NULL, `startedOn` TEXT NOT NULL, `endedOn` TEXT, `scheduleRevisionId` INTEGER NOT NULL, `prescribedDays` INTEGER NOT NULL, `startedAt` INTEGER, `endedAt` INTEGER)"
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `target_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `effectiveFrom` TEXT NOT NULL, `goalMinutes` INTEGER NOT NULL)"
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `treatment_notes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `occurredAt` INTEGER NOT NULL, `text` TEXT NOT NULL, `phaseId` INTEGER, `trayHistoryId` INTEGER, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)"
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `appointments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `startsAt` INTEGER NOT NULL, `durationMinutes` INTEGER NOT NULL, `title` TEXT NOT NULL, `note` TEXT NOT NULL, `reminderMinutesBefore` INTEGER, `completed` INTEGER NOT NULL)"
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `photo_metadata` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `capturedAt` INTEGER NOT NULL, `mimeType` TEXT NOT NULL, `byteSize` INTEGER NOT NULL, `sha256` TEXT NOT NULL, `caption` TEXT NOT NULL, `phaseId` INTEGER, `trayHistoryId` INTEGER, `width` INTEGER, `height` INTEGER, `ownedFileName` TEXT)"
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `tracker_state` (`id` INTEGER NOT NULL, `generation` TEXT NOT NULL, `revision` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `command_outcomes` (`idempotencyId` TEXT NOT NULL, `status` TEXT NOT NULL, `rejection` TEXT, `receivedAt` INTEGER NOT NULL, `beforeGeneration` TEXT NOT NULL, `beforeRevision` INTEGER NOT NULL, `afterGeneration` TEXT NOT NULL, `afterRevision` INTEGER NOT NULL, PRIMARY KEY(`idempotencyId`))"
                    )
                    db.execSQL(
                        "INSERT INTO treatment_phases (id, kind, ordinal, name, totalTrays, startedOn, completedOn, active) SELECT 1, 'ALIGNER', 1, 'Initial treatment', totalTrays, startDate, CASE WHEN completed = 1 THEN currentTrayStartedOn ELSE NULL END, CASE WHEN completed = 1 THEN 0 ELSE 1 END FROM treatment WHERE id = 1"
                    )
                    db.execSQL(
                        "INSERT INTO schedule_revisions (id, phaseId, createdAt, reason) SELECT 1, 1, trackingStartedAt, 'Migrated fixed schedule' FROM treatment WHERE id = 1"
                    )
                    db.execSQL(
                        "INSERT INTO tray_intervals (id, scheduleRevisionId, firstTray, lastTray, daysPerTray) SELECT 1, 1, 1, totalTrays, daysPerTray FROM treatment WHERE id = 1"
                    )
                    db.execSQL(
                        "INSERT INTO tray_history (id, phaseId, trayNumber, startedOn, endedOn, scheduleRevisionId, prescribedDays, startedAt, endedAt) SELECT 1, 1, currentTray, currentTrayStartedOn, CASE WHEN completed = 1 THEN currentTrayStartedOn ELSE NULL END, 1, daysPerTray, trackingStartedAt, completedAt FROM treatment WHERE id = 1"
                    )
                    db.execSQL(
                        "INSERT INTO target_history (id, effectiveFrom, goalMinutes) SELECT 1, startDate, dailyGoalMinutes FROM treatment WHERE id = 1"
                    )
                    db.execSQL(
                        "INSERT INTO tracker_state (id, generation, revision) SELECT 1, ?, 1 FROM treatment WHERE id = 1",
                        arrayOf(UUID.randomUUID().toString()),
                    )
                }
            }

        val MIGRATION_2_3 =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE treatment_new (id INTEGER NOT NULL PRIMARY KEY, startDate TEXT, totalTrays INTEGER, currentTray INTEGER, daysPerTray INTEGER, currentTrayStartedOn TEXT, dailyGoalMinutes INTEGER, zoneId TEXT NOT NULL, trackingStartedAt INTEGER NOT NULL, completed INTEGER NOT NULL, completedAt INTEGER)"
                    )
                    db.execSQL("INSERT INTO treatment_new SELECT * FROM treatment")
                    db.execSQL("DROP TABLE treatment")
                    db.execSQL("ALTER TABLE treatment_new RENAME TO treatment")
                    db.execSQL(
                        "CREATE TABLE phases_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, kind TEXT NOT NULL, ordinal INTEGER NOT NULL, name TEXT NOT NULL, totalTrays INTEGER NOT NULL, startedOn TEXT, completedOn TEXT, active INTEGER NOT NULL)"
                    )
                    db.execSQL("INSERT INTO phases_new SELECT * FROM treatment_phases")
                    db.execSQL("DROP TABLE treatment_phases")
                    db.execSQL("ALTER TABLE phases_new RENAME TO treatment_phases")
                    db.execSQL("ALTER TABLE target_history ADD COLUMN effectiveAt INTEGER")
                }
            }

        fun create(context: Context): TrackerDatabase =
            Room.databaseBuilder(
                    context.applicationContext,
                    TrackerDatabase::class.java,
                    "aligner-tracker.db",
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
    }
}
