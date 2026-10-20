package com.example.pikaroo.ui.products.data

import com.example.pikaroo.ui.products.model.Product

class RemoteProductsRepository : ProductsRepository {

    override suspend fun getProducts(): List<Product> =
        throw UnsupportedOperationException("PickarooAPI todavía no está conectada")
}
