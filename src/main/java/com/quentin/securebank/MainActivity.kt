package com.quentin.securebank

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun refreshClick(view: View) {
        refresh()
    }

    private fun getJson(url: String): Pair<Boolean, String> {
        var data = ""
        var connected = false
        val httpAsync = url
                .httpGet()
                .responseString { _, _, result ->
                    when (result) {
                        is Result.Failure -> {
                            connected = false
                        }
                        is Result.Success -> {
                            data = result.get()
                            connected = true
                        }
                    }
                }
        httpAsync.join()
        return Pair(connected, data)
    }

    private fun display(data: String, data2: String) {
        findViewById<TextView>(R.id.bankInfos).text = ""
        val personalInfos = JSONObject(data)
        val name = personalInfos.getString("name")
        val lastname = personalInfos.getString("lastname")
        val personalInfosText = getString(R.string.name) + name + getString(R.string.lastname) + lastname
        findViewById<TextView>(R.id.personalInfos).text = personalInfosText
        val accounts = JSONArray(data2)
        for (i in 0 until accounts.length()) {
            val account = accounts.getJSONObject(i)
            val id = account.getString("id")
            val accountName = account.getString("accountName")
            val amount = account.getString("amount")
            val iban = account.getString("iban")
            val currency = account.getString("currency")
            val accountText = getString(R.string.id) + id + getString(R.string.accountName) + accountName + getString(R.string.amount) + amount + getString(R.string.iban) + iban + getString(R.string.currency) + currency
            findViewById<TextView>(R.id.bankInfos).append(accountText)
        }
    }

    private fun refresh() {
        findViewById<TextView>(R.id.bankInfos).text = ""
        findViewById<TextView>(R.id.personalInfos).text = ""
        var accepted = false
        if (MasterKey().contentEquals(findViewById<EditText>(R.id.keyEditText).text)) {
            accepted = true
        }
        if (accepted) {
            val pair1 = getJson(API() + "/" + ID())
            val pair2 = getJson(API2())
            val connected = pair1.first && pair2.first
            var data = pair1.second
            var data2 = pair2.second
            if (connected) {
                findViewById<TextView>(R.id.error).text = ""
                display(data, data2)
                val os: OutputStream = openFileOutput("personal_data.txt", MODE_PRIVATE)
                val dataBytes = encryptMsg(data, Key())
                os.write(dataBytes)
                os.close()
                val os2: OutputStream = openFileOutput("account_data.txt", MODE_PRIVATE)
                val data2Bytes = encryptMsg(data2, Key())
                os2.write(data2Bytes)
                os2.close()
            } else {
                findViewById<TextView>(R.id.error).text = getString(R.string.interneterror)
                try {
                    val os3: InputStream = openFileInput("personal_data.txt")
                    data = decryptMsg(os3.readBytes(), Key())
                    os3.close()
                    val os4: InputStream = openFileInput("account_data.txt")
                    data2 = decryptMsg(os4.readBytes(), Key())
                    os4.close()
                    display(data, data2)
                } catch (e: Exception) {
                    findViewById<TextView>(R.id.error).text = getString(R.string.storageerror)
                }
            }
        } else {
            findViewById<TextView>(R.id.error).text = getString(R.string.rightserror)
        }
    }

    @Throws(java.lang.Exception::class)
    fun encryptMsg(plainText: String, key: String): ByteArray {
        val clean = plainText.toByteArray()

        // Generating IV.
        val ivSize = 16
        val iv = ByteArray(ivSize)
        val random = SecureRandom()
        random.nextBytes(iv)
        val ivParameterSpec = IvParameterSpec(iv)

        // Hashing key.
        val digest: MessageDigest = MessageDigest.getInstance("SHA-256")
        digest.update(key.toByteArray(charset("UTF-8")))
        val keyBytes = ByteArray(16)
        System.arraycopy(digest.digest(), 0, keyBytes, 0, keyBytes.size)
        val secretKeySpec = SecretKeySpec(keyBytes, "AES")

        // Encrypt.
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec)
        val encrypted = cipher.doFinal(clean)

        // Combine IV and encrypted part.
        val encryptedIVAndText = ByteArray(ivSize + encrypted.size)
        System.arraycopy(iv, 0, encryptedIVAndText, 0, ivSize)
        System.arraycopy(encrypted, 0, encryptedIVAndText, ivSize, encrypted.size)
        return encryptedIVAndText
    }

    @Throws(java.lang.Exception::class)
    fun decryptMsg(encryptedIvTextBytes: ByteArray, key: String): String {
        val ivSize = 16
        val keySize = 16

        // Extract IV.
        val iv = ByteArray(ivSize)
        System.arraycopy(encryptedIvTextBytes, 0, iv, 0, iv.size)
        val ivParameterSpec = IvParameterSpec(iv)

        // Extract encrypted part.
        val encryptedSize = encryptedIvTextBytes.size - ivSize
        val encryptedBytes = ByteArray(encryptedSize)
        System.arraycopy(encryptedIvTextBytes, ivSize, encryptedBytes, 0, encryptedSize)

        // Hash key.
        val keyBytes = ByteArray(keySize)
        val md = MessageDigest.getInstance("SHA-256")
        md.update(key.toByteArray())
        System.arraycopy(md.digest(), 0, keyBytes, 0, keyBytes.size)
        val secretKeySpec = SecretKeySpec(keyBytes, "AES")

        // Decrypt.
        val cipherDecrypt = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipherDecrypt.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec)
        val decrypted = cipherDecrypt.doFinal(encryptedBytes)
        return String(decrypted)
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    private external fun API(): String
    private external fun API2(): String
    private external fun ID(): String
    private external fun Key(): String
    private external fun MasterKey(): String

    companion object {
        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }
    }
}