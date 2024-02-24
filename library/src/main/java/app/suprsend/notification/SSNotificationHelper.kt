package app.suprsend.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.suprsend.BuildConfig
import app.suprsend.R
import app.suprsend.SSApi
import app.suprsend.SSApiInternal
import app.suprsend.base.Logger
import app.suprsend.base.SSConstants
import app.suprsend.base.SdkAndroidCreator
import app.suprsend.base.UrlUtils
import app.suprsend.base.appExecutorService
import app.suprsend.base.mapToEnum
import app.suprsend.base.safeBoolean
import app.suprsend.base.safeJsonArray
import app.suprsend.base.safeLong
import app.suprsend.base.safeString
import app.suprsend.base.toKotlinJsonObject
import app.suprsend.config.ConfigHelper
import app.suprsend.fcm.SSFirebaseMessagingService
import app.suprsend.xiaomi.SSXiaomiReceiver
import com.google.firebase.messaging.RemoteMessage
import com.xiaomi.mipush.sdk.ErrorCode
import com.xiaomi.mipush.sdk.MiPushClient
import com.xiaomi.mipush.sdk.MiPushCommandMessage
import com.xiaomi.mipush.sdk.MiPushMessage
import org.json.JSONObject

object SSNotificationHelper {

    fun showSSNotification(context: Context, notificationPayloadJson: String?) {
        try {
            if (notificationPayloadJson.isNullOrBlank())
                return
            appExecutorService.execute {
                showRawNotification(context = context.applicationContext, rawNotification = notificationPayloadJson.getRawNotification())
            }
        } catch (e: Exception) {
            Logger.e(SSFirebaseMessagingService.TAG, "Message data payload exception ", e)
        }
    }

    fun showFCMNotification(context: Context, remoteMessage: RemoteMessage) {
        try {
            Logger.i("notification", "showFCMNotification")
            appExecutorService.execute {
                Logger.i(SSFirebaseMessagingService.TAG, "Message Id : ${remoteMessage.messageId}")
                if (remoteMessage.isSuprSendRemoteMessage()) {
                    showRawNotification(context = context.applicationContext, rawNotification = remoteMessage.getRawNotification(), pushVendor = SSConstants.PUSH_VENDOR_FCM)
                }
            }
        } catch (e: Exception) {
            Logger.e(SSFirebaseMessagingService.TAG, "Message data payload exception ", e)
        }
    }

    fun showXiaomiNotification(context: Context, miPushMessage: MiPushMessage) {
        try {
            appExecutorService.execute {
                Logger.i(SSXiaomiReceiver.TAG, "Message Id : ${miPushMessage.messageId}")
                if (miPushMessage.isSuprSendPush()) {
                    showRawNotification(context = context.applicationContext, rawNotification = miPushMessage.getRawNotification(), pushVendor = SSConstants.PUSH_VENDOR_XIAOMI)
                }
            }
        } catch (e: Exception) {
            Logger.e(SSXiaomiReceiver.TAG, "Message data payload exception ", e)
        }
    }

    private fun showRawNotification(context: Context, rawNotification: RawNotification, pushVendor: String? = null) {
        try {
            Logger.i("notification", "showRawNotification $rawNotification")

            //Local flag to avoid duplicate notifications
            val showNotificationId = String.format(SSConstants.CONFIG_NOTIFICATION_GROUP_SHOWN, rawNotification.notificationGroupId)
            val isShown = ConfigHelper.getBoolean(showNotificationId)
            Logger.i("notification", "Notification notificationGroupId : ${rawNotification.notificationGroupId} isShown: $isShown silentPush : ${rawNotification.silentPush}")
            ConfigHelper.addOrUpdate(showNotificationId, true)

            // Notification Delivered Event
            val areNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
            if(!areNotificationsEnabled){
                Logger.e("notification","Notifications are disabled please request the Manifest.permission.POST_NOTIFICATIONS permission")
            }
            val isChannelEnabled = rawNotification.channelId
                ?.let { channelId ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.getNotificationChannel(channelId)?.importance != NotificationManager.IMPORTANCE_NONE
                    } else {
                        true
                    }
                } ?: true
            if(!isChannelEnabled){
                Logger.e("notification","User has disabled the channel ${rawNotification.channelId}")
            }
            val instance = SSApi.getInstanceFromCachedApiKey()
            SSApiInternal.saveTrackEventPayload(
                eventName = SSConstants.S_EVENT_NOTIFICATION_DELIVERED,
                propertiesJO = JSONObject().apply {
                    put("id", rawNotification.id)
                    if (pushVendor != null) {
                        put(SSConstants.PUSH_VENDOR, pushVendor)
                        put(SSConstants.ID_PROVIDER, pushVendor)
                    }
                    put("are_notifications_enabled", areNotificationsEnabled)
                    put("has_channel_support", Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    put("channel_id", rawNotification.channelId)
                    put("is_channel_enabled", isChannelEnabled)
                    put("is_silent_push", rawNotification.silentPush)
                }
            )
            instance.flush()

            if (isShown == true)
                return

            if (rawNotification.silentPush) {
                Logger.i("notification", "This is silent push ignored ui rendering")
                return
            }

            Logger.i("notification", "showNotificationInternal")
            showNotificationInternal(context, areNotificationsEnabled, rawNotification.getNotificationVo())

        } catch (e: Exception) {
            Logger.e("notification", "showRawNotification", e)
        }
    }

