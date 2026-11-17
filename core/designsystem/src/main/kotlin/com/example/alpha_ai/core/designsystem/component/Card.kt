package com.example.alpha_ai.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.alpha_ai.core.designsystem.R
import com.example.alpha_ai.core.designsystem.preview.AlphaPreviews
import com.example.alpha_ai.core.designsystem.theme.AlphaTheme

/**
 * A group of related content on the `surface` colour with an outline, so primary-coloured
 * text inside it keeps WCAG AA contrast. Clickable only when [onClick] is set.
 */
@Composable
fun AlphaCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.outlinedCardColors(containerColor = AlphaTheme.colors.scheme.surface)
    val border = BorderStroke(AlphaTheme.spacing.borderThin, AlphaTheme.colors.scheme.outline)
    val inner: @Composable ColumnScope.() -> Unit = {
        Column(Modifier.padding(AlphaTheme.spacing.lg), content = content)
    }
    if (onClick == null) {
        OutlinedCard(modifier.fillMaxWidth(), AlphaTheme.shapes.medium, colors, border = border, content = inner)
    } else {
        OutlinedCard(onClick, modifier.fillMaxWidth(), shape = AlphaTheme.shapes.medium, colors = colors, border = border, content = inner)
    }
}

@AlphaPreviews
@Composable
private fun CardPreview() {
    AlphaTheme {
        Surface {
            Column(Modifier.padding(AlphaTheme.spacing.lg)) {
                AlphaCard {
                    Text(stringResource(R.string.ds_loading), style = AlphaTheme.typography.titleMedium)
                    Text(stringResource(R.string.ds_in_progress, stringResource(R.string.ds_retry)))
                    AlphaTextButton(stringResource(R.string.ds_retry), {})
                }
            }
        }
    }
}
