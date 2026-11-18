package com.example.alpha_ai.feature.auth.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.alpha_ai.core.designsystem.component.AlphaIconButton
import com.example.alpha_ai.core.designsystem.component.AlphaTextField
import com.example.alpha_ai.core.designsystem.icon.AlphaIcons
import com.example.alpha_ai.core.designsystem.preview.AlphaPreviews
import com.example.alpha_ai.core.designsystem.theme.AlphaTheme
import com.example.alpha_ai.feature.auth.R

/**
 * A password field, hidden by default, with a show/hide button that screen readers announce
 * (contracts/auth-screens.md). When [visible] and [onVisibleChange] are given, the caller owns
 * the state (so one toggle can drive two fields); otherwise the field keeps its own.
 */
@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Done,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visible: Boolean? = null,
    onVisibleChange: ((Boolean) -> Unit)? = null,
) {
    var ownVisible by rememberSaveable { mutableStateOf(false) }
    val shown = visible ?: ownVisible
    val toggle: () -> Unit = {
        if (onVisibleChange != null) onVisibleChange(!shown) else ownVisible = !shown
    }
    AlphaTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        errorMessage = errorMessage,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            autoCorrectEnabled = false,
            imeAction = imeAction,
        ),
        keyboardActions = keyboardActions,
        visualTransformation = if (shown) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            AlphaIconButton(
                icon = if (shown) AlphaIcons.VisibilityOff else AlphaIcons.Visibility,
                contentDescription = stringResource(
                    if (shown) R.string.auth_password_hide else R.string.auth_password_show,
                ),
                onClick = toggle,
                enabled = enabled,
            )
        },
    )
}

@AlphaPreviews
@Composable
private fun PasswordFieldPreview() {
    AlphaTheme {
        Column(Modifier.padding(AlphaTheme.spacing.md)) {
            PasswordField(value = "secret12", onValueChange = {}, label = "Password")
            PasswordField(
                value = "short",
                onValueChange = {},
                label = "Password",
                errorMessage = "Use at least 8 characters.",
                visible = true,
                onVisibleChange = {},
            )
        }
    }
}
