package it.maicol07.gamerlogue.ui.views.list

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import at.released.igdbclient.model.GameCategoryEnum
import at.released.igdbclient.model.GameMode
import at.released.igdbclient.model.GameStatusEnum
import at.released.igdbclient.model.Genre
import at.released.igdbclient.model.IgdbImageSize
import at.released.igdbclient.model.PlayerPerspective
import at.released.igdbclient.model.Theme
import at.released.igdbclient.util.igdbImageUrl
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.gamelist__columns_count
import gamerlogue.sharedui.generated.resources.gamelist__company_role_developer
import gamerlogue.sharedui.generated.resources.gamelist__company_role_porting
import gamerlogue.sharedui.generated.resources.gamelist__company_role_publisher
import gamerlogue.sharedui.generated.resources.gamelist__company_role_supporting
import gamerlogue.sharedui.generated.resources.gamelist__critics_rating
import gamerlogue.sharedui.generated.resources.gamelist__filter_categories
import gamerlogue.sharedui.generated.resources.gamelist__filter_companies
import gamerlogue.sharedui.generated.resources.gamelist__filter_company_roles
import gamerlogue.sharedui.generated.resources.gamelist__filter_engines
import gamerlogue.sharedui.generated.resources.gamelist__filter_franchises
import gamerlogue.sharedui.generated.resources.gamelist__filter_keywords
import gamerlogue.sharedui.generated.resources.gamelist__filter_platforms
import gamerlogue.sharedui.generated.resources.gamelist__filter_player_perspectives
import gamerlogue.sharedui.generated.resources.gamelist__filter_status
import gamerlogue.sharedui.generated.resources.gamelist__filter_time_to_beat
import gamerlogue.sharedui.generated.resources.gamelist__filter_title
import gamerlogue.sharedui.generated.resources.gamelist__filter_type_to_search
import gamerlogue.sharedui.generated.resources.gamelist__grid_columns
import gamerlogue.sharedui.generated.resources.gamelist__hours_range
import gamerlogue.sharedui.generated.resources.gamelist__hours_range_open
import gamerlogue.sharedui.generated.resources.gamelist__release_all
import gamerlogue.sharedui.generated.resources.gamelist__release_released
import gamerlogue.sharedui.generated.resources.gamelist__release_status
import gamerlogue.sharedui.generated.resources.gamelist__release_upcoming
import gamerlogue.sharedui.generated.resources.gamelist__release_year
import gamerlogue.sharedui.generated.resources.gamelist__reset
import gamerlogue.sharedui.generated.resources.gamelist__search_companies
import gamerlogue.sharedui.generated.resources.gamelist__search_engines
import gamerlogue.sharedui.generated.resources.gamelist__search_franchises
import gamerlogue.sharedui.generated.resources.gamelist__search_keywords
import gamerlogue.sharedui.generated.resources.gamelist__sort_dir_asc
import gamerlogue.sharedui.generated.resources.gamelist__sort_dir_desc
import gamerlogue.sharedui.generated.resources.gamelist__sort_disabled_search
import gamerlogue.sharedui.generated.resources.gamelist__sort_field
import gamerlogue.sharedui.generated.resources.gamelist__sort_field_critics_rating
import gamerlogue.sharedui.generated.resources.gamelist__sort_field_name
import gamerlogue.sharedui.generated.resources.gamelist__sort_field_popularity
import gamerlogue.sharedui.generated.resources.gamelist__sort_field_release_date
import gamerlogue.sharedui.generated.resources.gamelist__sort_field_user_rating
import gamerlogue.sharedui.generated.resources.gamelist__user_rating
import gamerlogue.sharedui.generated.resources.game__game_modes_title
import gamerlogue.sharedui.generated.resources.game__genres_title
import gamerlogue.sharedui.generated.resources.game__themes_title
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ArrowDownwardW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ArrowUpwardW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CalendarMonthW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CategoryW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CloseW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CodeW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.DevicesW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.DomainW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ExploreW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.Grid4x4W500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.HistoryW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.HourglassW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.InfoW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.JoystickW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.LayersW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.NewsstandW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.PaletteW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.SearchW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.SortW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.StarW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.StyleW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.WandStarsW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.Icons as SimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.AndroidSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.IosSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.LinuxSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.MacosSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.Playstation4SimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.Playstation5SimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.svgl.Icons as SvglIcons
import io.github.kingsword09.symbolcraft.symbols.icons.svgl.icons.WindowsSvgl
import io.github.kingsword09.symbolcraft.symbols.icons.svgl.icons.XboxSvgl
import it.maicol07.gamerlogue.extensions.igdb.icon
import it.maicol07.gamerlogue.extensions.igdb.localizedName
import it.maicol07.gamerlogue.ui.components.ConnectedButtonGroup
import it.maicol07.gamerlogue.ui.components.RemoteImage
import it.maicol07.gamerlogue.ui.components.SingleSelectConnectedButtonGroup
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private val LogoSize = 18.dp

