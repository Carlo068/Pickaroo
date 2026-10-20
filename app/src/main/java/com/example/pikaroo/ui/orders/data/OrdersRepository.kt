package com.example.pikaroo.ui.orders.data

import com.example.pikaroo.ui.auth.data.AuthorizedCalls
import com.example.pikaroo.ui.orders.model.CreateOrderRequest
import com.example.pikaroo.ui.orders.model.OrderDto
import com.example.pikaroo.ui.orders.model.OrderEventDto
import com.example.pikaroo.ui.orders.network.OrdersApi

interface OrdersRepository {
    suspend fun create(request: CreateOrderRequest): OrderDto
    suspend fun listMine(): List<OrderDto>
    suspend fun get(id: String): OrderDto
    suspend fun events(id: String): List<OrderEventDto>
}

class ApiOrdersRepository(
    private val api: OrdersApi,
    private val authorized: AuthorizedCalls
) : OrdersRepository {

    override suspend fun create(request: CreateOrderRequest) = authorized.call { api.create(it, request) }

    override suspend fun listMine() = authorized.call { api.listMine(it) }

    override suspend fun get(id: String) = authorized.call { api.get(it, id) }

    override suspend fun events(id: String) = authorized.call { api.events(it, id) }
}
