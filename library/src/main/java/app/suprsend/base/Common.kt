package app.suprsend.base

import android.util.Patterns
import java.util.UUID

internal fun uuid(): String {
    return UUID.randomUUID().toString()
}

internal fun isMobileNumberValid(mobile: String): Boolean {
    return if (mobile.contains("+"))
        SdkAndroidCreator.phoneNumberUtils.isValidNumber(SdkAndroidCreator.phoneNumberUtils.parse(mobile, null))
    else
        false
}

fun CharSequence?.isValidEmail() = !isNullOrEmpty() && Patterns.EMAIL_ADDRESS.matcher(this?:"").matches()