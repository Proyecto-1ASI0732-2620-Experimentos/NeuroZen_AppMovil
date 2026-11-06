package com.example.neurozen_front.data.repository

import com.example.neurozen_front.data.local.AppointmentCacheDao
import com.example.neurozen_front.data.local.AppointmentCacheEntity
import com.example.neurozen_front.data.remote.ChangePasswordDto
import com.example.neurozen_front.data.remote.CreateAppointmentDto
import com.example.neurozen_front.data.remote.CreateAssessmentDto
import com.example.neurozen_front.data.remote.CreateHealthMetricDto
import com.example.neurozen_front.data.remote.CreateReportDto
import com.example.neurozen_front.data.remote.CreateReviewDto
import com.example.neurozen_front.data.remote.CreateSubscriptionDto
import com.example.neurozen_front.data.remote.CreateTriggerDto
import com.example.neurozen_front.data.remote.ForgotPasswordDto
import com.example.neurozen_front.data.remote.LastAssessmentDto
import com.example.neurozen_front.data.remote.NeuroZenApi
import com.example.neurozen_front.data.remote.ResetPasswordDto
import com.example.neurozen_front.data.remote.SignInDto
import com.example.neurozen_front.data.remote.SignUpDto
import com.example.neurozen_front.data.remote.UpdateMyProfessionalDto
import com.example.neurozen_front.data.remote.UpdateStatusDto
import com.example.neurozen_front.data.remote.UpdateUserDto
import com.example.neurozen_front.data.remote.apiCall
import com.example.neurozen_front.data.remote.apiCallNoBody
import com.example.neurozen_front.data.remote.toDomain
import com.example.neurozen_front.data.remote.toDto
import com.example.neurozen_front.data.remote.toSession
import com.example.neurozen_front.domain.logic.CardValidator
import com.example.neurozen_front.domain.logic.DateTimeUtils
import com.example.neurozen_front.domain.model.AppError
import com.example.neurozen_front.domain.model.Appointment
import com.example.neurozen_front.domain.model.AppointmentStatus
import com.example.neurozen_front.domain.model.AppointmentType
import com.example.neurozen_front.domain.model.Assessment
import com.example.neurozen_front.domain.model.BookingRequest
import com.example.neurozen_front.domain.model.HealthMetric
import com.example.neurozen_front.domain.model.LibraryResource
import com.example.neurozen_front.domain.model.Outcome
import com.example.neurozen_front.domain.model.PaymentData
import com.example.neurozen_front.domain.model.Professional
import com.example.neurozen_front.domain.model.ProfessionalProfile
import com.example.neurozen_front.domain.model.RegisterData
import com.example.neurozen_front.domain.model.Report
import com.example.neurozen_front.domain.model.Review
import com.example.neurozen_front.domain.model.Role
import com.example.neurozen_front.domain.model.Session
import com.example.neurozen_front.domain.model.StressStats
import com.example.neurozen_front.domain.model.StressTrigger
import com.example.neurozen_front.domain.model.Subscription
import com.example.neurozen_front.domain.model.UpdateProfessionalData
import com.example.neurozen_front.domain.model.UpdateProfileData
import com.example.neurozen_front.domain.model.UserProfile
import com.example.neurozen_front.domain.repository.AppointmentRepository
import com.example.neurozen_front.domain.repository.AuthRepository
import com.example.neurozen_front.domain.repository.LibraryRepository
import com.example.neurozen_front.domain.repository.ProfessionalRepository
import com.example.neurozen_front.domain.repository.ReportRepository
import com.example.neurozen_front.domain.repository.SessionRepository
import com.example.neurozen_front.domain.repository.StressRepository
import com.example.neurozen_front.domain.repository.SubscriptionRepository
import com.example.neurozen_front.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: NeuroZenApi,
    private val sessions: SessionRepository
) : AuthRepository {

    override suspend fun signIn(email: String, password: String): Outcome<Session> {
        // The backend expects the email in the "username" field.
        val result = apiCall({ api.signIn(SignInDto(email.trim(), password)) }) { it.toSession() }
        if (result is Outcome.Success) sessions.save(result.data)
        return result
    }

    override suspend fun signUp(data: RegisterData): Outcome<Session> {
        val parts = data.fullName.trim().split(Regex("\\s+"), limit = 2)
        val body = SignUpDto(
            username = data.email.trim(), email = data.email.trim(), password = data.password,
            fullName = data.fullName.trim(), firstName = parts.firstOrNull(), lastName = parts.getOrNull(1),
            role = data.role.name, acceptedTerms = data.acceptedTerms,
            specialty = data.specialty?.trim()?.takeIf { data.role == Role.PSYCHOLOGIST },
            licenseNumber = data.licenseNumber?.trim()?.takeIf { data.role == Role.PSYCHOLOGIST }
        )
        val result = apiCall({ api.signUp(body) }) { it.toSession() }
        if (result is Outcome.Success) sessions.save(result.data, isNewAccount = true)
        return result
    }

    override suspend fun forgotPassword(email: String): Outcome<String> =
        apiCall({ api.forgotPassword(ForgotPasswordDto(email.trim())) }) {
            it.message ?: "Si el correo está registrado, te enviamos un enlace para restablecer tu contraseña."
        }

    override suspend fun resetPassword(token: String, newPassword: String): Outcome<String> =
        apiCall({ api.resetPassword(ResetPasswordDto(token.trim(), newPassword)) }) {
            it.message ?: "Tu contraseña fue actualizada."
        }
}

