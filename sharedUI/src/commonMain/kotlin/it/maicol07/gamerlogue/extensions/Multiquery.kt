package it.maicol07.gamerlogue.extensions

import at.released.igdbclient.model.UnpackedMultiQueryResult

/** The validated [T] results of the sub-query named [name]. */
inline fun <reified T : Any> List<UnpackedMultiQueryResult<*>>.multiqueryResults(name: String): List<T> =
    firstOrNull { it.name == name }?.results.orEmpty().mapIndexed { index, value ->
        value as? T ?: throw IllegalStateException(
            "Multiquery '$name' result[$index] expected ${T::class.simpleName}, got ${value::class.simpleName}"
        )
    }
