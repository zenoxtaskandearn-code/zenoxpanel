package com.zenox.remotegallery.data.remote.api

import com.zenox.remotegallery.data.local.prefs.TokenStore
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenStore: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path = original.url.encodedPath
        val isAuthEndpoint = path.contains("/api/device/register") || path.contains("/api/device/auth") || path.contains("/api/device/login")
        val token = tokenStore.getAccessToken()
        val req = if (!isAuthEndpoint && !token.isNullOrBlank()) {
            original.newBuilder().header("Authorization", "Bearer $token").build()
        } else original
        return chain.proceed(req)
    }
}
