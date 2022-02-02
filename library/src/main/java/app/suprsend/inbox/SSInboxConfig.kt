package app.suprsend.inbox

import android.os.Parcel
import android.os.Parcelable
import app.suprsend.base.safeString

data class SSInboxConfig(
    //Change parcel details in constructor(parcel: Parcel) & writeToParcel place
    val statusBarColor: String = "#FFFFFF",
    val navigationBarColor: String = "#FFFFFF",
    val toolbarBgColor: String = "#FFFFFF",
    val toolbarTitle: String = "Inbox",
    val toolbarTitleColor: String = "#000000",
    val screenBgColor: String = "#FFFFFF",
    val backButtonColor: String = "#000000",
    val emptyScreenMessage: String = "No data available",
    val emptyScreenMessageTextColor: String = "#000000",
    val messageTextColor: String = "#000000",
    val messageActionBgColor: String = "#CCC9C9",
    val messageActionTextColor: String = "#000000"
) : Parcelable {

    constructor(parcel: Parcel) : this(
        statusBarColor = parcel.safeString(),
        navigationBarColor = parcel.safeString(),
        toolbarBgColor = parcel.safeString(),
        toolbarTitle = parcel.safeString(),
        toolbarTitleColor = parcel.safeString(),
        screenBgColor = parcel.safeString(),
        backButtonColor = parcel.safeString(),
        emptyScreenMessage = parcel.safeString(),
        emptyScreenMessageTextColor = parcel.safeString(),
        messageTextColor = parcel.safeString(),
        messageActionBgColor = parcel.safeString(),
        messageActionTextColor = parcel.safeString()
    )

    override fun writeToParcel(parcel: Parcel?, flag: Int) {
        parcel?.writeString(statusBarColor)
        parcel?.writeString(navigationBarColor)
        parcel?.writeString(toolbarBgColor)
        parcel?.writeString(toolbarTitle)
        parcel?.writeString(toolbarTitleColor)
        parcel?.writeString(screenBgColor)
        parcel?.writeString(backButtonColor)
        parcel?.writeString(emptyScreenMessage)
        parcel?.writeString(emptyScreenMessageTextColor)
        parcel?.writeString(messageTextColor)
        parcel?.writeString(messageActionBgColor)
        parcel?.writeString(messageActionTextColor)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<SSInboxConfig?> = object : Parcelable.Creator<SSInboxConfig?> {
            override fun createFromParcel(parcel: Parcel): SSInboxConfig {
                return SSInboxConfig(parcel)
            }

            override fun newArray(size: Int): Array<SSInboxConfig?> {
                return arrayOfNulls(size)
            }
        }
    }
}