    private fun showNotificationInternal(context: Context, areNotificationsEnabled: Boolean, notificationVo: NotificationVo) {

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (!areNotificationsEnabled) {
            Logger.e("notification", "Notifications are disabled please request the Manifest.permission.POST_NOTIFICATIONS permission")
            return
        }

        Logger.i("notification", "setChannel")
        setChannel(context = context, notificationManager = notificationManager, notificationChannelVo = notificationVo.notificationChannelVo)

        val notificationBuilder = NotificationCompat.Builder(context, notificationVo.notificationChannelVo.id)

        Logger.i("notification", "setBasicVo")
        setBasicVo(context = context, notificationBuilder = notificationBuilder, notificationVo = notificationVo)

        Logger.i("notification", "setStyle")
        setStyle(builder = notificationBuilder, notificationVo = notificationVo)

        Logger.i("notification", "setNotificationAction")
        setNotificationAction(context = context, notificationBuilder = notificationBuilder, notificationVo = notificationVo)

        Logger.i("notification", "notify")

        notificationVo.notificationBasicVo.group?.let {
            val notificationBasicVo = notificationVo.notificationBasicVo
            val smallIcon = context.getDrawableIdFromName(notificationBasicVo.smallIconDrawableName) ?: R.drawable.ic_notification
            val groupNotification = NotificationCompat
                .Builder(context, notificationVo.notificationChannelVo.id)
                .setGroup(notificationBasicVo.group)
                .setSmallIcon(smallIcon)
                .setAutoCancel(true)
                .setGroupSummary(true)

            notificationBasicVo.groupSubText?.let { subText ->
                groupNotification.setSubText(subText)
            }

            notificationBasicVo.groupShowWhenTimeStamp?.let { showWhenTimeStamp ->
                groupNotification.setShowWhen(showWhenTimeStamp)
            }

            notificationBasicVo.groupWhenTimeStamp?.let { whenTimeStamp ->
                groupNotification.setWhen(whenTimeStamp)
            }
            notificationManager
                .notify(
                    notificationBasicVo.group.hashCode(),
                    groupNotification
                        .build()
                )
        }
        notificationManager.notify(notificationVo.id.hashCode(), notificationBuilder.build())
    }

    private fun setNotificationAction(context: Context, notificationBuilder: NotificationCompat.Builder, notificationVo: NotificationVo) {

        try {
            notificationVo.actions?.forEachIndexed { index, notificationActionVo ->

                val actionIcon = context.getDrawableIdFromName(notificationActionVo.iconDrawableName) ?: 0

                val actionIntent = NotificationRedirectionActivity.getIntent(context, notificationActionVo)

                actionIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

                notificationBuilder.addAction(
                    actionIcon,
                    notificationActionVo.title,
                    PendingIntent.getActivity(
                        context,
                        (System.currentTimeMillis() + index).toInt(),
                        actionIntent,
                        getPendingIntentFlag()
                    )
                )
            }
        } catch (e: Exception) {
            Logger.e("notification", "setNotificationAction", e)
        }
    }

