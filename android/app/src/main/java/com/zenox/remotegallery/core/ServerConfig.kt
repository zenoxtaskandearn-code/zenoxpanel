package com.zenox.remotegallery.core

object ServerConfig {
    val baseUrl: String
        get() = if (com.zenox.remotegallery.BuildConfig.USE_PROD_SERVER) com.zenox.remotegallery.BuildConfig.PROD_BASE_URL
        else com.zenox.remotegallery.BuildConfig.DEV_BASE_URL

    val wsUrl: String
        get() = if (com.zenox.remotegallery.BuildConfig.USE_PROD_SERVER) com.zenox.remotegallery.BuildConfig.PROD_WS_URL
        else com.zenox.remotegallery.BuildConfig.DEV_WS_URL
}
