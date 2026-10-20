package com.example.pikaroo.ui.products.data

import com.example.pikaroo.ui.products.model.Product

interface ProductsRepository {
    suspend fun getProducts(): List<Product>
}
