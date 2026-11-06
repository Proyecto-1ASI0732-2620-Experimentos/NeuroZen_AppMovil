package com.example.neurozen_front.data.repository

import com.example.neurozen_front.data.local.AppointmentCacheDao
import com.example.neurozen_front.data.local.BreathingDao
import com.example.neurozen_front.data.local.BreathingSessionEntity
import com.example.neurozen_front.data.local.ChallengeDao
import com.example.neurozen_front.data.local.ChallengeEntity
import com.example.neurozen_front.data.local.WorkloadDao
import com.example.neurozen_front.data.local.WorkloadEntity
import com.example.neurozen_front.domain.model.BreathingSession
import com.example.neurozen_front.domain.model.ChallengeProgress
import com.example.neurozen_front.domain.model.WorkloadEntry
import com.example.neurozen_front.domain.repository.WellnessRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

class WellnessRepositoryImpl @Inject constructor(
    private val breathing: BreathingDao,
    private val workload: WorkloadDao,
    private val challenges: ChallengeDao,
    private val appointmentCache: AppointmentCacheDao
) : WellnessRepository {

    override fun observeBreathingSessions(): Flow<List<BreathingSession>> = breathing.observeAll().map { list ->
        list.map { BreathingSession(it.id, it.exerciseId, it.durationSeconds, Instant.ofEpochMilli(it.completedAtEpochMillis)) }
    }

    override suspend fun addBreathingSession(exerciseId: String, durationSeconds: Int) {
        breathing.insert(BreathingSessionEntity(exerciseId = exerciseId, durationSeconds = durationSeconds, completedAtEpochMillis = System.currentTimeMillis()))
    }

    override fun observeWorkload(): Flow<List<WorkloadEntry>> = workload.observeAll().map { list ->
        list.map { WorkloadEntry(it.id, LocalDate.parse(it.dateIso), it.hours, it.note) }
    }

    override suspend fun addWorkload(date: LocalDate, hours: Double, note: String) {
        require(hours > 0 && hours <= 24) { "Las horas deben estar entre 0 y 24" }
        workload.insert(WorkloadEntity(dateIso = date.toString(), hours = hours, note = note.trim()))
    }

    override suspend fun deleteWorkload(id: Long) = workload.delete(id)

    override fun observeChallenges(): Flow<List<ChallengeProgress>> = challenges.observeAll().map { list ->
        list.map { ChallengeProgress(it.challengeId, it.completedDays, it.lastCompletedDateIso?.let(LocalDate::parse)) }
    }

    /** At most one day per calendar day. */
    override suspend fun completeChallengeDay(challengeId: String, today: LocalDate) {
        val current = challenges.get(challengeId)
        if (current?.lastCompletedDateIso == today.toString()) return
        challenges.upsert(ChallengeEntity(challengeId, (current?.completedDays ?: 0) + 1, today.toString()))
    }

    override suspend fun resetChallenge(challengeId: String) = challenges.delete(challengeId)

    override suspend fun clearAll() {
        breathing.clear()
        workload.clear()
        challenges.clear()
        appointmentCache.clear()
    }
}