    private fun setBasicVo(context: Context, notificationBuilder: NotificationCompat.Builder, notificationVo: NotificationVo) {
        val notificationBasicVo = notificationVo.notificationBasicVo

        notificationBuilder.setChannelId(notificationVo.notificationChannelVo.id)

        notificationVo.notificationBasicVo.priority.let { priority ->
            notificationBuilder.priority = when (priority) {
                NotificationPriority.HIGH -> NotificationCompat.PRIORITY_HIGH
                NotificationPriority.LOW -> NotificationCompat.PRIORITY_LOW
                NotificationPriority.MAX -> NotificationCompat.PRIORITY_MAX
                NotificationPriority.MIN -> NotificationCompat.PRIORITY_MIN
                NotificationPriority.DEFAULT -> NotificationCompat.PRIORITY_DEFAULT
            }
        }

        notificationBasicVo.contentTitle.let { contentTitle ->
            notificationBuilder.setContentTitle(contentTitle)
        }

        notificationBasicVo.contentText.let { contentText ->
            notificationBuilder.setContentText(contentText)
        }

        notificationBasicVo.tickerText.let { tickerText ->
            notificationBuilder.setTicker(tickerText)
        }

        if (notificationVo.bigPictureVo == null) {
            notificationBasicVo.largeIconUrl?.let { largeIconUrl ->
                if (largeIconUrl.isNotBlank())
                    notificationBuilder
                        .setLargeIcon(
                            BitmapHelper
                                .getBitmapFromUrl(
                                    UrlUtils
                                        .createNotificationLogoImage(
                                            largeIconUrl,
                                            200,
                                            UrlUtils.calculateQuality(SdkAndroidCreator.networkInfo.getNetworkType())
                                        )
                                )
                        )
            }
        }

        notificationBasicVo.sound?.let { sound ->
            sound.createRawSoundUri(context)?.let { soundUri ->
                notificationBuilder.setSound(soundUri)
            }
        }


        notificationBasicVo.color?.let { stringColorCode ->
            if (stringColorCode.isNotBlank())
                notificationBuilder.color = Color.parseColor(stringColorCode)
        }

        val smallIcon = context.getDrawableIdFromName(notificationBasicVo.smallIconDrawableName) ?: R.drawable.ic_notification

        notificationBuilder.setSmallIcon(smallIcon)

        notificationBasicVo.subText?.let { subText ->
            notificationBuilder.setSubText(subText)
        }

        notificationBasicVo.showWhenTimeStamp?.let { showWhenTimeStamp ->
            notificationBuilder.setShowWhen(showWhenTimeStamp)
        }

        notificationBasicVo.whenTimeStamp?.let { whenTimeStamp ->
            notificationBuilder.setWhen(whenTimeStamp)
        }

        // The duration of time after which the notification is automatically dismissed.
        notificationBasicVo.timeoutAfter?.let { timeoutAfter ->
            notificationBuilder.setTimeoutAfter(timeoutAfter)
        }

        // Dismiss the notification on click?
        notificationBasicVo.autoCancel?.let { autoCancel ->
            notificationBuilder.setAutoCancel(autoCancel)
        }

        // Set whether this notification is sticky.
        notificationBasicVo.onGoing?.let { onGoing ->
            notificationBuilder.setOngoing(onGoing)
        }

        // Set the handler in the event that the notification is dismissed.
        val notificationDeleteIntent = SSNotificationDismissBroadcastReceiver.notificationDismissIntent(context, NotificationDismissVo(notificationId = notificationVo.id))
        notificationDeleteIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val notificationDeletePI = PendingIntent.getBroadcast(context, System.currentTimeMillis().toInt(), notificationDeleteIntent, getPendingIntentFlag())
        notificationBuilder.setDeleteIntent(notificationDeletePI)

        // The category of the notification which allows android to prioritize the notification as required.
        notificationBasicVo.category?.let { category ->
            notificationBuilder.setCategory(category)
        }

        // Set the key by which this notification will be grouped.
        notificationBasicVo.group?.let { group ->
            notificationBuilder.setGroup(group)
        }

        notificationBasicVo.sortKey?.let { sortKey ->
            notificationBuilder.setSortKey(sortKey)
        }

        // Set whether or not this notification is only relevant to the current device.
        notificationBasicVo.localOnly?.let { localOnly ->
            notificationBuilder.setLocalOnly(localOnly)
        }

        // notificationBuilder.setProgress(0,0,true)

//        notificationBuilder.addPerson(
//            Person
//                .Builder()
//                .setImportant(true)
//                .setName("")
//                .setIcon()
//                .setUri()
//                .build()
//        )

        try {
            // Todo : set big text / picture notification content intent
            val notificationActionVo = notificationVo.getNotificationBodyActionVo()
            val contentIntent = NotificationRedirectionActivity.getIntent(context, notificationActionVo)
            contentIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            val contentPI = PendingIntent.getActivity(context, System.currentTimeMillis().toInt(), contentIntent, getPendingIntentFlag())
            notificationBuilder.setContentIntent(contentPI)
        } catch (e: Exception) {
            Logger.e("notification", "setBasicVo", e)
        }
    }

