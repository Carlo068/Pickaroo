package com.example.pikaroo.common.di

import com.example.pikaroo.BuildConfig
import com.example.pikaroo.ui.products.data.MockProductsRepository
import com.example.pikaroo.ui.products.data.ProductsRepository
import com.example.pikaroo.ui.products.data.RemoteProductsRepository

/**
 * Único lugar donde se crean los repositorios. Para agregar uno: interfaz en
 * ui/<feature>/data, una implementación Mock y una Remote, y se expone aquí.
 */
class AppContainer(private val useMockApi: Boolean = BuildConfig.USE_MOCK_API) {

    val productsRepository: ProductsRepository by lazy {
        if (useMockApi) MockProductsRepository() else RemoteProductsRepository()
    }
}
