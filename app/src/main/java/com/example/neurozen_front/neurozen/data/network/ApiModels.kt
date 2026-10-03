package com.example.neurozen_front.neurozen.data.network

import com.google.gson.annotations.SerializedName

// --- Role Definition ---
enum class UserRole {
    @SerializedName("CLIENT", alternate = ["Client", "client", "PATIENT", "Patient", "patient"])
    CLIENT,
    
    @SerializedName("PSYCHOLOGIST", alternate = ["Psychologist", "psychologist", "PROFESSIONAL", "Professional", "professional"])
    PSYCHOLOGIST
}

// --- Auth Models ---
data class SignInRequest(
    @SerializedName(value = "Username", alternate = ["username"]) val username: String,
    @SerializedName(value = "Password", alternate = ["password"]) val password: String
)

data class SignInResponse(
    @SerializedName(value = "Id", alternate = ["id"]) val id: String, // UUID del usuario
    @SerializedName(value = "Username", alternate = ["username"]) val username: String,
    @SerializedName(value = "Token", alternate = ["token"]) val token: String,
    @SerializedName(value = "Email", alternate = ["email"]) val email: String? = "",
    @SerializedName(value = "Role", alternate = ["role"]) val role: String? = "CLIENT"
)

data class AuthSession(
    val token: String,
    val userId: String,
    val username: String,
    val email: String,
    val role: UserRole = UserRole.CLIENT,
    val professionalId: Int? = null
)

data class SignUpRequest(
    @SerializedName(value = "Username", alternate = ["username"]) val username: String,
    @SerializedName(value = "Password", alternate = ["password"]) val password: String,
    @SerializedName(value = "Email", alternate = ["email"]) val email: String,
    @SerializedName(value = "Role", alternate = ["role"]) val role: String? = "CLIENT"
)

data class SignUpResponse(
    @SerializedName(value = "Id", alternate = ["id"]) val id: String,
    @SerializedName(value = "Username", alternate = ["username"]) val username: String,
    @SerializedName(value = "Email", alternate = ["email"]) val email: String,
    @SerializedName(value = "Role", alternate = ["role"]) val role: String? = "CLIENT"
)

// --- Professionals Models ---
data class CreateProfessionalRequest(
    @SerializedName("FirstName") val firstName: String,
    @SerializedName("LastName") val lastName: String,
    @SerializedName("Specialization") val specialization: String,
    @SerializedName("Email") val email: String,
    @SerializedName("Phone") val phone: String,
    @SerializedName("ImageUrl") val imageUrl: String,
    @SerializedName("Price") val price: Double,
    @SerializedName("Bio") val bio: String,
    @SerializedName("Experience") val experience: String,
    @SerializedName("Availability") val availability: String
)

data class ProfessionalResource(
    @SerializedName(value = "Id", alternate = ["id"]) val id: Int = 0,
    @SerializedName(value = "FirstName", alternate = ["firstName", "Name", "name"]) val firstName: String,
    @SerializedName(value = "LastName", alternate = ["lastName", "lastname"]) val lastName: String? = "",
    @SerializedName(value = "Specialization", alternate = ["specialization", "Specialty", "specialty"]) val specialization: String,
    @SerializedName(value = "Email", alternate = ["email"]) val email: String? = "",
    @SerializedName(value = "Phone", alternate = ["phone"]) val phone: String? = "",
    @SerializedName(value = "ImageUrl", alternate = ["imageUrl", "Image", "image"]) val imageUrl: String = "",
    @SerializedName(value = "Rating", alternate = ["rating"]) val rating: Double? = 5.0,
    @SerializedName(value = "Experience", alternate = ["experience"]) val experience: String? = "",
    @SerializedName(value = "Bio", alternate = ["bio"]) val bio: String? = "",
    @SerializedName(value = "Price", alternate = ["price"]) val price: Double? = 0.0,
    @SerializedName(value = "Availability", alternate = ["availability"]) val availability: String? = "Disponible"
)

// --- Appointments Models ---
data class AppointmentRequest(
    @SerializedName("PatientId") val patientId: String,
    @SerializedName("ProfessionalId") val professionalId: Int,
    @SerializedName("AppointmentDate") val appointmentDate: String,
    @SerializedName("AppointmentType") val appointmentType: Int,
    @SerializedName("NotasAdicionales") val notasAdicionales: String? = null
)

data class AppointmentResponse(
    @SerializedName(value = "Id", alternate = ["id"]) val id: Int,
    @SerializedName(value = "PatientId", alternate = ["patientId"]) val patientId: String,
    @SerializedName(value = "ProfessionalId", alternate = ["professionalId"]) val professionalId: Int,
    @SerializedName(value = "AppointmentDate", alternate = ["appointmentDate", "appointmentDateTime", "AppointmentDateTime"]) val appointmentDate: String,
    @SerializedName(value = "Status", alternate = ["status"]) val status: String,
    @SerializedName(value = "ProfessionalName", alternate = ["professionalName"]) val professionalName: String? = null,
    @SerializedName(value = "PatientName", alternate = ["patientName"]) val patientName: String? = null
)

data class AppointmentTypeResource(
    @SerializedName(value = "Id", alternate = ["id"]) val id: Int,
    @SerializedName(value = "Name", alternate = ["name"]) val name: String,
    @SerializedName(value = "Description", alternate = ["description"]) val description: String? = ""
)

