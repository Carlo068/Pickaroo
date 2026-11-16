package com.example.pikaroo.ui.user.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.pikaroo.appContainer
import com.example.pikaroo.ui.auth.data.SessionExpiredException
import com.example.pikaroo.ui.user.data.UserRepository
import com.example.pikaroo.ui.user.model.UserState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class UserViewModel(private val repository: UserRepository) : ViewModel() {
    var state by mutableStateOf(UserState())
        private set

    init { refresh() }

    fun refresh() {
        if (state.isLoading) return
        state = state.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                state = UserState(profile = repository.me())
            } catch (e: CancellationException) { throw e
            } catch (_: SessionExpiredException) {
                state = UserState(error = "Inicia sesión para ver tu perfil.")
            } catch (_: Exception) {
                state = state.copy(isLoading = false, error = "No se pudo cargar tu perfil. Intenta de nuevo.")
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { UserViewModel(appContainer().userRepository) }
        }
    }
}
