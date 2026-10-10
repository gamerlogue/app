package it.maicol07.gamerlogue.extensions.igdb

import androidx.compose.runtime.Composable
import at.released.igdbclient.model.CollectionType
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.collection_type__series
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private val COLLECTION_TYPE_NAMES: Map<String, StringResource> = mapOf(
    "SERIES" to Res.string.collection_type__series,
)

/** The type's localized name, or IGDB's own name for a type added after this list. */
val CollectionType.localizedName: String
    @Composable
    get() = COLLECTION_TYPE_NAMES[name.uppercase()]?.let { stringResource(it) } ?: name
