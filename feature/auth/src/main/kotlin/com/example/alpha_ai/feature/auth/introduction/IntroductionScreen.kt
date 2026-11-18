package com.example.alpha_ai.feature.auth.introduction

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.alpha_ai.core.designsystem.component.AlphaPrimaryButton
import com.example.alpha_ai.core.designsystem.component.AlphaTextButton
import com.example.alpha_ai.core.designsystem.preview.AlphaPreviews
import com.example.alpha_ai.core.designsystem.theme.AlphaTheme
import com.example.alpha_ai.feature.auth.R
import com.example.alpha_ai.feature.auth.common.CollectEffects

private data class IntroPage(val icon: ImageVector, @StringRes val title: Int, @StringRes val body: Int)

private val pages = listOf(
    IntroPage(Icons.Filled.Edit, R.string.auth_intro_page1_title, R.string.auth_intro_page1_body),
    IntroPage(Icons.Filled.Build, R.string.auth_intro_page2_title, R.string.auth_intro_page2_body),
    IntroPage(Icons.Filled.DateRange, R.string.auth_intro_page3_title, R.string.auth_intro_page3_body),
)

@Composable
internal fun IntroductionEntry(
    onToSignIn: () -> Unit,
    viewModel: IntroductionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            IntroductionEffect.ToSignIn -> onToSignIn()
        }
    }
    IntroductionScreen(state = state, onIntent = viewModel::onIntent)
}

@Composable
internal fun IntroductionScreen(
    state: IntroductionUiState,
    onIntent: (IntroductionIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = AlphaTheme.motion
    val pagerState = rememberPagerState(initialPage = state.page) { state.pageCount }

    // The ViewModel owns the page; the pager follows it and reports swipes back.
    LaunchedEffect(state.page) {
        if (pagerState.currentPage != state.page) {
            pagerState.animateScrollToPage(state.page, animationSpec = motion.change())
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { onIntent(IntroductionIntent.GoToPage(it)) }
    }
    BackHandler(enabled = state.page > 0) { onIntent(IntroductionIntent.PreviousPage) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(AlphaTheme.spacing.lg),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            AlphaTextButton(
                text = stringResource(R.string.auth_intro_skip),
                onClick = { onIntent(IntroductionIntent.Skip) },
            )
        }
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { index ->
            IntroPageContent(pages[index])
        }
        PageIndicator(page = state.page, pageCount = state.pageCount)
        Spacer(Modifier.height(AlphaTheme.spacing.lg))
        if (state.isLastPage) {
            AlphaPrimaryButton(
                text = stringResource(R.string.auth_intro_get_started),
                onClick = { onIntent(IntroductionIntent.Finish) },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            AlphaPrimaryButton(
                text = stringResource(R.string.auth_intro_next),
                onClick = { onIntent(IntroductionIntent.NextPage) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun IntroPageContent(page: IntroPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AlphaTheme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            // Decorative: the page text says what it shows.
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(80.dp),
            )
        }
        Spacer(Modifier.height(AlphaTheme.spacing.xl))
        Text(
            text = stringResource(page.title),
            style = AlphaTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(AlphaTheme.spacing.md))
        Text(
            text = stringResource(page.body),
            style = AlphaTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PageIndicator(page: Int, pageCount: Int) {
    val description = stringResource(R.string.auth_intro_page_indicator, page + 1, pageCount)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(AlphaTheme.spacing.sm, Alignment.CenterHorizontally),
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == page) 10.dp else 8.dp)
                    .background(
                        color = if (index == page) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        shape = CircleShape,
                    ),
            )
        }
    }
}

@AlphaPreviews
@Composable
private fun IntroductionFirstPagePreview() {
    AlphaTheme { IntroductionScreen(state = IntroductionUiState(page = 0), onIntent = {}) }
}

@AlphaPreviews
@Composable
private fun IntroductionLastPagePreview() {
    AlphaTheme { IntroductionScreen(state = IntroductionUiState(page = 2), onIntent = {}) }
}
