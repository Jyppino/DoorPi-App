package com.jedeboer.doorpi

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricConstants
import androidx.biometric.BiometricPrompt
import androidx.preference.PreferenceManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var keystoreManager: KeyStoreManager
    private lateinit var requestBuilder: RequestBuilder
    private lateinit var unlockButton: FloatingActionButton
    private lateinit var hostName: TextView
    private lateinit var appMenu: Menu
    private lateinit var unlockStatus: TextView
    private var userId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)

        val sharedPref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val piIp = sharedPref.getString("pi_host", "")!!
        val piPort = Integer.parseInt(sharedPref.getString("pi_port", "3000")!!)
        val useHttps = sharedPref.getBoolean("ssl", true)
        val checkCert = sharedPref.getBoolean("sslCertificate", false)

        val hostAddress: TextView = findViewById(R.id.textview_hostaddress)
        unlockButton = findViewById(R.id.button_unlock)
        hostName = findViewById(R.id.textview_hostname)
        unlockStatus = findViewById(R.id.textview_unlock)

        hostAddress.text = "$piIp:$piPort"
        requestBuilder = RequestBuilder(piIp, piPort, useHttps, checkCert, this.applicationContext)

        unlockButton.setOnClickListener {
            requestChallenge(::callbackUnlock)
        }
    }

    private fun reset() {
        setDisconnected(null)
        hostName.setText(R.string.main_connecting)
        hostName.setTextColor(Color.YELLOW)
        handleServerSettings()
        if (::keystoreManager.isInitialized) {
            keystoreManager.cancelAuthentication()
        }
    }

    private fun handleServerSettings() {
        requestBuilder.getServerSettings(object : VolleyCallback {
            override fun onSuccess(result: JSONObject) {
                val serverId = result.getString("id")
                val serverName = result.getString("name")
                val isSetupMode = result.getBoolean("setup")
                hostName.text = serverName
                hostName.setTextColor(Color.GREEN)
                keystoreManager = KeyStoreManager(serverId)
                handleRegistrationCheck(isSetupMode)
            }

            override fun onError(result: String?) {
                setDisconnected(result)
            }
        })
    }

    private fun handleRegistrationCheck(isSetupMode: Boolean) {
        if (keystoreManager.hasBiometricAuthentication(this)) {
            requestBuilder.checkRegistration(keystoreManager.publicKey, object : VolleyCallback {
                override fun onSuccess(result: JSONObject) {
                    val isRegistered = result.getBoolean("registered")
                    val isAdmin = result.getBoolean("admin")
                    if (isRegistered) {
                        userId = result.getString("id")
                        unlockButton.isEnabled = true
                        if (isAdmin) {
                            appMenu.findItem(R.id.action_register).isVisible = true
                            appMenu.findItem(R.id.action_manage).isVisible = true
                        } else {
                            appMenu.findItem(R.id.action_delete).isVisible = true
                            appMenu.findItem(R.id.action_rename).isVisible = true
                        }

                    } else {
                        launchRegisterActivity(isSetupMode)
                    }
                }

                override fun onError(result: String?) {
                    setDisconnected(result)
                }
            })
        } else {
            unlockButton.isEnabled = false
            unlockStatus.setText(R.string.main_bio_unsupported)
            unlockStatus.setTextColor(Color.RED)
        }
    }

    private fun launchRegisterActivity(isSetupMode: Boolean) {
        val intent = Intent(this, RegisterActivity::class.java).apply {
            putExtra("setup", isSetupMode)
            putExtra("publicKey", keystoreManager.publicKey)
        }
        startActivity(intent)
        finish()
    }

    private fun requestChallenge(successCallback: (answer: String) -> Unit) {
        unlockStatus.text = ""

        requestBuilder.getChallenge(userId, true, object : VolleyCallback {
            override fun onSuccess(result: JSONObject) {
                val challenge = result.getString("challenge")
                launchBiometricPrompt(challenge, successCallback)
            }

            override fun onError(result: String?) {
                setDisconnected(result)
            }
        })
    }

    private fun launchBiometricPrompt(
        challenge: String,
        successCallback: (answer: String) -> Unit
    ) {
        unlockStatus.setText(R.string.main_bio_needed)
        unlockStatus.setTextColor(Color.YELLOW)
        val callbacks = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(
                errCode: Int,
                errString: CharSequence
            ) {
                super.onAuthenticationError(errCode, errString)

                if (errCode != BiometricConstants.ERROR_CANCELED && errCode != BiometricConstants.ERROR_USER_CANCELED) {
                    unlockStatus.text = errString
                    unlockStatus.setTextColor(Color.RED)
                }
            }

            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult
            ) {
                super.onAuthenticationSucceeded(result)
                val answer = keystoreManager.decryptData(challenge, result.cryptoObject?.cipher!!)
                successCallback(answer)
            }
        }

        keystoreManager.launchBiometricPrompt(this, callbacks)
    }

    private fun callbackUnlock(answer: String) {
        requestBuilder.unlock(userId, answer, object : VolleyCallback {
            override fun onSuccess(result: JSONObject) {
                val name = result.getString("name");
                unlockStatus.text = "Welcome $name!"
                unlockStatus.setTextColor(Color.GREEN)
            }

            override fun onError(result: String?) {
                setDisconnected(result)
            }
        })
    }

    private fun callbackInvite(answer: String) {
        val intent = Intent(this, InviteActivity::class.java).apply {
            putExtra("code", answer)
        }
        startActivity(intent)
    }

    private fun callbackManage(answer: String) {
        requestBuilder.getKeys(userId, answer, object : VolleyCallback {
            override fun onSuccess(result: JSONObject) {
                val keys = result.getJSONArray("keys")
                launchManageActivity(keys)
            }

            override fun onError(result: String?) {
                setDisconnected(result)
            }
        })
    }

    private fun callbackDelete(answer: String) {
        requestBuilder.deleteKey(userId, userId, answer, object : VolleyCallback {
            override fun onSuccess(result: JSONObject) {
                setDisconnected("Key has been deleted")
                reset()
            }

            override fun onError(result: String?) {
                setDisconnected(result)
            }
        })
    }

    private fun launchManageActivity(keys: JSONArray) {
        val intent = Intent(this, ManageActivity::class.java).apply {
            putExtra("keys", keys.toString())
            putExtra("userId", userId)
            putExtra("serverId", keystoreManager.keyAlias)
        }
        startActivity(intent)
    }

    private fun setDisconnected(message: String?) {
        unlockButton.isEnabled = false
        hostName.setText(R.string.main_disconnected)
        hostName.setTextColor(Color.RED)
        unlockStatus.text = ""
        userId = ""
        if (::appMenu.isInitialized) {
            appMenu.findItem(R.id.action_register).isVisible = false
            appMenu.findItem(R.id.action_manage).isVisible = false
            appMenu.findItem(R.id.action_delete).isVisible = false
            appMenu.findItem(R.id.action_rename).isVisible = false
        }

        if (message != null && message.isNotEmpty()) {
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        reset()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        appMenu = menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                reset()
                true
            }
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_exit -> {
                finish()
                true
            }
            R.id.action_register -> {
                requestChallenge(::callbackInvite)
                true
            }
            R.id.action_manage -> {
                requestChallenge(::callbackManage)
                true
            }
            R.id.action_delete -> {
                val confirmDialog = AlertDialog.Builder(this)
                confirmDialog.setTitle("Confirm")
                confirmDialog.setMessage("Delete this device as key?")
                confirmDialog.setPositiveButton(R.string.key_confirm_yes) { _, _ ->
                    requestChallenge(::callbackDelete)
                }
                confirmDialog.setNegativeButton(R.string.key_confirm_no) { _, _ -> }
                confirmDialog.show()
                true
            }
            R.id.action_rename -> {
                val confirmDialog = AlertDialog.Builder(this)
                val layout = LinearLayout(this)
                val newName = EditText(this)
                val layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )

                layoutParams.setMargins(70, 0, 70, 0)
                newName.layoutParams = layoutParams
                newName.inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                newName.requestFocus()
                layout.addView(newName)

                confirmDialog.setTitle("Rename")
                confirmDialog.setMessage(R.string.action_rename_text)
                confirmDialog.setView(layout)
                confirmDialog.setPositiveButton(R.string.key_confirm_submit) { _, _ ->
                    requestChallenge {
                        requestBuilder.renameKey(
                            userId,
                            userId,
                            newName.text.toString(),
                            it,
                            object : VolleyCallback {
                                override fun onSuccess(result: JSONObject) {
                                    Toast.makeText(
                                        applicationContext,
                                        "Key was renamed",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    unlockStatus.text = ""
                                }

                                override fun onError(result: String?) {
                                    setDisconnected(result)
                                }
                            })
                    }
                }
                confirmDialog.setNegativeButton(R.string.key_confirm_cancel) { _, _ -> }
                val d = confirmDialog.create()
                d.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                d.show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
