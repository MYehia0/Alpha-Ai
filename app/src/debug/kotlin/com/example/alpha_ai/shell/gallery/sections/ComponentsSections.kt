package com.example.alpha_ai.shell.gallery.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.example.alpha_ai.R
import com.example.alpha_ai.core.designsystem.component.AlphaCard
import com.example.alpha_ai.core.designsystem.component.AlphaEmptyState
import com.example.alpha_ai.core.designsystem.component.AlphaErrorState
import com.example.alpha_ai.core.designsystem.component.AlphaIconButton
import com.example.alpha_ai.core.designsystem.component.AlphaLoadingState
import com.example.alpha_ai.core.designsystem.component.AlphaMultilineTextField
import com.example.alpha_ai.core.designsystem.component.AlphaPrimaryButton
import com.example.alpha_ai.core.designsystem.component.AlphaSecondaryButton
import com.example.alpha_ai.core.designsystem.component.AlphaTextButton
import com.example.alpha_ai.core.designsystem.component.AlphaTextField
import com.example.alpha_ai.core.designsystem.component.AlphaTopBar
import com.example.alpha_ai.core.designsystem.preview.AlphaPreviews
import com.example.alpha_ai.core.designsystem.theme.AlphaTheme
import com.example.alpha_ai.core.domain.ai.AiFailure
import com.example.alpha_ai.core.ui.ai.messageRes
import com.example.alpha_ai.core.ui.state.AlphaScreenStateHost
import com.example.alpha_ai.core.ui.state.ScreenState
import com.example.alpha_ai.core.ui.state.toErrorState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Primary, secondary, text, and icon buttons in every state, plus a live busy demo. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ButtonsSection() {
    val label = stringResource(R.string.gallery_label_continue)
    var busy by rememberSaveable { mutableStateOf(false) }
    var accepted by rememberSaveable { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    Column(verticalArrangement = Arrangement.spacedBy(AlphaTheme.spacing.lg)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(AlphaTheme.spacing.sm)) {
            AlphaPrimaryButton(label, {})
            AlphaSecondaryButton(label, {})
            AlphaTextButton(label, {})
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(AlphaTheme.spacing.sm)) {
            AlphaPrimaryButton(label, {}, enabled = false)
            AlphaSecondaryButton(label, {}, enabled = false)
            AlphaTextButton(label, {}, enabled = false)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(AlphaTheme.spacing.sm)) {
            AlphaPrimaryButton(label, {}, busy = true)
            AlphaSecondaryButton(label, {}, busy = true)
            AlphaTextButton(label, {}, busy = true)
        }
        AlphaPrimaryButton(
            text = stringResource(R.string.gallery_label_long),
            onClick = {},
            leadingIcon = Icons.AutoMirrored.Filled.ArrowForward,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(AlphaTheme.spacing.sm)) {
            AlphaIconButton(Icons.Filled.Refresh, stringResource(R.string.gallery_motion_play), {})
            AlphaIconButton(Icons.Filled.Search, stringResource(R.string.gallery_field_label), {}, enabled = false)
        }
        // Tapping again while busy must not count (FR-012).
        AlphaPrimaryButton(
            text = stringResource(R.string.gallery_busy_demo),
            busy = busy,
            onClick = {
                accepted++
                busy = true
                scope.launch {
                    delay(2_000)
                    busy = false
                }
            },
        )
        Text(stringResource(R.string.gallery_accepted_taps, accepted), style = AlphaTheme.typography.bodyMedium)
    }
}

/** Text fields in every state. Typed text survives rotation and restore. */
@Composable
fun TextFieldsSection() {
    var short by rememberSaveable { mutableStateOf("") }
    var long by rememberSaveable { mutableStateOf("") }
    val label = stringResource(R.string.gallery_field_label)
    val placeholder = stringResource(R.string.gallery_field_placeholder)
    val sample = stringResource(R.string.gallery_mixed)
    Column(verticalArrangement = Arrangement.spacedBy(AlphaTheme.spacing.md)) {
        AlphaTextField(
            value = short,
            onValueChange = { short = it },
            label = label,
            placeholder = placeholder,
            errorMessage = stringResource(R.string.gallery_field_error).takeIf { short.isNotEmpty() && short.length < 3 },
        )
        AlphaTextField(value = sample, onValueChange = {}, label = label)
        AlphaTextField(value = sample, onValueChange = {}, label = label, enabled = false)
        AlphaTextField(value = "ab", onValueChange = {}, label = label, errorMessage = stringResource(R.string.gallery_field_error))
        AlphaMultilineTextField(
            value = long,
            onValueChange = { long = it },
            label = stringResource(R.string.gallery_field_long),
            placeholder = placeholder,
            maxLength = 500,
        )
    }
}

