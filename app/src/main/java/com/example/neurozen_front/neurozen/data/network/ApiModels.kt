package com.example.neurozen_front.neurozen.data.network

import com.google.gson.annotations.SerializedName

// --- Auth Models ---
data class SignInRequest(
    @SerializedName(value = "Username", alternate = ["username"]) val username: String,
    @SerializedName(value = "Password", alternate = ["password"]) val password: String
)

data class SignInResponse(
    @SerializedName(value = "Id", alternate = ["id"]) val id: String, // UUID del usuario
    @SerializedName(value = "Username", alternate = ["username"]) val username: String,
    @SerializedName(value = "Token", alternate = ["token"]) val token: String,
    @SerializedName(value = "Email", alternate = ["email"]) val email: String? = ""
)

data class AuthSession(
    val token: String,
    val userId: String, // UUID del usuario
    val username: String,
    val email: String
)

data class SignUpRequest(
    @SerializedName(value = "Username", alternate = ["username"]) val username: String,
    @SerializedName(value = "Password", alternate = ["password"]) val password: String,
    @SerializedName(value = "Email", alternate = ["email"]) val email: String
)

data class SignUpResponse(
    @SerializedName(value = "Id", alternate = ["id"]) val id: String, // UUID del usuario
    @SerializedName(value = "Username", alternate = ["username"]) val username: String,
    @SerializedName(value = "Email", alternate = ["email"]) val email: String
)

// --- Professionals Models ---
data class ProfessionalResource(
    @SerializedName(value = "Id", alternate = ["id"]) val id: Int, // Los profesionales parecen seguir siendo Int
    @SerializedName(value = "FirstName", alternate = ["firstName", "Name", "name"]) val firstName: String,
    @SerializedName(value = "LastName", alternate = ["lastName", "lastname"]) val lastName: String? = "",
    @SerializedName(value = "Specialization", alternate = ["specialization", "Specialty", "specialty"]) val specialization: String,
    @SerializedName(value = "Email", alternate = ["email"]) val email: String? = "",
    @SerializedName(value = "Phone", alternate = ["phone"]) val phone: String? = "",
    @SerializedName(value = "ImageUrl", alternate = ["imageUrl", "Image", "image"]) val imageUrl: String,
    @SerializedName(value = "Rating", alternate = ["rating"]) val rating: Double? = 0.0,
    @SerializedName(value = "Experience", alternate = ["experience"]) val experience: String? = "",
    @SerializedName(value = "Bio", alternate = ["bio"]) val bio: String? = "",
    @SerializedName(value = "Price", alternate = ["price"]) val price: Double? = 0.0,
    @SerializedName(value = "Availability", alternate = ["availability"]) val availability: String? = ""
)

// --- Appointments Models ---
data class AppointmentRequest(
    @SerializedName("PatientId") val patientId: String,
    @SerializedName("ProfessionalId") val professionalId: Int,
    @SerializedName("AppointmentDate") val appointmentDate: String, // Cambiado de AppointmentDateTime a AppointmentDate
    @SerializedName("AppointmentType") val appointmentType: Int,
    @SerializedName("NotasAdicionales") val notasAdicionales: String? = null
)

data class AppointmentResponse(
    @SerializedName(value = "Id", alternate = ["id"]) val id: Int,
    @SerializedName(value = "PatientId", alternate = ["patientId"]) val patientId: String,
    @SerializedName(value = "ProfessionalId", alternate = ["professionalId"]) val professionalId: Int,
    @SerializedName(value = "AppointmentDate", alternate = ["appointmentDate", "appointmentDateTime", "AppointmentDateTime"]) val appointmentDate: String,
    @SerializedName(value = "Status", alternate = ["status"]) val status: String,
    @SerializedName(value = "ProfessionalName", alternate = ["professionalName"]) val professionalName: String? = null
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

// --- Subscriptions Models ---
data class SubscriptionRequest(
    @SerializedName("UserId") val userId: String, // UUID
    @SerializedName("PlanId") val planId: Int,
    @SerializedName("NameUser") val nameUser: String,
    @SerializedName("LastNameUser") val lastNameUser: String,
    @SerializedName("EmailUser") val emailUser: String
)

data class SubscriptionResource(
    @SerializedName(value = "Id", alternate = ["id"]) val id: Int,
    @SerializedName(value = "UserId", alternate = ["userId"]) val userId: String, // UUID
    @SerializedName(value = "PlanName", alternate = ["planName"]) val planName: String,
    @SerializedName(value = "Status", alternate = ["status"]) val status: String,
    @SerializedName(value = "StartDate", alternate = ["startDate"]) val startDate: String,
    @SerializedName(value = "EndDate", alternate = ["endDate"]) val endDate: String
)

// --- Health Metrics Models (Triggers/Emociones) ---
data class HealthMetricRequest(
    @SerializedName("UserId") val userId: String, // UUID
    @SerializedName("StressLevel") val stressLevel: Int,
    @SerializedName("HeartRate") val heartRate: Int,
    @SerializedName("SleepHours") val sleepHours: Int,
    @SerializedName("Notes") val notes: String
)

data class HealthMetricResource(
    @SerializedName(value = "Id", alternate = ["id"]) val id: Int,
    @SerializedName(value = "UserId", alternate = ["userId"]) val userId: String, // UUID
    @SerializedName(value = "StressLevel", alternate = ["stressLevel"]) val stressLevel: Int,
    @SerializedName(value = "HeartRate", alternate = ["heartRate"]) val heartRate: Int,
    @SerializedName(value = "SleepHours", alternate = ["sleepHours"]) val sleepHours: Int,
    @SerializedName(value = "Notes", alternate = ["notes"]) val notes: String,
    @SerializedName(value = "CreatedAt", alternate = ["createdAt"]) val createdAt: String
)
