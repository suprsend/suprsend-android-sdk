package app.suprsend.base

import app.suprsend.event.Algo
import org.junit.Assert
import org.junit.Test

class CommonTests {

    @Test
    fun testMobileNumber() {
        Assert.assertFalse(isMobileNumberValid("8983364103"))
        Assert.assertTrue(isMobileNumberValid("+918983364103"))
        Assert.assertTrue(isMobileNumberValid("+91 8983364103"))
        Assert.assertTrue(isMobileNumberValid("+91 898 3364103"))
    }

    @Test
    fun testContentEncrypt() {
        val contentMd5 = "ABC".toMD5()
        Assert.assertEquals(
            contentMd5,
            "902fbdd2b1df0c4f70b4a5d23525e932"
        )
    }

    @Test
    fun testSignatureToSign() {
        val requestMethod = "POST"
        val contentMd5 = "ABC".toMD5()
        val contentType = "application/json"
        val date = "123"
        val requestURI = "/event"
        val envKey = "envKey"
        val secret = "secret"

        val stringToSign = requestMethod + "\n" +
            contentMd5 + "\n" +
            contentType + "\n" +
            date + "\n" +
            requestURI

        val hmac = Algo.generateHashWithHmac256(secret, stringToSign)
        // Hex - 1fd8bb8dee0abdc62c728874aa2c33b366060deb1ed554a3a3c3d27729316461
        val signature = Algo.base64(hmac)
        Assert.assertEquals(
            signature,
            "H9i7je4KvcYscoh0qiwzs2YGDese1VSjo8PSdykxZGE="
        )
    }
}
