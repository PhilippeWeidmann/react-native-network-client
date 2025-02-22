/*
 * Infomaniak Core - Android
 * Copyright (C) 2022 Infomaniak Network SA
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.infomaniak

import com.infomaniak.TokenAuthenticator.Companion.changeAccessToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class TokenInterceptor(
    private val tokenInterceptorListener: TokenInterceptorListener
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        runBlocking(Dispatchers.IO) {
            val apiToken = tokenInterceptorListener.getApiToken()
            val authorization = request.header("Authorization")
            if (apiToken.accessToken != authorization?.replaceFirst("Bearer ", "")) {
                request = changeAccessToken(request, apiToken)
            }
        }

        return chain.proceed(request)
    }
}

