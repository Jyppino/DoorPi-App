package com.jedeboer.doorpi

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import org.json.JSONObject

class RegisterActivity : AppCompatActivity() {
    private lateinit var requestBuilder: RequestBuilder
    private lateinit var editTextName: EditText
    private lateinit var editTextCode: EditText
    private lateinit var messageBox: TextView
    private lateinit var registerButton: Button
    private var publicKey = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
        val sharedPref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val piIp = sharedPref.getString("pi_host", "")!!
        val piPort = Integer.parseInt(sharedPref.getString("pi_port", "3000")!!)
        val useHttps = sharedPref.getBoolean("ssl", true)
        val checkCert = sharedPref.getBoolean("sslCertificate", false)


        val setupMode = intent.getBooleanExtra("setup", false)
        publicKey = intent.getStringExtra("publicKey")!!

        requestBuilder = RequestBuilder(piIp, piPort, useHttps, checkCert, this.applicationContext)
        editTextName = findViewById(R.id.edittext_name)
        editTextCode = findViewById(R.id.edittext_registrationcode)
        registerButton = findViewById(R.id.button_register)
        messageBox = findViewById(R.id.textview_message)
        messageBox.setTextColor(Color.RED)

        editTextName.inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

        val scanButton: ImageButton = findViewById(R.id.button_scan)

        if (setupMode) {
            editTextCode.visibility = View.GONE
            scanButton.visibility = View.GONE
        }

        registerButton.setOnClickListener {
            register(setupMode)
        }

        scanButton.setOnClickListener {
            scanQrCode()
        }
    }

    private fun scanQrCode() {
        val integrator = IntentIntegrator(this)
        integrator.setBeepEnabled(false)
        integrator.setBarcodeImageEnabled(true)
        integrator.initiateScan()
    }

    private fun register(isSetupMode: Boolean = false) {
        val name: String = editTextName.text.toString()
        val code: String = editTextCode.text.toString()

        if (!validateInput(isSetupMode)) {
            return
        }

        registerButton.isEnabled = false

        requestBuilder.register(publicKey, name, code, object : VolleyCallback {
            override fun onSuccess(result: JSONObject) {
                val intent = Intent(applicationContext, MainActivity::class.java)
                startActivity(intent)
                Toast.makeText(applicationContext, "Registered", Toast.LENGTH_LONG).show()
                finish()
            }

            override fun onError(result: String?) {
                registerButton.isEnabled = true
                messageBox.text = result
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
            }
        })
    }

    private fun validateInput(isSetupMode: Boolean): Boolean {
        val name: String = editTextName.text.toString()
        val code: String = editTextCode.text.toString()

        if (name.isEmpty()) {
            messageBox.setText(R.string.register_error_name)
            return false
        }

        if (!isSetupMode && code.isEmpty()) {
            messageBox.setText(R.string.register_error_code)
            return false
        }
        messageBox.text = ""
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result: IntentResult =
            IntentIntegrator.parseActivityResult(requestCode, resultCode, data)

        if (result.contents != null) {
            editTextCode.setText(result.contents)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}


