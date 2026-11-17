package com.example.alpha_ai.core.designsystem.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.example.alpha_ai.core.designsystem.R
import com.example.alpha_ai.core.designsystem.preview.AlphaPreviews
import com.example.alpha_ai.core.designsystem.theme.AlphaTheme

/**
 * The top bar every screen uses. The title stays on one line and ends with "…" when it does not
 * fit; the back arrow flips in right-to-left layouts. It draws under the status bar itself.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlphaTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = AlphaTheme.typography.titleLarge,
            )
        },
        navigationIcon = {
            if (onBack != null) {
                AlphaIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.ds_back),
                    onClick = onBack,
                )
            }
        },
        actions = actions,
        modifier = modifier,
    )
}

@AlphaPreviews
@Composable
private fun TopBarPreview() {
    AlphaTheme {
        val longTitle = stringResource(R.string.ds_in_progress, stringResource(R.string.ds_in_progress, stringResource(R.string.ds_loading)))
        androidx.compose.foundation.layout.Column {
            AlphaTopBar(title = stringResource(R.string.ds_loading), onBack = {})
            AlphaTopBar(title = longTitle, onBack = {}, actions = {
                AlphaIconButton(Icons.Filled.Refresh, stringResource(R.string.ds_retry), {})
            })
        }
    }
}
