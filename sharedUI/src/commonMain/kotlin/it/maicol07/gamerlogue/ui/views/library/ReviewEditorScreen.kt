package it.maicol07.gamerlogue.ui.views.library

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import at.released.igdbclient.model.Game
import com.github.michaelbull.result.get
import com.mohamedrejeb.richeditor.model.RichTextState
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.library__open_review
import gamerlogue.sharedui.generated.resources.library__overall_rating
import gamerlogue.sharedui.generated.resources.library__overall_review
import gamerlogue.sharedui.generated.resources.library__overall_review_description
import gamerlogue.sharedui.generated.resources.library__rating_average_description
import gamerlogue.sharedui.generated.resources.library__rating_description
import gamerlogue.sharedui.generated.resources.library__review
import gamerlogue.sharedui.generated.resources.library__review_add_section
import gamerlogue.sharedui.generated.resources.library__review_custom_section
import gamerlogue.sharedui.generated.resources.library__review_description
import gamerlogue.sharedui.generated.resources.library__review_remove_section
import gamerlogue.sharedui.generated.resources.library__review_sections
import gamerlogue.sharedui.generated.resources.library__review_sections_description
import gamerlogue.sharedui.generated.resources.library__review_spoiler
import gamerlogue.sharedui.generated.resources.library__save
import gamerlogue.sharedui.generated.resources.library__section_review
import gamerlogue.sharedui.generated.resources.review__not_available
import io.github.fopwoc.nav3ksp.annotation.Branch
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.AddW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CheckW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CloseW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.OpenInNewW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.RateReviewW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.VisibilityOffW500Rounded
import it.maicol07.gamerlogue.core.AppPreferences
import it.maicol07.gamerlogue.core.BaseViewModel
import it.maicol07.gamerlogue.core.LibraryChanges
import it.maicol07.gamerlogue.data.IgdbLookup
import it.maicol07.gamerlogue.data.LibraryEntry
import it.maicol07.gamerlogue.data.MAX_RATING
import it.maicol07.gamerlogue.data.ReviewAspect
import it.maicol07.gamerlogue.data.ReviewSection
import it.maicol07.gamerlogue.extensions.expressiveSegmentedColors
import it.maicol07.gamerlogue.ui.components.EmptyState
import it.maicol07.gamerlogue.ui.components.RatingBadge
import it.maicol07.gamerlogue.ui.components.ReviewMode
import it.maicol07.gamerlogue.ui.components.RichTextField
import it.maicol07.gamerlogue.ui.components.TooltipBox
import it.maicol07.gamerlogue.ui.components.currentRatingScale
import it.maicol07.gamerlogue.ui.components.game.CoverImage
import it.maicol07.gamerlogue.ui.components.layout.ScreenScaffold
import it.maicol07.gamerlogue.ui.components.layout.SegmentedListLayout
import it.maicol07.gamerlogue.ui.components.reviewSectionTitle
import it.maicol07.gamerlogue.ui.navigation.DetailPaneMetadata
import it.maicol07.gamerlogue.ui.navigation.LocalNavigationState
import it.maicol07.gamerlogue.ui.navigation.RootTree
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTree
import it.maicol07.gamerlogue.ui.theme.Dimens
import it.maicol07.gamerlogue.ui.views.auth.AuthenticatedContent
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import kotlin.math.roundToInt

/** The rating, sections and text of one of the signed-in user's library entries. */
@KoinViewModel
class ReviewEditorViewModel(@InjectedParam private val entryId: String) : BaseViewModel() {
    private val lookup by inject<IgdbLookup>()
    private val libraryChanges by inject<LibraryChanges>()
    private val preferences by inject<AppPreferences>()

    /** Read once: switching the setting does not reshape an editor already open. */
    val detailedReview = preferences.reviewMode.value == ReviewMode.DETAILED

    var entry by mutableStateOf<LibraryEntry?>(null)
    var game by mutableStateOf<Game?>(null)
    var loading by mutableStateOf(true)
    var saveLoading by mutableStateOf(false)

    var rating by mutableStateOf<Float?>(null)
    val review = RichTextState()
    var reviewSpoiler by mutableStateOf(false)
    val sections = mutableStateListOf<SectionDraft>()
    val customSectionTitle = TextFieldState()

    /** The sections' mean in a detailed review that has them, the slider's value otherwise. */
    val overallRating: Float?
        get() = if (detailedReview && sections.isNotEmpty()) {
            sections.map { it.rating }.average().toFloat()
        } else {
            rating
        }

