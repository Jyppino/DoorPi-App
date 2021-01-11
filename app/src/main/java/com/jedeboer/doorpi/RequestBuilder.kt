package com.jedeboer.doorpi

import android.content.Context
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONException
import org.json.JSONObject
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

interface VolleyCallback {
    @Throws(JSONException::class)
    fun onSuccess(result: JSONObject)

    @Throws(Exception::class)
    fun onError(result: String?)
}

class RequestBuilder constructor(
    apiHost: String,
    apiPort: Number,
    ssl: Boolean,
    sslCheck: Boolean,
    context: Context
) {
    private val tag = "DoorPi"
    private var volleyController: VolleyController = VolleyController.getInstance(context)
    private var apiUrl: String =
        if (ssl) "https://$apiHost:$apiPort" else "http://$apiHost:$apiPort"

    init {
        if (!sslCheck) {
            HttpsTrustManager().nuke()
        }
    }

    private fun addRequest(
        method: Int,
        url: String,
        params: JSONObject?,
        callback: VolleyCallback
    ) {
        volleyController.addToRequestQueue(
            JsonObjectRequest(method, url, params,
                Response.Listener { response ->
                    callback.onSuccess((response))
                },
                Response.ErrorListener { error ->
                    if (error.networkResponse === null) {
                        callback.onError(null)
                    } else {
                        callback.onError(JSONObject(String(error.networkResponse.data)).getString("message"))
                    }
                }), tag
        )
    }

    fun getChallenge(userId: String, registrationMode: Boolean = false, callback: VolleyCallback) {
        val params = JSONObject()
        params.put("id", userId)
        params.put("register", registrationMode)

        addRequest(Request.Method.POST, "$apiUrl/challenge", params, callback)
    }

    fun unlock(userId: String, code: String, callback: VolleyCallback) {
        val params = JSONObject()
        params.put("id", userId)
        params.put("answer", code)

        addRequest(Request.Method.POST, "$apiUrl/unlock", params, callback)
    }

    fun register(publicKey: String, name: String, code: String, callback: VolleyCallback) {
        val params = JSONObject()
        params.put("name", name)
        params.put("publicKey", publicKey)
        params.put("answer", code)

        addRequest(Request.Method.POST, "$apiUrl/register", params, callback)
    }

    fun deleteKey(userId: String, deleteId: String, answer: String, callback: VolleyCallback) {
        val params = JSONObject()
        params.put("id", userId)
        params.put("deleteId", deleteId)
        params.put("answer", answer)

        addRequest(Request.Method.POST, "$apiUrl/delete", params, callback)
    }

    fun renameKey(
        userId: String,
        nameId: String,
        name: String,
        answer: String,
        callback: VolleyCallback
    ) {
        val params = JSONObject()
        params.put("id", userId)
        params.put("nameId", nameId)
        params.put("name", name)
        params.put("answer", answer)

        addRequest(Request.Method.POST, "$apiUrl/setName", params, callback)
    }

    fun setAdminKey(
        userId: String,
        adminId: String,
        status: Boolean,
        answer: String,
        callback: VolleyCallback
    ) {
        val params = JSONObject()
        params.put("id", userId)
        params.put("adminId", adminId)
        params.put("answer", answer)
        params.put("status", status)

        addRequest(Request.Method.POST, "$apiUrl/setAdmin", params, callback)
    }

    fun getKeys(userId: String, answer: String, callback: VolleyCallback) {
        val params = JSONObject()
        params.put("id", userId)
        params.put("answer", answer)

        addRequest(Request.Method.POST, "$apiUrl/keys", params, callback)
    }


    fun getServerSettings(callback: VolleyCallback) {
        addRequest(Request.Method.GET, "$apiUrl/getSettings", null, callback)
    }

    fun checkRegistration(publicKey: String, callback: VolleyCallback) {
        val params = JSONObject()
        params.put("publicKey", publicKey)

        addRequest(Request.Method.POST, "$apiUrl/isRegistered", params, callback)
    }
}

// Will disable SSL certificate check (acceptable for this use case)
class HttpsTrustManager() {
    fun nuke() {
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {

                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    return arrayOf()
                }

                @Throws(CertificateException::class)
                override fun checkClientTrusted(
                    certs: Array<X509Certificate>,
                    authType: String
                ) {
                }

                @Throws(CertificateException::class)
                override fun checkServerTrusted(
                    certs: Array<X509Certificate>,
                    authType: String
                ) {
                }
            }
            )
            val sc = SSLContext.getInstance("SSL")
            sc.init(null, trustAllCerts, SecureRandom())
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
            HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
        } catch (e: java.lang.Exception) {
        }
    }
}