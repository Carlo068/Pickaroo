package com.example.pikaroo.common.di

import com.example.pikaroo.ui.products.data.MockProductsRepository
import com.example.pikaroo.ui.products.data.RemoteProductsRepository
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AppContainerTest {

    @Test
    fun `mock flag provides the mock repository`() {
        val container = AppContainer(useMockApi = true)

        assertTrue(container.productsRepository is MockProductsRepository)
    }

    @Test
    fun `remote flag provides the remote repository`() {
        val container = AppContainer(useMockApi = false)

        assertTrue(container.productsRepository is RemoteProductsRepository)
    }

    @Test
    fun `repositories are created only once`() {
        val container = AppContainer(useMockApi = true)

        assertSame(container.productsRepository, container.productsRepository)
    }
}
