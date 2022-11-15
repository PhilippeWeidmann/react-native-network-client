package com.infomaniak

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator(
    private val tokenInterceptorListener: TokenInterceptorListener
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request {
        return runBlocking(Dispatchers.IO) {
            mutex.withLock {
                val request = response.request
                var keychainToken = tokenInterceptorListener.getApiToken()

                if (keychainToken.expiresAt > currentToken?.expiresAt ?: 0) {
                    return@runBlocking changeAccessToken(request, keychainToken)
                } else {
                    try {
                        keychainToken = ApiController.refreshToken(keychainToken.refreshToken, tokenInterceptorListener)
                        return@runBlocking changeAccessToken(request, keychainToken)
                    } catch (exception: ApiController.RefreshTokenException) {
                        return@runBlocking request
                    }
                }
            }
        }
    }

    companion object {
        val mutex = Mutex()
        var currentToken: ApiToken? = null

        fun changeAccessToken(request: Request, apiToken: ApiToken): Request {
            val builder = request.newBuilder()
            builder.header("Authorization", "Bearer ${apiToken.accessToken}")
            currentToken = apiToken
            return builder.build()
        }
    }
}