    private fun getPendingIntentFlag(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
    }

    private fun setChannel(context: Context, notificationManager: NotificationManager, notificationChannelVo: NotificationChannelVo) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        notificationManager.getNotificationChannel(notificationChannelVo.id)?.run {
            name = notificationChannelVo.name
            description = notificationChannelVo.description
            notificationManager.createNotificationChannel(this)
            return
        }

        val importance = when (notificationChannelVo.channelImportance) {
            NotificationChannelImportance.HIGH -> NotificationManager.IMPORTANCE_HIGH
            NotificationChannelImportance.LOW -> NotificationManager.IMPORTANCE_LOW
            NotificationChannelImportance.MAX -> NotificationManager.IMPORTANCE_MAX
            NotificationChannelImportance.MIN -> NotificationManager.IMPORTANCE_MIN
            NotificationChannelImportance.DEFAULT -> NotificationManager.IMPORTANCE_DEFAULT
        }

        val notificationChannel = NotificationChannel(notificationChannelVo.id, notificationChannelVo.name, importance).apply {
            description = notificationChannelVo.description
            lockscreenVisibility = when (notificationChannelVo.channelLockScreenVisibility) {
                NotificationChannelVisibility.PUBLIC -> {
                    Notification.VISIBILITY_PUBLIC
                }
                NotificationChannelVisibility.PRIVATE -> {
                    Notification.VISIBILITY_PRIVATE
                }
                NotificationChannelVisibility.SECRET -> {
                    Notification.VISIBILITY_SECRET
                }
            }
            setShowBadge(notificationChannelVo.showBadge)
            notificationChannelVo.channelSound.createRawSoundUri(context = context)?.let { channelSoundUri ->
                setSound(channelSoundUri, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build())
            }

        }
        notificationManager.createNotificationChannel(notificationChannel)
    }


    private fun setStyle(builder: NotificationCompat.Builder, notificationVo: NotificationVo) {
        handleInboxStyleVo(notificationVo, builder)
        handleBigTextVo(notificationVo, builder)
        handleBigPictureVo(notificationVo, builder)
        handleMessagingStyleVo(notificationVo, builder)
    }

    private fun handleMessagingStyleVo(notificationVo: NotificationVo, builder: NotificationCompat.Builder) {
//        NotificationCompat
//            .MessagingStyle(
//                Person
//                    .Builder()
//                    .setImportant(true)
//                    .setName("")
//                    //.setIcon()
//                    //.setUri()
//                    .build()
//            )
//
//            .setConversationTitle(content.conversationTitle)
//            .also { s ->
//                content.messages.forEach { s.addMessage(it.text, it.timestamp, it.sender) }
//            }
    }

    private fun handleBigPictureVo(notificationVo: NotificationVo, builder: NotificationCompat.Builder) {
        val bigPictureVo = notificationVo.bigPictureVo ?: return
        // Big Picture
        val bigPictureStyle = NotificationCompat.BigPictureStyle()

        bigPictureVo.bigContentTitle?.let { bigContentTitle ->
            bigPictureStyle.setBigContentTitle(bigContentTitle)
        }

        bigPictureVo.summaryText?.let { summaryText ->
            bigPictureStyle.setSummaryText(summaryText)
        }

        bigPictureVo.bigPictureUrl?.let { bigPictureUrl ->
            bigPictureStyle
                .bigPicture(
                    BitmapHelper
                        .getBitmapFromUrl(
                            UrlUtils
                                .createNotificationBannerImage(
                                    bigPictureUrl,
                                    SdkAndroidCreator.deviceInfo.getDeviceWidthPixel(),
                                    UrlUtils.calculateQuality(SdkAndroidCreator.networkInfo.getNetworkType())
                                )
                        )
                )
        }

        bigPictureVo.largeIconUrl?.let { largeIconUrl ->
            bigPictureStyle
                .bigLargeIcon(
                    BitmapHelper
                        .getBitmapFromUrl(
                            UrlUtils
                                .createNotificationLogoImage(
                                    largeIconUrl,
                                    200,
                                    UrlUtils.calculateQuality(SdkAndroidCreator.networkInfo.getNetworkType())
                                )
                        )
                )
        }

        builder.setStyle(bigPictureStyle)
    }

    private fun handleBigTextVo(notificationVo: NotificationVo, builder: NotificationCompat.Builder) {
        val bigTextVo = notificationVo.bigTextVo ?: return

        val bigTextStyle = NotificationCompat.BigTextStyle()

        bigTextVo.bigContentTitle?.let { bigContentTitle ->
            bigTextStyle.setBigContentTitle(bigContentTitle)
        }

        bigTextVo.summaryText?.let { summaryText ->
            bigTextStyle.setSummaryText(summaryText)
        }

        bigTextVo.bigText?.let { bigText ->
            bigTextStyle.bigText(bigText)
        }

        builder.setStyle(bigTextStyle)
    }

    private fun handleInboxStyleVo(notificationVo: NotificationVo, builder: NotificationCompat.Builder) {
        val inboxStyleVo = notificationVo.inboxStyleVo ?: return

        val inboxStyle = NotificationCompat.InboxStyle()

        inboxStyleVo.bigContentTitle?.let { bigContentTitle ->
            inboxStyle.setBigContentTitle(bigContentTitle)
        }

        inboxStyleVo.summaryText?.let { summaryText ->
            inboxStyle.setSummaryText(summaryText)
        }

        inboxStyleVo.lines?.forEach { line ->
            inboxStyle.addLine(line)
        }

        builder.setStyle(inboxStyle)
    }
}

