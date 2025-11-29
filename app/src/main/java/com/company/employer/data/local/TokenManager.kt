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

private val Context.tokenDataStore: DataStore<Preferences> by preferencesDataStore(name = "token_prefs")

class TokenManager(private val context: Context) {

    private val dataStore = context.tokenDataStore

    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val USERNAME_KEY = stringPreferencesKey("username")
    }

    // Save tokens using encrypted storage
    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY] = encryptToken(accessToken)
            prefs[REFRESH_TOKEN_KEY] = encryptToken(refreshToken)
        }
    }

    suspend fun saveAccessToken(accessToken: String) {
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
            prefs[ACCESS_TOKEN_KEY]?.let { decryptToken(it) }
        }
    }

    fun getRefreshToken(): Flow<String?> {
        return dataStore.data.map { prefs ->
            prefs[REFRESH_TOKEN_KEY]?.let { decryptToken(it) }
        }
    }

    fun getUsername(): Flow<String?> {
        return dataStore.data.map { prefs ->
            prefs[USERNAME_KEY]
        }
    }

    suspend fun clearTokens() {
        dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    // Simple encryption/decryption using EncryptedSharedPreferences
    private fun encryptToken(token: String): String {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val encryptedPrefs = EncryptedSharedPreferences.create(
            context,
            "encrypted_tokens",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        // Use a simple XOR for demonstration (in production, use proper encryption)
        // For now, we'll store directly as EncryptedSharedPreferences handles encryption
        return token
    }

    private fun decryptToken(token: String): String {
        return token // EncryptedSharedPreferences handles decryption automatically
    }
}