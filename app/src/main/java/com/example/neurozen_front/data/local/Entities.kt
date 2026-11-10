package com.example.neurozen_front.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "breathing_sessions")
data class BreathingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: String,
    val durationSeconds: Int,
    val completedAtEpochMillis: Long
)

@Entity(tableName = "workload_entries")
data class WorkloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateIso: String,
    val hours: Double,
    val note: String
)

@Entity(tableName = "challenge_progress")
data class ChallengeEntity(
    @PrimaryKey val challengeId: String,
    val completedDays: Int,
    val lastCompletedDateIso: String?
)

/** Read-only cache of the user's appointments (offline view + reminders). No clinical notes are stored. */
@Entity(tableName = "appointments_cache")
data class AppointmentCacheEntity(
    @PrimaryKey val id: Long,
    val patientId: Long,
    val patientName: String?,
    val professionalId: Long,
    val dateTimeEpochMillis: Long,
    val type: Int,
    val status: String,
    val reviewed: Boolean
)
