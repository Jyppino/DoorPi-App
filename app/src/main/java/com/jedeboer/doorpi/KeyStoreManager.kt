package com.jedeboer.doorpi

import android.os.Handler
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.MGF1ParameterSpec
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import javax.security.auth.x500.X500Principal

class KeyStoreManager constructor(alias: String) {
    val keyAlias = alias
    private val pubKey: PublicKey
    private val privKey: PrivateKey
    val publicKey: String
    private lateinit var bioPrompt: BiometricPrompt

    init {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        if (!keyStore.containsAlias(keyAlias)) generateAsymmetricKey() // Generate public/private key pair if non existing in KeyStore

        // TODO: CHECK IF KEY HAS BEEN INVALIDATED
        pubKey = keyStore.getCertificate(keyAlias).publicKey
        privKey = keyStore.getKey(keyAlias, null) as PrivateKey
        publicKey = Base64.getEncoder().encodeToString(pubKey.encoded)
    }

    private fun generateAsymmetricKey() {
        val keyPairGenerator: KeyPairGenerator =
            KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")

        val parameterSpec: KeyGenParameterSpec = KeyGenParameterSpec.Builder(
            this.keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).run {
            setCertificateSerialNumber(BigInteger.valueOf(1888))
            setCertificateSubject(X500Principal("CN=$keyAlias"))
            setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
            setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
            setUserAuthenticationRequired(true)
            setInvalidatedByBiometricEnrollment(true)
            build()
        }

        keyPairGenerator.initialize(parameterSpec)
        keyPairGenerator.genKeyPair()
    }

    private fun getCipher(): Cipher {
        return Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
    }

    private fun getEncryptCipher(): Cipher {
        val cipher = getCipher()
        val spec =
            OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT)
        cipher.init(Cipher.ENCRYPT_MODE, pubKey, spec)
        return cipher
    }

    fun encryptData(data: String): String {
        val cipher = getEncryptCipher()
        val bytes = cipher.doFinal(data.toByteArray())
        return Base64.getEncoder().encodeToString(bytes)
    }

    private fun getDecryptCipher(): Cipher {
        val cipher = getCipher()
        val spec =
            OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT)
        cipher.init(Cipher.DECRYPT_MODE, privKey, spec)
        return cipher
    }

    fun decryptData(data: String, cipher: Cipher): String {
        val bytes = Base64.getDecoder().decode(data)
        return String(cipher.doFinal(bytes))
    }

    fun cancelAuthentication() {
        if (::bioPrompt.isInitialized) {
            bioPrompt.cancelAuthentication()
        }
    }

    fun launchBiometricPrompt(
        context: FragmentActivity,
        callbacks: BiometricPrompt.AuthenticationCallback
    ) {
        val cipherDecrypt = getDecryptCipher()
        val promptInfo = generateBiometricPromptInfo()

        if (::bioPrompt.isInitialized) {
            bioPrompt.cancelAuthentication()
            bioPrompt = generateBiometricPrompt(context, callbacks)
            Handler().postDelayed({
                bioPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipherDecrypt))
            }, 1)
        } else {
            bioPrompt = generateBiometricPrompt(context, callbacks)
            bioPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipherDecrypt))
        }
    }

    private fun generateBiometricPrompt(
        context: FragmentActivity,
        callbacks: BiometricPrompt.AuthenticationCallback
    ): BiometricPrompt {
        val overrideCallbacks = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(
                errCode: Int,
                errString: CharSequence
            ) {
                callbacks.onAuthenticationError(errCode, errString)
            }

            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult
            ) {
                callbacks.onAuthenticationSucceeded(result)
            }

            override fun onAuthenticationFailed() {
                callbacks.onAuthenticationFailed()
            }
        }
        val executor = ContextCompat.getMainExecutor(context)
        return BiometricPrompt(context, executor, overrideCallbacks)
    }

    private fun generateBiometricPromptInfo(): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle("Authenticate")
            .setSubtitle("Please confirm your identity")
            .setNegativeButtonText("Cancel")
            .build()
    }

    fun hasBiometricAuthentication(context: FragmentActivity): Boolean {
        val biometricManager = BiometricManager.from(context)
        when (biometricManager.canAuthenticate()) {
            BiometricManager.BIOMETRIC_SUCCESS ->
                return true
//            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
//            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
//            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
        }
        return false
    }
}