private fun Context.getDrawableIdFromName(drawableName: String?): Int? {
    return getIdentifierIdFromName(drawableName, "drawable")
}

private fun Context.getIdentifierIdFromName(resourceName: String?, defType: String): Int? {
    resourceName ?: return null
    return try {
        val id = resources.getIdentifier(resourceName, defType, packageName)
        return if (id == 0)
            null
        else id
    } catch (e: Exception) {
        Logger.e("utils", "$defType $resourceName not found")
        null
    }
}

fun RemoteMessage.isSuprSendRemoteMessage(): Boolean {
    return data.containsKey(SSConstants.NOTIFICATION_PAYLOAD)
}

fun RemoteMessage.getRawNotification(): RawNotification {
    val notificationPayload = (data[SSConstants.NOTIFICATION_PAYLOAD] ?: "")
    return notificationPayload.getRawNotification()
}

private fun String?.getRawNotification(): RawNotification {
    this ?: return RawNotification("1", "1")

    val notificationPayloadJO = toKotlinJsonObject()

    if (BuildConfig.DEBUG) {
        Logger.i("push_", "Payload : $this")
    }

    val id = notificationPayloadJO.safeString("id") ?: ""

    return RawNotification(
        id = id,
        notificationGroupId = notificationPayloadJO.safeString("notificationGroupId") ?: id,
        silentPush = notificationPayloadJO.safeBoolean("silentPush") ?: false,
        channelId = notificationPayloadJO.safeString("channelId"),
        channelName = notificationPayloadJO.safeString("channelName"),
        channelDescription = notificationPayloadJO.safeString("channelDescription"),
        channelShowBadge = notificationPayloadJO.safeBoolean("channelShowBadge"),
        channelLockScreenVisibility = notificationPayloadJO.safeString("channelLockScreenVisibility").mapToEnum<NotificationChannelVisibility>(),
        channelImportance = notificationPayloadJO.safeString("channelImportance").mapToEnum<NotificationChannelImportance>(),
        channelSound = notificationPayloadJO.safeString("channelSound"),

        priority = notificationPayloadJO.safeString("priority").mapToEnum<NotificationPriority>(),

        smallIconDrawableName = notificationPayloadJO.safeString("smallIconIdentifierName"),
        color = notificationPayloadJO.safeString("color"),
        notificationTitle = notificationPayloadJO.safeString("notificationTitle"),
        subText = notificationPayloadJO.safeString("subText"),
        shortDescription = notificationPayloadJO.safeString("shortDescription"),
        longDescription = notificationPayloadJO.safeString("longDescription"),
        tickerText = notificationPayloadJO.safeString("tickerText"),
        iconUrl = notificationPayloadJO.safeString("iconUrl"),
        imageUrl = notificationPayloadJO.safeString("imageUrl"),
        deeplink = notificationPayloadJO.safeString("deeplink"),
        sound = notificationPayloadJO.safeString("sound"),

        category = notificationPayloadJO.safeString("category"),

        group = notificationPayloadJO.safeString("group"),
        groupSubText = notificationPayloadJO.safeString("groupSubText"),
        groupShowWhenTimeStamp = notificationPayloadJO.safeBoolean("groupShowWhenTimeStamp"),
        groupWhenTimeStamp = notificationPayloadJO.safeLong("groupWhenTimeStamp"),
        sortKey = notificationPayloadJO.safeString("sortKey"),

        onGoing = notificationPayloadJO.safeBoolean("onGoing"),
        autoCancel = notificationPayloadJO.safeBoolean("autoCancel"),

        timeoutAfter = notificationPayloadJO.safeLong("timeoutAfter"),

        showWhenTimeStamp = notificationPayloadJO.safeBoolean("showWhenTimeStamp"),
        whenTimeStamp = notificationPayloadJO.safeLong("whenTimeStamp"),

        localOnly = notificationPayloadJO.safeBoolean("localOnly"),

        actions = getActions(notificationPayloadJO)
    )

}

