package org.alignertracker.app.wear.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "acknowledged_status")
internal data class AcknowledgedStatusEntity(
    @androidx.room.PrimaryKey val id: Int = 1,
    val available: Boolean,
    val generation: String,
    val revision: Long,
    val wearing: Boolean,
    val completed: Boolean,
    val hasPlan: Boolean,
    val tray: Int?,
    val lastAt: Long?,
    val acknowledgedAt: Long,
    val syncError: String? = null,
)

@Entity(tableName = "wear_commands")
internal data class WearCommandEntity(
    @androidx.room.PrimaryKey val idempotencyId: String,
    val expectedGeneration: String,
    val expectedRevision: Long,
    val wearing: Boolean,
    val requestedAt: Long,
    val state: String,
    val rejection: String? = null,
    val receivedAt: Long? = null,
    val resultingGeneration: String? = null,
    val resultingRevision: Long? = null,
    val lastError: String? = null,
)

@Dao
internal interface WearDao {
    @Query("SELECT * FROM acknowledged_status WHERE id = 1")
    fun observeStatus(): Flow<AcknowledgedStatusEntity?>

    @Query("SELECT * FROM acknowledged_status WHERE id = 1")
    suspend fun status(): AcknowledgedStatusEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putStatus(status: AcknowledgedStatusEntity)

    @Query("SELECT * FROM wear_commands ORDER BY requestedAt DESC")
    fun observeCommands(): Flow<List<WearCommandEntity>>

    @Query("SELECT * FROM wear_commands WHERE state = 'PENDING' ORDER BY requestedAt")
    suspend fun pendingCommands(): List<WearCommandEntity>

    @Query("SELECT * FROM wear_commands WHERE state != 'PENDING' ORDER BY requestedAt DESC LIMIT 1")
    suspend fun latestResolvedCommand(): WearCommandEntity?

    @Query("SELECT * FROM wear_commands WHERE idempotencyId = :id")
    suspend fun command(id: String): WearCommandEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCommand(command: WearCommandEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putCommand(command: WearCommandEntity)

    @Query("DELETE FROM wear_commands WHERE state != 'PENDING'") suspend fun clearResolvedCommands()
}

@Database(
    entities = [AcknowledgedStatusEntity::class, WearCommandEntity::class],
    version = 1,
    exportSchema = true,
)
internal abstract class WearDatabase : RoomDatabase() {
    abstract fun wearDao(): WearDao

    companion object {
        fun create(context: Context): WearDatabase =
            Room.databaseBuilder(context, WearDatabase::class.java, "aligner-wear.db").build()
    }
}
