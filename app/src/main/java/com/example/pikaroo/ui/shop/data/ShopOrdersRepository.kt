package com.example.pikaroo.ui.shop.data

import com.example.pikaroo.ui.auth.data.AuthorizedCalls
import com.example.pikaroo.ui.orders.model.OrderDto
import com.example.pikaroo.ui.shop.model.ShopStatusRequest
import com.example.pikaroo.ui.shop.network.ShopOrdersApi

interface ShopOrdersRepository {
    suspend fun orders(): List<OrderDto>
    suspend fun update(id: String, request: ShopStatusRequest): OrderDto
}

class ApiShopOrdersRepository(private val api: ShopOrdersApi, private val authorized: AuthorizedCalls) : ShopOrdersRepository {
    override suspend fun orders() = authorized.call { api.orders(it) }
    override suspend fun update(id: String, request: ShopStatusRequest) = authorized.call { api.update(it, id, request) }
}
