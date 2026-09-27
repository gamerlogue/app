package it.maicol07.gamerlogue.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon

private val SectionIconSize = 36.dp
private val SectionIconGlyphSize = 20.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SectionIcon(icon: ImageVector, shape: RoundedPolygon, containerColor: Color, contentColor: Color) = Box(
    modifier = Modifier.size(SectionIconSize).background(containerColor, shape.toShape()),
    contentAlignment = Alignment.Center
) {
    Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(SectionIconGlyphSize))
}
