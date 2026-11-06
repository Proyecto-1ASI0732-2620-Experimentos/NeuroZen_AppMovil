package com.example.neurozen_front.data.remote

import com.google.gson.annotations.SerializedName

// All fields nullable: the app must not crash if the backend omits a value.

data class ErrorResponseDto(val message: String?, val errors: Map<String, List<String>>?)
data class MessageDto(val message: String?)

data class SignInDto(val username: String, val password: String)

data class SignUpDto(
    val username: String,
    val email: String,
    val password: String,
    val fullName: String,
    val firstName: String?,
    val lastName: String?,
    val role: String,
    val acceptedTerms: Boolean,
    val specialty: String? = null,
    val licenseNumber: String? = null
)

data class AuthenticatedUserDto(val id: Long?, val fullName: String?, val email: String?, val role: String?, val token: String?)
data class ForgotPasswordDto(val email: String)
data class ResetPasswordDto(val token: String, val newPassword: String)
data class ChangePasswordDto(val currentPassword: String, val newPassword: String)

data class UserDto(
    val id: Long?, val email: String?, val fullName: String?, val role: String?, val phoneNumber: String?,
    val address: String?, val avatarUrl: String?, val dateOfBirth: String?, val ageRange: String?,
    val occupation: String?, val workType: String?, val createdAt: String?
)

data class UpdateUserDto(
    val username: String?, val email: String?, val fullName: String?, val phoneNumber: String?,
    val ageRange: String?, val occupation: String?, val workType: String?
)

data class TriggerDto(
    val id: Long?, val userId: Long?, val description: String?, val category: String?,
    val stressLevel: Int?, val intensity: String?, val triggeredAt: String?
)

data class CreateTriggerDto(val userId: Long, val description: String, val category: String, val stressLevel: Int, val triggeredAt: String)

data class AssessmentDto(val id: Long?, val userId: Long?, val answers: List<Int>?, val score: Int?, val level: String?, val completedAt: String?)
data class CreateAssessmentDto(val userId: Long, val answers: List<Int>)

data class HealthMetricDto(
    val id: Long?, val stressLevel: Int?, val heartRate: Int?, val sleepHours: Double?, val notes: String?,
    @SerializedName(value = "recordedAt", alternate = ["createdAt", "date", "timestamp"]) val recordedAt: String?
)

data class CreateHealthMetricDto(val stressLevel: Int, val heartRate: Int?, val sleepHours: Double?, val notes: String?)

data class ProfessionalDto(
    val id: Long?, val userId: Long?, val name: String?, val specialty: String?, val bio: String?,
    val experienceYears: Int?, val price: Double?, val rating: Double?, val reviews: Int?,
    val availability: String?, val image: String?, val verificationStatus: String?
)

data class ScheduleDto(val days: List<String>?, val start: String?, val end: String?)

data class MyProfessionalDto(
    val id: Long?, val userId: Long?, val name: String?, val email: String?, val specialty: String?,
    val licenseNumber: String?, val verificationStatus: String?, val rejectionReason: String?, val bio: String?,
    val experienceYears: Int?, val price: Double?, val rating: Double?, val reviews: Int?,
    val availabilitySchedule: ScheduleDto?, val availability: String?, val createdAt: String?
)

data class UpdateMyProfessionalDto(
    val fullName: String, val specialty: String, val licenseNumber: String, val bio: String,
    val experienceYears: Int?, val price: Double?, val availabilitySchedule: ScheduleDto?, val availability: String
)

data class ReviewDto(val id: Long?, val userName: String?, val rating: Int?, val comment: String?, val createdAt: String?)
data class CreateReviewDto(val appointmentId: Long, val rating: Int, val comment: String)

data class AppointmentTypeDto(val value: Int?, val name: String?, val displayName: String?, val description: String?, val estimatedDurationMinutes: Int?)

data class AppointmentDto(
    val id: Long?, val patientId: Long?, val patientName: String?, val patientEmail: String?, val professionalId: Long?,
    val appointmentDateTime: String?, val appointmentType: Int?, val status: String?, val notasAdicionales: String?, val reviewed: Boolean?
)

data class CreateAppointmentDto(
    val patientId: Long, val professionalId: Long, val appointmentDateTime: String, val appointmentType: Int, val notasAdicionales: String?
)

data class UpdateStatusDto(val status: String)

data class LastAssessmentDto(val score: Int?, val level: String? = null, val completedAt: String?)

data class ReportDto(
    val id: Long?, val patientId: Long?, val patientName: String?, val professionalId: Long?, val appointmentId: Long?,
    val stressCurrent: Double?, val stressAverage: Double?, val weeklyChange: Double?, val peakHours: String?,
    val triggersCount: Int?, val topCategory: String?, val lastAssessment: LastAssessmentDto?, val note: String?, val createdAt: String?
)

data class CreateReportDto(
    val patientId: Long, val professionalId: Long, val appointmentId: Long, val stressCurrent: Double, val stressAverage: Double,
    val weeklyChange: Double, val peakHours: String, val triggersCount: Int, val topCategory: String,
    val lastAssessment: LastAssessmentDto?, val note: String?
)

data class ResourceDto(
    val id: Long?, val title: String?, val description: String?, val content: String?, val resourceType: String?,
    val contentUrl: String?, val duration: Int?, val thumbnail: String?, val tags: List<String>?, val author: String?, val createdAt: String?
)

data class SubscriptionDto(
    val id: Long?, val userId: Long?, val planId: Int?, val isActive: Boolean?, val nameUser: String?, val lastNameUser: String?,
    val emailUser: String?, val cardLast4: String?, val expirationDate: String?, val createdAt: String?
)

data class CreateSubscriptionDto(
    val userId: Long, val planId: Int, val nameUser: String, val lastNameUser: String, val emailUser: String,
    val numberCard: String, val expirationDate: String, val cvv: String
)
