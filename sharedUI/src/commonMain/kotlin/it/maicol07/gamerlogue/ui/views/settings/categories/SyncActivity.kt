package it.maicol07.gamerlogue.ui.views.settings.categories

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.settings__service_phase_reading
import gamerlogue.sharedui.generated.resources.settings__service_phase_writing
import gamerlogue.sharedui.generated.resources.sync__cancel
import gamerlogue.sharedui.generated.resources.sync__duration_hours
import gamerlogue.sharedui.generated.resources.sync__duration_minutes
import gamerlogue.sharedui.generated.resources.sync__duration_seconds
import gamerlogue.sharedui.generated.resources.sync__elapsed
import gamerlogue.sharedui.generated.resources.sync__in
import gamerlogue.sharedui.generated.resources.sync__next_periodic
import gamerlogue.sharedui.generated.resources.sync__notification_progress
import gamerlogue.sharedui.generated.resources.sync__pending_push
import gamerlogue.sharedui.generated.resources.sync__skip
import gamerlogue.sharedui.generated.resources.sync__soon
import gamerlogue.sharedui.generated.resources.sync__stage_signing_in
import gamerlogue.sharedui.generated.resources.sync__status_running
import gamerlogue.sharedui.generated.resources.sync__status_running_progress
import gamerlogue.sharedui.generated.resources.sync__status_waiting
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CloseW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.HourglassW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ScheduleW500Rounded
import it.maicol07.gamerlogue.extensions.expressiveSegmentedColors
import it.maicol07.gamerlogue.services.DueSync
import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.services.SyncQueue
import it.maicol07.gamerlogue.services.SyncStage
import it.maicol07.gamerlogue.services.SyncStep
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** What the history lists above the runs: the syncs going on or waiting, and the ones planned. */
internal sealed interface SyncActivity {
    val key: String

    /** The current background sync, X of N in the [queue]. */
    data class Current(val sync: DueSync, val queue: SyncQueue) : SyncActivity {
        override val key get() = "current-${sync.id}"
    }

    /** A sync the user runs on the sync screen, or a removal from the service: not in the queue. */
    data class Manual(val service: ExternalService) : SyncActivity {
        override val key get() = "manual-$service"
    }

    data class Waiting(val sync: DueSync) : SyncActivity {
        override val key get() = "waiting-${sync.id}"
    }

    /** Library changes to push [at] (epoch millis). */
    data class PendingPush(val at: Long) : SyncActivity {
        override val key get() = "pending-push"
    }

    /** The next periodic sync, [at] (epoch millis). */
    data class NextPeriodic(val at: Long) : SyncActivity {
        override val key get() = "next-periodic"
    }
}

