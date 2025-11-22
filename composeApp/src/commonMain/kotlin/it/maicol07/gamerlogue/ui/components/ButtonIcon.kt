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

@Composable
fun ButtonIcon(
    bitmap: ImageBitmap,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    spacerModifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    end: Boolean = false
) {
    if (end) Spacer(modifier = spacerModifier.size(ButtonDefaults.IconSpacing))
    Icon(bitmap = bitmap, contentDescription = contentDescription, modifier.size(ButtonDefaults.IconSize), tint)
    if (!end) Spacer(modifier = spacerModifier.size(ButtonDefaults.IconSpacing))
}

@Composable
fun ButtonIcon(
    painter: Painter,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    spacerModifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    end: Boolean = false
) {
    if (end) Spacer(modifier = spacerModifier.size(ButtonDefaults.IconSpacing))
    Icon(painter = painter, contentDescription = contentDescription, modifier.size(ButtonDefaults.IconSize), tint)
    if (!end) Spacer(modifier = spacerModifier.size(ButtonDefaults.IconSpacing))
}

@Composable
fun ButtonIcon(
    imageVector: ImageVector,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    spacerModifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    end: Boolean = false
) {
    if (end) Spacer(modifier = spacerModifier.size(ButtonDefaults.IconSpacing))
    Icon(imageVector = imageVector, contentDescription = contentDescription, modifier.size(ButtonDefaults.IconSize), tint)
    if (!end) Spacer(modifier = spacerModifier.size(ButtonDefaults.IconSpacing))
}

