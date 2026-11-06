package com.example.neurozen_front

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.neurozen_front.data.local.AppointmentCacheEntity
import com.example.neurozen_front.data.local.NeuroZenDatabase
import com.example.neurozen_front.data.repository.WellnessRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Real Room database in memory: repository + DAOs + SQLite on the device. */
@RunWith(AndroidJUnit4::class)
class RoomIntegrationTest {
    private lateinit var db: NeuroZenDatabase
    private lateinit var repo: WellnessRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, NeuroZenDatabase::class.java).allowMainThreadQueries().build()
        repo = WellnessRepositoryImpl(db.breathingDao(), db.workloadDao(), db.challengeDao(), db.appointmentCacheDao())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun breathingSessions_areStoredNewestFirst() = runTest {
        repo.addBreathingSession("box", 64)
        repo.addBreathingSession("478", 76)
        val sessions = repo.observeBreathingSessions().first()
        assertEquals(2, sessions.size)
        assertEquals("478", sessions.first().exerciseId)
    }

    @Test
    fun workload_addAndDelete() = runTest {
        repo.addWorkload(LocalDate.of(2026, 11, 10), 9.5, "Cierre de mes")
        val entry = repo.observeWorkload().first().single()
        assertEquals(9.5, entry.hours, 0.0)
        repo.deleteWorkload(entry.id)
        assertTrue(repo.observeWorkload().first().isEmpty())
    }

    @Test
    fun challenge_countsOncePerDay() = runTest {
        val today = LocalDate.of(2026, 11, 10)
        repo.completeChallengeDay("breath7", today)
        repo.completeChallengeDay("breath7", today)
        repo.completeChallengeDay("breath7", today.plusDays(1))
        assertEquals(2, repo.observeChallenges().first().single().completedDays)
    }

    @Test
    fun clearAll_removesEveryLocalTable_onLogout() = runTest {
        repo.addBreathingSession("box", 64)
        repo.addWorkload(LocalDate.of(2026, 11, 10), 8.0, "")
        repo.completeChallengeDay("walk7", LocalDate.of(2026, 11, 10))
        db.appointmentCacheDao().insertAll(listOf(AppointmentCacheEntity(1, 5, null, 3, 0L, 1, "PENDING", false)))

        repo.clearAll()

        assertTrue(repo.observeBreathingSessions().first().isEmpty())
        assertTrue(repo.observeWorkload().first().isEmpty())
        assertTrue(repo.observeChallenges().first().isEmpty())
        assertTrue(db.appointmentCacheDao().observeAll().first().isEmpty())
    }
}