/** The current background sync first, then the other running ones, the queued ones in order, and what's planned. */
internal fun syncActivities(
    running: Set<ExternalService>,
    queue: SyncQueue,
    pendingPush: Long?,
    nextPeriodicAt: Long?,
): List<SyncActivity> = buildList {
    queue.current?.let { add(SyncActivity.Current(it, queue)) }
    running.filter { it != queue.current?.service }.forEach { add(SyncActivity.Manual(it)) }
    queue.waiting.forEach { add(SyncActivity.Waiting(it)) }
    pendingPush?.let { add(SyncActivity.PendingPush(it)) }
    nextPeriodicAt?.let { add(SyncActivity.NextPeriodic(it)) }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SyncActivityItem(
    activity: SyncActivity,
    shapes: ListItemShapes,
    onCancel: () -> Unit,
    onCancelManual: (ExternalService) -> Unit,
    onSkip: (DueSync) -> Unit,
) {
    val colors = ListItemDefaults.expressiveSegmentedColors()
    val loading: @Composable () -> Unit = { LoadingIndicator(Modifier.size(SectionIconSize)) }
    when (activity) {
        is SyncActivity.Current -> CurrentItem(activity, shapes, onCancel)
        is SyncActivity.Manual -> SegmentedListItem(
            shapes = shapes,
            colors = colors,
            leadingContent = loading,
            trailingContent = {
                CloseButton(stringResource(Res.string.sync__cancel)) { onCancelManual(activity.service) }
            },
            supportingContent = { Text(stringResource(Res.string.sync__status_running)) },
        ) { ServiceName(activity.service) }
        is SyncActivity.Waiting -> SegmentedListItem(
            shapes = shapes,
            colors = colors,
            leadingContent = { RowIcon(Icons.ScheduleW500Rounded) },
            trailingContent = { CloseButton(stringResource(Res.string.sync__skip)) { onSkip(activity.sync) } },
            supportingContent = {
                Text("${stringResource(activity.sync.kind.label)} · ${stringResource(Res.string.sync__status_waiting)}")
            },
        ) { ServiceName(activity.sync.service) }
        is SyncActivity.PendingPush -> SegmentedListItem(
            shapes = shapes,
            colors = colors,
            leadingContent = { RowIcon(Icons.HourglassW500Rounded) },
            supportingContent = { Text(inText(activity.at, rememberNow())) },
        ) { Text(stringResource(Res.string.sync__pending_push), style = MaterialTheme.typography.titleMedium) }
        is SyncActivity.NextPeriodic -> SegmentedListItem(
            shapes = shapes,
            colors = colors,
            leadingContent = { RowIcon(Icons.ScheduleW500Rounded) },
            supportingContent = { Text(inText(activity.at, rememberNow())) },
        ) { Text(stringResource(Res.string.sync__next_periodic), style = MaterialTheme.typography.titleMedium) }
    }
}

/** The current background sync: X of N, kind, step and time so far, with [onCancel] to stop it. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CurrentItem(activity: SyncActivity.Current, shapes: ListItemShapes, onCancel: () -> Unit) {
    val queue = activity.queue
    val now = rememberNow()
    SegmentedListItem(
        shapes = shapes,
        colors = ListItemDefaults.expressiveSegmentedColors(),
        leadingContent = { LoadingIndicator(Modifier.size(SectionIconSize)) },
        trailingContent = { CloseButton(stringResource(Res.string.sync__cancel), onCancel) },
        supportingContent = {
            Column {
                val status = stringResource(Res.string.sync__status_running_progress, queue.done + 1, queue.total)
                Text("${stringResource(activity.sync.kind.label)} · $status")
                val elapsed = queue.startedAt?.let { stringResource(Res.string.sync__elapsed, durationText(now - it)) }
                Text(listOfNotNull(queue.step?.text(), elapsed).joinToString(" · "))
            }
        },
    ) { ServiceName(activity.sync.service) }
}

@Composable
private fun RowIcon(icon: ImageVector) = Icon(icon, contentDescription = null, Modifier.size(SectionIconSize))

@Composable
private fun ServiceName(service: ExternalService) =
    Text(stringResource(service.labelRes()), style = MaterialTheme.typography.titleMedium)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CloseButton(description: String, onClick: () -> Unit) =
    IconButton(onClick = onClick, shapes = IconButtonDefaults.shapes()) {
        Icon(Icons.CloseW500Rounded, contentDescription = description)
    }

/** "Writing… 12 of 40"; the count only once the step knows its total. */
@Composable
internal fun SyncStep.text(): String {
    val label = stringResource(
        when (stage) {
            SyncStage.SIGNING_IN -> Res.string.sync__stage_signing_in
            SyncStage.READING -> Res.string.settings__service_phase_reading
            SyncStage.WRITING -> Res.string.settings__service_phase_writing
        },
    )
    return if (total > 0) "$label ${stringResource(Res.string.sync__notification_progress, done, total)}" else label
}

/** "in 3 min" until [at], "soon" once it's past (the platform hasn't run it yet). */
@Composable
private fun inText(at: Long, now: Long): String =
    if (at > now) stringResource(Res.string.sync__in, durationText(at - now)) else stringResource(Res.string.sync__soon)

/** "45 s", "3 min", "2 h 5 min". */
@Composable
private fun durationText(millis: Long): String {
    val duration = millis.coerceAtLeast(0).milliseconds
    return when {
        duration < 1.minutes -> stringResource(Res.string.sync__duration_seconds, duration.inWholeSeconds.toInt())
        duration < 1.hours -> stringResource(Res.string.sync__duration_minutes, duration.inWholeMinutes.toInt())
        else -> stringResource(
            Res.string.sync__duration_hours,
            duration.inWholeHours.toInt(),
            (duration.inWholeMinutes % 1.hours.inWholeMinutes).toInt(),
        )
    }
}

/** The time now (epoch millis), ticking every second while shown. */
@Composable
private fun rememberNow(): Long {
    var now by remember { mutableLongStateOf(Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1.seconds)
            now = Clock.System.now().toEpochMilliseconds()
        }
    }
    return now
}

/** Matches [it.maicol07.gamerlogue.ui.components.SectionIcon]'s size, so these rows line up with the run headers. */
private val SectionIconSize = 36.dp
