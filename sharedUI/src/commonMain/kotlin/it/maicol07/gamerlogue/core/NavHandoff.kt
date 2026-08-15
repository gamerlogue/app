package it.maicol07.gamerlogue.core

/**
 * One-shot, in-memory handoff of a value between two navigation destinations.
 *
 * Navigation 3 keys are serialized, so anything bigger than an id travels next to the back stack
 * instead of inside it: the origin [put]s the value it already loaded and the destination [take]s it
 * once, while its own request is still in flight. Nothing here survives process death — every reader
 * must be able to fetch the value itself.
 */
class NavHandoff<K, V> {
    private val pending = mutableMapOf<K, V>()

    fun put(key: K, value: V) {
        pending[key] = value
    }

    /** The pending value for [key], removing it: a handoff is consumed by its first reader. */
    fun take(key: K): V? = pending.remove(key)
}
