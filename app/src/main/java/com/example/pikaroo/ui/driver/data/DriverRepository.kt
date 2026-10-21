package com.example.pikaroo.ui.driver.data

import com.example.pikaroo.ui.auth.data.AuthorizedCalls
import com.example.pikaroo.ui.driver.model.AvailableOrderDto
import com.example.pikaroo.ui.driver.network.DriverApi
import com.example.pikaroo.ui.orders.model.OrderDto
import retrofit2.HttpException

interface DriverRepository {
    suspend fun available(): List<AvailableOrderDto>

    /** La entrega en curso del repartidor; null si no tiene ninguna. */
    suspend fun current(): OrderDto?

    suspend fun accept(orderId: String): OrderDto

    suspend fun pickup(orderId: String): OrderDto

    suspend fun deliver(orderId: String): OrderDto
}

class ApiDriverRepository(
    private val api: DriverApi,
    private val authorized: AuthorizedCalls
) : DriverRepository {

    override suspend fun available() = authorized.call { api.available(it) }

    override suspend fun current(): OrderDto? = try {
        authorized.call { api.current(it) }
    } catch (e: HttpException) {
        if (e.code() == 404) null else throw e
    }

    override suspend fun accept(orderId: String) = authorized.call { api.accept(it, orderId) }

    override suspend fun pickup(orderId: String) = authorized.call { api.pickup(it, orderId) }

    override suspend fun deliver(orderId: String) = authorized.call { api.deliver(it, orderId) }
}
