package com.example.pikaroo.ui.products.data

import com.example.pikaroo.ui.products.model.Product
import com.example.pikaroo.ui.products.network.GistService
import com.example.pikaroo.ui.products.network.RetrofitClient

class MockProductsRepository(
    private val service: GistService = RetrofitClient.gistService
) : ProductsRepository {

    override suspend fun getProducts(): List<Product> = service.getProducts().products
}
