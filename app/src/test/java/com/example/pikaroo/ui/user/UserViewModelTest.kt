package com.example.pikaroo.ui.user

import com.example.pikaroo.R
import com.example.pikaroo.common.text.UiText
import com.example.pikaroo.MainDispatcherRule
import com.example.pikaroo.common.locale.AppLanguage
import com.example.pikaroo.common.locale.AppLanguageStore
import com.example.pikaroo.ui.auth.data.SessionExpiredException
import com.example.pikaroo.ui.auth.model.*
import com.example.pikaroo.ui.user.data.UserRepository
import com.example.pikaroo.ui.user.viewmodel.UserViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class UserViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val profile = MeResponse(UserDto("user_1", "customer1", "Cliente Demo", "customer1@pickaroo.test"), listOf(RoleAssignment("customer")), emptyList())
    private inner class Repository : UserRepository {
        var calls = 0
        var failure: Exception? = null
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun me(): MeResponse { calls++; gate?.await(); failure?.let { throw it }; return profile }
    }

    private class FakeLanguageStore(var language: AppLanguage = AppLanguage.SYSTEM) : AppLanguageStore {
        val writes = mutableListOf<AppLanguage>()
        override fun current() = language
        override fun set(language: AppLanguage) {
            writes += language
            this.language = language
        }
    }

    private fun viewModel(store: AppLanguageStore = FakeLanguageStore()) = UserViewModel(Repository(), store)

    @Test fun `loads authenticated name email and roles`() = runTest {
        val vm = UserViewModel(Repository(), FakeLanguageStore()); runCurrent()
        assertEquals(profile, vm.state.profile); assertFalse(vm.state.isLoading); assertNull(vm.state.error)
    }
    @Test fun `failed load has no invented profile and retry recovers`() = runTest {
        val r = Repository(); r.failure = IllegalStateException("Offline")
        val vm = UserViewModel(r, FakeLanguageStore()); runCurrent()
        assertNull(vm.state.profile); assertEquals(UiText.Res(R.string.user_profile_error_load), vm.state.error)
        r.failure = null; vm.refresh(); runCurrent()
        assertEquals(profile, vm.state.profile); assertNull(vm.state.error)
    }
    @Test fun `repeated retry does not overlap requests`() = runTest {
        val r = Repository(); r.gate = CompletableDeferred()
        val vm = UserViewModel(r, FakeLanguageStore()); vm.refresh(); vm.refresh(); runCurrent()
        assertTrue(vm.state.isLoading); assertEquals(1, r.calls)
        r.gate!!.complete(Unit); runCurrent(); assertFalse(vm.state.isLoading)
    }
    @Test fun `expired session clears previously displayed profile`() = runTest {
        val r = Repository(); val vm = UserViewModel(r, FakeLanguageStore()); runCurrent()
        r.failure = SessionExpiredException(); vm.refresh(); runCurrent()
        assertNull(vm.state.profile); assertFalse(vm.state.isLoading)
        assertEquals(UiText.Res(R.string.user_profile_error_session), vm.state.error)
    }

    @Test fun `starts with the stored language`() = runTest {
        val vm = viewModel(FakeLanguageStore(AppLanguage.SPANISH)); runCurrent()
        assertEquals(AppLanguage.SPANISH, vm.state.language)
        assertFalse(vm.state.showLanguageDialog)
    }

    @Test fun `loading the profile keeps the language and an open dialog`() = runTest {
        val r = Repository(); r.gate = CompletableDeferred()
        val vm = UserViewModel(r, FakeLanguageStore(AppLanguage.ENGLISH))
        vm.openLanguageDialog()
        r.gate!!.complete(Unit); runCurrent()
        assertEquals(profile, vm.state.profile)
        assertEquals(AppLanguage.ENGLISH, vm.state.language)
        assertTrue(vm.state.showLanguageDialog)
    }

    @Test fun `selecting a language stores it and closes the dialog`() = runTest {
        val store = FakeLanguageStore()
        val vm = viewModel(store); runCurrent()
        vm.openLanguageDialog()
        assertTrue(vm.state.showLanguageDialog)

        vm.selectLanguage(AppLanguage.ENGLISH)

        assertEquals(listOf(AppLanguage.ENGLISH), store.writes)
        assertEquals(AppLanguage.ENGLISH, vm.state.language)
        assertFalse(vm.state.showLanguageDialog)
    }

    @Test fun `selecting the current language changes nothing in the system`() = runTest {
        val store = FakeLanguageStore(AppLanguage.SPANISH)
        val vm = viewModel(store); runCurrent()
        vm.openLanguageDialog()
        vm.selectLanguage(AppLanguage.SPANISH)
        assertEquals(emptyList<AppLanguage>(), store.writes)
        assertFalse(vm.state.showLanguageDialog)
    }

    @Test fun `refresh picks up a language changed in the system settings`() = runTest {
        val store = FakeLanguageStore()
        val vm = viewModel(store); runCurrent()
        store.language = AppLanguage.ENGLISH
        vm.refreshLanguage()
        assertEquals(AppLanguage.ENGLISH, vm.state.language)
    }

    @Test fun `dismissing the dialog keeps the language`() = runTest {
        val store = FakeLanguageStore()
        val vm = viewModel(store); runCurrent()
        vm.openLanguageDialog()
        vm.dismissLanguageDialog()
        assertFalse(vm.state.showLanguageDialog)
        assertEquals(emptyList<AppLanguage>(), store.writes)
    }
}
