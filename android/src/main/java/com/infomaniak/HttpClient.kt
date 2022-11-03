package com.infomaniak

import com.facebook.stetho.okhttp3.StethoInterceptor
import com.mattermost.networkclient.BuildConfig
import okhttp3.OkHttpClient

object HttpClient {
    val okHttpClientNoInterceptor: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .apply {
                if (BuildConfig.DEBUG) addNetworkInterceptor(StethoInterceptor())
            }.build()
    }
}
