package it.maicol07.gamerlogue.ui.views.discover

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.CarouselState
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.released.igdbclient.model.Game
import at.released.igdbclient.model.ReleaseDate
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.home__release_in_days
import gamerlogue.sharedui.generated.resources.home__release_today
import gamerlogue.sharedui.generated.resources.home__release_tomorrow
import it.maicol07.gamerlogue.extensions.igdb.displayDate
import it.maicol07.gamerlogue.extensions.igdb.ratingScore
import it.maicol07.gamerlogue.extensions.focusableOnlyByKeyboard
import it.maicol07.gamerlogue.extensions.mouseDragScrollsHorizontally
import it.maicol07.gamerlogue.ui.components.CarouselWithArrows
import it.maicol07.gamerlogue.ui.components.GameCoverCarousel
import it.maicol07.gamerlogue.ui.components.game.GameCoverCard
import it.maicol07.gamerlogue.ui.components.pressMorphShape
import it.maicol07.gamerlogue.ui.theme.Dimens
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.Instant

/** Carousel items narrower than this (the small, peeking ones) hide the game title. */
private val CardTitleMinWidth = 100.dp
private val StarBadgeSize = 44.dp
private val RankedItemWidth = 196.dp
private val RankedNumeralSize = 120.sp

/** Two stacked digits have to fit the card height, so they are drawn smaller than a single one. */
private val RankedStackedNumeralSize = 96.sp
private const val COUNTDOWN_MAX_DAYS = 30

/** A multi-browse carousel of covers, each with the badge its [section] cares about. */
@Composable
internal fun GameCarousel(section: DiscoverSection, games: List<Game>, onGameClick: (Game) -> Unit) {
    // Upcoming dates start relative; tapping any date pill flips the whole row between the two.
    var absoluteDates by rememberSaveable { mutableStateOf(false) }
    GameCarousel(games = games, onGameClick = onGameClick, metadata = { emptyList() }) { game ->
        SectionBadge(
            section = section,
            game = game,
            absoluteDates = absoluteDates,
            onDateClick = { absoluteDates = !absoluteDates },
            modifier = Modifier.align(Alignment.TopStart).padding(Dimens.ItemGap)
        )
    }
}

/**
 * A multi-browse carousel of covers with press-morphing cards: [metadata] feeds the card's own
 * badges, [badge] overlays a custom one on top of the cover.
 */
@Composable
internal fun GameCarousel(
    games: List<Game>,
    onGameClick: (Game) -> Unit,
    metadata: @Composable (Game) -> List<String>,
    badge: @Composable BoxScope.(Game) -> Unit
) {
    val titleMinWidthPx = with(LocalDensity.current) { CardTitleMinWidth.toPx() }
    GameCoverCarousel(
        itemCount = games.count(),
        preferredItemWidth = CardWidth,
        modifier = Modifier.height(CardHeight)
    ) { i ->
        val game = games[i]
        val interactionSource = remember { MutableInteractionSource() }
        Box(Modifier.maskClip(pressMorphShape(interactionSource, CardCorner, CardPressedCorner))) {
            GameCoverCard(
                game = game,
                metadata = metadata(game),
                showTitle = carouselItemDrawInfo.size > titleMinWidthPx,
                // The carousel sizes its items, and a large item can be wider than the preferred width.
                sizeModifier = Modifier.fillMaxSize(),
                interactionSource = interactionSource,
                onClick = onGameClick
            )
            badge(game)
        }
    }
}

/**
 * "Most loved" as a ranking: each cover overlaps a large numeral.
 * Uncontained, so the items keep their size, and the numerals are never squeezed by the mask.
 */
@Composable
internal fun RankedCarousel(games: List<Game>, onGameClick: (Game) -> Unit) {
    val state = rememberCarouselState { games.size }
    val step = with(LocalDensity.current) { (RankedItemWidth + Dimens.ItemGap).toPx() }
    CarouselWithArrows(
        state = state,
        modifier = Modifier,
        onBack = { state.animateScrollBy(-step) },
        onForward = { state.animateScrollBy(step) }
    ) {
        RankedItems(state, games, onGameClick)
    }
}

