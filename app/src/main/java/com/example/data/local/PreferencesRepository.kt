package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.accountingPrefs: DataStore<Preferences> by preferencesDataStore(name = "hesabdar_prefs")

data class UserPreferences(
    val showInRial: Boolean = false,
    val darkMode: Boolean = false,
    val autoApproveSms: Boolean = true,
    val smsBaselineTimestampMillis: Long = 0L,
    val lastBackupMillis: Long = 0L,
    val pinLockEnabled: Boolean = false,
    val pinHash: String = "",
    val securityQuestion: String = "",
    val securityAnswerHash: String = "",
    val biometricEnabled: Boolean = false,
    val dismissedUpdateTag: String = ""
)

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val SHOW_IN_RIAL = booleanPreferencesKey("show_in_rial")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val AUTO_APPROVE_SMS = booleanPreferencesKey("auto_approve_sms")
        val SMS_BASELINE_TIMESTAMP_MILLIS = longPreferencesKey("sms_baseline_timestamp_millis")
        val LAST_BACKUP_MILLIS = longPreferencesKey("last_backup_millis")
        val PIN_LOCK_ENABLED = booleanPreferencesKey("pin_lock_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val SECURITY_QUESTION = stringPreferencesKey("security_question")
        val SECURITY_ANSWER_HASH = stringPreferencesKey("security_answer_hash")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val DISMISSED_UPDATE_TAG = stringPreferencesKey("dismissed_update_tag")
    }

    val preferencesFlow: Flow<UserPreferences> = context.accountingPrefs.data.map { prefs ->
        UserPreferences(
            showInRial = prefs[Keys.SHOW_IN_RIAL] ?: false,
            darkMode = prefs[Keys.DARK_MODE] ?: false,
            autoApproveSms = prefs[Keys.AUTO_APPROVE_SMS] ?: true,
            smsBaselineTimestampMillis = prefs[Keys.SMS_BASELINE_TIMESTAMP_MILLIS] ?: 0L,
            lastBackupMillis = prefs[Keys.LAST_BACKUP_MILLIS] ?: 0L,
            pinLockEnabled = prefs[Keys.PIN_LOCK_ENABLED] ?: false,
            pinHash = prefs[Keys.PIN_HASH] ?: "",
            securityQuestion = prefs[Keys.SECURITY_QUESTION] ?: "",
            securityAnswerHash = prefs[Keys.SECURITY_ANSWER_HASH] ?: "",
            biometricEnabled = prefs[Keys.BIOMETRIC_ENABLED] ?: false,
            dismissedUpdateTag = prefs[Keys.DISMISSED_UPDATE_TAG] ?: ""
        )
    }

    suspend fun setShowInRial(showInRial: Boolean) {
        context.accountingPrefs.edit { it[Keys.SHOW_IN_RIAL] = showInRial }
    }

    suspend fun setDarkMode(darkMode: Boolean) {
        context.accountingPrefs.edit { it[Keys.DARK_MODE] = darkMode }
    }

    suspend fun setAutoApproveSms(enabled: Boolean) {
        context.accountingPrefs.edit { it[Keys.AUTO_APPROVE_SMS] = enabled }
    }

    suspend fun setSmsBaselineTimestampMillis(millis: Long) {
        context.accountingPrefs.edit { it[Keys.SMS_BASELINE_TIMESTAMP_MILLIS] = millis }
    }

    suspend fun setLastBackupMillis(millis: Long) {
        context.accountingPrefs.edit { it[Keys.LAST_BACKUP_MILLIS] = millis }
    }

    suspend fun enablePinLock(
        pinHash: String,
        securityQuestion: String,
        securityAnswerHash: String,
        biometricEnabled: Boolean
    ) {
        context.accountingPrefs.edit {
            it[Keys.PIN_LOCK_ENABLED] = true
            it[Keys.PIN_HASH] = pinHash
            it[Keys.SECURITY_QUESTION] = securityQuestion
            it[Keys.SECURITY_ANSWER_HASH] = securityAnswerHash
            it[Keys.BIOMETRIC_ENABLED] = biometricEnabled
        }
    }

    suspend fun updatePinHash(pinHash: String) {
        context.accountingPrefs.edit {
            it[Keys.PIN_HASH] = pinHash
            it[Keys.PIN_LOCK_ENABLED] = true
        }
    }

    suspend fun updateSecurityRecovery(question: String, answerHash: String) {
        context.accountingPrefs.edit {
            it[Keys.SECURITY_QUESTION] = question
            it[Keys.SECURITY_ANSWER_HASH] = answerHash
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.accountingPrefs.edit { it[Keys.BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun disablePinLock() {
        context.accountingPrefs.edit {
            it[Keys.PIN_LOCK_ENABLED] = false
            it[Keys.PIN_HASH] = ""
            it[Keys.SECURITY_QUESTION] = ""
            it[Keys.SECURITY_ANSWER_HASH] = ""
            it[Keys.BIOMETRIC_ENABLED] = false
        }
    }

    suspend fun setDismissedUpdateTag(tag: String) {
        context.accountingPrefs.edit { it[Keys.DISMISSED_UPDATE_TAG] = tag }
    }
}
