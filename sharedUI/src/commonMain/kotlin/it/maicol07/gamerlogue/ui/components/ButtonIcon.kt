@file:Suppress("unused")

package it.maicol07.gamerlogue.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp

@Composable
fun ButtonIcon(
    bitmap: ImageBitmap,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    spacerModifier: Modifier = Modifier,
    size: Dp = ButtonDefaults.IconSize,
    spacing: Dp = ButtonDefaults.IconSpacing,
    tint: Color = LocalContentColor.current,
    end: Boolean = false
) = ButtonIcon(spacerModifier, spacing, end) {
    Icon(bitmap = bitmap, contentDescription = contentDescription, modifier = modifier.size(size), tint = tint)
}

@Composable
fun ButtonIcon(
    painter: Painter,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    spacerModifier: Modifier = Modifier,
    size: Dp = ButtonDefaults.IconSize,
    spacing: Dp = ButtonDefaults.IconSpacing,
    tint: Color = LocalContentColor.current,
    end: Boolean = false
) = ButtonIcon(spacerModifier, spacing, end) {
    Icon(painter, contentDescription, modifier.size(size), tint)
}

@Composable
fun ButtonIcon(
    imageVector: ImageVector,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    spacerModifier: Modifier = Modifier,
    size: Dp = ButtonDefaults.IconSize,
    spacing: Dp = ButtonDefaults.IconSpacing,
    tint: Color = LocalContentColor.current,
    end: Boolean = false
) = ButtonIcon(spacerModifier, spacing, end) {
    Icon(imageVector, contentDescription, modifier.size(size), tint)
}

@Composable
fun ButtonIcon(
    spacerModifier: Modifier = Modifier,
    spacing: Dp = ButtonDefaults.IconSpacing,
    end: Boolean = false,
    icon: @Composable () -> Unit
) {
    if (end) Spacer(spacerModifier.size(spacing))
    icon()
    if (!end) Spacer(spacerModifier.size(spacing))
}

