package com.example.replay

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "flight_replays")
data class FlightReplayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val aircraftId: String,
    val originIcao: String,
    val destinationIcao: String,
    val weatherPreset: String,
    val durationSec: Float,
    val landingFpm: Int,
    val recordedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "replay_frames",
    foreignKeys = [
        ForeignKey(
            entity = FlightReplayEntity::class,
            parentColumns = ["id"],
            childColumns = ["replayId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("replayId")]
)
data class ReplayFrameEntity(
    @PrimaryKey(autoGenerate = true) val frameId: Long = 0L,
    val replayId: Long,
    val timestampSec: Float,
    val xMeters: Float,
    val zMeters: Float,
    val altitudeMeters: Float,
    val pitchDeg: Float,
    val rollDeg: Float,
    val headingDeg: Float,
    val speedKts: Float,
    val throttle: Float,
    val flapsLevel: Int,
    val gearDown: Boolean,
    val eventTag: String = ""
)

@Dao
interface ReplayDao {
    @Query("SELECT * FROM flight_replays ORDER BY recordedAtEpochMs DESC")
    fun observeReplays(): Flow<List<FlightReplayEntity>>

    @Query("SELECT * FROM replay_frames WHERE replayId = :replayId ORDER BY timestampSec ASC")
    suspend fun getFramesForReplay(replayId: Long): List<ReplayFrameEntity>

    @Insert
    suspend fun insertReplay(replay: FlightReplayEntity): Long

    @Insert
    suspend fun insertFrames(frames: List<ReplayFrameEntity>)

    @Query("DELETE FROM flight_replays WHERE id = :replayId")
    suspend fun deleteReplay(replayId: Long)
}

@Database(
    entities = [FlightReplayEntity::class, ReplayFrameEntity::class],
    version = 1,
    exportSchema = false
)
abstract class BdasLocalDatabase : RoomDatabase() {
    abstract fun replayDao(): ReplayDao

    companion object {
        @Volatile
        private var INSTANCE: BdasLocalDatabase? = null

        fun getInstance(context: Context): BdasLocalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BdasLocalDatabase::class.java,
                    "bdas_local_replays.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

class ReplayRepository(private val dao: ReplayDao) {
    val replays: Flow<List<FlightReplayEntity>> = dao.observeReplays()

    suspend fun saveRecordedReplay(
        title: String,
        aircraftId: String,
        originIcao: String,
        destinationIcao: String,
        weatherPreset: String,
        durationSec: Float,
        landingFpm: Int,
        frames: List<ReplayFrameEntity>
    ): Long {
        val replayId = dao.insertReplay(
            FlightReplayEntity(
                title = title,
                aircraftId = aircraftId,
                originIcao = originIcao,
                destinationIcao = destinationIcao,
                weatherPreset = weatherPreset,
                durationSec = durationSec,
                landingFpm = landingFpm
            )
        )
        if (frames.isNotEmpty()) {
            dao.insertFrames(frames.map { it.copy(replayId = replayId) })
        }
        return replayId
    }

    suspend fun loadFrames(replayId: Long): List<ReplayFrameEntity> = dao.getFramesForReplay(replayId)

    suspend fun deleteReplay(replayId: Long) = dao.deleteReplay(replayId)
}
