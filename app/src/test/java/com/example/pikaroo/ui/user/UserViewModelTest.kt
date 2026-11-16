package com.example.pikaroo.ui.user

import com.example.pikaroo.MainDispatcherRule
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
    @Test fun `loads authenticated name email and roles`() = runTest {
        val vm = UserViewModel(Repository()); runCurrent()
        assertEquals(profile, vm.state.profile); assertFalse(vm.state.isLoading); assertNull(vm.state.error)
    }
    @Test fun `failed load has no invented profile and retry recovers`() = runTest {
        val r = Repository(); r.failure = IllegalStateException("Offline")
        val vm = UserViewModel(r); runCurrent()
        assertNull(vm.state.profile); assertNotNull(vm.state.error)
        r.failure = null; vm.refresh(); runCurrent()
        assertEquals(profile, vm.state.profile); assertNull(vm.state.error)
    }
    @Test fun `repeated retry does not overlap requests`() = runTest {
        val r = Repository(); r.gate = CompletableDeferred()
        val vm = UserViewModel(r); vm.refresh(); vm.refresh(); runCurrent()
        assertTrue(vm.state.isLoading); assertEquals(1, r.calls)
        r.gate!!.complete(Unit); runCurrent(); assertFalse(vm.state.isLoading)
    }
    @Test fun `expired session clears previously displayed profile`() = runTest {
        val r = Repository(); val vm = UserViewModel(r); runCurrent()
        r.failure = SessionExpiredException(); vm.refresh(); runCurrent()
        assertNull(vm.state.profile); assertFalse(vm.state.isLoading)
    }
}
