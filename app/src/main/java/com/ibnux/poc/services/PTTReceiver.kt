package com.ibnux.poc.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ibnux.poc.VoicePingClientApp
import com.ibnux.poc.ui.MainActivity

class PTTReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("POC","PTTReceiver onReceive")
        if(!VoicePingClientApp.activityVisible){
            context.startActivity(Intent(context, MainActivity::class.java));
        }
    }
}