class UserRepositoryImpl @Inject constructor(
    private val api: NeuroZenApi,
    private val sessions: SessionRepository
) : UserRepository {
    override suspend fun getMe(): Outcome<UserProfile> = apiCall({ api.getUser(sessions.requireUserId()) }) { it.toDomain() }

    override suspend fun updateMe(data: UpdateProfileData): Outcome<UserProfile> {
        val body = UpdateUserDto(
            username = data.email.trim(), email = data.email.trim(), fullName = data.fullName.trim(),
            phoneNumber = data.phoneNumber?.trim()?.ifBlank { null }, ageRange = data.ageRange,
            occupation = data.occupation?.trim()?.ifBlank { null }, workType = data.workType
        )
        val result = apiCall({ api.updateUser(sessions.requireUserId(), body) }) { it.toDomain() }
        if (result is Outcome.Success) {
            sessions.session.value?.let { sessions.save(it.copy(fullName = result.data.fullName, email = result.data.email)) }
        }
        return result
    }

    override suspend fun changePassword(current: String, new: String): Outcome<Unit> =
        apiCallNoBody { api.changePassword(sessions.requireUserId(), ChangePasswordDto(current, new)) }

    override suspend fun deleteMe(): Outcome<Unit> = apiCallNoBody { api.deleteUser(sessions.requireUserId()) }
}

class StressRepositoryImpl @Inject constructor(
    private val api: NeuroZenApi,
    private val sessions: SessionRepository
) : StressRepository {
    override suspend fun getTriggers(): Outcome<List<StressTrigger>> =
        apiCall({ api.getTriggers(sessions.requireUserId()) }) { list -> list.map { it.toDomain() }.sortedByDescending { it.triggeredAt } }

    override suspend fun addTrigger(category: String, level: Int, description: String, at: Instant): Outcome<StressTrigger> {
        if (level !in 1..10) return Outcome.Failure(AppError.Validation("El nivel debe estar entre 1 y 10."))
        if (at.isAfter(Instant.now())) return Outcome.Failure(AppError.Validation("La fecha no puede estar en el futuro."))
        val body = CreateTriggerDto(sessions.requireUserId(), description.trim(), category, level, at.toString())
        return apiCall({ api.createTrigger(body) }) { it.toDomain() }
    }

    override suspend fun deleteTrigger(id: Long): Outcome<Unit> = apiCallNoBody { api.deleteTrigger(id) }

    override suspend fun getAssessments(): Outcome<List<Assessment>> =
        apiCall({ api.getAssessments(sessions.requireUserId()) }) { list -> list.map { it.toDomain() }.sortedByDescending { it.completedAt } }

    override suspend fun submitAssessment(answers: List<Int>): Outcome<Assessment> =
        apiCall({ api.createAssessment(CreateAssessmentDto(sessions.requireUserId(), answers)) }) { it.toDomain() }

    override suspend fun getHealthMetrics(): Outcome<List<HealthMetric>> =
        apiCall({ api.getHealthMetrics(sessions.requireUserId()) }) { list -> list.map { it.toDomain() } }

    override suspend fun addHealthMetric(metric: HealthMetric): Outcome<Unit> = apiCallNoBody {
        api.createHealthMetric(sessions.requireUserId(), CreateHealthMetricDto(metric.stressLevel, metric.heartRate, metric.sleepHours, metric.notes))
    }
}

