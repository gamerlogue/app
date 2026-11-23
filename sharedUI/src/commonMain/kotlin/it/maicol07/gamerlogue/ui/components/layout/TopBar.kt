package it.maicol07.gamerlogue.ui.components.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.common__back
import gamerlogue.sharedui.generated.resources.exception__action_show
import gamerlogue.sharedui.generated.resources.nav__notifications
import gamerlogue.sharedui.generated.resources.sync__pending_push
import gamerlogue.sharedui.generated.resources.sync__status_new
import gamerlogue.sharedui.generated.resources.sync__status_running
import gamerlogue.sharedui.generated.resources.sync__status_running_progress
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ArrowBackW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ErrorW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.HourglassW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.NotificationsW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.SyncW500Rounded
import it.maicol07.gamerlogue.core.ExceptionReporter
import it.maicol07.gamerlogue.core.UnreadNotifications
import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.services.SyncCoordinator
import it.maicol07.gamerlogue.services.SyncHistory
import it.maicol07.gamerlogue.services.SyncQueue
import it.maicol07.gamerlogue.services.SyncResult
import it.maicol07.gamerlogue.services.SyncScheduler
import it.maicol07.gamerlogue.ui.components.TooltipBox
import it.maicol07.gamerlogue.ui.navigation.AppNavigationState
import it.maicol07.gamerlogue.ui.navigation.LocalNavigationState
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTree
import it.maicol07.gamerlogue.ui.views.settings.categories.label
import it.maicol07.gamerlogue.ui.views.settings.categories.labelRes
import it.maicol07.gamerlogue.ui.views.settings.categories.text
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun AppTopBar(
    title: StringResource?,
    modifier: Modifier = Modifier,
    navigationState: AppNavigationState = LocalNavigationState.current,
    actions: @Composable RowScope.() -> Unit = {}
) = AppTopBar(title?.let { stringResource(it) }, modifier, navigationState, subtitle = null, actions = actions)

/** [AppTopBar] with a runtime title, e.g. another user's nickname, and an optional [subtitle] under it. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppTopBar(
    title: String?,
    modifier: Modifier = Modifier,
    navigationState: AppNavigationState = LocalNavigationState.current,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = { title?.let { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
        subtitle = { subtitle?.let { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
        modifier = modifier,
        navigationIcon = {
            if (navigationState.backStack.size > 1) {
                IconButton(
                    onClick = navigationState::navigateBack,
                    shapes = IconButtonDefaults.shapes()
                ) {
                    Icon(Icons.ArrowBackW500Rounded, stringResource(Res.string.common__back))
                }
            }
        },
        actions = {
            actions()
            SyncStatusAction(navigationState)
            GlobalErrorAction()
        }
    )
}

/** The bell with the unread count, on the signed-in user's own profile. */
@Composable
fun NotificationsAction(navigationState: AppNavigationState = LocalNavigationState.current) {
    val count by koinInject<UnreadNotifications>().count.collectAsStateWithLifecycle()
    IconButton(
        onClick = { navigationState.backStack.add(RootNavTree.Notifications) },
        shapes = IconButtonDefaults.shapes()
    ) {
        BadgedBox(badge = { if (count > 0) Badge { Text(count.toString()) } }) {
            Icon(Icons.NotificationsW500Rounded, contentDescription = stringResource(Res.string.nav__notifications))
        }
    }
}

/**
 * Shown while a sync runs (the sync icon in a ring filling with the background queue, X of N), while library changes
 * wait to be pushed (an hourglass) and after a sync the user hasn't looked at yet (the count of new results, red when
 * one failed); a tooltip tells which. It opens the sync history, which marks them seen.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SyncStatusAction(navigationState: AppNavigationState = LocalNavigationState.current) {
    val coordinator = koinInject<SyncCoordinator>()
    val running by coordinator.running.collectAsStateWithLifecycle()
    val queue by coordinator.queue.collectAsStateWithLifecycle()
    val pendingPush by koinInject<SyncScheduler>().pendingPushAt.collectAsStateWithLifecycle(null)
    val runs by koinInject<SyncHistory>().runs.collectAsStateWithLifecycle()
    val unseen = runs.filterNot { it.seen }
    val syncing = running.isNotEmpty() || !queue.isEmpty
    val pending = !syncing && pendingPush != null
    AnimatedVisibility(syncing || pending || unseen.isNotEmpty()) {
        val label = when {
            syncing -> syncingLabel(running, queue)
            pending -> stringResource(Res.string.sync__pending_push)
            else -> stringResource(Res.string.sync__status_new)
        }
        TooltipBox(tooltip = { PlainTooltip { Text(label) } }) {
            IconButton(
                onClick = { navigationState.backStack.add(RootNavTree.SyncHistory) },
                shapes = IconButtonDefaults.shapes()
            ) {
                if (syncing) {
                    SyncProgressIcon(queue, label)
                } else if (pending) {
                    Icon(Icons.HourglassW500Rounded, contentDescription = label)
                } else {
                    val failed = unseen.any { it.result == SyncResult.FAILED || it.result == SyncResult.LOGIN_REQUIRED }
                    BadgedBox(
                        badge = {
                            Badge(
                                containerColor = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            ) { Text(unseen.size.toString()) }
                        }
                    ) {
                        Icon(Icons.SyncW500Rounded, contentDescription = label)
                    }
                }
            }
        }
    }
}

/** The sync icon in a ring: filled by the done share of a background queue of more than one, else indeterminate. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SyncProgressIcon(queue: SyncQueue, label: String) {
    Box(Modifier.semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        if (queue.total > 1) {
            val progress by animateFloatAsState(queue.done.toFloat() / queue.total)
            CircularWavyProgressIndicator(progress = { progress }, modifier = Modifier.size(SyncRingSize))
        } else {
            CircularWavyProgressIndicator(Modifier.size(SyncRingSize))
        }
        Icon(Icons.SyncW500Rounded, contentDescription = null, Modifier.size(SyncRingIconSize))
    }
}

/**
 * "Syncing · 2 of 4 · Steam · Library · Writing… 3 of 10" for the current background sync, else the services the user
 * syncs.
 */
@Composable
internal fun syncingLabel(running: Set<ExternalService>, queue: SyncQueue): String {
    val current = queue.current
    return if (current != null) {
        listOfNotNull(
            stringResource(Res.string.sync__status_running_progress, queue.done + 1, queue.total),
            stringResource(current.service.labelRes()),
            stringResource(current.kind.label),
            queue.step?.text(),
        )
    } else {
        listOf(stringResource(Res.string.sync__status_running)) + running.map { stringResource(it.labelRes()) }
    }.joinToString(" · ")
}

private val SyncRingSize = 32.dp
private val SyncRingIconSize = 16.dp

@Composable
fun GlobalErrorAction() {
    val reporter = koinInject<ExceptionReporter>()
    val errorState by reporter.state.collectAsStateWithLifecycle()
    AnimatedVisibility(errorState != null) {
        IconButton(onClick = { reporter.show() }, shapes = IconButtonDefaults.shapes()) {
            Icon(
                Icons.ErrorW500Rounded,
                contentDescription = stringResource(Res.string.exception__action_show),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
