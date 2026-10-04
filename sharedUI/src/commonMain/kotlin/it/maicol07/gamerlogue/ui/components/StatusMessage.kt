package it.maicol07.gamerlogue.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon

private val StatusIconContainerSize = 112.dp
private val StatusIconSize = 48.dp

/** Full-screen message: [icon] in an expressive [shape], [message], then an optional [action]. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StatusMessage(
    icon: ImageVector,
    message: String,
    containerColor: Color = MaterialTheme.colorScheme.tertiaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onTertiaryContainer,
    shape: RoundedPolygon = MaterialShapes.Cookie9Sided,
    action: @Composable () -> Unit = {}
) = Column(
    Modifier.fillMaxSize().padding(horizontal = 32.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    Box(
        Modifier
            .size(StatusIconContainerSize)
            .background(containerColor, shape.toShape()),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(StatusIconSize)
        )
    }
    Text(message, style = MaterialTheme.typography.titleMediumEmphasized, textAlign = TextAlign.Center)
    action()
}
