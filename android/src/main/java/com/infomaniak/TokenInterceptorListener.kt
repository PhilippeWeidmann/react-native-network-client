package com.infomaniak

interface TokenInterceptorListener {
    suspend fun onRefreshTokenSuccess(apiToken: ApiToken)
    suspend fun onRefreshTokenError()
    suspend fun getApiToken(): ApiToken
}
