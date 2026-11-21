package com.zenox.remotegallery.data.local.prefs

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.zenox.remotegallery.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val onboardingCompleted = booleanPreferencesKey("onboarding_completed")
        val permissionsExplained = booleanPreferencesKey("permissions_explained")
        val remoteAccessConsentGranted = booleanPreferencesKey("remote_access_consent_granted")
        val consentTimestamp = longPreferencesKey("consent_timestamp")
        val authorizedAdminEmail = stringPreferencesKey("authorized_admin_email")
        val serverUrlOverride = stringPreferencesKey("server_url_override")
        val themeMode = stringPreferencesKey("theme_mode")
        val deviceUuid = stringPreferencesKey("device_uuid")
        val deviceDbId = longPreferencesKey("device_db_id")
        val deviceName = stringPreferencesKey("device_name")
        val ownerEmail = stringPreferencesKey("owner_email")
        val registered = booleanPreferencesKey("registered")
        val lastSyncAt = longPreferencesKey("last_sync_at")
        val remoteAccessEnabled = booleanPreferencesKey("remote_access_enabled")
        val lastConnectionStatus = stringPreferencesKey("last_connection_status")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { p ->
        UserSettings(
            onboardingCompleted = p[Keys.onboardingCompleted] ?: false,
            permissionsExplained = p[Keys.permissionsExplained] ?: false,
            remoteAccessConsentGranted = p[Keys.remoteAccessConsentGranted] ?: false,
            consentTimestamp = p[Keys.consentTimestamp],
            authorizedAdminEmail = p[Keys.authorizedAdminEmail],
            serverUrlOverride = p[Keys.serverUrlOverride],
            themeMode = p[Keys.themeMode] ?: "system",
            deviceUuid = p[Keys.deviceUuid],
            registered = p[Keys.registered] ?: false,
            lastSyncAt = p[Keys.lastSyncAt],
            remoteAccessEnabled = p[Keys.remoteAccessEnabled] ?: false
        )
    }

    val rawFlow: Flow<Preferences> = context.dataStore.data

    suspend fun update(transform: (MutablePreferences) -> Unit) {
        context.dataStore.edit { transform(it) }
    }

    suspend fun setServerUrl(url: String?) = update { if (url.isNullOrBlank()) it.remove(Keys.serverUrlOverride) else it[Keys.serverUrlOverride] = url }
    suspend fun setTheme(mode: String) = update { it[Keys.themeMode] = mode }
    suspend fun setOnboardingCompleted(v: Boolean) = update { it[Keys.onboardingCompleted] = v }
    suspend fun setPermissionsExplained(v: Boolean) = update { it[Keys.permissionsExplained] = v }
    suspend fun setConsent(granted: Boolean, adminEmail: String?) = update {
        it[Keys.remoteAccessConsentGranted] = granted
        it[Keys.remoteAccessEnabled] = granted
        if (granted) it[Keys.consentTimestamp] = System.currentTimeMillis() else it.remove(Keys.consentTimestamp)
        if (!adminEmail.isNullOrBlank()) it[Keys.authorizedAdminEmail] = adminEmail else if (!granted) it.remove(Keys.authorizedAdminEmail)
    }
    suspend fun setRegistered(deviceUuid: String, deviceDbId: Long, deviceName: String, ownerEmail: String) = update {
        it[Keys.registered] = true
        it[Keys.deviceUuid] = deviceUuid
        it[Keys.deviceDbId] = deviceDbId
        it[Keys.deviceName] = deviceName
        it[Keys.ownerEmail] = ownerEmail
    }
    suspend fun setLastSync(ts: Long) = update { it[Keys.lastSyncAt] = ts }
    suspend fun setRemoteAccessEnabled(enabled: Boolean) = update { it[Keys.remoteAccessEnabled] = enabled }
    suspend fun clearRegistration() = update {
        listOf(Keys.registered, Keys.deviceUuid, Keys.deviceDbId, Keys.deviceName, Keys.ownerEmail, Keys.lastSyncAt, Keys.lastConnectionStatus).forEach { k -> it.remove(k) }
    }

    companion object {
        val DEVICE_DB_ID = longPreferencesKey("device_db_id")
        val DEVICE_NAME = stringPreferencesKey("device_name")
        val OWNER_EMAIL = stringPreferencesKey("owner_email")
        val LAST_CONNECTION_STATUS = stringPreferencesKey("last_connection_status")
    }
}
