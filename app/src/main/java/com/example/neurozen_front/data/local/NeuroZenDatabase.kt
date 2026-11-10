package com.example.neurozen_front.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [BreathingSessionEntity::class, WorkloadEntity::class, ChallengeEntity::class, AppointmentCacheEntity::class],
    version = 4,
    exportSchema = false
)
abstract class NeuroZenDatabase : RoomDatabase() {
    abstract fun breathingDao(): BreathingDao
    abstract fun workloadDao(): WorkloadDao
    abstract fun challengeDao(): ChallengeDao
    abstract fun appointmentCacheDao(): AppointmentCacheDao
}
