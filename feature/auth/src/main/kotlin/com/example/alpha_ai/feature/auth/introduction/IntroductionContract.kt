package com.example.alpha_ai.feature.auth.introduction

/** contracts/auth-screens.md "Introduction". */
internal data class IntroductionUiState(val page: Int = 0) {
    val pageCount: Int get() = PAGE_COUNT
    val isLastPage: Boolean get() = page == PAGE_COUNT - 1

    companion object {
        const val PAGE_COUNT = 3
    }
}

internal sealed interface IntroductionIntent {
    data object NextPage : IntroductionIntent

    data object PreviousPage : IntroductionIntent

    /** The user swiped to [page]. */
    data class GoToPage(val page: Int) : IntroductionIntent

    data object Skip : IntroductionIntent

    data object Finish : IntroductionIntent
}

internal sealed interface IntroductionEffect {
    data object ToSignIn : IntroductionEffect
}
