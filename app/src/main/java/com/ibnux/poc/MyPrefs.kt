package com.ibnux.poc

import android.content.Context
import android.content.SharedPreferences

object MyPrefs {
    private val sharedPrefs: SharedPreferences =
        VoicePingClientApp.context.getSharedPreferences("voiceping_sdk.sp", Context.MODE_PRIVATE)

    private const val USER_ID = "user_id"
    private const val COMPANY = "company"
    private const val SERVER_URL = "server_url"
    private const val CREDENTIALS = "credentials"
    private const val CHANNELS = "channels"

    var userId: String?
        get() = sharedPrefs.getString(USER_ID, "")
        set(value) {
            sharedPrefs.edit().putString(USER_ID, value).apply()
        }

    var company: String?
        get() = sharedPrefs.getString(COMPANY, "")
        set(value) {
            sharedPrefs.edit().putString(COMPANY, value).apply()
        }

    var serverUrl: String?
        get() = sharedPrefs.getString(SERVER_URL, "wss://poc.ibnux.com")
        set(value) {
            sharedPrefs.edit().putString(SERVER_URL, value).apply()
        }

    var channels: String?
        get() = sharedPrefs.getString(CHANNELS, "")
        set(value) {
            sharedPrefs.edit().putString(CHANNELS, value).apply()
        }

    var credentials: String?
        get() = sharedPrefs.getString(CREDENTIALS, "")
        set(value) {
            sharedPrefs.edit().putString(CREDENTIALS, value).apply()
        }

    fun clear() {
        sharedPrefs.edit().clear().apply()
    }
}