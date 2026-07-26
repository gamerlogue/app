package it.maicol07.gamerlogue.ui.views.game.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import at.released.igdbclient.model.Game
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.game__details_languages
import gamerlogue.sharedui.generated.resources.game__languages_audio
import gamerlogue.sharedui.generated.resources.game__languages_interface
import gamerlogue.sharedui.generated.resources.game__languages_subtitles
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CheckW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CloseW500Rounded
import it.maicol07.gamerlogue.extensions.expressiveShape
import it.maicol07.gamerlogue.extensions.getDisplayLanguage
import it.maicol07.gamerlogue.extensions.getFlag
import org.jetbrains.compose.resources.stringResource

private data class LanguageSupportRow(
    val name: String,
    val flag: androidx.compose.ui.graphics.vector.ImageVector?,
    val hasAudio: Boolean,
    val hasSubtitles: Boolean,
    val hasInterface: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagesBottomSheet(
    game: Game,
    onDismissRequest: () -> Unit = { }
) = ModalBottomSheet(
    onDismissRequest = onDismissRequest,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val currentLocale = remember { Locale.current }

    val rows = remember(game.language_supports, currentLocale) {
        game.language_supports
            .groupBy { it.language?.name ?: "Unknown" }
            .map { (rawName, supports) ->
                val (locale, localizedName) = parseLanguageInfo(rawName, currentLocale)
                val flag = locale?.getFlag()

                var hasAudio = false
                var hasSubtitles = false
                var hasInterface = false

                for (s in supports) {
                    val typeId = s.language_support_type?.id?.toInt()
                    val typeName = s.language_support_type?.name?.lowercase() ?: ""
                    if (typeId == 1 || "audio" in typeName || "voice" in typeName) {
                        hasAudio = true
                    }
                    if (typeId == 2 || "subtitle" in typeName) {
                        hasSubtitles = true
                    }
                    if (typeId == 3 || "interface" in typeName) {
                        hasInterface = true
                    }
                }

                LanguageSupportRow(
                    name = localizedName,
                    flag = flag,
                    hasAudio = hasAudio,
                    hasSubtitles = hasSubtitles,
                    hasInterface = hasInterface
                )
            }.sortedBy { it.name }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(
                stringResource(Res.string.game__details_languages),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // Table Header with fully rounded corners
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.game__details_languages).uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(2.5f)
                    )
                    Text(
                        text = stringResource(Res.string.game__languages_audio).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(Res.string.game__languages_subtitles).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(Res.string.game__languages_interface).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Table Rows
        itemsIndexed(rows) { index, row ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ListItemDefaults.expressiveShape(index == 0, index == rows.lastIndex))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(2.5f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (row.flag != null) {
                            Image(
                                imageVector = row.flag,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(24.dp, 16.dp)
                                    .clip(MaterialTheme.shapes.extraSmall)
                            )
                        }
                        Text(
                            text = row.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Audio
                    StatusIcon(
                        enabled = row.hasAudio,
                        modifier = Modifier.weight(1f)
                    )

                    // Subtitles
                    StatusIcon(
                        enabled = row.hasSubtitles,
                        modifier = Modifier.weight(1f)
                    )

                    // Interface
                    StatusIcon(
                        enabled = row.hasInterface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusIcon(enabled: Boolean, modifier: Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (enabled) {
            Icon(
                imageVector = Icons.CheckW500Rounded,
                contentDescription = null,
                tint = Color(0xFF4CAF50), // Green check
                modifier = Modifier.size(20.dp)
            )
        } else {
            Icon(
                imageVector = Icons.CloseW500Rounded,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f), // Red cross
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun parseLanguageInfo(rawName: String, currentLocale: Locale): Pair<Locale?, String> {
    val lower = rawName.lowercase()
    val isItalian = currentLocale.language.equals("it", ignoreCase = true)

    val (locale, name) = when {
        "traditional" in lower || "tradizionale" in lower -> {
            Locale("zh-TW") to (if (isItalian) "Cinese (Tradizionale)" else "Chinese (Traditional)")
        }
        "simplified" in lower || "semplificato" in lower -> {
            Locale("zh-CN") to (if (isItalian) "Cinese (Semplificato)" else "Chinese (Simplified)")
        }
        "chinese" in lower || "mandarin" in lower -> {
            Locale("zh-CN") to (if (isItalian) "Cinese" else "Chinese")
        }
        "english" in lower -> Locale("en-US") to null
        "italian" in lower -> Locale("it-IT") to null
        "french" in lower -> Locale("fr-FR") to null
        "german" in lower -> Locale("de-DE") to null
        "spanish" in lower -> Locale("es-ES") to null
        "japanese" in lower -> Locale("ja-JP") to null
        "russian" in lower -> Locale("ru-RU") to null
        "portuguese" in lower -> Locale("pt-BR") to null
        "korean" in lower -> Locale("ko-KR") to null
        "polish" in lower -> Locale("pl-PL") to null
        "dutch" in lower -> Locale("nl-NL") to null
        "swedish" in lower -> Locale("sv-SE") to null
        "turkish" in lower -> Locale("tr-TR") to null
        "arabic" in lower -> Locale("ar-SA") to null
        "czech" in lower -> Locale("cs-CZ") to null
        "danish" in lower -> Locale("da-DK") to null
        "finnish" in lower -> Locale("fi-FI") to null
        "greek" in lower -> Locale("el-GR") to null
        "hungarian" in lower -> Locale("hu-HU") to null
        "norwegian" in lower -> Locale("no-NO") to null
        "romanian" in lower -> Locale("ro-RO") to null
        "ukrainian" in lower -> Locale("uk-UA") to null
        "vietnamese" in lower -> Locale("vi-VN") to null
        "thai" in lower -> Locale("th-TH") to null
        "hindi" in lower -> Locale("hi-IN") to null
        "indonesian" in lower -> Locale("id-ID") to null
        else -> null to rawName
    }

    val finalName = name ?: locale?.getDisplayLanguage(currentLocale)?.replaceFirstChar { it.uppercase() } ?: rawName
    return locale to finalName
}
