package com.example.neurozen_front.domain.model

import java.time.Instant
import java.time.LocalDate

enum class Role {
    USER, PSYCHOLOGIST, ADMIN, UNKNOWN;

    companion object {
        fun from(value: String?): Role =
            entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: UNKNOWN
    }
}

data class Session(
    val token: String,
    val userId: Long,
    val fullName: String,
    val email: String,
    val role: Role,
    val expiresAtEpochSeconds: Long? = null
) {
    fun isExpired(nowEpochSeconds: Long): Boolean =
        expiresAtEpochSeconds?.let { nowEpochSeconds >= it } ?: false
}

data class RegisterData(
    val fullName: String,
    val email: String,
    val password: String,
    val role: Role,
    val acceptedTerms: Boolean,
    val specialty: String? = null,
    val licenseNumber: String? = null
)

data class UserProfile(
    val id: Long,
    val email: String,
    val fullName: String,
    val role: Role,
    val phoneNumber: String?,
    val ageRange: String?,
    val occupation: String?,
    val workType: String?
)

data class UpdateProfileData(
    val fullName: String,
    val email: String,
    val phoneNumber: String?,
    val ageRange: String?,
    val occupation: String?,
    val workType: String?
)

enum class StressLevel(val label: String) {
    LOW("Bajo"), MODERATE("Moderado"), HIGH("Alto");

    companion object {
        /** Accepts the server value in English or Spanish. */
        fun fromServer(value: String?): StressLevel? = when (value?.trim()?.uppercase()) {
            "LOW", "BAJO" -> LOW
            "MODERATE", "MEDIUM", "MODERADO", "MEDIO" -> MODERATE
            "HIGH", "ALTO" -> HIGH
            else -> null
        }

        /** Same thresholds the backend uses for trigger intensity (1-3, 4-6, 7-10). */
        fun fromTriggerLevel(level: Int): StressLevel = when {
            level <= 3 -> LOW
            level <= 6 -> MODERATE
            else -> HIGH
        }
    }
}

data class StressTrigger(
    val id: Long,
    val description: String,
    val category: String,
    val stressLevel: Int,
    val intensity: String,
    val triggeredAt: Instant
)

data class Assessment(
    val id: Long,
    val answers: List<Int>,
    val score: Int,
    val level: StressLevel,
    val completedAt: Instant
)

data class HealthMetric(
    val id: Long?,
    val stressLevel: Int,
    val heartRate: Int?,
    val sleepHours: Double?,
    val notes: String?,
    val recordedAt: Instant?
)

data class Professional(
    val id: Long,
    val userId: Long,
    val name: String,
    val specialty: String,
    val bio: String,
    val experienceYears: Int?,
    val price: Double?,
    val rating: Double,
    val reviews: Int,
    val availability: String,
    val imageUrl: String?
)

data class Review(
    val id: Long,
    val userName: String,
    val rating: Int,
    val comment: String,
    val createdAt: Instant?
)

enum class VerificationStatus(val label: String) {
    PENDING("En revisión"), VERIFIED("Verificado"), REJECTED("Rechazado"), UNKNOWN("Desconocido");

    companion object {
        fun from(value: String?): VerificationStatus =
            entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: UNKNOWN
    }
}

data class AvailabilitySchedule(
    val days: List<String>,
    val start: String,
    val end: String
)

data class ProfessionalProfile(
    val id: Long,
    val userId: Long,
    val name: String,
    val email: String,
    val specialty: String,
    val licenseNumber: String,
    val status: VerificationStatus,
    val rejectionReason: String?,
    val bio: String,
    val experienceYears: Int?,
    val price: Double?,
    val rating: Double,
    val reviews: Int,
    val schedule: AvailabilitySchedule?,
    val availability: String
) {
    /** The 4 steps shown as "x de 4 pasos completados": bio, experience, price, availability. */
    val completedSteps: List<Boolean>
        get() = listOf(
            bio.isNotBlank(),
            (experienceYears ?: 0) > 0,
            (price ?: 0.0) > 0.0,
            schedule != null && schedule.days.isNotEmpty()
        )
}

data class UpdateProfessionalData(
    val fullName: String,
    val specialty: String,
    val licenseNumber: String,
    val bio: String,
    val experienceYears: Int?,
    val price: Double?,
    val schedule: AvailabilitySchedule?,
    val availability: String
)

data class AppointmentType(
    val value: Int,
    val displayName: String,
    val description: String,
    val durationMinutes: Int
)

enum class AppointmentStatus(val label: String) {
    PENDING("Pendiente"), CONFIRMED("Confirmada"), COMPLETED("Atendida"), CANCELLED("Cancelada"), UNKNOWN("Desconocido");

    companion object {
        fun from(value: String?): AppointmentStatus =
            entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: UNKNOWN
    }
}

data class Appointment(
    val id: Long,
    val patientId: Long,
    val patientName: String?,
    val patientEmail: String?,
    val professionalId: Long,
    val dateTime: Instant,
    val type: Int,
    val status: AppointmentStatus,
    val notes: String?,
    val reviewed: Boolean
)

data class BookingRequest(
    val professionalId: Long,
    val date: LocalDate,
    val slot: String,
    val type: Int,
    val notes: String?
)

data class LastAssessmentSummary(val score: Int, val level: StressLevel?, val completedAt: Instant?)

data class StressStats(
    val current: Double,
    val average: Double,
    val weeklyChange: Double,
    val peakHours: String,
    val triggersCount: Int,
    val topCategory: String,
    val lastAssessment: LastAssessmentSummary?
)

data class Report(
    val id: Long,
    val patientId: Long,
    val patientName: String?,
    val professionalId: Long,
    val appointmentId: Long,
    val stats: StressStats,
    val note: String?,
    val createdAt: Instant?
)

data class LibraryResource(
    val id: Long,
    val title: String,
    val description: String,
    val content: String?,
    val type: String,
    val contentUrl: String?,
    val durationMinutes: Int,
    val thumbnail: String?,
    val tags: List<String>,
    val author: String?
)

data class Subscription(
    val id: Long,
    val planId: Int,
    val isActive: Boolean,
    val cardLast4: String?,
    val expirationDate: String?
)

data class SubscriptionPlan(val id: Int, val name: String, val pricePen: Double, val features: List<String>)

data class PaymentData(
    val planId: Int,
    val firstName: String,
    val lastName: String,
    val email: String,
    val cardNumber: String,
    val expiration: String,
    val cvv: String
)

data class BreathingSession(val id: Long, val exerciseId: String, val durationSeconds: Int, val completedAt: Instant)

data class WorkloadEntry(val id: Long, val date: LocalDate, val hours: Double, val note: String)

data class ChallengeProgress(val challengeId: String, val completedDays: Int, val lastCompletedDate: LocalDate?)