/** A plain card and a clickable one. */
@Composable
fun CardsSection() {
    Column(verticalArrangement = Arrangement.spacedBy(AlphaTheme.spacing.md)) {
        AlphaCard {
            Text(stringResource(R.string.gallery_card_title), style = AlphaTheme.typography.titleMedium)
            Text(stringResource(R.string.gallery_mixed), style = AlphaTheme.typography.bodyMedium)
            AlphaTextButton(stringResource(R.string.gallery_label_continue), {})
        }
        AlphaCard(onClick = {}) {
            Text(stringResource(R.string.gallery_card_title), style = AlphaTheme.typography.titleMedium)
            Text(stringResource(R.string.gallery_long_url), style = AlphaTheme.typography.bodyMedium)
        }
    }
}

/** The top bar with a short title, a long title, and actions. */
@Composable
fun TopBarSection() {
    Column(verticalArrangement = Arrangement.spacedBy(AlphaTheme.spacing.md)) {
        AlphaTopBar(title = stringResource(R.string.gallery_card_title), onBack = {})
        AlphaTopBar(
            title = stringResource(R.string.gallery_long_title),
            onBack = {},
            actions = { AlphaIconButton(Icons.Filled.Search, stringResource(R.string.gallery_field_label), {}) },
        )
        AlphaTopBar(title = stringResource(R.string.gallery_long_title))
    }
}

/** Loading, empty, and error states, and a live host that shows the loading timing rules. */
@Composable
fun StatesSection() {
    var state by rememberSaveable(stateSaver = ScreenStateSaver) { mutableStateOf<ScreenState<Int>>(ScreenState.Content(R.string.gallery_quick_done)) }
    val scope = rememberCoroutineScope()
    fun run(millis: Long, result: ScreenState<Int>) {
        state = ScreenState.Loading(cancellable = true)
        scope.launch {
            delay(millis)
            state = result
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(AlphaTheme.spacing.md)) {
        AlphaSecondaryButton(stringResource(R.string.gallery_run_quick), { run(200, ScreenState.Content(R.string.gallery_quick_done)) })
        AlphaSecondaryButton(stringResource(R.string.gallery_run_slow), { run(2_000, AiFailure.TimedOut.toErrorState()) })
        AlphaCard {
            AlphaScreenStateHost(
                state = state,
                onRetry = { run(2_000, ScreenState.Content(R.string.gallery_quick_done)) },
                onCancel = { state = ScreenState.Empty(R.string.gallery_empty_message) },
            ) { Text(stringResource(it), style = AlphaTheme.typography.bodyLarge) }
        }

        AlphaLoadingState(onCancel = {}, showImmediately = true)
        AlphaEmptyState(
            message = stringResource(R.string.gallery_empty_message),
            actionLabel = stringResource(R.string.gallery_empty_action),
            onAction = {},
        )
        AlphaErrorState(message = stringResource(AiFailure.NoConnection.messageRes()), onRetry = {})
        AlphaErrorState(message = stringResource(AiFailure.ServiceUnavailable.messageRes()))
    }
}

/** Keeps the demo state across rotation; it holds only resource ids and flags. */
private val ScreenStateSaver = androidx.compose.runtime.saveable.Saver<ScreenState<Int>, List<Any>>(
    save = {
        when (it) {
            is ScreenState.Loading -> listOf("loading", it.cancellable)
            is ScreenState.Empty -> listOf("empty", it.messageRes)
            is ScreenState.Error -> listOf("error", it.messageRes, it.retryable)
            is ScreenState.Content -> listOf("content", it.value)
        }
    },
    restore = {
        when (it[0]) {
            "loading" -> ScreenState.Loading(it[1] as Boolean)
            "empty" -> ScreenState.Empty(it[1] as Int)
            "error" -> ScreenState.Error(it[1] as Int, it[2] as Boolean)
            else -> ScreenState.Content(it[1] as Int)
        }
    },
)

@AlphaPreviews
@Composable
private fun ButtonsSectionPreview() = SectionPreview { ButtonsSection() }

@AlphaPreviews
@Composable
private fun TextFieldsSectionPreview() = SectionPreview { TextFieldsSection() }

@AlphaPreviews
@Composable
private fun CardsSectionPreview() = SectionPreview { CardsSection() }

@AlphaPreviews
@Composable
private fun TopBarSectionPreview() = SectionPreview { TopBarSection() }

@AlphaPreviews
@Composable
private fun StatesSectionPreview() = SectionPreview { StatesSection() }
