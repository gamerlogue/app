package it.maicol07.gamerlogue.core

import kotlinx.coroutines.flow.MutableStateFlow

abstract class StateViewModel<S>(initialState: S) : BaseViewModel() {
    val uiState = MutableStateFlow(initialState)
    protected val state: S get() = uiState.value
}
