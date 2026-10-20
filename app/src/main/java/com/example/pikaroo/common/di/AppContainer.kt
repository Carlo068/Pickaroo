package com.example.pikaroo.common.di

import com.example.pikaroo.BuildConfig
import com.example.pikaroo.common.mock.MockPickarooApi
import com.example.pikaroo.common.preferences.StringStore
import com.example.pikaroo.ui.auth.data.AuthorizedCalls
import com.example.pikaroo.ui.auth.data.SessionRepository
import com.example.pikaroo.ui.auth.network.AuthApi
import com.example.pikaroo.ui.auth.network.MockAuthService
import com.example.pikaroo.ui.auth.network.MockAuthenticator
import com.example.pikaroo.ui.auth.network.RemoteAuthApi
import com.example.pikaroo.ui.orders.data.ApiOrdersRepository
import com.example.pikaroo.ui.orders.data.OrdersRepository
import com.example.pikaroo.ui.orders.network.MockOrdersService
import com.example.pikaroo.ui.orders.network.OrdersApi
import com.example.pikaroo.ui.orders.network.RemoteOrdersApi
import com.example.pikaroo.ui.products.data.MockProductsRepository
import com.example.pikaroo.ui.products.data.ProductsRepository
import com.example.pikaroo.ui.products.data.RemoteProductsRepository

/**
 * Único lugar donde se crean los repositorios. Para agregar uno: interfaz en
 * ui/<feature>/data, una implementación Mock y una Remote, y se expone aquí.
 */
class AppContainer(
    private val useMockApi: Boolean = BuildConfig.USE_MOCK_API,
    sessionStore: StringStore,
    mockApiFactory: () -> MockPickarooApi
) {

    val sessionRepository: SessionRepository by lazy { SessionRepository(sessionStore) }

    val mockApi: MockPickarooApi by lazy(mockApiFactory)

    val authorizedCalls: AuthorizedCalls by lazy { AuthorizedCalls(sessionRepository, authApi) }

    val ordersApi: OrdersApi by lazy {
        if (useMockApi) MockOrdersService(mockApi, MockAuthenticator { System.currentTimeMillis() / 1000 }) else RemoteOrdersApi()
    }

    val ordersRepository: OrdersRepository by lazy { ApiOrdersRepository(ordersApi, authorizedCalls) }

    val authApi: AuthApi by lazy {
        if (useMockApi) MockAuthService(mockApi) else RemoteAuthApi()
    }

    val productsRepository: ProductsRepository by lazy {
        if (useMockApi) MockProductsRepository() else RemoteProductsRepository()
    }
}
