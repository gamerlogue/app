package it.maicol07.gamerlogue.extensions

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update as flowUpdate

/** Apply-style [MutableStateFlow.update]: mutate [S] in-place instead of returning a copy. */
fun <S> MutableStateFlow<S>.update(block: S.() -> Unit) = flowUpdate { it.apply(block) }
