package com.example.pikaroo.ui.onboarding.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.pikaroo.R
import com.example.pikaroo.ui.onboarding.data.OnboardingPreferences
import com.example.pikaroo.ui.onboarding.model.OnboardingPage
import com.example.pikaroo.ui.onboarding.model.OnboardingUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class OnboardingViewModel(application: Application) :
    AndroidViewModel(application) {

    private val preferences = OnboardingPreferences(application)

    private val pages = listOf(
        OnboardingPage(
            imageRes = R.drawable.onboarding_store,
            title = "Elige Tu\nTienda",
            description = "Explora nuestras sucursales y selecciona la " +
                    "tienda más cercana con los productos que buscas."
        ),
        OnboardingPage(
            imageRes = R.drawable.onboarding_cart,
            title = "Llena Tu\nCarrito",
            description = "Navega el catálogo, agrega tus favoritos y " +
                    "revisa tu pedido antes de confirmar la compra."
        ),
        OnboardingPage(
            imageRes = R.drawable.onboarding_pickup,
            title = "Compra\ny Recoge",
            description = "Paga en minutos y recoge en tienda o recibe " +
                    "tu pedido directamente en casa."
        )
    )

    private val _uiState =
        MutableStateFlow(OnboardingUiState(pages = pages))

    val uiState = _uiState.asStateFlow()

    fun onPageChanged(index: Int) {
        if (index in pages.indices) {
            _uiState.update { it.copy(currentPageIndex = index) }
        }
    }

    fun goToNextPage() {
        _uiState.update {
            it.copy(
                currentPageIndex =
                    (it.currentPageIndex + 1).coerceAtMost(pages.lastIndex)
            )
        }
    }

    fun goToPreviousPage() {
        _uiState.update {
            it.copy(
                currentPageIndex =
                    (it.currentPageIndex - 1).coerceAtLeast(0)
            )
        }
    }

    fun completeOnboarding() {
        preferences.setOnboardingCompleted()
    }
}