class ReportRepositoryImpl @Inject constructor(
    private val api: NeuroZenApi,
    private val sessions: SessionRepository
) : ReportRepository {
    override suspend fun getMyReports(): Outcome<List<Report>> =
        apiCall({ api.getReports(patientId = sessions.requireUserId()) }) { list -> list.map { it.toDomain() } }

    override suspend fun getReportsForProfessional(professionalId: Long): Outcome<List<Report>> =
        apiCall({ api.getReports(professionalId = professionalId) }) { list -> list.map { it.toDomain() } }

    override suspend fun share(appointmentId: Long, professionalId: Long, stats: StressStats, note: String?): Outcome<Report> {
        val body = CreateReportDto(
            patientId = sessions.requireUserId(), professionalId = professionalId, appointmentId = appointmentId,
            stressCurrent = stats.current, stressAverage = stats.average, weeklyChange = stats.weeklyChange,
            peakHours = stats.peakHours, triggersCount = stats.triggersCount, topCategory = stats.topCategory,
            lastAssessment = stats.lastAssessment?.let { LastAssessmentDto(it.score, completedAt = it.completedAt?.toString()) },
            note = note
        )
        return apiCall({ api.createReport(body) }) { it.toDomain() }
    }
}

class ProfessionalRepositoryImpl @Inject constructor(private val api: NeuroZenApi) : ProfessionalRepository {
    override suspend fun getDirectory(): Outcome<List<Professional>> =
        apiCall({ api.getProfessionals() }) { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: Long): Outcome<Professional> = apiCall({ api.getProfessional(id) }) { it.toDomain() }

    override suspend fun getReviews(id: Long): Outcome<List<Review>> = apiCall({ api.getReviews(id) }) { list -> list.map { it.toDomain() } }

    override suspend fun addReview(professionalId: Long, appointmentId: Long, rating: Int, comment: String): Outcome<Review> {
        if (rating !in 1..5) return Outcome.Failure(AppError.Validation("Elige de 1 a 5 estrellas."))
        return apiCall({ api.createReview(professionalId, CreateReviewDto(appointmentId, rating, comment.trim())) }) { it.toDomain() }
    }

    override suspend fun getAvailableSlots(id: Long, date: LocalDate): Outcome<List<String>> =
        apiCall({ api.getAvailableSlots(id, date.toString()) }) { it.sorted() }

    override suspend fun getMyProfile(): Outcome<ProfessionalProfile> = apiCall({ api.getMyProfessional() }) { it.toDomain() }

    override suspend fun updateMyProfile(data: UpdateProfessionalData): Outcome<ProfessionalProfile> {
        val body = UpdateMyProfessionalDto(
            fullName = data.fullName.trim(), specialty = data.specialty.trim(), licenseNumber = data.licenseNumber.trim(),
            bio = data.bio.trim(), experienceYears = data.experienceYears, price = data.price,
            availabilitySchedule = data.schedule?.toDto(), availability = data.availability
        )
        return apiCall({ api.updateMyProfessional(body) }) { it.toDomain() }
    }
}

