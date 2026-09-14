package com.example.pikaroo.ui.onboarding.model

import androidx.annotation.DrawableRes

data class OnboardingPage(
    @DrawableRes val imageRes: Int,
    val title: String,
    val description: String
)

data class OnboardingUiState(
    val pages: List<OnboardingPage> = emptyList(),
    val currentPageIndex: Int = 0
) {
    val isLastPage: Boolean
        get() = currentPageIndex == pages.lastIndex
}