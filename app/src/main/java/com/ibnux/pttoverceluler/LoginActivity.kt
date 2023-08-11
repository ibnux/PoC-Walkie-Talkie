package com.ibnux.pttoverceluler

import android.Manifest
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import pub.devrel.easypermissions.EasyPermissions.PermissionCallbacks
import android.os.Bundle
import android.util.Base64
import pub.devrel.easypermissions.EasyPermissions
import android.widget.Toast
import com.smartwalkie.voicepingsdk.callback.ConnectCallback
import android.util.Log
import android.view.View
import com.ibnux.pttoverceluler.databinding.ActivityLoginBinding
import com.smartwalkie.voicepingsdk.VoicePing
import com.smartwalkie.voicepingsdk.exception.VoicePingException
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.IOException

class LoginActivity : AppCompatActivity(), PermissionCallbacks {
    private val TAG = "LoginActivity"
    private lateinit var binding: ActivityLoginBinding
    lateinit var folder:File;
    private var isStorage  = false;
    private var isSdcard  = false;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title = getString(R.string.app_name)
        binding.buttonConnect.setOnClickListener {
            getFileConfiguration();
        }
        folder = getExternalFilesDir(null)!!;

        audioPermission();
    }

    fun audioPermission(){
        EasyPermissions.requestPermissions(
            this@LoginActivity,
            "This app needs your permission to allow recording audio",
            101,
            Manifest.permission.RECORD_AUDIO
        )
    }

    fun filePermission(){
        EasyPermissions.requestPermissions(
            this@LoginActivity,
            "This app needs your permission to read file to get configuration",
            102,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }


    fun getFileConfiguration(){
        try {
            val file = File(folder, "poc.nux")
            if (file.exists()) {
                Toast.makeText(this, folder.toString(), Toast.LENGTH_SHORT).show()
                val inputAsString = FileInputStream(file).bufferedReader().use { it.readText() }
                try{
                    val creds = String(Base64.decode(inputAsString.split(".").get(1), Base64.DEFAULT));
                    val json = JSONObject(creds);
                    MyPrefs.userId = json.getString("name")
                    MyPrefs.company = json.getString("company")
                    MyPrefs.serverUrl = "wss://"+json.getString("server")
                    MyPrefs.channels = json.getJSONArray("channels").toString()
                    MyPrefs.credentials = inputAsString
                    showProgress(true)
                    val userId = MyPrefs.userId ?: ""
                    val company = MyPrefs.company ?: ""
                    val serverUrl = MyPrefs.serverUrl ?: ""
                    VoicePing.connect(serverUrl, inputAsString, userId, company, object : ConnectCallback {
                        override fun onConnected() {
                            Log.v(TAG, "onConnected")
                            showProgress(false)
                            MyPrefs.userId = userId
                            MyPrefs.company = company
                            MyPrefs.serverUrl = serverUrl
                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                            finish()
                        }

                        override fun onFailed(exception: VoicePingException) {
                            Log.v(TAG, "onFailed")
                            showProgress(false)
                            Toast.makeText(
                                this@LoginActivity,
                                R.string.failed_to_sign_in,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Gagal membaca isi poc.nux\n\n"+e.message, Toast.LENGTH_SHORT).show()
                }
            }else{
                if(folder.toString().equals("/")) {
                    if(!isStorage) {
                        isStorage = true;
                        folder = File("/storage/self/primary");
                    }else if(!isSdcard){
                        isSdcard = true;
                        folder = File("/sdcard");
                    }else {
                        Toast.makeText(
                            this,
                            "File poc.nux tidak ditemukan. " + file.toString(),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }else{
                    folder = File(folder?.parent ?: "/");
                    Log.d("POC", "to parent: "+folder.toString())
                    getFileConfiguration();
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Gagal mengambil data konfigurasi poc.nux\n\n"+e.message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStart() {
        super.onStart()
//        val userId = MyPrefs.userId ?: ""
//        val company = MyPrefs.company ?: ""
//        val serverUrl = MyPrefs.serverUrl ?: ""
//        if (userId.isNotBlank() && company.isNotBlank() && serverUrl.isNotBlank()) {
//            startActivity(Intent(this, MainActivity::class.java))
//            finish()
//        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        if (requestCode == 101){
            filePermission();
        }else if (requestCode == 102){
            getFileConfiguration();
        }else if (requestCode == 103){
            getFileConfiguration();
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        Toast.makeText(this, "You need to allow the permission request!", Toast.LENGTH_SHORT).show()
    }

    private fun showProgress(show: Boolean) {
        binding.progressConnect.visibility = if (show) View.VISIBLE else View.GONE
        binding.layoutConnect.visibility = if (show) View.GONE else View.VISIBLE
    }
}