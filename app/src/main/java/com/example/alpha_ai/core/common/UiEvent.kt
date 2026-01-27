package com.example.alpha_ai.core.common

sealed class UiEvent {
    data class ShowSnackbar(
        val message: String,
        val action: SnackbarAction? = null,
        val duration: SnackbarDuration = SnackbarDuration.SHORT
    ) : UiEvent()

    data class ShowDialog(
        val title: String,
        val message: String,
        val positiveButton: DialogAction? = null,
        val negativeButton: DialogAction? = null,
        val dismissible: Boolean = true
    ) : UiEvent()

    data class Navigate(val destination: NavigationDestination) : UiEvent()
}

data class SnackbarAction(
    val label: String,
    val action: () -> Unit
)

data class DialogAction(
    val label: String,
    val action: () -> Unit
)

enum class SnackbarDuration {
    SHORT, LONG, INDEFINITE
}

sealed class NavigationDestination {
    data object Back : NavigationDestination()
    data object OnBoarding : NavigationDestination()
    data object Login : NavigationDestination()
    data object Register : NavigationDestination()
    data object Home : NavigationDestination()
}