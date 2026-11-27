package com.example.neurozen_front.domain.logic

import java.util.Locale

/** Rating shown for a professional: "no ratings yet" instead of a misleading 0.0. */
sealed interface RatingDisplay {
    data object None : RatingDisplay

    /** [text] uses a decimal comma, e.g. "4,5". */
    data class Rated(val value: Double, val text: String, val reviews: Int) : RatingDisplay
}

/** Formatting of the public professional data without filler values (B1). */
object ProfessionalFormat {
    private const val MAX_RATING = 5.0

    /** rating 0 or no reviews -> [RatingDisplay.None] («Sin valoraciones aún»). */
    fun rating(rating: Double, reviews: Int): RatingDisplay =
        if (rating <= 0.0 || reviews <= 0 || rating.isNaN()) RatingDisplay.None
        else {
            val value = rating.coerceAtMost(MAX_RATING)
            RatingDisplay.Rated(value, String.format(Locale.US, "%.1f", value).replace('.', ','), reviews)
        }

    /** "120.00" for the price amount, or null when the price is not defined (null or <= 0): never shows 0. */
    fun priceAmount(price: Double?): String? =
        price?.takeIf { it > 0.0 && !it.isNaN() && !it.isInfinite() }?.let { String.format(Locale.US, "%.2f", it) }
}
