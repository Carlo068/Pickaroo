package com.example.pikaroo.ui.user.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.pikaroo.R
import com.example.pikaroo.appContainer
import com.example.pikaroo.common.locale.AppCompatLanguageStore
import com.example.pikaroo.common.locale.AppLanguage
import com.example.pikaroo.common.locale.AppLanguageStore
import com.example.pikaroo.common.text.UiText
import com.example.pikaroo.ui.auth.data.SessionExpiredException
import com.example.pikaroo.ui.user.data.UserRepository
import com.example.pikaroo.ui.user.model.UserState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class UserViewModel(
    private val repository: UserRepository,
    private val languages: AppLanguageStore = AppCompatLanguageStore()
) : ViewModel() {
    var state by mutableStateOf(UserState(language = languages.current()))
        private set

    init { refresh() }

    // Profile results replace only the profile fields, so the language setting is kept.
    fun refresh() {
        if (state.isLoading) return
        state = state.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val profile = repository.me()
                state = state.copy(isLoading = false, profile = profile, error = null)
            } catch (e: CancellationException) { throw e
            } catch (_: SessionExpiredException) {
                state = state.copy(isLoading = false, profile = null, error = UiText.Res(R.string.user_profile_error_session))
            } catch (_: Exception) {
                state = state.copy(isLoading = false, error = UiText.Res(R.string.user_profile_error_load))
            }
        }
    }

    /** The language can also change in the system settings while this ViewModel lives on. */
    fun refreshLanguage() {
        state = state.copy(language = languages.current())
    }

    fun openLanguageDialog() {
        state = state.copy(showLanguageDialog = true)
    }

    fun dismissLanguageDialog() {
        state = state.copy(showLanguageDialog = false)
    }

    /** Closes the dialog and applies the language; the system then recreates the activity. */
    fun selectLanguage(language: AppLanguage) {
        state = state.copy(language = language, showLanguageDialog = false)
        if (language != languages.current()) languages.set(language)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { UserViewModel(appContainer().userRepository) }
        }
    }
}
