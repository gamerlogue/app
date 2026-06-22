package it.maicol07.gamerlogue.core

import androidx.lifecycle.ViewModel
import org.koin.core.component.KoinComponent

/**
 * Base class for every ViewModel in the app.
 *
 * Centralizes the [ViewModel] + [KoinComponent] combination so that:
 *  - dependencies can be resolved with `by inject()`,
 *  - screen state survives configuration changes through [androidx.lifecycle.viewModelScope].
 *
 * ViewModels expose their state as an immutable `StateFlow<XxxUiState>` and receive
 * navigation as callbacks from the screen, so they hold no navigation dependency.
 */
abstract class BaseViewModel : ViewModel(), KoinComponent
