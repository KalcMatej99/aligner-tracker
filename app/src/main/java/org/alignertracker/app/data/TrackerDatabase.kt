package org.alignertracker.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import org.alignertracker.app.domain.TreatmentPlan
import org.alignertracker.app.domain.WearEvent

@Entity(tableName = "treatment")
data class PlanEntity(
    @PrimaryKey val id: Long,
    val startDate: String,
    val totalTrays: Int,
    val currentTray: Int,
    val daysPerTray: Int,
    val currentTrayStartedOn: String,
    val dailyGoalMinutes: Int,
    val zoneId: String,
    val trackingStartedAt: Long,
    val completed: Boolean,
    val completedAt: Long?,
) {
    fun model() = TreatmentPlan(id, startDate, totalTrays, currentTray, daysPerTray,
        currentTrayStartedOn, dailyGoalMinutes, zoneId, trackingStartedAt, completed, completedAt)

    companion object {
        fun from(plan: TreatmentPlan) = PlanEntity(plan.id, plan.startDate, plan.totalTrays,
            plan.currentTray, plan.daysPerTray, plan.currentTrayStartedOn, plan.dailyGoalMinutes,
            plan.zoneId, plan.trackingStartedAt, plan.completed, plan.completedAt)
    }
}

@Entity(tableName = "wear_events")
data class EventEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val at: Long, val wearing: Boolean) {
    fun model() = WearEvent(id, at, wearing)
}

@Dao
interface TrackerDao {
    @Query("SELECT * FROM treatment WHERE id = 1") suspend fun plan(): PlanEntity?
    @Query("SELECT * FROM wear_events ORDER BY at, id") suspend fun events(): List<EventEntity>
    @Insert suspend fun insertPlan(plan: PlanEntity)
    @Update suspend fun updatePlan(plan: PlanEntity)
    @Insert suspend fun insertEvent(event: EventEntity): Long
    @Insert suspend fun insertEvents(events: List<EventEntity>)
    @Update suspend fun updateEvent(event: EventEntity)
    @Query("DELETE FROM wear_events") suspend fun deleteEvents()
    @Query("DELETE FROM treatment") suspend fun deletePlan()
}

@Database(entities = [PlanEntity::class, EventEntity::class], version = 1, exportSchema = true)
abstract class TrackerDatabase : RoomDatabase() {
    abstract fun trackerDao(): TrackerDao

    companion object {
        fun create(context: Context): TrackerDatabase =
            Room.databaseBuilder(context.applicationContext, TrackerDatabase::class.java, "aligner-tracker.db").build()
    }
}
