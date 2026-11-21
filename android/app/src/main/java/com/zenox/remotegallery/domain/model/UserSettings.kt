package com.zenox.remotegallery.domain.model

data class UserSettings(
    val onboardingCompleted: Boolean = false,
    val permissionsExplained: Boolean = false,
    val remoteAccessConsentGranted: Boolean = false,
    val consentTimestamp: Long? = null,
    val authorizedAdminEmail: String? = null,
    val serverUrlOverride: String? = null,
    val themeMode: String = "system",
    val deviceUuid: String? = null,
    val registered: Boolean = false,
    val lastSyncAt: Long? = null,
    val remoteAccessEnabled: Boolean = false
)
