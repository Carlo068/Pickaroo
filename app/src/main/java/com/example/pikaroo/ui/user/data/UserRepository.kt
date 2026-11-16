package com.example.pikaroo.ui.user.data

import com.example.pikaroo.ui.auth.data.AuthorizedCalls
import com.example.pikaroo.ui.auth.model.MeResponse
import com.example.pikaroo.ui.auth.network.AuthApi

interface UserRepository {
    suspend fun me(): MeResponse
}

class ApiUserRepository(private val api: AuthApi, private val authorized: AuthorizedCalls) : UserRepository {
    override suspend fun me() = authorized.call { api.me(it) }
}
