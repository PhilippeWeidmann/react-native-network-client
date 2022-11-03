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
                val authorization = request.header("Authorization")
                var apiToken = tokenInterceptorListener.getApiToken()

                if (apiToken.accessToken != authorization?.replaceFirst("Bearer ", "")) {
                    return@runBlocking changeAccessToken(request, apiToken)
                } else {
                    apiToken = ApiController.refreshToken(apiToken.refreshToken, tokenInterceptorListener)
                    return@runBlocking changeAccessToken(request, apiToken)
                }
            }
        }
    }

    companion object {
        val mutex = Mutex()

        fun changeAccessToken(request: Request, apiToken: ApiToken): Request {
            val builder = request.newBuilder()
            builder.header("Authorization", "Bearer ${apiToken.accessToken}")
            return builder.build()
        }
    }
}
