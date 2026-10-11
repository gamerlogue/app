package it.maicol07.gamerlogue.ui.views.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import at.released.igdbclient.model.Game
import com.raedghazal.kotlinx_datetime_ext.LocalDateTimeFormatter
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.calendar__empty
import gamerlogue.sharedui.generated.resources.calendar__empty_hint
import gamerlogue.sharedui.generated.resources.calendar__jump_to_day
import gamerlogue.sharedui.generated.resources.calendar__today
import gamerlogue.sharedui.generated.resources.common_cancel
import gamerlogue.sharedui.generated.resources.common_ok
import gamerlogue.sharedui.generated.resources.nav__calendar
import io.github.fopwoc.nav3ksp.annotation.Branch
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CalendarMonthW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.DateRangeW500Rounded
import it.maicol07.gamerlogue.extensions.igdb.detailNavKey
import it.maicol07.gamerlogue.ui.components.EmptyState
import it.maicol07.gamerlogue.ui.components.game.CoverAspectRatio
import it.maicol07.gamerlogue.ui.components.game.GameCoverCard
import it.maicol07.gamerlogue.ui.components.layout.AppVerticalScrollbar
import it.maicol07.gamerlogue.ui.components.layout.ScreenScaffold
import it.maicol07.gamerlogue.ui.components.pickerMillisToDate
import it.maicol07.gamerlogue.ui.components.toPickerMillis
import it.maicol07.gamerlogue.ui.navigation.ListPaneMetadata
import it.maicol07.gamerlogue.ui.navigation.LocalNavigationState
import it.maicol07.gamerlogue.ui.navigation.RootTree
import it.maicol07.gamerlogue.ui.theme.Dimens
import it.maicol07.gamerlogue.ui.views.auth.LoginView
import it.maicol07.gamerlogue.ui.views.auth.isAuthenticated
import it.maicol07.gamerlogue.ui.views.list.ActiveFilterChips
import it.maicol07.gamerlogue.ui.views.list.FilterButton
import it.maicol07.gamerlogue.ui.views.list.FilterSheetScope
import it.maicol07.gamerlogue.ui.views.list.GameListFilterSheet
import it.maicol07.gamerlogue.ui.views.list.hasActiveFilters
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock

private val RailWidth = 56.dp
private val RailLineWidth = 2.dp
private val DayBadgeSize = 48.dp
private val DayGap = 16.dp
private val DayCoverWidth = 110.dp

/** One row of the timeline: a pinned month header, or a release day. */
private sealed interface TimelineRow {
    data class Month(val firstDay: LocalDate) : TimelineRow

    data class Day(val day: LocalDate, val games: List<Game>) : TimelineRow
}

/**
 * The view model is created only once signed in, since IGDB goes through the authenticated backend
 * proxy; its actions live in the top bar, so the login gate wraps the whole scaffold.
 */
@Branch(RootTree::class, metadata = ListPaneMetadata::class)
@Composable
fun Calendar() = if (isAuthenticated()) CalendarScreen() else ScreenScaffold(title = Res.string.nav__calendar) { LoginView() }

