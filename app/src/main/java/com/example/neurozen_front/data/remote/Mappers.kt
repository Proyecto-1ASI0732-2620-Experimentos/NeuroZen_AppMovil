package com.example.neurozen_front.data.remote

import com.example.neurozen_front.domain.logic.AssessmentScoring
import com.example.neurozen_front.domain.logic.DateTimeUtils.parseInstant
import com.example.neurozen_front.domain.logic.JwtDecoder
import com.example.neurozen_front.domain.model.Appointment
import com.example.neurozen_front.domain.model.AppointmentStatus
import com.example.neurozen_front.domain.model.AppointmentType
import com.example.neurozen_front.domain.model.Assessment
import com.example.neurozen_front.domain.model.AvailabilitySchedule
import com.example.neurozen_front.domain.model.HealthMetric
import com.example.neurozen_front.domain.model.LastAssessmentSummary
import com.example.neurozen_front.domain.model.LibraryResource
import com.example.neurozen_front.domain.model.Professional
import com.example.neurozen_front.domain.model.ProfessionalProfile
import com.example.neurozen_front.domain.model.Report
import com.example.neurozen_front.domain.model.Review
import com.example.neurozen_front.domain.model.Role
import com.example.neurozen_front.domain.model.Session
import com.example.neurozen_front.domain.model.StressLevel
import com.example.neurozen_front.domain.model.StressStats
import com.example.neurozen_front.domain.model.StressTrigger
import com.example.neurozen_front.domain.model.Subscription
import com.example.neurozen_front.domain.model.UserProfile
import com.example.neurozen_front.domain.model.VerificationStatus
import java.time.Instant

fun AuthenticatedUserDto.toSession(): Session {
    val jwt = requireNotNull(token) { "El servidor no devolvió un token" }
    return Session(
        token = jwt,
        userId = requireNotNull(id) { "El servidor no devolvió el id" },
        fullName = fullName.orEmpty(),
        email = email.orEmpty(),
        role = Role.from(role),
        expiresAtEpochSeconds = JwtDecoder.expirationEpochSeconds(jwt)
    )
}

fun UserDto.toDomain() = UserProfile(
    id = id ?: 0, email = email.orEmpty(), fullName = fullName.orEmpty(), role = Role.from(role),
    phoneNumber = phoneNumber, ageRange = ageRange, occupation = occupation, workType = workType
)

fun TriggerDto.toDomain() = StressTrigger(
    id = id ?: 0, description = description.orEmpty(), category = category ?: "other",
    stressLevel = stressLevel ?: 0, intensity = intensity.orEmpty(),
    triggeredAt = parseInstant(triggeredAt) ?: Instant.EPOCH
)

fun AssessmentDto.toDomain(): Assessment {
    val s = score ?: 0
    return Assessment(
        id = id ?: 0, answers = answers.orEmpty(), score = s,
        level = StressLevel.fromServer(level) ?: AssessmentScoring.level(s),
        completedAt = parseInstant(completedAt) ?: Instant.EPOCH
    )
}

fun HealthMetricDto.toDomain() = HealthMetric(id, stressLevel ?: 0, heartRate, sleepHours, notes, parseInstant(recordedAt))

fun ProfessionalDto.toDomain() = Professional(
    id = id ?: 0, userId = userId ?: 0, name = name.orEmpty(), specialty = specialty.orEmpty(), bio = bio.orEmpty(),
    experienceYears = experienceYears, price = price, rating = rating ?: 0.0, reviews = reviews ?: 0,
    availability = availability.orEmpty(), imageUrl = image?.takeIf { it.startsWith("https://") }
)

fun ScheduleDto.toDomain() = AvailabilitySchedule(days.orEmpty(), start.orEmpty(), end.orEmpty())
fun AvailabilitySchedule.toDto() = ScheduleDto(days, start, end)

fun MyProfessionalDto.toDomain() = ProfessionalProfile(
    id = id ?: 0, userId = userId ?: 0, name = name.orEmpty(), email = email.orEmpty(), specialty = specialty.orEmpty(),
    licenseNumber = licenseNumber.orEmpty(), status = VerificationStatus.from(verificationStatus), rejectionReason = rejectionReason,
    bio = bio.orEmpty(), experienceYears = experienceYears, price = price, rating = rating ?: 0.0, reviews = reviews ?: 0,
    schedule = availabilitySchedule?.toDomain()?.takeIf { it.days.isNotEmpty() }, availability = availability.orEmpty()
)

fun ReviewDto.toDomain() = Review(id ?: 0, userName ?: "Anónimo", rating ?: 0, comment.orEmpty(), parseInstant(createdAt))

fun AppointmentTypeDto.toDomain() = AppointmentType(
    value = value ?: 1, displayName = displayName ?: name ?: "Sesión", description = description.orEmpty(),
    durationMinutes = estimatedDurationMinutes ?: 60
)

fun AppointmentDto.toDomain() = Appointment(
    id = id ?: 0, patientId = patientId ?: 0, patientName = patientName, patientEmail = patientEmail,
    professionalId = professionalId ?: 0, dateTime = parseInstant(appointmentDateTime) ?: Instant.EPOCH,
    type = appointmentType ?: 1, status = AppointmentStatus.from(status), notes = notasAdicionales, reviewed = reviewed ?: false
)

fun ReportDto.toDomain() = Report(
    id = id ?: 0, patientId = patientId ?: 0, patientName = patientName, professionalId = professionalId ?: 0,
    appointmentId = appointmentId ?: 0,
    stats = StressStats(
        current = stressCurrent ?: 0.0, average = stressAverage ?: 0.0, weeklyChange = weeklyChange ?: 0.0,
        peakHours = peakHours.orEmpty(), triggersCount = triggersCount ?: 0, topCategory = topCategory.orEmpty(),
        lastAssessment = lastAssessment?.let { LastAssessmentSummary(it.score ?: 0, StressLevel.fromServer(it.level), parseInstant(it.completedAt)) }
    ),
    note = note, createdAt = parseInstant(createdAt)
)

fun ResourceDto.toDomain() = LibraryResource(
    id = id ?: 0, title = title.orEmpty(), description = description.orEmpty(), content = content,
    type = resourceType ?: "article", contentUrl = contentUrl?.takeIf { it.startsWith("https://") }, durationMinutes = duration ?: 0,
    thumbnail = thumbnail?.takeIf { it.startsWith("https://") }, tags = tags.orEmpty(), author = author
)

fun SubscriptionDto.toDomain() = Subscription(id ?: 0, planId ?: 0, isActive ?: false, cardLast4, expirationDate)
