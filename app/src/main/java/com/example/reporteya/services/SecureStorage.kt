package com.example.reporteya.services

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

object SecureStorage {
    private const val PREF_NAME = "secure_session"

    private fun prefs(context: Context) = EncryptedSharedPreferences.create(
        PREF_NAME,
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun setSession(context: Context, dni: String, jwt: String, refresh: String?) {
        prefs(context).edit().apply {
            putString("dni", dni)
            putString("jwt", jwt)
            if (refresh != null) putString("refresh", refresh) else remove("refresh")
        }.apply()
    }

    fun getJwt(context: Context): String? = prefs(context).getString("jwt", null)
    fun getRefresh(context: Context): String? = prefs(context).getString("refresh", null)
    fun getDni(context: Context): String? = prefs(context).getString("dni", null)
    fun clear(context: Context) { prefs(context).edit().clear().apply() }
}