/** Slider stops every 5 hours across the 0..[MaxHoursToBeat] span. */
private const val HoursToBeatSteps = 19

private const val OptionsFadeInMillis = 200
private const val OptionsFadeOutMillis = 120

/** Incoming options slide up from a quarter of their own height. */
private const val SlideFraction = 4

private val CompanyRole.label: StringResource
    get() = when (this) {
        CompanyRole.DEVELOPER -> Res.string.gamelist__company_role_developer
        CompanyRole.PUBLISHER -> Res.string.gamelist__company_role_publisher
        CompanyRole.PORTING -> Res.string.gamelist__company_role_porting
        CompanyRole.SUPPORTING -> Res.string.gamelist__company_role_supporting
    }

/** Slider stops of both rating sliders: one every 5 points across 0..[MaxRating]. */
private const val RatingSteps = 19

/** IGDB ids of the option sets that are fixed rather than looked up. */
private val PlayerPerspectiveIds = 1..7
private val GenreIds = listOf(4, 5, 7, 8, 9, 10, 12, 13, 14, 15, 25, 31, 32, 33, 34, 35, 36)
private val ThemeIds = listOf(1, 17, 18, 19, 20, 21, 22, 23, 27, 33, 35, 38, 39, 43, 44)
private val GameModeIds = 1..6

private val FilterCategories = listOf(
    GameCategoryEnum.MAIN_GAME,
    GameCategoryEnum.DLC_ADDON,
    GameCategoryEnum.EXPANSION,
    GameCategoryEnum.REMAKE,
    GameCategoryEnum.REMASTER,
    GameCategoryEnum.BUNDLE,
    GameCategoryEnum.STANDALONE_EXPANSION,
    GameCategoryEnum.MOD,
)

private val FilterStatuses = listOf(
    GameStatusEnum.RELEASED,
    GameStatusEnum.EARLY_ACCESS,
    GameStatusEnum.ALPHA,
    GameStatusEnum.BETA,
    GameStatusEnum.DELISTED,
    GameStatusEnum.CANCELLED,
)

private data class FilterPlatform(val id: Int, val name: String, val icon: ImageVector?)

