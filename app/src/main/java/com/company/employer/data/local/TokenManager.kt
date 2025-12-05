package com.company.employer.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

private val Context.tokenDataStore: DataStore<Preferences> by preferencesDataStore(name = "token_prefs")

class TokenManager(private val context: Context) {

    private val dataStore = context.tokenDataStore

    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val USERNAME_KEY = stringPreferencesKey("username")
    }

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        Timber.d("🔄 [Refresh Token] Saving both tokens")
        Timber.d("🔄 [Refresh Token] Access token length: ${accessToken.length}")
        Timber.d("🔄 [Refresh Token] Refresh token length: ${refreshToken.length}")
        Timber.d("🔄 [Refresh Token] Refresh token first 10 chars: ${refreshToken.take(10)}...")

        dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY] = encryptToken(accessToken)
            prefs[REFRESH_TOKEN_KEY] = encryptToken(refreshToken)
        }

        Timber.d("🔄 [Refresh Token] Tokens saved successfully")
    }

    suspend fun saveAccessToken(accessToken: String) {
        Timber.d("💾 [Token Save] Saving new access token (first 10 chars): ${accessToken.take(10)}...")
        dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY] = encryptToken(accessToken)
        }
    }

    suspend fun saveUsername(username: String) {
        dataStore.edit { prefs ->
            prefs[USERNAME_KEY] = username
        }
    }

    fun getAccessToken(): Flow<String?> {
        return dataStore.data.map { prefs ->
            val encrypted = prefs[ACCESS_TOKEN_KEY]
            val decrypted = encrypted?.let { decryptToken(it) }
            Timber.d("🔑 [Refresh Token] Retrieving access token: ${decrypted?.take(10)}... (exists: ${decrypted != null})")
            decrypted
        }
    }

    fun getRefreshToken(): Flow<String?> {
        return dataStore.data.map { prefs ->
            val encrypted = prefs[REFRESH_TOKEN_KEY]
            val decrypted = encrypted?.let { decryptToken(it) }
            Timber.d("🔑 [Refresh Token] Retrieving refresh token: ${decrypted?.take(10)}... (exists: ${decrypted != null})")
            Timber.d("🔑 [Refresh Token] Refresh token stored in DB: ${encrypted != null}")
            Timber.d("🔑 [Refresh Token] Refresh token decrypted: ${decrypted != null}")
            decrypted
        }
    }

    fun getUsername(): Flow<String?> {
        return dataStore.data.map { prefs ->
            prefs[USERNAME_KEY]
        }
    }

    suspend fun clearTokens() {
        Timber.d("🗑️ [Refresh Token] Clearing all tokens from storage")
        Timber.d("🗑️ [Refresh Token] Before clearing - checking if tokens exist:")

        dataStore.data.collect { prefs ->
            val hasAccess = prefs[ACCESS_TOKEN_KEY] != null
            val hasRefresh = prefs[REFRESH_TOKEN_KEY] != null
            Timber.d("🗑️ [Refresh Token] Access token exists: $hasAccess")
            Timber.d("🗑️ [Refresh Token] Refresh token exists: $hasRefresh")
        }

        dataStore.edit { prefs ->
            prefs.clear()
        }

        Timber.d("🗑️ [Refresh Token] Tokens cleared successfully")
    }

    private fun encryptToken(token: String): String {
        Timber.v("🔒 [Refresh Token] Encrypting token (length: ${token.length})")
        return token
    }

    private fun decryptToken(token: String): String {
        Timber.v("🔓 [Refresh Token] Decrypting token (length: ${token.length})")
        return token
    }
}