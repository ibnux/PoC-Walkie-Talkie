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
    private const val LAST_CHANNEL = "last_channel"
    private const val PTT_BUTTON = "button"

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

    var lastChannel: Int?
        get() = sharedPrefs.getInt(LAST_CHANNEL, 0)
        set(value) {
            if (value != null) {
                sharedPrefs.edit().putInt(LAST_CHANNEL, value).apply()
            }else{
                sharedPrefs.edit().putInt(LAST_CHANNEL, 0).apply()
            }
        }

    var button_ptt: Int?
        get() = sharedPrefs.getInt(PTT_BUTTON, 265)
        set(value) {
            if (value != null) {
                sharedPrefs.edit().putInt(PTT_BUTTON, value).apply()
            }else{
                sharedPrefs.edit().putInt(PTT_BUTTON, 265).apply()
            }
        }

    fun clear() {
        sharedPrefs.edit().clear().apply()
    }
}