private val popularPlatforms = listOf(
    FilterPlatform(6, "PC", SvglIcons.WindowsSvgl),
    FilterPlatform(167, "PS5", SimpleIcons.Playstation5SimpleIcons),
    FilterPlatform(48, "PS4", SimpleIcons.Playstation4SimpleIcons),
    FilterPlatform(169, "Xbox Series X|S", SvglIcons.XboxSvgl),
    FilterPlatform(49, "Xbox One", SvglIcons.XboxSvgl),
    FilterPlatform(130, "Switch", Icons.JoystickW500Rounded),
    FilterPlatform(14, "Mac", SimpleIcons.MacosSimpleIcons),
    FilterPlatform(3, "Linux", SimpleIcons.LinuxSimpleIcons),
    FilterPlatform(39, "iOS", SimpleIcons.IosSimpleIcons),
    FilterPlatform(34, "Android", SimpleIcons.AndroidSimpleIcons),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
@Suppress("LongMethod")
fun GameListFilterSheet(
    filterState: GameListFilterState,
    columnCount: Int,
    filterSearches: Map<FilterSearchTarget, FilterSearchState>,
    defaultOptions: Map<FilterSearchTarget, List<NamedSearchResult>>,
    onFilterSearch: (FilterSearchTarget, String) -> Unit,
    onColumnCountChange: (Int) -> Unit,
    onFilterChange: (GameListFilterState) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.gamelist__filter_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    shapes = ButtonDefaults.shapes(),
                    onClick = onReset
                ) {
                    Text(stringResource(Res.string.gamelist__reset))
                }
            }

            // Grid Column Size Selector (Slider with steps: 2 to 5 columns, default 3)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterSectionHeader(
                    icon = Icons.Grid4x4W500Rounded,
                    title = Res.string.gamelist__grid_columns,
                    trailingText = stringResource(Res.string.gamelist__columns_count, columnCount)
                )
                Slider(
                    value = columnCount.toFloat(),
                    onValueChange = { onColumnCountChange(it.toInt()) },
                    valueRange = MinColumns.toFloat()..MaxColumns.toFloat(),
                    steps = MaxColumns - MinColumns - 1,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            HorizontalDivider()

            // Sort Section with Direction Toggle IconButton. IGDB cannot sort a search, so the
            // whole group is disabled while a query is active rather than silently ignored.
            val sortEnabled = filterState.searchQuery.isBlank()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.SortW500Rounded,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(Res.string.gamelist__sort_field),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(
                        enabled = sortEnabled,
                        shapes = IconButtonDefaults.shapes(),
                        onClick = {
                            val newDir = if (filterState.sortDirection == SortDirection.DESC) SortDirection.ASC else SortDirection.DESC
                            onFilterChange(filterState.copy(sortDirection = newDir))
                        }
                    ) {
                        Icon(
                            imageVector = if (filterState.sortDirection == SortDirection.DESC) {
                                Icons.ArrowDownwardW500Rounded
                            } else {
                                Icons.ArrowUpwardW500Rounded
                            },
                            contentDescription = stringResource(
                                if (filterState.sortDirection == SortDirection.DESC) {
                                    Res.string.gamelist__sort_dir_desc
                                } else {
                                    Res.string.gamelist__sort_dir_asc
                                }
                            ),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                SingleSelectConnectedButtonGroup(
                    options = SortField.entries,
                    selected = filterState.sortField,
                    onSelectedChange = { newField ->
                        onFilterChange(filterState.copy(sortField = newField ?: SortField.POPULARITY))
                    },
                    toggleButtonText = { field ->
                        stringResource(
                            when (field) {
                                SortField.POPULARITY -> Res.string.gamelist__sort_field_popularity
                                SortField.USER_RATING -> Res.string.gamelist__sort_field_user_rating
                                SortField.CRITICS_RATING -> Res.string.gamelist__sort_field_critics_rating
                                SortField.RELEASE_DATE -> Res.string.gamelist__sort_field_release_date
                                SortField.NAME -> Res.string.gamelist__sort_field_name
                            }
                        )
                    },
                    toggleButtonEnabled = { sortEnabled },
                    deselectable = false
                )
                if (!sortEnabled) {
                    Text(
                        text = stringResource(Res.string.gamelist__sort_disabled_search),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            RangeFilterSection(
                icon = Icons.StarW500Rounded,
                title = Res.string.gamelist__user_rating,
                trailingText = "★ ${filterState.minUserRating.toInt()} – ${filterState.maxUserRating.toInt()}",
                value = filterState.minUserRating..filterState.maxUserRating,
                valueRange = 0f..MaxRating,
                steps = RatingSteps,
                onValueChange = { range ->
                    onFilterChange(filterState.copy(minUserRating = range.start, maxUserRating = range.endInclusive))
                }
            )

            RangeFilterSection(
                icon = Icons.WandStarsW500Rounded,
                title = Res.string.gamelist__critics_rating,
                trailingText = "★ ${filterState.minCriticsRating.toInt()} – ${filterState.maxCriticsRating.toInt()}",
                value = filterState.minCriticsRating..filterState.maxCriticsRating,
                valueRange = 0f..MaxRating,
                steps = RatingSteps,
                onValueChange = { range ->
                    onFilterChange(
                        filterState.copy(minCriticsRating = range.start, maxCriticsRating = range.endInclusive)
                    )
                }
            )

            RangeFilterSection(
                icon = Icons.HistoryW500Rounded,
                title = Res.string.gamelist__release_year,
                trailingText = "${filterState.minReleaseYear} – ${filterState.maxReleaseYear}",
                value = filterState.minReleaseYear.toFloat()..filterState.maxReleaseYear.toFloat(),
                valueRange = MinReleaseYear.toFloat()..MaxReleaseYear.toFloat(),
                // One stop per year, both ends included.
                steps = MaxReleaseYear - MinReleaseYear - 1,
                onValueChange = { range ->
                    onFilterChange(
                        filterState.copy(
                            minReleaseYear = range.start.toInt(),
                            maxReleaseYear = range.endInclusive.toInt()
                        )
                    )
                }
            )

            // Hours of the "normally" completion time.
            RangeFilterSection(
                icon = Icons.HourglassW500Rounded,
                title = Res.string.gamelist__filter_time_to_beat,
                trailingText = if (filterState.maxHoursToBeat >= MaxHoursToBeat) {
                    stringResource(Res.string.gamelist__hours_range_open, filterState.minHoursToBeat.toInt())
                } else {
                    stringResource(
                        Res.string.gamelist__hours_range,
                        filterState.minHoursToBeat.toInt(),
                        filterState.maxHoursToBeat.toInt()
                    )
                },
                value = filterState.minHoursToBeat..filterState.maxHoursToBeat,
                valueRange = 0f..MaxHoursToBeat,
                steps = HoursToBeatSteps,
                onValueChange = { range ->
                    onFilterChange(filterState.copy(minHoursToBeat = range.start, maxHoursToBeat = range.endInclusive))
                }
            )

            // Release Status Filter
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterSectionHeader(
                    icon = Icons.CalendarMonthW500Rounded,
                    title = Res.string.gamelist__release_status
                )
                SingleSelectConnectedButtonGroup(
                    options = ReleaseStatusFilter.entries,
                    selected = filterState.releaseStatus,
                    onSelectedChange = { newStatus ->
                        onFilterChange(filterState.copy(releaseStatus = newStatus ?: ReleaseStatusFilter.ALL))
                    },
                    toggleButtonText = { status ->
                        stringResource(
                            when (status) {
                                ReleaseStatusFilter.ALL -> Res.string.gamelist__release_all
                                ReleaseStatusFilter.RELEASED -> Res.string.gamelist__release_released
                                ReleaseStatusFilter.UPCOMING -> Res.string.gamelist__release_upcoming
                            }
                        )
                    },
                    deselectable = false
                )
            }

            MultiSelectFilterSection(
                icon = Icons.DevicesW500Rounded,
                title = Res.string.gamelist__filter_platforms,
                options = popularPlatforms,
                idOf = FilterPlatform::id,
                selected = filterState.platformIds,
                onSelectedChange = { onFilterChange(filterState.copy(platformIds = it)) },
                label = { it.name },
                optionIcon = { it.icon }
            )

            MultiSelectFilterSection(
                icon = Icons.ExploreW500Rounded,
                title = Res.string.gamelist__filter_player_perspectives,
                options = PlayerPerspectiveIds.map { PlayerPerspective(it.toLong()) },
                idOf = { it.id.toInt() },
                selected = filterState.playerPerspectiveIds,
                onSelectedChange = { onFilterChange(filterState.copy(playerPerspectiveIds = it)) },
                label = { it.localizedName },
                optionIcon = { it.icon }
            )

            MultiSelectFilterSection(
                icon = Icons.LayersW500Rounded,
                title = Res.string.gamelist__filter_categories,
                options = FilterCategories,
                idOf = GameCategoryEnum::ordinal,
                selected = filterState.categoryIds,
                onSelectedChange = { onFilterChange(filterState.copy(categoryIds = it)) },
                label = { it.localizedName }
            )

            MultiSelectFilterSection(
                icon = Icons.InfoW500Rounded,
                title = Res.string.gamelist__filter_status,
                options = FilterStatuses,
                idOf = GameStatusEnum::ordinal,
                selected = filterState.statusIds,
                onSelectedChange = { onFilterChange(filterState.copy(statusIds = it)) },
                label = { it.localizedName }
            )

            MultiSelectFilterSection(
                icon = Icons.CategoryW500Rounded,
                title = Res.string.game__genres_title,
                options = GenreIds.map { Genre(it.toLong()) },
                idOf = { it.id.toInt() },
                selected = filterState.genreIds,
                onSelectedChange = { onFilterChange(filterState.copy(genreIds = it)) },
                label = { it.localizedName },
                optionIcon = { it.icon }
            )

            MultiSelectFilterSection(
                icon = Icons.PaletteW500Rounded,
                title = Res.string.game__themes_title,
                options = ThemeIds.map { Theme(it.toLong()) },
                idOf = { it.id.toInt() },
                selected = filterState.themeIds,
                onSelectedChange = { onFilterChange(filterState.copy(themeIds = it)) },
                label = { it.localizedName },
                optionIcon = { it.icon }
            )

            MultiSelectFilterSection(
                icon = Icons.JoystickW500Rounded,
                title = Res.string.game__game_modes_title,
                options = GameModeIds.map { GameMode(it.toLong()) },
                idOf = { it.id.toInt() },
                selected = filterState.gameModeIds,
                onSelectedChange = { onFilterChange(filterState.copy(gameModeIds = it)) },
                label = { it.localizedName },
                optionIcon = { it.icon }
            )

            // Developer / Publisher, with the role the company had on the game
            SearchableFilterSection(
                icon = Icons.DomainW500Rounded,
                title = Res.string.gamelist__filter_companies,
                placeholder = stringResource(Res.string.gamelist__search_companies),
                target = FilterSearchTarget.COMPANY,
                defaultOptions = defaultOptions[FilterSearchTarget.COMPANY].orEmpty(),
                searchState = filterSearches[FilterSearchTarget.COMPANY] ?: FilterSearchState(),
                selected = filterState.companyIds,
                showLogos = true,
                onFilterSearch = onFilterSearch,
                onSelectedChange = { ids ->
                    // Drop the roles of a deselected company: a leftover entry (even an empty role
                    // set) would keep the filter looking active while matching nothing.
                    onFilterChange(
                        filterState.copy(
                            companyIds = ids,
                            companyRoles = filterState.companyRoles.filterKeys { it in ids }
                        )
                    )
                }
            ) { selectedCompanies ->
                // One role group per selected company: the role applies to that company alone.
                selectedCompanies.forEach { company ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(Res.string.gamelist__filter_company_roles, company.name),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val roles = filterState.companyRoles[company.id].orEmpty()
                        ConnectedButtonGroup(
                            options = CompanyRole.entries,
                            checked = { role -> roles.contains(role) },
                            onCheckedChange = { role, isChecked ->
                                val updated = if (isChecked) roles + role else roles - role
                                // An empty set means "any role"; storing it would leave the filter
                                // looking active with nothing selected.
                                val companyRoles = if (updated.isEmpty()) {
                                    filterState.companyRoles - company.id
                                } else {
                                    filterState.companyRoles + (company.id to updated)
                                }
                                onFilterChange(filterState.copy(companyRoles = companyRoles))
                            },
                            toggleButtonText = { role -> stringResource(role.label) },
                            showChecks = true,
                            multiple = true
                        )
                    }
                }
            }

            SearchableFilterSection(
                icon = Icons.NewsstandW500Rounded,
                title = Res.string.gamelist__filter_franchises,
                placeholder = stringResource(Res.string.gamelist__search_franchises),
                target = FilterSearchTarget.FRANCHISE,
                defaultOptions = defaultOptions[FilterSearchTarget.FRANCHISE].orEmpty(),
                searchState = filterSearches[FilterSearchTarget.FRANCHISE] ?: FilterSearchState(),
                selected = filterState.franchiseIds,
                onFilterSearch = onFilterSearch,
                onSelectedChange = { onFilterChange(filterState.copy(franchiseIds = it)) }
            )

            SearchableFilterSection(
                icon = Icons.CodeW500Rounded,
                title = Res.string.gamelist__filter_engines,
                placeholder = stringResource(Res.string.gamelist__search_engines),
                target = FilterSearchTarget.ENGINE,
                defaultOptions = defaultOptions[FilterSearchTarget.ENGINE].orEmpty(),
                searchState = filterSearches[FilterSearchTarget.ENGINE] ?: FilterSearchState(),
                selected = filterState.gameEngineIds,
                showLogos = true,
                onFilterSearch = onFilterSearch,
                onSelectedChange = { onFilterChange(filterState.copy(gameEngineIds = it)) }
            )

            SearchableFilterSection(
                icon = Icons.StyleW500Rounded,
                title = Res.string.gamelist__filter_keywords,
                placeholder = stringResource(Res.string.gamelist__search_keywords),
                target = FilterSearchTarget.KEYWORD,
                defaultOptions = defaultOptions[FilterSearchTarget.KEYWORD].orEmpty(),
                searchState = filterSearches[FilterSearchTarget.KEYWORD] ?: FilterSearchState(),
                selected = filterState.keywordIds,
                onFilterSearch = onFilterSearch,
                onSelectedChange = { onFilterChange(filterState.copy(keywordIds = it)) }
            )
        }
    }
}

