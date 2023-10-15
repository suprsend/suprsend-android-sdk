package app.suprsend.event

import android.util.Base64
import app.suprsend.base.Logger
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object Algo {
    fun generateHashWithHmac256(key: String, message: String): ByteArray? {
        try {
            return hmac(key = key.toByteArray(), message = message.toByteArray())
        } catch (e: Exception) {
            Logger.e("algo", "generateHashWithHmac256", e)
        }
        return null
    }

    /**
     * algorithm - "HmacSHA256" //or "HmacSHA1", "HmacSHA512"
     */
    private fun hmac(algorithm: String = "HmacSHA256", key: ByteArray, message: ByteArray): ByteArray {
        val mac = Mac.getInstance(algorithm)
        mac.init(SecretKeySpec(key, algorithm))
        return mac.doFinal(message)
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexArray = "0123456789abcdef".toCharArray()
        val hexChars = CharArray(bytes.size * 2)
        var j = 0
        var v: Int
        while (j < bytes.size) {
            v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
            j++
        }
        return String(hexChars)
    }

    fun base64(data: ByteArray?): String {
        return try {
            Base64.encodeToString(data, Base64.NO_WRAP)
        } catch (e: Exception) {
            Logger.e("algo", "base64", e)
            ""
        }
    }
}