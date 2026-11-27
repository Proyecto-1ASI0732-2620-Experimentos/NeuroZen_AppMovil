package com.example.neurozen_front.presentation.components

import androidx.annotation.StringRes
import com.example.neurozen_front.R
import com.example.neurozen_front.domain.model.AppointmentStatus
import com.example.neurozen_front.domain.model.StressLevel
import kotlin.math.pow

/** Pure UI mappings (no Compose), so they can be unit tested. */
object UiMappings {

    @StringRes
    fun stressLevelLabel(level: StressLevel): Int = when (level) {
        StressLevel.LOW -> R.string.stress_level_low
        StressLevel.MODERATE -> R.string.stress_level_moderate
        StressLevel.HIGH -> R.string.stress_level_high
    }

    /** Displayed plan price: single source in strings.xml, same values as the web (the backend exposes no prices). */
    @StringRes
    fun planPrice(planId: Int): Int = when (planId) {
        2 -> R.string.plan_price_advanced
        3 -> R.string.plan_price_professional
        else -> R.string.plan_price_basic
    }
}

/** Icon of an appointment status, so the state never depends on colour alone. */
enum class StatusIcon { CLOCK, CHECK, CLOSE, DONE_ALL, HELP }

/**
 * Appointment status chip: text, icon, accessible description and colours (ARGB) for light and dark themes.
 * Every content/container pair has a contrast ratio >= 4.5:1 (checked in AppointmentStatusStyleTest).
 */
data class StatusStyle(
    @param:StringRes val label: Int,
    val icon: StatusIcon,
    val lightContainer: Int,
    val lightContent: Int,
    val darkContainer: Int,
    val darkContent: Int
)

object AppointmentStatusStyles {
    fun of(status: AppointmentStatus): StatusStyle = when (status) {
        AppointmentStatus.PENDING ->
            StatusStyle(R.string.status_pending, StatusIcon.CLOCK, 0xFFFFF1D6.toInt(), 0xFF6B4A00.toInt(), 0xFF4A3A12.toInt(), 0xFFFFE0A3.toInt())
        AppointmentStatus.CONFIRMED ->
            StatusStyle(R.string.status_confirmed, StatusIcon.CHECK, 0xFFD8EEE3.toInt(), 0xFF2D5A4A.toInt(), 0xFF1F4D3D.toInt(), 0xFFC4EBD8.toInt())
        AppointmentStatus.COMPLETED ->
            StatusStyle(R.string.status_completed, StatusIcon.DONE_ALL, 0xFFDDE7F4.toInt(), 0xFF1E4473.toInt(), 0xFF22384F.toInt(), 0xFFCFE0F5.toInt())
        AppointmentStatus.CANCELLED ->
            StatusStyle(R.string.status_cancelled, StatusIcon.CLOSE, 0xFFECEDEC.toInt(), 0xFF474F4A.toInt(), 0xFF353B38.toInt(), 0xFFE0E4E1.toInt())
        AppointmentStatus.UNKNOWN ->
            StatusStyle(R.string.status_unknown, StatusIcon.HELP, 0xFFECEDEC.toInt(), 0xFF474F4A.toInt(), 0xFF353B38.toInt(), 0xFFE0E4E1.toInt())
    }
}

/** WCAG 2.x contrast ratio between two opaque ARGB colours (1.0 to 21.0). */
object Contrast {
    fun ratio(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    private fun channel(value: Int): Double {
        val c = value / 255.0
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    fun luminance(argb: Int): Double =
        0.2126 * channel((argb shr 16) and 0xFF) + 0.7152 * channel((argb shr 8) and 0xFF) + 0.0722 * channel(argb and 0xFF)
}
