package com.example.alpha_ai.core.designsystem.preview

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.content.res.Configuration.UI_MODE_TYPE_NORMAL
import androidx.compose.ui.tooling.preview.Preview

/**
 * Renders a preview in the four theme and language combinations, plus 200% font on a narrow
 * phone. Every component and screen in the app uses it (constitution IV).
 */
@Preview(name = "Light · EN", locale = "en", showBackground = true)
@Preview(name = "Dark · EN", locale = "en", showBackground = true, uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL)
@Preview(name = "Light · AR", locale = "ar", showBackground = true)
@Preview(name = "Dark · AR", locale = "ar", showBackground = true, uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL)
@Preview(name = "Font 200%", locale = "en", showBackground = true, fontScale = 2f, widthDp = 360)
annotation class AlphaPreviews