private fun Set<Int>.toggle(id: Int, isChecked: Boolean) = if (isChecked) this + id else this - id

/**
 * A filter section over a fixed option set: header plus a multi-select button group.
 *
 * [idOf] maps an option to the id stored in [GameListFilterState] — IGDB enums are stored by ordinal,
 * entities by their own id.
 */
@Composable
private fun <T> MultiSelectFilterSection(
    icon: ImageVector,
    title: StringResource,
    options: List<T>,
    idOf: (T) -> Int,
    selected: Set<Int>,
    onSelectedChange: (Set<Int>) -> Unit,
    label: @Composable (T) -> String,
    optionIcon: (T) -> ImageVector? = { null },
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterSectionHeader(icon = icon, title = title)
        ConnectedButtonGroup(
            options = options,
            checked = { option -> selected.contains(idOf(option)) },
            onCheckedChange = { option, isChecked -> onSelectedChange(selected.toggle(idOf(option), isChecked)) },
            toggleButtonText = label,
            toggleButtonIcon = optionIcon,
            showChecks = true,
            multiple = true
        )
    }
}

/** A filter section over a numeric range: header with the current span plus a range slider. */
@Composable
private fun RangeFilterSection(
    icon: ImageVector,
    title: StringResource,
    trailingText: String,
    value: ClosedFloatingPointRange<Float>,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FilterSectionHeader(icon = icon, title = title, trailingText = trailingText)
        RangeSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * A filter section whose options come from IGDB: a search field plus a button group listing the
 * matches (or a curated fallback when the field is empty).
 *
 * Already-selected options stay in the group even after the query changes, otherwise a selection
 * made from a search result would silently disappear from the UI while still filtering the list.
 */
@Composable
private fun SearchableFilterSection(
    icon: ImageVector,
    title: StringResource,
    placeholder: String,
    target: FilterSearchTarget,
    defaultOptions: List<NamedSearchResult>,
    searchState: FilterSearchState,
    selected: Set<Int>,
    onFilterSearch: (FilterSearchTarget, String) -> Unit,
    onSelectedChange: (Set<Int>) -> Unit,
    showLogos: Boolean = false,
    extraContent: @Composable (selected: List<NamedSearchResult>) -> Unit = {},
) {
    val options = searchState.results.ifEmpty { defaultOptions }
    val seen = remember { mutableStateMapOf<Int, NamedSearchResult>() }
    LaunchedEffect(options) { options.forEach { seen[it.id] = it } }
    val selectedOptions = selected.mapNotNull { seen[it] }
    val display = (selectedOptions + options).distinctBy { it.id }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterSectionHeader(icon = icon, title = title)
        FilterSearchBar(
            query = searchState.query,
            onQueryChange = { onFilterSearch(target, it) },
            placeholder = placeholder,
            loading = searchState.loading
        )
        // Keyed on the option ids, so a new set of results animates in but merely checking one
        // of the buttons already on screen does not re-run the transition.
        AnimatedContent(
            targetState = display,
            contentKey = { options -> options.map(NamedSearchResult::id) },
            transitionSpec = {
                (fadeIn(tween(OptionsFadeInMillis)) + slideInVertically { it / SlideFraction })
                    .togetherWith(fadeOut(tween(OptionsFadeOutMillis)))
            }
        ) { options ->
            if (options.isEmpty()) {
                Text(
                    text = stringResource(Res.string.gamelist__filter_type_to_search),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                ConnectedButtonGroup(
                    options = options,
                    checked = { option -> selected.contains(option.id) },
                    onCheckedChange = { option, isChecked -> onSelectedChange(selected.toggle(option.id, isChecked)) },
                    toggleButtonText = { option -> option.name },
                    toggleButtonLeading = if (showLogos) {
                        { option -> OptionLogo(option, icon) }
                    } else {
                        null
                    },
                    showChecks = true,
                    multiple = true
                )
            }
        }
        extraContent(selectedOptions)
    }
}

@Composable
private fun OptionLogo(option: NamedSearchResult, fallbackIcon: ImageVector) {
    val logoUrl = option.logoImageId?.let { igdbImageUrl(it, IgdbImageSize.LOGO_MEDIUM) }
    if (logoUrl != null) {
        RemoteImage(
            url = logoUrl,
            contentDescription = option.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(LogoSize)
        )
    } else {
        Icon(fallbackIcon, contentDescription = null, modifier = Modifier.size(LogoSize))
    }
}

/**
 * Plain search field for a filter section.
 *
 * Not a [androidx.compose.material3.DockedSearchBar]: that one hardcodes `width(SearchBarMinWidth)`
 * after the caller's modifier, so it cannot fill the sheet and the trailing icon ends up short of
 * the edge. Nothing here ever expands, so a text field is the right component.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FilterSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    loading: Boolean = false,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.SearchW500Rounded, contentDescription = null) },
        trailingIcon = {
            if (loading) {
                LoadingIndicator(modifier = Modifier.size(LogoSize))
            } else if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.CloseW500Rounded, contentDescription = null)
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun FilterSectionHeader(
    icon: ImageVector,
    title: StringResource,
    trailingText: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (trailingText != null) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
