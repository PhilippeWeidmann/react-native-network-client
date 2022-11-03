package com.infomaniak

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.mattermost.networkclient.BuildConfig.LOGIN_ENDPOINT_URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MultipartBody
import okhttp3.Request
import java.lang.reflect.Type
import java.util.*

object ApiController {

    val clientId = "20af5539-a4fb-421c-b45a-f43af3d90c14"

    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    var gson: Gson = GsonBuilder()
        .create()

    fun init(typeAdapterList: ArrayList<Pair<Type, Any>>) {
        val gsonBuilder = gson.newBuilder()

        for (typeAdapter in typeAdapterList) {
            gsonBuilder.registerTypeAdapter(typeAdapter.first, typeAdapter.second)
        }

        gson = gsonBuilder.create()
    }

    suspend fun refreshToken(refreshToken: String, tokenInterceptorListener: TokenInterceptorListener): ApiToken {
        val formBuilder: MultipartBody.Builder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("grant_type", "refresh_token")
            .addFormDataPart("client_id", clientId)
            .addFormDataPart("refresh_token", refreshToken)

        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("${LOGIN_ENDPOINT_URL}token")
                .post(formBuilder.build())
                .build()

            val response = HttpClient.okHttpClientNoInterceptor.newCall(request).execute()
            response.use {
                val bodyResponse = it.body?.string()

                when {
                    it.isSuccessful -> {
                        val apiToken = gson.fromJson(bodyResponse, ApiToken::class.java)
                        apiToken.expiresAt = System.currentTimeMillis() + (apiToken.expiresIn * 1000)
                        tokenInterceptorListener.onRefreshTokenSuccess(apiToken)
                        return@withContext apiToken
                    }
                    else -> {
                        var invalidGrant = false
                        try {
                            invalidGrant = JsonParser.parseString(bodyResponse)
                                .asJsonObject
                                .getAsJsonPrimitive("error")
                                .asString == "invalid_grant"
                        } catch (_: Exception) {
                        }

                        if (invalidGrant) {
                            tokenInterceptorListener.onRefreshTokenError()
                            throw RefreshTokenException()
                        }
                        throw Exception(bodyResponse)
                    }
                }
            }
        }
    }

    class RefreshTokenException : Exception()

    enum class ApiMethod {
        GET, PUT, POST, DELETE, PATCH
    }
}
