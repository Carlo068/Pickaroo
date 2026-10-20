package com.example.pikaroo.common.di

import com.example.pikaroo.common.mock.FileSeedSource
import com.example.pikaroo.common.mock.InMemoryStringStore
import com.example.pikaroo.common.mock.MockPickarooApi
import com.example.pikaroo.common.mock.MockStore
import com.example.pikaroo.ui.auth.network.MockAuthService
import com.example.pikaroo.ui.auth.network.RemoteAuthApi
import com.example.pikaroo.ui.products.data.MockProductsRepository
import com.example.pikaroo.ui.products.data.RemoteProductsRepository
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AppContainerTest {

    private fun container(useMockApi: Boolean) = AppContainer(
        useMockApi = useMockApi,
        mockApiFactory = { MockPickarooApi(MockStore(FileSeedSource(), InMemoryStringStore())) }
    )

    @Test
    fun `mock flag provides the mock repository`() {
        val container = container(useMockApi = true)

        assertTrue(container.productsRepository is MockProductsRepository)
    }

    @Test
    fun `remote flag provides the remote repository`() {
        val container = container(useMockApi = false)

        assertTrue(container.productsRepository is RemoteProductsRepository)
    }

    @Test
    fun `repositories are created only once`() {
        val container = container(useMockApi = true)

        assertSame(container.productsRepository, container.productsRepository)
    }

    @Test
    fun `auth api follows the flag`() {
        assertTrue(container(useMockApi = true).authApi is MockAuthService)
        assertTrue(container(useMockApi = false).authApi is RemoteAuthApi)
    }
}