private fun getActions(notificationPayloadJO: JSONObject): List<NotificationActionVo>? {
    val safeActions = notificationPayloadJO.safeJsonArray("actions")
    safeActions ?: return null
    val actionsList = arrayListOf<NotificationActionVo>()
    for (i in 0 until safeActions.length()) {
        val actionObj = safeActions.getJSONObject(i)
        actionsList.add(
            NotificationActionVo(
                id = actionObj.safeString("id"),
                title = actionObj.safeString("title"),
                link = actionObj.safeString("link"),
                iconDrawableName = actionObj.safeString("iconIdentifierName"),
                notificationId = actionObj.safeString("notificationId"),
                notificationActionType = actionObj.safeString("notificationActionType").mapToEnum<NotificationActionType>()
            )
        )
    }
    return actionsList
}

fun MiPushMessage.isSuprSendPush(): Boolean {
    return content.toKotlinJsonObject().has(SSConstants.NOTIFICATION_PAYLOAD)
}

fun MiPushMessage.getRawNotification(): RawNotification {
    return content.toKotlinJsonObject().getString(SSConstants.NOTIFICATION_PAYLOAD).getRawNotification()
}

fun MiPushCommandMessage?.getToken(): String? {

    Logger.i(
        SSXiaomiReceiver.TAG, "getToken\n" +
                "Command : ${this?.command} \n" +
                "resultCode : ${this?.resultCode} \n" +
                "token : ${this?.commandArguments?.firstOrNull()} \n" +
                "reason : ${this?.reason} \n"
    )

    this ?: return null

    val token = commandArguments?.firstOrNull()

    if (MiPushClient.COMMAND_REGISTER == command && resultCode == ErrorCode.SUCCESS.toLong() && !token.isNullOrBlank()) {
        return token
    }

    return null
}

private fun String?.createRawSoundUri(context: Context): Uri? {
    var soundFile = this ?: return null
    if (soundFile.isBlank()) {
        return null
    }

    soundFile = soundFile.substringBeforeLast(".")

    //If resource not found in raw folder then return
    context.getIdentifierIdFromName(soundFile, "raw") ?: return null

    return Uri.parse(
        ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context
            .packageName + "/raw/" + soundFile
    )
}