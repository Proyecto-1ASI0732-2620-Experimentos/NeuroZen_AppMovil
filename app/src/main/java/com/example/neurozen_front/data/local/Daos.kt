package com.example.neurozen_front.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface BreathingDao {
    @Query("SELECT * FROM breathing_sessions ORDER BY completedAtEpochMillis DESC")
    fun observeAll(): Flow<List<BreathingSessionEntity>>

    @Insert
    suspend fun insert(entity: BreathingSessionEntity): Long

    @Query("DELETE FROM breathing_sessions")
    suspend fun clear()
}

@Dao
interface WorkloadDao {
    @Query("SELECT * FROM workload_entries ORDER BY dateIso DESC, id DESC")
    fun observeAll(): Flow<List<WorkloadEntity>>

    @Insert
    suspend fun insert(entity: WorkloadEntity): Long

    @Query("DELETE FROM workload_entries WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM workload_entries")
    suspend fun clear()
}

@Dao
interface ChallengeDao {
    @Query("SELECT * FROM challenge_progress")
    fun observeAll(): Flow<List<ChallengeEntity>>

    @Query("SELECT * FROM challenge_progress WHERE challengeId = :id")
    suspend fun get(id: String): ChallengeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ChallengeEntity)

    @Query("DELETE FROM challenge_progress WHERE challengeId = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM challenge_progress")
    suspend fun clear()
}

@Dao
interface AppointmentCacheDao {
    @Query("SELECT * FROM appointments_cache ORDER BY dateTimeEpochMillis ASC")
    fun observeAll(): Flow<List<AppointmentCacheEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<AppointmentCacheEntity>)

    @Query("DELETE FROM appointments_cache")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(items: List<AppointmentCacheEntity>) {
        clear()
        insertAll(items)
    }
}
