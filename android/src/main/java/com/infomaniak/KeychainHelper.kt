package com.infomaniak

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson

object KeychainHelper {
    private lateinit var encryptedSharedPreferences: SharedPreferences

    private const val TOKEN_KEY = "token"

    private val jsonCoder = Gson()

    fun init(context: Context) {
        encryptedSharedPreferences = EncryptedSharedPreferences.create(
            context,
            "credentials",
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getSavedToken(): ApiToken? {
        val tokenJson = encryptedSharedPreferences.getString(TOKEN_KEY, "")
        return jsonCoder.fromJson(tokenJson, ApiToken::class.java)
    }

    fun deleteToken() {
        encryptedSharedPreferences
            .edit()
            .clear()
            .apply()
    }

    fun saveToken(apiToken: ApiToken) {
        val tokenJson = jsonCoder.toJson(apiToken)
        encryptedSharedPreferences
            .edit()
            .putString(TOKEN_KEY, tokenJson)
            .apply()
    }
}
