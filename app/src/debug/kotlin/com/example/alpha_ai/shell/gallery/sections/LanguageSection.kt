package com.example.alpha_ai.shell.gallery.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import com.example.alpha_ai.R
import com.example.alpha_ai.core.designsystem.component.AlphaCard
import com.example.alpha_ai.core.designsystem.preview.AlphaPreviews
import com.example.alpha_ai.core.designsystem.theme.AlphaTheme
import java.text.NumberFormat

/** Shows what right-to-left changes and what it must not change (FR-008, edge cases). */
@Composable
fun LanguageSection() {
    val locale = LocalConfiguration.current.locales[0]
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Column(verticalArrangement = Arrangement.spacedBy(AlphaTheme.spacing.lg)) {
        Text(
            text = stringResource(
                R.string.gallery_locale,
                locale.getDisplayName(locale),
                stringResource(if (rtl) R.string.gallery_direction_rtl else R.string.gallery_direction_ltr),
            ),
            style = AlphaTheme.typography.bodyLarge,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.gallery_row_start), style = AlphaTheme.typography.titleMedium)
            Text(stringResource(R.string.gallery_row_end), style = AlphaTheme.typography.titleMedium)
        }
        IconRow(R.string.gallery_icons_mirror, Icons.AutoMirrored.Filled.ArrowBack, Icons.AutoMirrored.Filled.KeyboardArrowRight)
        IconRow(R.string.gallery_icons_fixed, Icons.Filled.Settings, Icons.Filled.Search)
        Text(stringResource(R.string.gallery_mixed), style = AlphaTheme.typography.bodyLarge)
        Text(
            text = stringResource(R.string.gallery_number, NumberFormat.getInstance(locale).format(1_234_567.89)),
            style = AlphaTheme.typography.bodyLarge,
        )
        AlphaCard {
            Text(stringResource(R.string.gallery_long_word), style = AlphaTheme.typography.bodyLarge)
            Text(stringResource(R.string.gallery_long_url), style = AlphaTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun IconRow(label: Int, first: ImageVector, second: ImageVector) {
    Row(horizontalArrangement = Arrangement.spacedBy(AlphaTheme.spacing.md)) {
        Text(stringResource(label), style = AlphaTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        // Decorative samples: the label beside them already says what they show.
        Icon(first, contentDescription = null)
        Icon(second, contentDescription = null)
    }
}

@AlphaPreviews
@Composable
private fun LanguageSectionPreview() = SectionPreview { LanguageSection() }
