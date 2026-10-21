package it.maicol07.gamerlogue.ui.views.profile

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.lifecycle.viewModelScope
import at.released.igdbclient.model.Game
import at.released.igdbclient.model.Platform
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import it.maicol07.gamerlogue.core.StateViewModel
import it.maicol07.gamerlogue.data.ApiErrorCodes
import it.maicol07.gamerlogue.data.IgdbLookup
import it.maicol07.gamerlogue.data.User
import it.maicol07.gamerlogue.data.UserSchema
import it.maicol07.gamerlogue.data.UserStore
import it.maicol07.gamerlogue.data.saveSessionUser
import it.maicol07.gamerlogue.extensions.jsonApiErrorCode
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import org.koin.core.component.inject

/** A wide image of a game, offered as the profile banner. */
data class BannerOption(val gameId: Int, val imageId: String)

/** Edits the signed-in user's profile; only the fields that changed are sent. */
@KoinViewModel
class EditProfileViewModel : StateViewModel<EditProfileViewModel.UiState>(UiState()) {
    data class UiState(
        val user: User? = null,
        val loading: Boolean = true,
        val saving: Boolean = false,
        /** The server refused the input, most likely a nickname already taken or outside the rules. */
        val invalid: Boolean = false,
        val private: Boolean = false,
        val platformIds: Set<Int> = emptySet(),
        val banner: BannerOption? = null,
        val allPlatforms: List<Platform> = emptyList(),
        /** The game picked in the game list, whose images are offered as the banner. */
        val bannerGame: Game? = null,
    ) {
        /** Banner candidates: the artworks and screenshots of [bannerGame]. */
        val bannerOptions: List<BannerOption>
            get() = bannerGame?.let { game ->
                val images = game.artworks.map { it.image_id } + game.screenshots.map { it.image_id }
                images.map { BannerOption(game.id.toInt(), it) }
            }.orEmpty()
    }

    private val authProvider by inject<AuthTokenProvider>()
    private val userStore by inject<UserStore>()
    private val lookup by inject<IgdbLookup>()

    val nickname = TextFieldState()
    val firstName = TextFieldState()
    val lastName = TextFieldState()
    val bio = TextFieldState()

    /**
     * Fills the form from the server when the editor opens, unless it holds edits left for the banner
     * pick in the game list.
     */
    fun loadIfNeeded() {
        if (state.user == null) load()
    }

    /** Drops the form and its unsaved edits: the next opening loads it again. */
    fun discard() = update { UiState() }

    private fun load() = viewModelScope.launch {
        update { copy(loading = true, invalid = false) }
        val userId = authProvider.session.value.userId ?: return@launch
        val user = safeRequest { User.find(userId).data }.get()
        if (user == null) {
            update { copy(loading = false) }
            return@launch
        }
        nickname.setTextAndPlaceCursorAtEnd(user.nickname.orEmpty())
        firstName.setTextAndPlaceCursorAtEnd(user.firstName.orEmpty())
        lastName.setTextAndPlaceCursorAtEnd(user.lastName.orEmpty())
        bio.setTextAndPlaceCursorAtEnd(user.bio.orEmpty())
        val bannerGame = user.bannerGameId
        val bannerImage = user.bannerImageId
        update {
            copy(
                user = user,
                loading = false,
                private = user.isPrivate,
                platformIds = user.platformsIds.toSet(),
                banner = if (bannerGame != null && bannerImage != null) BannerOption(bannerGame, bannerImage) else null,
            )
        }
    }

    fun loadPlatforms() = viewModelScope.launch {
        if (state.allPlatforms.isNotEmpty()) return@launch
        safeRequest { lookup.platforms() }.get()?.let { update { copy(allPlatforms = it) } }
    }

    fun setPrivate(private: Boolean) = update { copy(private = private) }

    fun togglePlatform(id: Int) = update {
        copy(platformIds = if (id in platformIds) platformIds - id else platformIds + id)
    }

    fun setBanner(banner: BannerOption?) = update { copy(banner = banner) }

    /** Offers the images of the game [gameId], picked in the game list, as the banner. */
    fun pickBannerGame(gameId: Int) = viewModelScope.launch {
        val game = safeRequest { lookup.games(listOf(gameId))[gameId] }.get()
        update { copy(bannerGame = game) }
    }

    fun closeBannerImages() = update { copy(bannerGame = null) }

    fun save(onSaved: () -> Unit) = viewModelScope.launch {
        val user = state.user ?: return@launch
        update { copy(saving = true, invalid = false) }
        val edited = User().apply {
            id = user.id
            isPersisted = true
        }
        edited.setIfChanged(user.nickname, nickname.text.toString().trim().ifBlank { null }) { nickname = it }
        edited.setIfChanged(user.firstName, firstName.text.toString().trim().ifBlank { null }) { firstName = it }
        edited.setIfChanged(user.lastName, lastName.text.toString().trim().ifBlank { null }) { lastName = it }
        val newBio = bio.text.toString().trim().take(UserSchema.MAX_BIO_LENGTH).ifBlank { null }
        edited.setIfChanged(user.bio, newBio) { bio = it }
        val visibility = if (state.private) UserSchema.VISIBILITY_PRIVATE else UserSchema.VISIBILITY_PUBLIC
        edited.setIfChanged(user.visibility, visibility) { this.visibility = it }
        edited.setIfChanged(user.platformsIds, state.platformIds.sorted()) { platformsIds = it }
        val banner = state.banner
        edited.setIfChanged(
            user.bannerGameId to user.bannerImageId,
            banner?.gameId to banner?.imageId
        ) { (game, image) ->
            bannerGameId = game
            bannerImageId = image
        }
        val result = safeRequest(ApiErrorCodes::isValidation) { edited.save() }
        val invalid = result.getError()?.jsonApiErrorCode?.let(ApiErrorCodes::isValidation) == true
        update { copy(saving = false, invalid = invalid) }
        if (result.isOk) {
            userStore.saveSessionUser(authProvider, edited)
            onSaved()
        }
    }
}

private inline fun <T> User.setIfChanged(old: T, new: T, set: User.(T) -> Unit) {
    if (old != new) set(new)
}
