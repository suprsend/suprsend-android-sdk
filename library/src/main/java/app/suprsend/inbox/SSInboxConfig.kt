package app.suprsend.inbox

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import app.suprsend.base.safeString

data class SSInboxConfig(
    //Change parcel details in constructor(parcel: Parcel) & writeToParcel place
    var toolbarBgColor: String = "#FFFFFF",
    var toolbarTitle: String = "Inbox",
    var toolbarTitleColor: String = "#000000",
    var screenBgColor: String = "#FFFFFF",
    var backButtonColor: String = "#000000",
    var emptyScreenMessage: String = "No data available",
    var emptyScreenMessageTextColor: String = "#000000"
) : Parcelable {

    constructor(parcel: Parcel) : this(
        toolbarBgColor = parcel.safeString(),
        toolbarTitle = parcel.safeString(),
        toolbarTitleColor = parcel.safeString(),
        screenBgColor = parcel.safeString(),
        backButtonColor = parcel.safeString(),
        emptyScreenMessage = parcel.safeString(),
        emptyScreenMessageTextColor = parcel.safeString()
    )

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel?, flag: Int) {
        parcel?.writeString(toolbarBgColor)
        parcel?.writeString(toolbarTitle)
        parcel?.writeString(toolbarTitleColor)
        parcel?.writeString(screenBgColor)
        parcel?.writeString(backButtonColor)
        parcel?.writeString(emptyScreenMessage)
        parcel?.writeString(emptyScreenMessageTextColor)
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