    init {
        viewModelScope.launch {
            val loaded = safeRequest { LibraryEntry.find(entryId).data }.get()
            if (loaded != null) {
                rating = loaded.rating?.toFloat()
                review.setMarkdown(loaded.review.orEmpty())
                reviewSpoiler = loaded.reviewSpoiler
                loaded.reviewSections.forEach { sections.add(SectionDraft(it)) }
                game = safeRequest { lookup.games(listOf(loaded.gameId)) }.get()?.get(loaded.gameId)
            }
            entry = loaded
            loading = false
        }
    }

    /** Adds a section unless [key] is blank or already there. */
    fun addSection(key: String) {
        if (key.isNotBlank() && sections.none { it.key == key }) {
            sections.add(SectionDraft(key, DEFAULT_SECTION_RATING, ""))
        }
    }

    fun addCustomSection() {
        addSection(customSectionTitle.text.toString().trim())
        customSectionTitle.clearText()
    }

    /** [onSaved] runs only once the server accepted the review. */
    fun save(onSaved: () -> Unit) = viewModelScope.launch {
        val entry = entry ?: return@launch
        saveLoading = true
        entry.rating = overallRating?.roundToInt()
        // A basic review leaves the sections of an earlier detailed one untouched.
        if (detailedReview) {
            entry.ratingDetails = sections.associate { it.key to it.toAttribute() }.ifEmpty { null }
        }
        entry.review = review.toMarkdown()
        entry.reviewSpoiler = reviewSpoiler && entry.hasReview
        val saved = safeRequest { entry.save() }.isOk
        saveLoading = false
        if (saved) {
            libraryChanges.notifyChanged()
            onSaved()
        }
    }

    private companion object {
        const val DEFAULT_SECTION_RATING = MAX_RATING / 2f
    }
}

/** A detailed review's section while it is edited. */
class SectionDraft(val key: String, rating: Float, review: String) {
    constructor(section: ReviewSection) : this(section.key, section.rating?.toFloat() ?: 0f, section.review)

    var rating by mutableFloatStateOf(rating)
    val review = RichTextState().setMarkdown(review)

    /** The `rating_details` value of this section; its text is Markdown, like the review's. */
    fun toAttribute(): Map<String, Any> = mapOf("rating" to rating.roundToInt(), "review" to review.toMarkdown())
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Branch(RootTree::class, metadata = DetailPaneMetadata::class)
@Composable
fun ReviewEditorView(entryId: String) {
    val navigationState = LocalNavigationState.current
    val viewModel = koinViewModel<ReviewEditorViewModel> { parametersOf(entryId) }
    ScreenScaffold(title = Res.string.library__review) {
        AuthenticatedContent {
            when {
                viewModel.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ContainedLoadingIndicator()
                }
                viewModel.entry == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        Icons.RateReviewW500Rounded,
                        stringResource(Res.string.review__not_available),
                        hint = null
                    )
                }
                else -> Box(Modifier.fillMaxSize()) {
                    ReviewEditorContent(viewModel)
                    EditorToolbar(
                        saving = viewModel.saveLoading,
                        // The detail shows what was saved: there is nothing to open before a first save.
                        canOpenReview = viewModel.entry?.hasReview == true,
                        onOpenReview = { navigationState.backStack.add(RootNavTree.ReviewDetail(entryId, null)) },
                        onSave = { viewModel.save { navigationState.backStack.removeLastOrNull() } }
                    )
                }
            }
        }
    }
}

