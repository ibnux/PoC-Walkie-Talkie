package com.ibnux.poc.ui

import android.annotation.SuppressLint
import android.content.*
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.ibnux.poc.*
import com.ibnux.poc.databinding.ActivityMainBinding
import com.smartwalkie.voicepingsdk.ConnectionState
import com.smartwalkie.voicepingsdk.VoicePing
import com.smartwalkie.voicepingsdk.VoicePingButton
import com.smartwalkie.voicepingsdk.callback.ConnectCallback
import com.smartwalkie.voicepingsdk.exception.ErrorCode
import com.smartwalkie.voicepingsdk.exception.VoicePingException
import com.smartwalkie.voicepingsdk.listener.*
import com.smartwalkie.voicepingsdk.model.Channel
import com.smartwalkie.voicepingsdk.model.ChannelType
import org.json.JSONArray
import java.nio.ByteBuffer
import java.util.*

class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener,
    ConnectionStateListener, IncomingTalkListener {
    private val TAG = "MainActivity"
    private lateinit var binding: ActivityMainBinding

    private var mDestinationPath: String? = null
    private var mToast: Toast? = null
    private var channelType = ChannelType.GROUP
    private var disconnectConfirmationDialog: DisconnectConfirmationDialog? = null
    private var channelsJson = JSONArray()
    private var groupID = "0";

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userId = MyPrefs.userId ?: ""
        val company = MyPrefs.company ?: ""
        val serverUrl = MyPrefs.serverUrl ?: ""
        val lastChannel = MyPrefs.lastChannel ?: 0
        channelsJson = JSONArray(MyPrefs.channels ?: "[]")
        if (userId.isBlank() || company.isBlank() || serverUrl.isBlank()) {
            finish()
            return
        }
        initToolbar(userId, company)
        val channels = Array(channelsJson.length()) {
            channelsJson.getJSONObject(it).getString("name")
        }
        val adapter = ArrayAdapter(this, R.layout.spinner_item, channels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerChannelList.adapter = adapter
        binding.spinnerChannelList.onItemSelectedListener = this
        if(lastChannel<channels.size){
            binding.spinnerChannelList.setSelection(lastChannel)
            groupID = channelsJson.getJSONObject(lastChannel).getString("id");
        }else{
            groupID = channelsJson.getJSONObject(0).getString("id");
        }
        binding.voicePingButton.receiverId = groupID

        binding.voicePingButton.setButtonEnabled(true)
        binding.voicePingButton.channelType = ChannelType.GROUP
        binding.voicePingButton.listener = object : VoicePingButton.Listener {
            override fun onStarted() {
                log("VoicePingButton, PTT onStarted")
            }

            override fun onStopped() {
                log("VoicePingButton, PTT onStopped")
            }

            override fun onError(errorMessage: String) {
                log("VoicePingButton, PTT error: $errorMessage")
                //binding.textInformation.text = "VoicePingButton, PTT error: $errorMessage"
                speak(errorMessage)
            }
        }
        VoicePing.setIncomingTalkListener(this)
        updateConnectionState(VoicePing.getConnectionState())
        VoicePing.setConnectionStateListener(this)
        if (VoicePing.getConnectionState() == ConnectionState.DISCONNECTED) {
            VoicePing.connect(serverUrl, userId, company, object : ConnectCallback {
                override fun onConnected() {
                    // Ignored
                }

                override fun onFailed(exception: VoicePingException) {
                    // Ignored
                }
            })
        }

        joinGroup()
    }

    private fun initToolbar(userId: String, company: String) {
        supportActionBar?.title = "$userId - $company"
        supportActionBar?.subtitle = "PoC NuX"
    }

    private fun updateConnectionState(connectionState: ConnectionState) {
        log("updateConnectionState: ${connectionState.name}")
        binding.textConnectionState.text = connectionState.name
        if(connectionState == ConnectionState.CONNECTED) {
            speak("connected")
        }else if(connectionState == ConnectionState.CONNECTING){
            speak("connecting")
        }else if(connectionState == ConnectionState.DISCONNECTED){
            speak("disconnected")
        }
        val colorResId = when (connectionState) {
            ConnectionState.DISCONNECTED -> R.color.red
            ConnectionState.CONNECTING -> R.color.yellow
            ConnectionState.CONNECTED -> R.color.white
        }
        binding.textConnectionState.setTextColor(ContextCompat.getColor(this, colorResId))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId){
            R.id.action_change_ptt -> {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Click The Button")
                    .setOnKeyListener(object : DialogInterface.OnKeyListener {
                        override fun onKey(
                            dialog: DialogInterface,
                            keyCode: Int,
                            event: KeyEvent?
                        ): Boolean {
                            Toast.makeText(applicationContext,
                                "Key $keyCode selected", Toast.LENGTH_SHORT).show()
                            MyPrefs.button_ptt = keyCode
                            dialog.dismiss()
                            return true
                        }
                    })
                builder.setPositiveButton("Close") { dialog, which ->
                    dialog.dismiss()
                }
                builder.show()
                true
            }
            R.id.action_open_player -> {
                PlayerActivity.generateIntent(this, mDestinationPath)
                true
            }
            R.id.action_disconnect -> {
                showDisconnectConfirmationDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // OnItemSelectedListener
    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        if (parent === binding.spinnerChannelList) {
            leaveGroup();
            MyPrefs.lastChannel = position;
            groupID = channelsJson.getJSONObject(position).getString("id");
            log("Join Group $groupID")
            binding.voicePingButton.receiverId= groupID;
            joinGroup()
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}

    // ConnectionStateListener
    override fun onConnectionStateChanged(connectionState: ConnectionState) {
        runOnUiThread {
            updateConnectionState(connectionState)
        }
    }

    override fun onConnectionError(e: VoicePingException) {
        runOnUiThread {
            if (e.errorCode == ErrorCode.DUPLICATE_CONNECT) {
                if (!isFinishing) {
                    VoicePing.unmuteAll()
                    MyPrefs.clear()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            } else {
                showToast(e.message)
            }
        }
    }

    // IncomingTalkListener
    override fun onIncomingTalkStarted(
        audioReceiver: AudioReceiver,
        activeChannels: List<Channel>
    ) {
        log("onIncomingTalkStarted, channel: ${audioReceiver.channel.toString()}, session id: ${audioReceiver.audioSessionId}")
        // Audio processing
        Utils.enhanceLoudnessIfPossible(audioReceiver.audioSessionId, 300)
        Utils.boostBassIfPossible(audioReceiver.audioSessionId, 100.toShort())
        runOnUiThread {
            val channelType = audioReceiver.channel?.type ?: -1
            binding.layoutIncomingTalk.visibility = View.VISIBLE
            binding.textIncomingChannelType.text = ChannelType.getText(channelType)
            binding.textIncomingSenderId.text = audioReceiver.channel?.pureSenderId
        }
//        mDestinationPath = getExternalFilesDir(null).toString() + "/incoming_ptt_audio.opus"
//        audioReceiver.saveToLocal(mDestinationPath)
        audioReceiver.setInterceptorAfterDecoded(object : AudioInterceptor {
            override fun proceed(data: ByteArray, channel: Channel): ByteArray {
                val sb = ByteBuffer.wrap(data).asShortBuffer()
                val dataShortArray = ShortArray(sb.limit())
                sb[dataShortArray]
                val amplitude = Utils.getRmsAmplitude(dataShortArray)
                runOnUiThread {
                    binding.progressIncomingTalk.progress = amplitude.toInt() - 7000
                }
                return data
            }
        })
    }

    override fun onIncomingTalkStopped(
        audioMetaData: AudioMetaData,
        activeChannels: List<Channel>
    ) {
        log("onIncomingTalkStopped, channel: ${audioMetaData.channel.toString()}, download url: ${audioMetaData.downloadUrl}, active channels count: ${activeChannels.size}")
        if (activeChannels.isEmpty()) {
            runOnUiThread {
                binding.layoutIncomingTalk.visibility = View.GONE
            }
        }
    }

    override fun onIncomingTalkError(e: VoicePingException) {
        e.printStackTrace()
        runOnUiThread {
            binding.layoutIncomingTalk.visibility = View.GONE
        }
    }

    private fun joinGroup() {
        log("joinGroup, group ID: $groupID")
        VoicePing.joinGroup(groupID)
        showToast("Joined to "+channelsJson.getJSONObject(binding.spinnerChannelList.selectedItemPosition).getString("name"))
        Utils.closeKeyboard(this, currentFocus)
    }

    private fun leaveGroup() {
        log("leaveGroup, group ID: $groupID")
        VoicePing.leaveGroup(groupID)
        Utils.closeKeyboard(this, currentFocus)
    }

    private fun muteChannel() {
        val receiverId = groupID
        log("muteChannel, target ID: $receiverId, channel type: ${ChannelType.getText(channelType)}")
        VoicePing.mute(receiverId, channelType)
        showToast("Channel $receiverId muted")
    }

    private fun unmuteChannel() {
        val receiverId = groupID
        log("unmuteChannel, target ID: $receiverId, channel type: ${ChannelType.getText(channelType)}")
        VoicePing.unmute(receiverId, channelType)
        showToast("Channel $receiverId unmuted")
    }

    private fun showDisconnectConfirmationDialog() {
        if (disconnectConfirmationDialog == null) {
            disconnectConfirmationDialog =
                DisconnectConfirmationDialog(this, object : DisconnectConfirmationDialog.Listener {
                    override fun onDisconnected() {
                        startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                        finish()
                    }
                })
        }
        disconnectConfirmationDialog?.show()
    }

    private fun showToast(message: String?) {
        runOnUiThread {
            mToast?.cancel()
            mToast = Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT)
            mToast?.show()
        }
    }

    private fun log(message: String) {
        Log.d(TAG, "------------------------")
        Log.d(TAG, message)
        Log.d(TAG, "------------------------")
    }

    override fun onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
        super.onPause()
    }

    override fun onDestroy() {
        VoicePingClientApp.activityVisible = false
        leaveGroup()
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        log("keyCode: $keyCode");
        if(keyCode== MyPrefs.button_ptt){
            binding.voicePingButton.startTalking()
            return true;
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        log("keyCode: $keyCode");
        if(keyCode== MyPrefs.button_ptt){
            binding.voicePingButton.stopTalking()
            return true;
        }
        return super.onKeyUp(keyCode, event)
    }

    public fun speak(text: String){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Call Lollipop+ function
            VoicePingClientApp.textToSpeechEngine.speak(text, TextToSpeech.QUEUE_ADD, null, "tts1")
        } else {
            // Call Legacy function
            VoicePingClientApp.textToSpeechEngine.speak(text, TextToSpeech.QUEUE_ADD, null)
        }
    }

    override fun onResume() {
        VoicePingClientApp.activityVisible = true
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, IntentFilter("broadcasr_message"))
        super.onResume()
    }


    override fun onBackPressed() {
        VoicePingClientApp.activityVisible = false;
        moveTaskToBack(true)
    }

    var receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null) {
                if(intent.hasExtra("message")) {
                    speak(intent.getStringExtra("message"))
                }
            }
        }
    }
}