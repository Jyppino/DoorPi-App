package com.jedeboer.doorpi

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class ManageActivity : AppCompatActivity() {
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var adapter: RecyclerAdapter
    private lateinit var keystoreManager: KeyStoreManager
    private lateinit var requestBuilder: RequestBuilder
    private var userId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage)

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
        val sharedPref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val piIp = sharedPref.getString("pi_host", "")!!
        val piPort = Integer.parseInt(sharedPref.getString("pi_port", "3000")!!)
        val useHttps = sharedPref.getBoolean("ssl", true)
        val checkCert = sharedPref.getBoolean("sslCertificate", false)

        val keys = JSONArray(intent.getStringExtra("keys"))
        val serverId = intent.getStringExtra("serverId")!!
        userId = intent.getStringExtra("userId")!!

        val recyclerView: RecyclerView = findViewById(R.id.key_recycle_view)
        linearLayoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = linearLayoutManager
        adapter = RecyclerAdapter(generateRecyclerKeys(keys))
        recyclerView.adapter = adapter

        keystoreManager = KeyStoreManager(serverId)
        requestBuilder = RequestBuilder(piIp, piPort, useHttps, checkCert, this.applicationContext)
    }

    private fun generateRecyclerKeys(keys: JSONArray): MutableList<Key> {
        val result = mutableListOf<Key>()

        for (i in 0 until keys.length()) {
            val key = keys.getJSONObject(i)
            var latestUnlock: LocalDateTime? = null
            val latestUnlockString = key.getString("latestUnlock")
            if (latestUnlockString != "null") {
                val parsedDate = Instant.parse(latestUnlockString)
                latestUnlock = parsedDate.atZone(ZoneId.systemDefault()).toLocalDateTime()
            }
            val created = Instant.ofEpochMilli(key.getString("created").toLong())
            val localCreated = created.atZone(ZoneId.systemDefault()).toLocalDateTime()
            val id = key.getString("id")
            val isCurrentUser = id == userId
            result.add(
                Key(
                    id,
                    key.getString("name"),
                    key.getInt("unlocks"),
                    latestUnlock,
                    key.getBoolean("admin"),
                    this,
                    isCurrentUser,
                    localCreated
                )
            )
        }
        return result.toMutableList()
    }

    fun deleteKey(key: Key) {
        requestBuilder.getChallenge(userId, true, object : VolleyCallback {
            override fun onSuccess(result: JSONObject) {
                val challenge = result.getString("challenge")
                launchBiometricPrompt(challenge) {
                    requestBuilder.deleteKey(userId, key.id, it, object : VolleyCallback {
                        override fun onSuccess(result: JSONObject) {
                            adapter.keys.remove(key)
                            adapter.notifyDataSetChanged()
                            if (key.isCurrertUser) {
                                finish()
                            }
                        }

                        override fun onError(result: String?) {
                            handleError(result)
                        }
                    })
                }
            }

            override fun onError(result: String?) {
                handleError(result)
            }
        })
    }

    fun updateAdmin(keyHolder: KeyHolder, key: Key, status: Boolean) {
        requestBuilder.getChallenge(userId, true, object : VolleyCallback {
            override fun onSuccess(result: JSONObject) {
                val challenge = result.getString("challenge")
                launchBiometricPrompt(challenge) {
                    requestBuilder.setAdminKey(userId, key.id, status, it, object : VolleyCallback {
                        override fun onSuccess(result: JSONObject) {
                            keyHolder.setAdmin(status)
                            if (key.isCurrertUser) {
                                finish()
                            }
                        }

                        override fun onError(result: String?) {
                            handleError(result)
                        }
                    })
                }
            }

            override fun onError(result: String?) {
                handleError(result)
            }
        })
    }

    fun updateName(keyHolder: KeyHolder, key: Key, name: String) {
        requestBuilder.getChallenge(userId, true, object : VolleyCallback {
            override fun onSuccess(result: JSONObject) {
                val challenge = result.getString("challenge")
                launchBiometricPrompt(challenge) {
                    requestBuilder.renameKey(userId, key.id, name, it, object : VolleyCallback {
                        override fun onSuccess(result: JSONObject) {
                            keyHolder.setName(name)
                            adapter.notifyDataSetChanged()
                        }

                        override fun onError(result: String?) {
                            handleError(result)
                        }
                    })
                }
            }

            override fun onError(result: String?) {
                handleError(result)
            }
        })
    }

    private fun launchBiometricPrompt(
        challenge: String,
        successCallback: (answer: String) -> Unit
    ) {
        val callbacks = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(
                errCode: Int,
                errString: CharSequence
            ) {
                super.onAuthenticationError(errCode, errString)
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

    private fun handleError(err: String?) {
        if (err != null && err.isNotEmpty()) {
            Toast.makeText(applicationContext, err, Toast.LENGTH_LONG).show()
        }
        finish()
    }
}
