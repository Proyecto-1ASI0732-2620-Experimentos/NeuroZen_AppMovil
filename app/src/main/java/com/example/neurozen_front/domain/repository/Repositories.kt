package com.example.neurozen_front.domain.repository

import com.example.neurozen_front.domain.model.Appointment
import com.example.neurozen_front.domain.model.AppointmentStatus
import com.example.neurozen_front.domain.model.AppointmentType
import com.example.neurozen_front.domain.model.Assessment
import com.example.neurozen_front.domain.model.BookingRequest
import com.example.neurozen_front.domain.model.BreathingSession
import com.example.neurozen_front.domain.model.ChallengeProgress
import com.example.neurozen_front.domain.model.HealthMetric
import com.example.neurozen_front.domain.model.LibraryResource
import com.example.neurozen_front.domain.model.Outcome
import com.example.neurozen_front.domain.model.PaymentData
import com.example.neurozen_front.domain.model.Professional
import com.example.neurozen_front.domain.model.ProfessionalProfile
import com.example.neurozen_front.domain.model.RegisterData
import com.example.neurozen_front.domain.model.Report
import com.example.neurozen_front.domain.model.Review
import com.example.neurozen_front.domain.model.Session
import com.example.neurozen_front.domain.model.StressStats
import com.example.neurozen_front.domain.model.StressTrigger
import com.example.neurozen_front.domain.model.Subscription
import com.example.neurozen_front.domain.model.UpdateProfessionalData
import com.example.neurozen_front.domain.model.UpdateProfileData
import com.example.neurozen_front.domain.model.UserProfile
import com.example.neurozen_front.domain.model.WorkloadEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.time.LocalDate

/** Current session (persisted encrypted). */
interface SessionRepository {
    val session: StateFlow<Session?>
    val isLoaded: StateFlow<Boolean>
    suspend fun save(session: Session, isNewAccount: Boolean = false)
    suspend fun clear()
    suspend fun consumeOnboardingFlag(): Boolean
    suspend fun shouldShowVerifiedWelcome(userId: Long): Boolean
    suspend fun markVerifiedWelcomeShown(userId: Long)
    fun requireUserId(): Long
}

interface AuthRepository {
    suspend fun signIn(email: String, password: String): Outcome<Session>
    suspend fun signUp(data: RegisterData): Outcome<Session>
    suspend fun forgotPassword(email: String): Outcome<String>
    suspend fun resetPassword(token: String, newPassword: String): Outcome<String>
}

interface UserRepository {
    suspend fun getMe(): Outcome<UserProfile>
    suspend fun updateMe(data: UpdateProfileData): Outcome<UserProfile>
    suspend fun changePassword(current: String, new: String): Outcome<Unit>
    suspend fun deleteMe(): Outcome<Unit>
}

interface StressRepository {
    suspend fun getTriggers(): Outcome<List<StressTrigger>>
    suspend fun addTrigger(category: String, level: Int, description: String, at: Instant): Outcome<StressTrigger>
    suspend fun deleteTrigger(id: Long): Outcome<Unit>
    suspend fun getAssessments(): Outcome<List<Assessment>>
    suspend fun submitAssessment(answers: List<Int>): Outcome<Assessment>
    suspend fun getHealthMetrics(): Outcome<List<HealthMetric>>
    suspend fun addHealthMetric(metric: HealthMetric): Outcome<Unit>
}

interface ReportRepository {
    suspend fun getMyReports(): Outcome<List<Report>>
    suspend fun getReportsForProfessional(professionalId: Long): Outcome<List<Report>>
    suspend fun share(appointmentId: Long, professionalId: Long, stats: StressStats, note: String?): Outcome<Report>
}

interface ProfessionalRepository {
    suspend fun getDirectory(): Outcome<List<Professional>>
    suspend fun getById(id: Long): Outcome<Professional>
    suspend fun getReviews(id: Long): Outcome<List<Review>>
    suspend fun addReview(professionalId: Long, appointmentId: Long, rating: Int, comment: String): Outcome<Review>
    suspend fun getAvailableSlots(id: Long, date: LocalDate): Outcome<List<String>>
    suspend fun getMyProfile(): Outcome<ProfessionalProfile>
    suspend fun updateMyProfile(data: UpdateProfessionalData): Outcome<ProfessionalProfile>
}

interface AppointmentRepository {
    suspend fun getTypes(): Outcome<List<AppointmentType>>
    suspend fun getMine(): Outcome<List<Appointment>>
    suspend fun getForProfessional(professionalId: Long): Outcome<List<Appointment>>
    suspend fun book(request: BookingRequest): Outcome<Appointment>
    suspend fun updateStatus(id: Long, status: AppointmentStatus): Outcome<Appointment>
    fun observeCached(): Flow<List<Appointment>>
}

interface LibraryRepository {
    suspend fun getAll(): Outcome<List<LibraryResource>>
    suspend fun getById(id: Long): Outcome<LibraryResource>
}

interface SubscriptionRepository {
    /** Success(null) means the user has no plan yet (backend 404). */
    suspend fun getMine(): Outcome<Subscription?>
    suspend fun subscribe(payment: PaymentData): Outcome<Subscription>
}

/** Local-only wellness data (Room). */
interface WellnessRepository {
    fun observeBreathingSessions(): Flow<List<BreathingSession>>
    suspend fun addBreathingSession(exerciseId: String, durationSeconds: Int)
    fun observeWorkload(): Flow<List<WorkloadEntry>>
    suspend fun addWorkload(date: LocalDate, hours: Double, note: String)
    suspend fun deleteWorkload(id: Long)
    fun observeChallenges(): Flow<List<ChallengeProgress>>
    suspend fun completeChallengeDay(challengeId: String, today: LocalDate)
    suspend fun resetChallenge(challengeId: String)
    suspend fun clearAll()
}
