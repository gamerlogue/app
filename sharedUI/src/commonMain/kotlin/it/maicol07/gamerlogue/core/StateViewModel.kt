package it.maicol07.gamerlogue.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Base class for ViewModels that expose a single immutable UI state as a [StateFlow].
 *
 * Subclasses read the current value via [state] and mutate it with [update], passing a reducer
 * with the state as receiver — `update { copy(loading = true) }` instead of the verbose
 * `_state.update { it.copy(loading = true) }`. The backing [MutableStateFlow] stays private, so
 * state can only change through [update].
 */
abstract class StateViewModel<S>(initialState: S) : BaseViewModel() {
    val uiState: StateFlow<S>
        field = MutableStateFlow(initialState)

    /** The current UI state. */
    protected val state: S get() = uiState.value

    /** Atomically replace the state, with the previous state as receiver. */
    protected fun update(reducer: S.() -> S) = uiState.update { it.reducer() }
}
