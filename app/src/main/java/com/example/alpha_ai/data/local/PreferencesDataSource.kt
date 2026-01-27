package com.example.alpha_ai.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_FILE_NAME = "alpha_ai_secure_prefs"

        // Keys for credentials
        private const val KEY_REMEMBER_ME = "remember_me"
        private const val KEY_SAVED_EMAIL = "saved_email"
        private const val KEY_SAVED_PASSWORD = "saved_password"

        // Keys for user session
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // ========== Credential Storage ==========

    fun saveCredentials(email: String, password: String) {
        encryptedPrefs.edit().apply {
            putBoolean(KEY_REMEMBER_ME, true)
            putString(KEY_SAVED_EMAIL, email)
            putString(KEY_SAVED_PASSWORD, password)
            apply()
        }
    }

    fun getSavedEmail(): String? {
        return if (isRememberMeEnabled()) {
            encryptedPrefs.getString(KEY_SAVED_EMAIL, null)
        } else null
    }

    fun getSavedPassword(): String? {
        return if (isRememberMeEnabled()) {
            encryptedPrefs.getString(KEY_SAVED_PASSWORD, null)
        } else null
    }

    fun isRememberMeEnabled(): Boolean {
        return encryptedPrefs.getBoolean(KEY_REMEMBER_ME, false)
    }

    fun clearCredentials() {
        encryptedPrefs.edit().apply {
            remove(KEY_REMEMBER_ME)
            remove(KEY_SAVED_EMAIL)
            remove(KEY_SAVED_PASSWORD)
            apply()
        }
    }

    // ========== User Session Storage ==========

    fun saveUserSession(userId: String, userName: String?, userEmail: String?) {
        encryptedPrefs.edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_NAME, userName)
            putString(KEY_USER_EMAIL, userEmail)
            apply()
        }
    }

    fun getUserId(): String? {
        return encryptedPrefs.getString(KEY_USER_ID, null)
    }

    fun getUserName(): String? {
        return encryptedPrefs.getString(KEY_USER_NAME, null)
    }

    fun getUserEmail(): String? {
        return encryptedPrefs.getString(KEY_USER_EMAIL, null)
    }

    fun clearUserSession() {
        encryptedPrefs.edit().apply {
            remove(KEY_USER_ID)
            remove(KEY_USER_NAME)
            remove(KEY_USER_EMAIL)
            apply()
        }
    }

    // ========== Clear All Data ==========

    fun clearAll() {
        encryptedPrefs.edit().clear().apply()
    }
}