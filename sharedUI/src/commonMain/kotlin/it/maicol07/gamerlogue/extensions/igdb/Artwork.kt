package it.maicol07.gamerlogue.extensions.igdb

import at.released.igdbclient.model.Artwork
import com.squareup.wire.ProtoReader
import okio.Buffer

/** IGDB artwork types that are the bare game logo: white, black, color and historical. */
private val LOGO_ARTWORK_TYPES = setOf(5L, 6L, 7L, 13L)

/** Protobuf tag of `artwork_type` in IGDB's Artwork message, and of `id` inside it. */
private const val ARTWORK_TYPE_TAG = 10
private const val ID_TAG = 1

/** The game field [isLogo] reads; the typed field DSL lacks it, so request it as a raw string. */
const val ARTWORKS_TYPE_FIELD = "artworks.artwork_type"

/** Whether this artwork is the game's logo on its own; needs [ARTWORKS_TYPE_FIELD] in the query. */
val Artwork.isLogo: Boolean get() = artworkTypeId() in LOGO_ARTWORK_TYPES

/**
 * igdbclient 0.8's Artwork predates IGDB's `artwork_type`, but Wire keeps the fields it does not
 * know in [Artwork.unknownFields], so the type id is decoded from there.
 *
 * ponytail: drop this once igdbclient exposes `artwork_type`.
 */
private fun Artwork.artworkTypeId(): Long? {
    var typeId: Long? = null
    ProtoReader(Buffer().write(unknownFields)).run {
        forEachTag { tag ->
            if (tag == ARTWORK_TYPE_TAG) {
                forEachTag { innerTag ->
                    if (innerTag == ID_TAG) readVarint64().also { typeId = it } else skip()
                }
            } else {
                skip()
            }
        }
    }
    return typeId
}
