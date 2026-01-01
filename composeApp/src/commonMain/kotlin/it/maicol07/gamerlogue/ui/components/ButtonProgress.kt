package it.maicol07.gamerlogue.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ButtonProgress(
    loading: Boolean,
    content: @Composable RowScope.() -> Unit,
) {
    var width by remember { mutableStateOf(0.dp) }
    AnimatedVisibility(loading) {
        Column(Modifier.width(width), horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(ButtonDefaults.IconSize),
                color = LocalContentColor.current,
                strokeWidth = 1.5f.dp
            )
        }
    }

    val density = LocalDensity.current
    AnimatedVisibility(!loading) {
        Row(
            content = content,
            modifier = Modifier.onSizeChanged { newSize ->
                if (width == 0.dp) {
                    width = with(density) {
                        newSize
                            .toSize()
                            .toDpSize()
                            .width
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
private fun LoadingButtonPreview() {
    var loading by remember { mutableStateOf(false) }
    Button(
        shapes = ButtonDefaults.shapes(),
        onClick = { loading = !loading }
    ) {
        ButtonProgress(loading) {
            Text("Loading")
        }
    }
}
