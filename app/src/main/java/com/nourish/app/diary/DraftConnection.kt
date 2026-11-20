package com.nourish.app.diary

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.net.URI
import javax.net.ssl.HttpsURLConnection
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal class DraftConnection(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("draft_connection", Context.MODE_PRIVATE)
    val origin: String get() = prefs.getString("origin", "").orEmpty()
    private fun key(): SecretKey {
        val alias = "nourish-draft-access"
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance("AES", "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    suspend fun connect(address: String, token: String) = withContext(Dispatchers.IO) {
        val uri = URI(address.trim().trimEnd('/'))
        require(uri.scheme == "https" && uri.host != null && uri.rawUserInfo == null &&
            uri.rawQuery == null && uri.rawFragment == null && uri.path.isNullOrEmpty() && token.length >= 32)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val enc = Base64.encodeToString(cipher.iv + cipher.doFinal(token.toByteArray()), Base64.NO_WRAP)
        check(prefs.edit().putString("origin", uri.toASCIIString()).putString("access", enc).commit())
    }
    fun disconnect() = prefs.edit().clear().apply()

    suspend fun request(path: String, method: String, body: JSONObject? = null): JSONObject = withContext(Dispatchers.IO) {
        check(origin.isNotEmpty())
        val bytes = Base64.decode(prefs.getString("access", ""), Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0,12)))
        val token = cipher.doFinal(bytes.copyOfRange(12,bytes.size)).decodeToString()
        val connection = URI(origin + path).toURL().openConnection() as HttpsURLConnection
        try {
            connection.requestMethod = method
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 12000
            connection.readTimeout = 55000
            connection.setRequestProperty("Authorization","Bearer $token")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type","application/json")
                val payload = body.toString().toByteArray()
                connection.setFixedLengthStreamingMode(payload.size)
                connection.outputStream.use { it.write(payload) }
            }
            if (connection.responseCode !in 200..299) throw ServiceError(when (connection.responseCode) {
                401,403 -> "Connection declined. Check the access token in Settings."
                429 -> "Request limit reached. Try again later."
                503 -> "This service is unavailable. You can still search the offline catalog."
                else -> "The service couldn’t complete this request. You can retry."
            })
            val text = connection.inputStream.bufferedReader().use { it.readText() }
            if (text.isBlank()) JSONObject() else JSONObject(text)
        } finally { connection.disconnect() }
    }
}

internal class ServiceError(message: String) : Exception(message)
