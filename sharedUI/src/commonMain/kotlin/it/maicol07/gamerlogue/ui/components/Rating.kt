package it.maicol07.gamerlogue.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.maicol07.gamerlogue.core.AppPreferences
import it.maicol07.gamerlogue.data.MAX_RATING
import net.sergeych.sprintf.sprintf
import org.koin.compose.koinInject
import kotlin.math.roundToInt

/** How ratings are shown; only a conversion of the server's 0–[MAX_RATING] value. */
enum class RatingScale(val max: Int, private val decimals: Int) {
    TEN(10, 1),
    HUNDRED(100, 0);

    /** [rating] (server scale) converted to this one. */
    fun format(rating: Number): String {
        val value = rating.toDouble() * max / MAX_RATING
        // sprintf renders "%.0f" with a trailing dot ("75.").
        return if (decimals == 0) value.roundToInt().toString() else "%.${decimals}f".sprintf(value)
    }
}

@Composable
fun currentRatingScale(): RatingScale {
    val scale by koinInject<AppPreferences>().ratingScale.collectAsStateWithLifecycle()
    return scale
}