@Composable
private fun CalendarScreen(viewModel: CalendarViewModel = koinViewModel()) {
    val navigationState = LocalNavigationState.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filterOptions by viewModel.filterOptions.state.collectAsStateWithLifecycle()

    ScreenScaffold(
        title = Res.string.nav__calendar,
        actions = {
            JumpToDayButton(onDaySelected = viewModel::jumpTo)
            FilterButton(
                hasActiveFilters = uiState.filterState.hasActiveFilters,
                onClick = { viewModel.toggleFilterSheet(true) }
            )
        }
    ) {
        Column(Modifier.fillMaxSize()) {
            AnimatedVisibility(visible = uiState.filterState.hasActiveFilters) {
                ActiveFilterChips(
                    filterState = uiState.filterState,
                    knownOptions = filterOptions.knownOptions,
                    presetPlatform = null,
                    onFilterChange = viewModel::updateFilter,
                    onReset = viewModel::resetFilter,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            CalendarTimeline(
                uiState = uiState,
                onGameClick = { navigationState.backStack.add(it.detailNavKey) },
                onEndReached = viewModel::onEndReached,
                onScrolledToTarget = viewModel::onScrolledToTarget,
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (uiState.showFilterSheet) {
        GameListFilterSheet(
            filterState = uiState.filterState,
            scope = FilterSheetScope.CALENDAR,
            options = filterOptions,
            onFilterSearch = viewModel.filterOptions::search,
            onFilterChange = viewModel::updateFilter,
            onReset = viewModel::resetFilter,
            onDismiss = { viewModel.toggleFilterSheet(false) }
        )
    }
}

@Composable
private fun JumpToDayButton(onDaySelected: (LocalDate) -> Unit) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    IconButton(onClick = { showPicker = true }, shapes = IconButtonDefaults.shapes()) {
        Icon(Icons.DateRangeW500Rounded, contentDescription = stringResource(Res.string.calendar__jump_to_day))
    }
    if (showPicker) {
        JumpToDayDialog(
            onDismiss = { showPicker = false },
            onDaySelected = {
                showPicker = false
                onDaySelected(it)
            }
        )
    }
}

/** A date picker that only offers today and later: the timeline starts today. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun JumpToDayDialog(onDismiss: () -> Unit, onDaySelected: (LocalDate) -> Unit) {
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = today.toPickerMillis(),
        yearRange = today.year..DatePickerDefaults.YearRange.last,
        selectableDates = remember(today) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = pickerMillisToDate(utcTimeMillis) >= today

                override fun isSelectableYear(year: Int) = year >= today.year
            }
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                shapes = ButtonDefaults.shapes(),
                enabled = state.selectedDateMillis != null,
                onClick = { state.selectedDateMillis?.let { onDaySelected(pickerMillisToDate(it)) } }
            ) { Text(stringResource(Res.string.common_ok)) }
        },
        dismissButton = {
            TextButton(shapes = ButtonDefaults.shapes(), onClick = onDismiss) {
                Text(stringResource(Res.string.common_cancel))
            }
        }
    ) { DatePicker(state = state) }
}

/** A vertical timeline: one node per release day, with that day's covers beside it. */
@Composable
private fun CalendarTimeline(
    uiState: CalendarViewModel.UiState,
    onGameClick: (Game) -> Unit,
    onEndReached: () -> Unit,
    onScrolledToTarget: () -> Unit,
    modifier: Modifier,
) {
    val listState = rememberLazyListState()
    val rows = remember(uiState.games) { uiState.games.groupByReleaseDay().toTimelineRows() }
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    var monthHeaderHeight by remember { mutableIntStateOf(0) }

    // Read in a snapshotFlow rather than as effect keys, so scrolling does not recompose the list.
    // Keyed on the item count too, so a page too short to fill the screen still asks for the next.
    val currentOnEndReached by rememberUpdatedState(onEndReached)
    LaunchedEffect(listState) {
        snapshotFlow {
            val layout = listState.layoutInfo
            layout.totalItemsCount.takeIf { (layout.visibleItemsInfo.lastOrNull()?.index ?: 0) >= it - 2 }
        }.collect { if (it != null) currentOnEndReached() }
    }

    LaunchedEffect(uiState.scrollTarget) {
        val target = uiState.scrollTarget ?: return@LaunchedEffect
        // The first day on or after the target; past the last release, the last day.
        val index = rows.indexOfFirst { it is TimelineRow.Day && it.day >= target }.takeIf { it >= 0 } ?: rows.lastIndex
        when {
            index < 0 -> {}
            // A month's first day is reached through its header, so the header shows above it.
            rows.getOrNull(index - 1) is TimelineRow.Month -> listState.animateScrollToItem(index - 1)
            // Any other day lands below the header pinned over the top of the list.
            else -> listState.animateScrollToItem(index, -monthHeaderHeight)
        }
        onScrolledToTarget()
    }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Dimens.ScreenPadding),
        ) {
            rows.forEach { row ->
                when (row) {
                    is TimelineRow.Month -> stickyHeader(key = "month-${row.firstDay.year}-${row.firstDay.month}") {
                        MonthHeader(row.firstDay, Modifier.onSizeChanged { monthHeaderHeight = it.height })
                    }
                    is TimelineRow.Day -> item(key = row.day.toString()) {
                        TimelineDay(
                            day = row.day,
                            isToday = row.day == today,
                            games = row.games,
                            onGameClick = onGameClick,
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
            loadingOrEmpty(uiState)
        }
        AppVerticalScrollbar(listState, Modifier.align(Alignment.CenterEnd).fillMaxHeight())
    }
}

/** Days with a month header before the first day of each month. */
private fun Map<LocalDate, List<Game>>.toTimelineRows(): List<TimelineRow> = buildList {
    var previous: LocalDate? = null
    for ((day, games) in this@toTimelineRows) {
        if (previous?.let { it.year == day.year && it.month == day.month } != true) add(TimelineRow.Month(day))
        add(TimelineRow.Day(day, games))
        previous = day
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun LazyListScope.loadingOrEmpty(uiState: CalendarViewModel.UiState) {
    if (uiState.loading) {
        item(key = "loading") {
            Box(Modifier.fillMaxWidth().padding(Dimens.ScreenPadding), contentAlignment = Alignment.Center) {
                // The first page gets the contained, more prominent indicator.
                if (uiState.games.isEmpty()) ContainedLoadingIndicator() else LoadingIndicator()
            }
        }
    } else if (uiState.games.isEmpty()) {
        item(key = "empty") {
            EmptyState(
                icon = Icons.CalendarMonthW500Rounded,
                title = stringResource(Res.string.calendar__empty),
                hint = stringResource(Res.string.calendar__empty_hint),
                modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp)
            )
        }
    }
}

/** Opaque, so the days scrolling under it stay hidden. */
@Composable
private fun MonthHeader(day: LocalDate, modifier: Modifier) = Surface(color = MaterialTheme.colorScheme.background, modifier = modifier) {
    Text(
        text = day.format("MMMM yyyy").replaceFirstChar(Char::uppercaseChar),
        style = MaterialTheme.typography.titleLargeEmphasized,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
    )
}

/**
 * One timeline node: the day badge on a rail drawn along the node's full height, so consecutive
 * days join into one line. Today's badge is the screen's single highlight.
 */
@Composable
private fun TimelineDay(
    day: LocalDate,
    isToday: Boolean,
    games: List<Game>,
    onGameClick: (Game) -> Unit,
    modifier: Modifier,
) {
    val railColor = MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier = modifier.fillMaxWidth().drawBehind {
            val x = RailWidth.toPx() / 2
            drawLine(railColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = RailLineWidth.toPx())
        },
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.width(RailWidth), contentAlignment = Alignment.TopCenter) { DayBadge(day, isToday) }
        Column(
            modifier = Modifier.weight(1f).padding(bottom = DayGap),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (isToday) stringResource(Res.string.calendar__today) else day.format("EEEE"),
                style = MaterialTheme.typography.titleSmallEmphasized,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap),
                verticalArrangement = Arrangement.spacedBy(Dimens.CardGap)
            ) {
                games.forEach { game ->
                    GameCoverCard(
                        game = game,
                        metadata = emptyList(),
                        showTitle = true,
                        modifier = Modifier.clip(MaterialTheme.shapes.large),
                        sizeModifier = Modifier.width(DayCoverWidth).aspectRatio(CoverAspectRatio),
                        onClick = onGameClick
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DayBadge(day: LocalDate, isToday: Boolean) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = (if (isToday) MaterialShapes.Cookie9Sided else MaterialShapes.Circle).toShape(),
        color = if (isToday) colors.primary else colors.secondaryContainer,
        contentColor = if (isToday) colors.onPrimary else colors.onSecondaryContainer,
        modifier = Modifier.size(DayBadgeSize)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(day.format("EEE").uppercase(), style = MaterialTheme.typography.labelSmall)
            Text(day.day.toString(), style = MaterialTheme.typography.titleMediumEmphasized)
        }
    }
}

/** [pattern] in the current locale, for the day and month names. */
@Composable
private fun LocalDate.format(pattern: String): String = LocalDateTimeFormatter.ofPattern(
    pattern,
    com.raedghazal.kotlinx_datetime_ext.Locale.forLanguageTag(Locale.current.toLanguageTag())
).format(atTime(0, 0))
