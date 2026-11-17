package com.example.alpha_ai.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.example.alpha_ai.core.designsystem.R
import com.example.alpha_ai.core.designsystem.preview.AlphaPreviews
import com.example.alpha_ai.core.designsystem.theme.AlphaTheme

/**
 * A single-line text field. When [errorMessage] is set, it shows below the field, marks the field
 * as in error for screen readers, and is spoken as soon as it appears (FR-013).
 */
@Composable
fun AlphaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    errorMessage: String? = null,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        isError = errorMessage != null,
        supportingText = errorMessage?.let { { ErrorText(it) } },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = AlphaTheme.shapes.small,
        modifier = modifier
            .fillMaxWidth()
            .semantics { if (errorMessage != null) error(errorMessage) },
    )
}

/**
 * A text field for long input. It grows from [minLines] to [maxLines], then scrolls. With
 * [maxLength] it shows "count / max" and keeps the text within the limit.
 */
@Composable
fun AlphaMultilineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    errorMessage: String? = null,
    enabled: Boolean = true,
    minLines: Int = 3,
    maxLines: Int = 8,
    maxLength: Int? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val supporting: (@Composable () -> Unit)? =
        if (errorMessage == null && maxLength == null) {
            null
        } else {
            {
                Row(horizontalArrangement = Arrangement.spacedBy(AlphaTheme.spacing.sm)) {
                    if (errorMessage != null) ErrorText(errorMessage, Modifier.weight(1f))
                    else androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                    if (maxLength != null) {
                        Text(stringResource(R.string.ds_char_count, value.length, maxLength))
                    }
                }
            }
        }
    OutlinedTextField(
        value = value,
        onValueChange = { new -> onValueChange(if (maxLength == null) new else new.take(maxLength)) },
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        isError = errorMessage != null,
        supportingText = supporting,
        enabled = enabled,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        shape = AlphaTheme.shapes.small,
        modifier = modifier
            .fillMaxWidth()
            .semantics { if (errorMessage != null) error(errorMessage) },
    )
}

@Composable
private fun ErrorText(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@AlphaPreviews
@Composable
private fun TextFieldsPreview() {
    AlphaTheme {
        Surface {
            val label = stringResource(R.string.ds_loading)
            val text = stringResource(R.string.ds_in_progress, label)
            Column(
                verticalArrangement = Arrangement.spacedBy(AlphaTheme.spacing.sm),
                modifier = Modifier.padding(AlphaTheme.spacing.lg),
            ) {
                AlphaTextField(value = "", onValueChange = {}, label = label)
                AlphaTextField(value = text, onValueChange = {}, label = label)
                AlphaTextField(value = text, onValueChange = {}, label = label, enabled = false)
                AlphaTextField(value = text, onValueChange = {}, label = label, errorMessage = text)
                AlphaMultilineTextField(value = text, onValueChange = {}, label = label, maxLength = 40)
            }
        }
    }
}
