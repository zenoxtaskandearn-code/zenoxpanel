package com.zenox.remotegallery.data.local.prefs

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenStore(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "secure_tokens",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun save(accessToken: String?, refreshToken: String?, deviceSecret: String? = null) {
        prefs.edit().apply {
            if (accessToken != null) putString("access", accessToken) else remove("access")
            if (refreshToken != null) putString("refresh", refreshToken) else remove("refresh")
            if (deviceSecret != null) putString("device_secret", deviceSecret)
        }.apply()
    }

    fun saveDeviceSecret(secret: String) { prefs.edit().putString("device_secret", secret).apply() }
    fun getAccessToken(): String? = prefs.getString("access", null)
    fun getRefreshToken(): String? = prefs.getString("refresh", null)
    fun getDeviceSecret(): String? = prefs.getString("device_secret", null)
    fun clear() { prefs.edit().clear().apply() }
}
