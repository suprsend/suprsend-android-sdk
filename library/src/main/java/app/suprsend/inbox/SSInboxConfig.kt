package app.suprsend.inbox

import android.os.Parcel
import android.os.Parcelable
import app.suprsend.base.safeString
import org.json.JSONObject

data class SSInboxConfig(
    // Change parcel details in constructor(parcel: Parcel) & writeToParcel place
    val statusBarColor: String = "#3700B3",
    val navigationBarColor: String = "#FFFFFF",
    val toolbarBgColor: String = "#6200EE",
    val toolbarTitle: String = "Inbox",
    val toolbarTitleColor: String = "#FFFFFF",
    val screenBgColor: String = "#FFFFFF",
    val cardBackgroundColor: String = "#FFFFFF",
    val cardBorderColor: String = "#E4E4E4",
    val backButtonColor: String = "#FFFFFF",
    val emptyScreenMessageTextColor: String = "#000000",
    val emptyScreenMessage: String = "No data available",
    val messageTextColor: String = "#000000",
    val bellIconColor: String = "#FFFFFF",
    val bellIconCountBgColor: String = "#FF0000",
    val bellIconCountTextColor: String = "#FFFFFF",
    val newUpdatesAvailableText: String = "New Updates Available",
    val newUpdatesAvailablePosition: String = "bottom",
    val inboxFetchInterval: Long = 10000
) : Parcelable {
    constructor(response: JSONObject) :
        this(
            statusBarColor = response.optString("statusBarColor"),
            navigationBarColor = response.optString("navigationBarColor"),
            toolbarBgColor = response.optString("toolbarBgColor"),
            toolbarTitleColor = response.optString("toolbarTitleColor"),
            toolbarTitle = response.optString("toolbarTitle"),
            screenBgColor = response.optString("screenBgColor"),
            cardBackgroundColor = response.optString("cardBackgroundColor"),
            cardBorderColor = response.optString("cardBorderColor"),
            backButtonColor = response.optString("backButtonColor"),
            emptyScreenMessageTextColor = response.optString("emptyScreenMessageTextColor"),
            emptyScreenMessage = response.optString("emptyScreenMessage"),
            messageTextColor = response.optString("messageTextColor"),
            bellIconColor = response.optString("bellIconColor"),
            bellIconCountBgColor = response.optString("bellIconCountBgColor"),
            bellIconCountTextColor = response.optString("bellIconCountTextColor"),
            newUpdatesAvailableText = response.optString("newUpdatesAvailableText"),
            newUpdatesAvailablePosition = response.optString("newUpdatesAvailablePosition"),
            inboxFetchInterval = response.optLong("inboxFetchInterval")
        )

    constructor(parcel: Parcel) : this(
        statusBarColor = parcel.safeString(),
        navigationBarColor = parcel.safeString(),
        toolbarBgColor = parcel.safeString(),
        toolbarTitle = parcel.safeString(),
        toolbarTitleColor = parcel.safeString(),
        screenBgColor = parcel.safeString(),
        cardBackgroundColor = parcel.safeString(),
        cardBorderColor = parcel.safeString(),
        backButtonColor = parcel.safeString(),
        emptyScreenMessageTextColor = parcel.safeString(),
        emptyScreenMessage = parcel.safeString(),
        messageTextColor = parcel.safeString(),
        bellIconColor = parcel.safeString(),
        bellIconCountBgColor = parcel.safeString(),
        bellIconCountTextColor = parcel.safeString(),
        newUpdatesAvailableText = parcel.safeString(),
        newUpdatesAvailablePosition = parcel.safeString(),
        inboxFetchInterval = parcel.readLong()
    )

    override fun writeToParcel(parcel: Parcel, flag: Int) {
        parcel.writeString(statusBarColor)
        parcel.writeString(navigationBarColor)
        parcel.writeString(toolbarBgColor)
        parcel.writeString(toolbarTitle)
        parcel.writeString(toolbarTitleColor)
        parcel.writeString(screenBgColor)
        parcel.writeString(cardBackgroundColor)
        parcel.writeString(cardBorderColor)
        parcel.writeString(backButtonColor)
        parcel.writeString(emptyScreenMessageTextColor)
        parcel.writeString(emptyScreenMessage)
        parcel.writeString(messageTextColor)
        parcel.writeString(bellIconColor)
        parcel.writeString(bellIconCountBgColor)
        parcel.writeString(bellIconCountTextColor)
        parcel.writeString(newUpdatesAvailableText)
        parcel.writeString(newUpdatesAvailablePosition)
        parcel.writeLong(inboxFetchInterval)
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
