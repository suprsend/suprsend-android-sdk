package app.suprsend.base

import app.suprsend.model.ApiResponse
import org.json.JSONObject
import org.junit.Assert

fun ApiResponse.assertMessageId() {
    Assert.assertEquals(true, isSuccess())
    val messageId = this.body?.let {
        JSONObject(it).getString("message_id")
    }
    Assert.assertNotNull(messageId)
}

fun ApiResponse.assertIsSuccess() {
    if (!isSuccess()) {
        Assert.assertEquals("", message)
    }
    Assert.assertEquals(true, isSuccess())
}

fun ApiResponse.assertIsFailure() {
    Assert.assertEquals(false, isSuccess())
}

fun <T> Response<T>.assertIsFailure() {
    Assert.assertEquals(false, isSuccess())
}

fun <T> Response<T>.assertIsSuccess() {
    if (!isSuccess()) {
        Assert.assertEquals("", ((this as? Response.Error)?.message + (this as? Response.Error)?.ex?.message))
    }
    Assert.assertEquals(true, isSuccess())
}