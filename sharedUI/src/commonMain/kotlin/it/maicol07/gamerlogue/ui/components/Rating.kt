package it.maicol07.gamerlogue.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.graphics.shapes.RoundedPolygon
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.settings__review_mode_basic
import gamerlogue.sharedui.generated.resources.settings__review_mode_detailed
import it.maicol07.gamerlogue.core.AppPreferences
import it.maicol07.gamerlogue.data.LibraryEntrySchema
import it.maicol07.gamerlogue.data.MAX_RATING
import it.maicol07.gamerlogue.data.ReviewAspect
import net.sergeych.sprintf.sprintf
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
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

/** Whether the library sheet writes a plain review or one split into sections. */
enum class ReviewMode(val label: StringResource) {
    BASIC(Res.string.settings__review_mode_basic),
    DETAILED(Res.string.settings__review_mode_detailed)
}

@Composable
fun currentRatingScale(): RatingScale {
    val scale by koinInject<AppPreferences>().ratingScale.collectAsStateWithLifecycle()
    return scale
}

@Composable
fun reviewSectionTitle(key: String): String = ReviewAspect.of(key)?.let { stringResource(it.label) } ?: key

/** The review text, or the titles of its sections when it has none. */
@Composable
fun reviewPreview(entry: LibraryEntrySchema): String =
    entry.review?.takeIf { it.isNotBlank() } ?: entry.reviewSections.map { reviewSectionTitle(it.key) }.joinToString(" · ")

/**
 * [rating] (server scale) inside an expressive [shape] that pops in when first shown and turns with
 * the value, so dragging a slider winds the shape up.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RatingBadge(
    rating: Number?,
    shape: RoundedPolygon,
    size: Dp,
    containerColor: Color,
    contentColor: Color,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    val scale = currentRatingScale()
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val pop by animateFloatAsState(if (shown) 1f else 0f, MaterialTheme.motionScheme.slowSpatialSpec())
    val turn by animateFloatAsState(
        (rating?.toFloat() ?: 0f) / MAX_RATING * BADGE_TURN_DEGREES,
        MaterialTheme.motionScheme.defaultSpatialSpec()
    )
    Box(
        modifier.size(size).graphicsLayer {
            scaleX = pop
            scaleY = pop
        },
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.matchParentSize().graphicsLayer { rotationZ = turn }.background(containerColor, shape.toShape()))
        Text(rating?.let(scale::format) ?: "–", style = textStyle, color = contentColor, maxLines = 1)
    }
}

/** How far a badge turns between the lowest and the highest rating. */
private const val BADGE_TURN_DEGREES = 90f
