package com.example.alpha_ai.shell.gallery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.alpha_ai.R
import com.example.alpha_ai.core.designsystem.preview.AlphaPreviews
import com.example.alpha_ai.core.designsystem.theme.AlphaTheme

/** The gallery index: one row per section. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(onOpenSection: (GallerySection) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxSize()) {
        Column {
            TopAppBar(title = { Text(stringResource(R.string.gallery_title), maxLines = 1) })
            LazyColumn {
                items(GallerySection.entries, key = { it.name }) { section ->
                    Text(
                        text = stringResource(section.labelRes),
                        style = AlphaTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = AlphaTheme.spacing.minTouchTarget)
                            .clickable { onOpenSection(section) }
                            .padding(
                                horizontal = AlphaTheme.spacing.lg,
                                vertical = AlphaTheme.spacing.md,
                            ),
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

/** One section: a top bar with back, then the section's content, always scrollable (FR-010). */
@Composable
fun GallerySectionScreen(section: GallerySection, onBack: () -> Unit, modifier: Modifier = Modifier) {
    SectionFrame(title = stringResource(section.labelRes), onBack = onBack, modifier = modifier) {
        SectionContent(section)
    }
}

@Composable
private fun ColumnScope.SectionContent(section: GallerySection) {
    when (section) {
        else -> Text(
            text = stringResource(R.string.gallery_placeholder),
            style = AlphaTheme.typography.bodyMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SectionFrame(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(modifier.fillMaxSize()) {
        Column {
            TopAppBar(title = { Text(title, maxLines = 1, style = MaterialTheme.typography.titleLarge) })
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(AlphaTheme.spacing.lg),
                content = content,
            )
        }
    }
}

@AlphaPreviews
@Composable
private fun GalleryScreenPreview() {
    AlphaTheme { GalleryScreen(onOpenSection = {}) }
}

@AlphaPreviews
@Composable
private fun GallerySectionPlaceholderPreview() {
    AlphaTheme { GallerySectionScreen(section = GallerySection.Cards, onBack = {}) }
}