@Composable
private fun RankedItems(state: CarouselState, games: List<Game>, onGameClick: (Game) -> Unit) =
    HorizontalUncontainedCarousel(
        state = state,
        itemWidth = RankedItemWidth,
        modifier = Modifier.fillMaxWidth().height(CardHeight).mouseDragScrollsHorizontally(state),
        itemSpacing = Dimens.ItemGap,
        contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding)
    ) { i ->
        val game = games[i]
        val interactionSource = remember { MutableInteractionSource() }
        Box(Modifier.fillMaxSize().focusableOnlyByKeyboard()) {
            // The strip left of the cover fits one digit: longer ranks stack one digit per line.
            val rank = "${i + 1}"
            val numeralSize = if (rank.length == 1) RankedNumeralSize else RankedStackedNumeralSize
            Text(
                text = rank.toList().joinToString("\n"),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.displayLargeEmphasized.copy(
                    fontSize = numeralSize,
                    lineHeight = numeralSize
                ),
                modifier = Modifier.align(Alignment.BottomStart)
            )
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .clip(pressMorphShape(interactionSource, CardCorner, CardPressedCorner))
            ) {
                GameCoverCard(
                    game = game,
                    metadata = emptyList(),
                    showTitle = true,
                    interactionSource = interactionSource,
                    onClick = onGameClick
                )
                game.ratingScore()?.let {
                    StarBadge(it, Modifier.align(Alignment.TopStart).padding(Dimens.ItemGap))
                }
            }
        }
    }

@Composable
private fun SectionBadge(
    section: DiscoverSection,
    game: Game,
    absoluteDates: Boolean,
    onDateClick: () -> Unit,
    modifier: Modifier
) {
    // Block body on purpose: the nullable `let` branch would make an expression body return `Unit?`
    when (section) {
        DiscoverSection.MOST_LOVED -> game.ratingScore()?.let { StarBadge(it, modifier) }
        DiscoverSection.UPCOMING -> ToggleablePill(
            text = if (absoluteDates) {
                ReleaseDate(
                    date = game.first_release_date
                ).displayDate()
            } else {
                releaseCountdown(game)
            },
            onClick = onDateClick,
            modifier = modifier
        )
        DiscoverSection.RECENTLY_RELEASED -> InfoPill(
            ReleaseDate(date = game.first_release_date).displayDate(),
            modifier
        )
        DiscoverSection.POPULAR -> Unit
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StarBadge(score: String, modifier: Modifier) = Box(
    modifier = modifier
        .size(StarBadgeSize)
        .background(MaterialTheme.colorScheme.tertiaryContainer, MaterialShapes.Sunny.toShape()),
    contentAlignment = Alignment.Center
) {
    Text(
        text = score,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
        style = MaterialTheme.typography.labelMediumEmphasized
    )
}

@Composable
private fun InfoPill(text: String, modifier: Modifier) = Surface(
    color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    shape = CircleShape,
    modifier = modifier
) {
    PillText(text)
}

/**
 * A pill with its own tap target, which takes the tap instead of the card under it. A new [text]
 * cross-fades in while the pill resizes to fit it.
 */
@Composable
private fun ToggleablePill(text: String, onClick: () -> Unit, modifier: Modifier) {
    val fadeSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    val sizeSpec = MaterialTheme.motionScheme.fastSpatialSpec<IntSize>()
    // The 48dp minimum touch target would center the pill in it, lower than the other badges.
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        Surface(
            onClick = onClick,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = CircleShape,
            modifier = modifier
        ) {
            AnimatedContent(
                targetState = text,
                transitionSpec = { fadeIn(fadeSpec) togetherWith fadeOut(fadeSpec) using SizeTransform { _, _ -> sizeSpec } }
            ) { PillText(it) }
        }
    }
}

@Composable
private fun PillText(text: String) = Text(
    text = text,
    style = MaterialTheme.typography.labelMediumEmphasized,
    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
)

/** "Today", "Tomorrow" or "In N days" for the next month; the plain date after that. */
@Composable
private fun releaseCountdown(game: Game): String {
    val releaseDate = ReleaseDate(date = game.first_release_date)
    val release = game.first_release_date ?: return releaseDate.displayDate()
    // IGDB release dates are midnight UTC, so the UTC calendar day is the release day.
    val releaseDay = Instant.fromEpochSeconds(release.getEpochSecond()).toLocalDateTime(TimeZone.UTC).date
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    return when (val days = today.daysUntil(releaseDay)) {
        0 -> stringResource(Res.string.home__release_today)
        1 -> stringResource(Res.string.home__release_tomorrow)
        in 2..COUNTDOWN_MAX_DAYS -> pluralStringResource(Res.plurals.home__release_in_days, days, days)
        else -> releaseDate.displayDate()
    }
}