/** A lazy list, so that sections slide in and out with [animateItem] as they are added and removed. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ReviewEditorContent(viewModel: ReviewEditorViewModel) {
    val motion = MaterialTheme.motionScheme
    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        // The floating toolbar sits over the end of the list.
        contentPadding = PaddingValues(
            start = Dimens.ScreenPadding,
            top = Dimens.ScreenPadding,
            end = Dimens.ScreenPadding,
            bottom = ToolbarClearance
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.SectionGap)
    ) {
        viewModel.game?.let { game -> item(key = "header") { EditorHeader(game) } }
        item(key = "rating") { RatingCard(viewModel) }
        if (viewModel.detailedReview) {
            item(key = "sections-title") {
                SectionTitle(Res.string.library__review_sections, Res.string.library__review_sections_description)
            }
            items(viewModel.sections, key = { "section-${it.key}" }) { section ->
                SectionCard(
                    section = section,
                    onRemove = { viewModel.sections.remove(section) },
                    modifier = Modifier.animateItem(
                        fadeInSpec = motion.defaultEffectsSpec(),
                        placementSpec = motion.defaultSpatialSpec(),
                        fadeOutSpec = motion.fastEffectsSpec()
                    )
                )
            }
            item(key = "add-section") {
                AddSectionCard(viewModel, Modifier.animateItem(placementSpec = motion.defaultSpatialSpec()))
            }
        }
        item(key = "text") {
            ReviewTextSection(viewModel, Modifier.animateItem(placementSpec = motion.defaultSpatialSpec()))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EditorHeader(game: Game) = Row(
    horizontalArrangement = Arrangement.spacedBy(Dimens.SectionGap),
    verticalAlignment = Alignment.CenterVertically
) {
    game.CoverImage(
        Modifier.clip(MaterialTheme.shapes.medium),
        sizeModifier = Modifier.width(HeaderCoverWidth).height(HeaderCoverHeight)
    )
    Text(game.name, style = MaterialTheme.typography.headlineSmallEmphasized, modifier = Modifier.weight(1f))
}

/**
 * The overall rating as the screen's highlight. With sections it is their mean, so the slider folds
 * away as soon as the first one is added.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RatingCard(viewModel: ReviewEditorViewModel) = Surface(
    color = MaterialTheme.colorScheme.surfaceContainer,
    shape = MaterialTheme.shapes.extraLarge
) {
    val scale = currentRatingScale()
    val averaged = viewModel.detailedReview && viewModel.sections.isNotEmpty()
    Row(
        Modifier.padding(Dimens.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(Dimens.ScreenPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RatingBadge(
            rating = viewModel.overallRating,
            shape = MaterialShapes.Cookie9Sided,
            size = HeroBadgeSize,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            textStyle = MaterialTheme.typography.headlineSmallEmphasized
        )
        Column(Modifier.weight(1f).animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec())) {
            Text(
                stringResource(Res.string.library__overall_rating),
                style = MaterialTheme.typography.titleMediumEmphasized
            )
            Text(
                if (averaged) {
                    stringResource(Res.string.library__rating_average_description)
                } else {
                    stringResource(Res.string.library__rating_description, scale.max)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!averaged) {
                RatingSlider(viewModel.rating ?: 0f, onValueChange = { viewModel.rating = it })
            }
        }
    }
}

@Composable
private fun SectionTitle(title: StringResource, description: StringResource) = Column(
    verticalArrangement = Arrangement.spacedBy(4.dp)
) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.titleMediumEmphasized,
        color = MaterialTheme.colorScheme.primary
    )
    Text(
        stringResource(description),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SectionCard(section: SectionDraft, onRemove: () -> Unit, modifier: Modifier) = Card(
    modifier = modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
) {
    Column(Modifier.padding(Dimens.ScreenPadding), verticalArrangement = Arrangement.spacedBy(Dimens.ItemGap)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.SectionGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RatingBadge(
                rating = section.rating,
                shape = MaterialShapes.Cookie6Sided,
                size = SectionBadgeSize,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                textStyle = MaterialTheme.typography.labelLargeEmphasized
            )
            Text(
                reviewSectionTitle(section.key),
                style = MaterialTheme.typography.titleMediumEmphasized,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRemove, shapes = IconButtonDefaults.shapes()) {
                Icon(
                    Icons.CloseW500Rounded,
                    contentDescription = stringResource(Res.string.library__review_remove_section)
                )
            }
        }
        RatingSlider(section.rating, onValueChange = { section.rating = it })
        RichTextField(
            state = section.review,
            label = stringResource(Res.string.library__section_review, reviewSectionTitle(section.key)),
            supportingText = null,
            minHeight = SectionFieldMinHeight,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** The predefined sections still missing, plus a field for a custom one. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AddSectionCard(viewModel: ReviewEditorViewModel, modifier: Modifier) = OutlinedCard(
    modifier = modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.large
) {
    Column(Modifier.padding(Dimens.ScreenPadding), verticalArrangement = Arrangement.spacedBy(Dimens.ItemGap)) {
        Text(stringResource(Res.string.library__review_add_section), style = MaterialTheme.typography.titleSmall)
        FlowRow(
            modifier = Modifier.animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec()),
            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemGap)
        ) {
            for (aspect in ReviewAspect.entries) {
                if (viewModel.sections.none { it.key == aspect.key }) {
                    AssistChip(
                        onClick = { viewModel.addSection(aspect.key) },
                        label = { Text(stringResource(aspect.label)) },
                        leadingIcon = { Icon(Icons.AddW500Rounded, contentDescription = null) }
                    )
                }
            }
        }
        TextField(
            state = viewModel.customSectionTitle,
            label = { Text(stringResource(Res.string.library__review_custom_section)) },
            trailingIcon = {
                IconButton(onClick = viewModel::addCustomSection, shapes = IconButtonDefaults.shapes()) {
                    Icon(
                        Icons.AddW500Rounded,
                        contentDescription = stringResource(Res.string.library__review_add_section)
                    )
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            onKeyboardAction = { viewModel.addCustomSection() },
            lineLimits = TextFieldLineLimits.SingleLine,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ReviewTextSection(viewModel: ReviewEditorViewModel, modifier: Modifier) = Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(Dimens.ItemGap)
) {
    // Next to the sections' texts, this one has to say it is about the whole game.
    if (viewModel.detailedReview) {
        SectionTitle(Res.string.library__overall_review, Res.string.library__overall_review_description)
    }
    RichTextField(
        state = viewModel.review,
        label = stringResource(
            if (viewModel.detailedReview) Res.string.library__overall_review else Res.string.library__review
        ),
        supportingText = stringResource(Res.string.library__review_description),
        minHeight = ReviewFieldMinHeight,
        modifier = Modifier.fillMaxWidth()
    )
    SegmentedListLayout {
        SegmentedListItem(
            checked = viewModel.reviewSpoiler,
            onCheckedChange = { viewModel.reviewSpoiler = it },
            shapes = ListItemDefaults.segmentedShapes(0, 1),
            colors = ListItemDefaults.expressiveSegmentedColors(),
            leadingContent = { Icon(Icons.VisibilityOffW500Rounded, contentDescription = null) },
            trailingContent = {
                Switch(checked = viewModel.reviewSpoiler, onCheckedChange = { viewModel.reviewSpoiler = it })
            }
        ) {
            Text(stringResource(Res.string.library__review_spoiler))
        }
    }
}

/** Opening the saved review, and saving as the toolbar's main action. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BoxScope.EditorToolbar(
    saving: Boolean,
    canOpenReview: Boolean,
    onOpenReview: () -> Unit,
    onSave: () -> Unit
) = HorizontalFloatingToolbar(
    expanded = true,
    floatingActionButton = {
        TooltipBox(tooltip = { PlainTooltip { Text(stringResource(Res.string.library__save)) } }) {
            FloatingToolbarDefaults.VibrantFloatingActionButton(onClick = { if (!saving) onSave() }) {
                if (saving) {
                    LoadingIndicator(color = LocalContentColor.current)
                } else {
                    Icon(Icons.CheckW500Rounded, contentDescription = stringResource(Res.string.library__save))
                }
            }
        }
    },
    modifier = Modifier
        .align(Alignment.BottomEnd)
        .navigationBarsPadding()
        .imePadding()
        .offset(x = -FloatingToolbarDefaults.ScreenOffset, y = -FloatingToolbarDefaults.ScreenOffset)
) {
    TooltipBox(tooltip = { PlainTooltip { Text(stringResource(Res.string.library__open_review)) } }) {
        IconButton(onClick = onOpenReview, enabled = canOpenReview, shapes = IconButtonDefaults.shapes()) {
            Icon(Icons.OpenInNewW500Rounded, contentDescription = stringResource(Res.string.library__open_review))
        }
    }
}

@Composable
private fun RatingSlider(value: Float, onValueChange: (Float) -> Unit) = Slider(
    value = value,
    onValueChange = onValueChange,
    valueRange = 0f..MAX_RATING.toFloat(),
    steps = MAX_RATING - 1,
    modifier = Modifier.fillMaxWidth()
)

private val HeaderCoverWidth = 64.dp
private val HeaderCoverHeight = 85.dp
private val HeroBadgeSize = 88.dp
private val SectionBadgeSize = 48.dp
private val ReviewFieldMinHeight = 200.dp
private val SectionFieldMinHeight = 96.dp

/** Room left under the list for the floating toolbar. */
private val ToolbarClearance = 112.dp
