package com.example.alpha_ai.feature.auth.signin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.alpha_ai.core.designsystem.theme.AlphaTheme
import com.example.alpha_ai.core.domain.auth.AuthFailure
import com.example.alpha_ai.core.domain.auth.FieldError
import com.example.alpha_ai.core.domain.auth.FormField
import com.example.alpha_ai.feature.auth.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The stateless sign-in screen; the ViewModel is covered by JVM tests. */
@RunWith(AndroidJUnit4::class)
class SignInScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun text(id: Int) = context.getString(id)

    @Test
    fun emptySubmitShowsTheFieldError() {
        compose.setContent {
            AlphaTheme {
                SignInScreen(
                    state = SignInUiState(fieldErrors = mapOf(FormField.Email to FieldError.Empty)),
                    onIntent = {},
                )
            }
        }
        compose.onNodeWithText(text(R.string.auth_field_email_empty)).assertExists()
    }

    @Test
    fun submittingDisablesTheButton() {
        compose.setContent {
            AlphaTheme { SignInScreen(state = SignInUiState(submitting = true, cancellable = true), onIntent = {}) }
        }
        // The title and the button share the text; the button is the clickable one.
        compose.onNode(hasText(text(R.string.auth_sign_in_submit)) and hasClickAction())
            .assertIsNotEnabled()
        compose.onNodeWithText(context.getString(com.example.alpha_ai.core.designsystem.R.string.ds_cancel))
            .assertExists()
    }

    @Test
    fun invalidCredentialsShowsOneMessage() {
        compose.setContent {
            AlphaTheme {
                SignInScreen(state = SignInUiState(failure = AuthFailure.InvalidCredentials), onIntent = {})
            }
        }
        compose.onAllNodesWithText(text(R.string.auth_error_invalid_credentials)).fetchSemanticsNodes()
            .let { assertEquals(1, it.size) }
    }

    @Test
    fun passwordIsHiddenUntilTheToggleIsTapped() {
        var state by mutableStateOf(SignInUiState(password = "secret12"))
        compose.setContent {
            AlphaTheme {
                SignInScreen(
                    state = state,
                    onIntent = { intent ->
                        if (intent == SignInIntent.TogglePasswordVisibility) {
                            state = state.copy(passwordVisible = !state.passwordVisible)
                        }
                    },
                )
            }
        }
        // InputText always holds the raw value; EditableText is what is drawn on screen.
        val password = hasSetTextAction() and hasText("secret12")
        assertNotEquals("secret12", displayedText(password))
        compose.onNodeWithContentDescription(text(R.string.auth_password_show)).performClick()
        assertEquals("secret12", displayedText(password))
        compose.onNodeWithContentDescription(text(R.string.auth_password_hide)).assertExists()
    }

    private fun displayedText(matcher: SemanticsMatcher): String =
        compose.onNode(matcher).fetchSemanticsNode().config[SemanticsProperties.EditableText].text
}