// --- Dashboard / Home Compatibility Models ---
data class DashboardResponse(
    @SerializedName(value = "StressLevel", alternate = ["stressLevel"]) val stressLevel: Int,
    @SerializedName(value = "HeartRate", alternate = ["heartRate"]) val heartRate: Int,
    @SerializedName(value = "SleepHours", alternate = ["sleepHours"]) val sleepHours: Int,
    @SerializedName(value = "NextAppointment", alternate = ["nextAppointment"]) val nextAppointment: NextAppointment?
)

data class NextAppointment(
    @SerializedName(value = "Id", alternate = ["id"]) val id: Int,
    @SerializedName(value = "AppointmentDateTime", alternate = ["appointmentDateTime", "AppointmentDate"]) val appointmentDateTime: String,
    @SerializedName(value = "ProfessionalName", alternate = ["professionalName"]) val professionalName: String,
    @SerializedName(value = "ProfessionalSpecialty", alternate = ["professionalSpecialty", "Specialty"]) val professionalSpecialty: String,
    @SerializedName(value = "Status", alternate = ["status"]) val status: String
)

data class MeditationResource(
    @SerializedName(value = "Id", alternate = ["id"]) val id: Int,
    @SerializedName(value = "Title", alternate = ["title"]) val title: String,
    @SerializedName(value = "Description", alternate = ["description"]) val description: String,
    @SerializedName(value = "DurationMinutes", alternate = ["durationMinutes"]) val durationMinutes: Int,
    @SerializedName(value = "ImageUrl", alternate = ["imageUrl", "Image"]) val imageUrl: String,
    @SerializedName(value = "AudioUrl", alternate = ["audioUrl", "Audio"]) val audioUrl: String
)

// --- Triggers Models (POST /triggers) ---
data class TriggerRequest(
    @SerializedName("UserId") val userId: String,
    @SerializedName("StressLevel") val stressLevel: Int,
    @SerializedName("TriggerSource") val triggerSource: String,
    @SerializedName("Description") val description: String,
    @SerializedName("CreatedAt") val createdAt: String? = null
)

data class TriggerResource(
    @SerializedName(value = "Id", alternate = ["id"]) val id: Int,
    @SerializedName(value = "UserId", alternate = ["userId"]) val userId: String,
    @SerializedName(value = "StressLevel", alternate = ["stressLevel"]) val stressLevel: Int,
    @SerializedName(value = "TriggerSource", alternate = ["triggerSource"]) val triggerSource: String,
    @SerializedName(value = "Description", alternate = ["description"]) val description: String,
    @SerializedName(value = "CreatedAt", alternate = ["createdAt"]) val createdAt: String
)

// --- Resource Library Models (GET & POST /resource-libraries) ---
data class ResourceLibraryItem(
    @SerializedName(value = "Id", alternate = ["id"]) val id: Int = 0,
    @SerializedName(value = "Title", alternate = ["title"]) val title: String,
    @SerializedName(value = "Type", alternate = ["type"]) val type: String, // "Audio", "Video", "Article", "Exercise"
    @SerializedName(value = "Category", alternate = ["category"]) val category: String, // "Mindfulness", "Ansiedad", "Sueño"
    @SerializedName(value = "Description", alternate = ["description"]) val description: String,
    @SerializedName(value = "ContentUrl", alternate = ["contentUrl", "url"]) val contentUrl: String,
    @SerializedName(value = "ImageUrl", alternate = ["imageUrl", "image"]) val imageUrl: String? = "",
    @SerializedName(value = "Author", alternate = ["author"]) val author: String? = "NeuroZen Team"
)

// --- Subscriptions Models ---
data class SubscriptionRequest(
    @SerializedName("UserId") val userId: String,
    @SerializedName("PlanId") val planId: Int,
    @SerializedName("NameUser") val nameUser: String,
    @SerializedName("LastNameUser") val lastNameUser: String,
    @SerializedName("EmailUser") val emailUser: String
)

data class SubscriptionResource(
    @SerializedName(value = "Id", alternate = ["id"]) val id: Int,
    @SerializedName(value = "UserId", alternate = ["userId"]) val userId: String,
    @SerializedName(value = "PlanName", alternate = ["planName", "PlanId"]) val planName: String,
    @SerializedName(value = "Status", alternate = ["status", "IsActive"]) val status: String,
    @SerializedName(value = "StartDate", alternate = ["startDate"]) val startDate: String,
    @SerializedName(value = "EndDate", alternate = ["endDate"]) val endDate: String
)

// --- Legacy Compatibility Health Metric ---
data class HealthMetricRequest(
    @SerializedName("UserId") val userId: String,
    @SerializedName("StressLevel") val stressLevel: Int,
    @SerializedName("HeartRate") val heartRate: Int,
    @SerializedName("SleepHours") val sleepHours: Int,
    @SerializedName("Notes") val notes: String
)

data class HealthMetricResource(
    @SerializedName(value = "Id", alternate = ["id"]) val id: Int,
    @SerializedName(value = "UserId", alternate = ["userId"]) val userId: String,
    @SerializedName(value = "StressLevel", alternate = ["stressLevel"]) val stressLevel: Int,
    @SerializedName(value = "HeartRate", alternate = ["heartRate"]) val heartRate: Int,
    @SerializedName(value = "SleepHours", alternate = ["sleepHours"]) val sleepHours: Int,
    @SerializedName(value = "Notes", alternate = ["notes"]) val notes: String,
    @SerializedName(value = "CreatedAt", alternate = ["createdAt"]) val createdAt: String
)