class AppointmentRepositoryImpl @Inject constructor(
    private val api: NeuroZenApi,
    private val sessions: SessionRepository,
    private val cache: AppointmentCacheDao
) : AppointmentRepository {

    override suspend fun getTypes(): Outcome<List<AppointmentType>> =
        apiCall({ api.getAppointmentTypes() }) { list -> list.map { it.toDomain() } }

    override suspend fun getMine(): Outcome<List<Appointment>> {
        val result = apiCall({ api.getAppointments(patientId = sessions.requireUserId()) }) { list -> list.map { it.toDomain() } }
        if (result is Outcome.Success) cache.replaceAll(result.data.map { it.toCache() })
        return result
    }

    override suspend fun getForProfessional(professionalId: Long): Outcome<List<Appointment>> {
        val result = apiCall({ api.getAppointments(professionalId = professionalId) }) { list -> list.map { it.toDomain() } }
        if (result is Outcome.Success) cache.replaceAll(result.data.map { it.toCache() })
        return result
    }

    override suspend fun book(request: BookingRequest): Outcome<Appointment> {
        val body = CreateAppointmentDto(
            patientId = sessions.requireUserId(), professionalId = request.professionalId,
            appointmentDateTime = DateTimeUtils.limaSlotToUtcIso(request.date, request.slot),
            appointmentType = request.type, notasAdicionales = request.notes?.trim()?.ifBlank { null }
        )
        return apiCall({ api.createAppointment(body) }) { it.toDomain() }
    }

    override suspend fun updateStatus(id: Long, status: AppointmentStatus): Outcome<Appointment> =
        apiCall({ api.updateAppointmentStatus(id, UpdateStatusDto(status.name)) }) { it.toDomain() }

    override fun observeCached(): Flow<List<Appointment>> = cache.observeAll().map { list ->
        list.map {
            Appointment(it.id, it.patientId, it.patientName, null, it.professionalId, Instant.ofEpochMilli(it.dateTimeEpochMillis),
                it.type, AppointmentStatus.from(it.status), null, it.reviewed)
        }
    }

    private fun Appointment.toCache() = AppointmentCacheEntity(
        id, patientId, patientName, professionalId, dateTime.toEpochMilli(), type, status.name, reviewed
    )
}

class LibraryRepositoryImpl @Inject constructor(private val api: NeuroZenApi) : LibraryRepository {
    override suspend fun getAll(): Outcome<List<LibraryResource>> = apiCall({ api.getResources() }) { list -> list.map { it.toDomain() } }
    override suspend fun getById(id: Long): Outcome<LibraryResource> = apiCall({ api.getResource(id) }) { it.toDomain() }
}

class SubscriptionRepositoryImpl @Inject constructor(
    private val api: NeuroZenApi,
    private val sessions: SessionRepository
) : SubscriptionRepository {
    override suspend fun getMine(): Outcome<Subscription?> =
        when (val result = apiCall({ api.getSubscription(sessions.requireUserId()) }) { it.toDomain() }) {
            is Outcome.Success -> result
            is Outcome.Failure -> if (result.error is AppError.NotFound) Outcome.Success(null) else result
        }

    override suspend fun subscribe(payment: PaymentData): Outcome<Subscription> {
        if (!CardValidator.isValidNumber(payment.cardNumber)) return Outcome.Failure(AppError.Validation("Número de tarjeta inválido."))
        if (!CardValidator.isValidExpiration(payment.expiration)) return Outcome.Failure(AppError.Validation("Fecha de vencimiento inválida (MM/AA)."))
        if (!CardValidator.isValidCvv(payment.cvv)) return Outcome.Failure(AppError.Validation("CVV inválido."))
        val body = CreateSubscriptionDto(
            userId = sessions.requireUserId(), planId = payment.planId, nameUser = payment.firstName.trim(),
            lastNameUser = payment.lastName.trim(), emailUser = payment.email.trim(),
            numberCard = CardValidator.digitsOnly(payment.cardNumber), expirationDate = payment.expiration, cvv = payment.cvv
        )
        return apiCall({ api.createSubscription(body) }) { it.toDomain() }
    }
}
