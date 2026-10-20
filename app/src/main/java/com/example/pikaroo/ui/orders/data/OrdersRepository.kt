package com.example.pikaroo.ui.orders.data

import com.example.pikaroo.ui.auth.data.AuthorizedCalls
import com.example.pikaroo.ui.orders.model.DriverLocationDto
import com.example.pikaroo.ui.orders.model.OrderDto
import com.example.pikaroo.ui.orders.model.OrderEventDto
import com.example.pikaroo.ui.orders.network.OrdersApi
import retrofit2.HttpException

interface OrdersRepository {
    suspend fun listMine(): List<OrderDto>
    suspend fun get(id: String): OrderDto
    suspend fun events(id: String): List<OrderEventDto>

    /** Última ubicación del repartidor; null si todavía no ha reportado una. */
    suspend fun driverLocation(id: String): DriverLocationDto?
}

class ApiOrdersRepository(
    private val api: OrdersApi,
    private val authorized: AuthorizedCalls
) : OrdersRepository {

    override suspend fun listMine() = authorized.call { api.listMine(it) }

    override suspend fun get(id: String) = authorized.call { api.get(it, id) }

    override suspend fun events(id: String) = authorized.call { api.events(it, id) }

    override suspend fun driverLocation(id: String): DriverLocationDto? = try {
        authorized.call { api.driverLocation(it, id) }
    } catch (e: HttpException) {
        if (e.code() == 404) null else throw e
    }
}
