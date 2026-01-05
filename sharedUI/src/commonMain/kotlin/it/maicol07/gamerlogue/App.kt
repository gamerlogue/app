package it.maicol07.gamerlogue

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.github.michaelbull.result.unwrap
import it.maicol07.gamerlogue.auth.AuthState
import it.maicol07.gamerlogue.data.User
import it.maicol07.gamerlogue.data.UserStore
import it.maicol07.gamerlogue.di.appModule
import it.maicol07.gamerlogue.di.httpModule
import it.maicol07.gamerlogue.ui.components.layout.AppScaffold
import it.maicol07.gamerlogue.ui.components.layout.GlobalExceptionBottomSheet
import it.maicol07.gamerlogue.ui.components.layout.LocalAppUiState
import it.maicol07.gamerlogue.ui.theme.AppTheme
import it.maicol07.gamerlogue.ui.views.auth.LoginView
import it.maicol07.gamerlogue.ui.views.calendar.Calendar
import it.maicol07.gamerlogue.ui.views.game.GameDetailScreen
import it.maicol07.gamerlogue.ui.views.home.Home
import it.maicol07.gamerlogue.ui.views.library.Library
import it.maicol07.gamerlogue.ui.views.profile.Profile
import org.koin.compose.KoinApplication
import org.koin.dsl.module

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun App() {
    val backStack = rememberNavBackStack(NavKeys.savedStateConfiguration, NavKeys.Discover)
    val userStore = remember { UserStore() }

    LaunchedEffect(Unit) {
        if (BuildConfig.APP_ENV === AppEnvironment.LOCAL) {
            Logger.setMinSeverity(Severity.Verbose)
            Logger.i("Running in LOCAL environment")
        }
        val savedUser = userStore.getUser()
        if (savedUser != null) {
            AuthState.currentUser = savedUser
        }
    }

    LaunchedEffect(AuthState.token, AuthState.userId) {
        Logger.d("AuthState changed: token=${AuthState.token}, userId=${AuthState.userId}")
        if (AuthState.token != null) {
            if (AuthState.currentUser == null && AuthState.userId != null) {
                val result = safeRequest { User.find(AuthState.userId!!).data }
                if (result.isOk) {
                    val user = result.unwrap()
                    AuthState.currentUser = user
                    userStore.saveUser(user)
                }
            }
        } else {
            AuthState.currentUser = null
            AuthState.userId = null
            userStore.clear()
        }
    }

    KoinApplication({
        modules(appModule, httpModule)
        modules(
            // Compose specific module to provide the NavBackStack
            module {
                single<NavBackStack> { backStack }
            }
        )
    }) {
        AppTheme {
            AppScaffold(
                currentNavKey = backStack.last(),
                canNavigateBack = backStack.size > 1,
                navigateUp = { backStack.removeAt(backStack.lastIndex) },
            ) {
                Column(
                    modifier = Modifier.padding(it),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Override the defaults so that there isn't a horizontal space between the panes.
                    // See b/418201867
                    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
                    val directive = remember(windowAdaptiveInfo) {
                        calculatePaneScaffoldDirective(windowAdaptiveInfo)
                            .copy(horizontalPartitionSpacerSize = 0.dp)
                    }
                    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)

                    SharedTransitionLayout {
                        NavDisplay(
                            backStack = backStack,
                            sceneStrategy = listDetailStrategy,
                            entryDecorators = listOf(
                                rememberSaveableStateHolderNavEntryDecorator(),
                                rememberViewModelStoreNavEntryDecorator()
                            ),
                            entryProvider = entryProvider {
                                entry<NavKeys.Discover>(
                                    metadata = ListDetailSceneStrategy.listPane(
                                        detailPlaceholder = {
                                            Text("Choose a conversation from the list")
                                        }
                                    )
                                ) { Home() }
                                entry<NavKeys.Library>(
                                    metadata = ListDetailSceneStrategy.listPane()
                                ) {
                                    if (AuthState.token == null) {
                                        LoginView()
                                    } else {
                                        Library(
                                            onGameClick = { game ->
                                                backStack.add(NavKeys.GameDetail(game.id.toInt()))
                                            }
                                        )
                                    }
                                }
                                entry<NavKeys.Calendar>(
                                    metadata = ListDetailSceneStrategy.detailPane()
                                ) {
                                    if (AuthState.token == null) {
                                        LoginView()
                                    } else {
                                        Calendar()
                                    }
                                }
                                entry<NavKeys.Profile>(
                                    metadata = ListDetailSceneStrategy.detailPane()
                                ) {
                                    if (AuthState.token == null) {
                                        LoginView()
                                    } else {
                                        Profile()
                                    }
                                }
                                entry<NavKeys.GameDetail>(
                                    metadata = ListDetailSceneStrategy.detailPane()
                                ) { navKey ->
                                    GameDetailScreen(gameId = navKey.gameId)
                                }
                                entry<NavKeys.GameList>(
                                    metadata = ListDetailSceneStrategy.detailPane()
                                ) {
//                                GameListScreen(title = it.title, navigateTo = { backStack.add(it) })
                                }
                            }
                        )
                    }

                    val appUiState = LocalAppUiState.current
                    val showExceptionBottomSheet by remember {
                        derivedStateOf {
                            appUiState.networkException.value != null && appUiState.showExceptionBottomSheet.value
                        }
                    }
                    if (showExceptionBottomSheet) {
                        GlobalExceptionBottomSheet()
                    }
                }
            }
        }
    }
}
