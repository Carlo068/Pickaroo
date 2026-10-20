package com.example.pikaroo.ui.auth

import androidx.test.platform.app.InstrumentationRegistry
import com.example.pikaroo.common.mock.MockPickarooApi
import com.example.pikaroo.ui.auth.model.LoginRequest
import com.example.pikaroo.ui.auth.model.RefreshRequest
import com.example.pikaroo.ui.auth.model.RegisterRequest
import com.example.pikaroo.ui.auth.network.MockAuthService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockAuthServiceInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun newApi() = MockPickarooApi.create(context, latencyMillis = { 0L })
    private fun newService() = MockAuthService(newApi())

    @After
    fun cleanUp() {
        newApi().reset()
    }

    @Test
    fun bundledUsersCanLogIn() = runBlocking {
        val response = newService().login(LoginRequest("driver1", "demo1234"))

        assertEquals("user_5", response.user.id)
        assertTrue("delivery:accept" in response.permissions)
    }

    @Test
    fun registeredUserAndSessionsSurviveANewInstance() = runBlocking {
        val registered = newService().register(RegisterRequest("Ana Pérez", "ana", "ana@pickaroo.test", "secret1"))

        val restarted = newService()
        val login = restarted.login(LoginRequest("ana", "secret1"))
        val refreshed = restarted.refresh(RefreshRequest(registered.refresh))

        assertEquals("Ana Pérez", login.user.name)
        assertTrue(refreshed.access.isNotBlank())
    }
}
