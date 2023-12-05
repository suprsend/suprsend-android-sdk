package app.suprsend.notification

import java.io.Serializable

internal data class RawNotification(
    val id: String,
    val notificationGroupId: String,

    // Channel Details
    val channelId: String? = null,
    val channelName: String? = null,
    val channelDescription: String? = null,
    val channelShowBadge: Boolean? = null,
    val channelLockScreenVisibility: NotificationChannelVisibility? = null,
    val channelImportance: NotificationChannelImportance? = null,
    val channelSound: String? = null,

    val priority: NotificationPriority? = null,

    // Notification Details
    val smallIconDrawableName: String? = null,
    val color: String? = null,
    val notificationTitle: String? = null,
    val subText: String? = null,
    val shortDescription: String? = null,
    val longDescription: String? = null,
    val tickerText: String? = null,
    val iconUrl: String? = null,
    val imageUrl: String? = null,
    val deeplink: String? = null,
    val sound: String? = null,

    val category: String? = null,

    val group: String? = null,
    val groupSubText: String? = null,
    val groupShowWhenTimeStamp: Boolean? = null,
    val groupWhenTimeStamp: Long? = null,
    val sortKey: String? = null,

    val onGoing: Boolean? = null,
    val autoCancel: Boolean? = null,

    val timeoutAfter: Long? = null,

    val showWhenTimeStamp: Boolean? = null,
    val whenTimeStamp: Long? = null,

    val localOnly: Boolean? = null,

    // Actions
    val actions: List<NotificationActionVo>? = null

) : Serializable {
    fun getNotificationVo(): NotificationVo {
        var notificationVo = NotificationVo(
            id = id,
            notificationChannelVo = NotificationChannelVo(
                id = channelId ?: "main",
                name = channelName ?: "Main",
                description = channelDescription ?: "",
                showBadge = channelShowBadge ?: true,
                channelLockScreenVisibility = channelLockScreenVisibility ?: NotificationChannelVisibility.PUBLIC,
                channelImportance = channelImportance ?: NotificationChannelImportance.HIGH,
                channelSound = channelSound
            ),
            notificationBasicVo = NotificationBasicVo(
                priority = priority ?: NotificationPriority.DEFAULT,
                contentTitle = notificationTitle ?: "",
                contentText = shortDescription ?: "",
                tickerText = tickerText ?: "",
                largeIconUrl = iconUrl,
                color = color,
                subText = subText,
                showWhenTimeStamp = showWhenTimeStamp,
                whenTimeStamp = whenTimeStamp,
                onGoing = onGoing,
                autoCancel = autoCancel,
                smallIconDrawableName = smallIconDrawableName,
                category = category,
                group = group,
                groupSubText = groupSubText,
                groupShowWhenTimeStamp = groupShowWhenTimeStamp,
                groupWhenTimeStamp = groupWhenTimeStamp,
                sortKey = sortKey,
                localOnly = localOnly,
                timeoutAfter = timeoutAfter,
                deeplink = deeplink,
                sound = sound
            ),
            actions = actions
        )

        notificationVo = if (!imageUrl.isNullOrBlank()) {
            notificationVo.copy(
                bigPictureVo = BigPictureVo(
                    bigContentTitle = notificationTitle ?: "",
                    summaryText = shortDescription ?: "",
                    bigPictureUrl = imageUrl,
                    largeIconUrl = iconUrl
                )
            )
        } else {
            notificationVo.copy(
                bigTextVo = BigTextVo(
                    title = notificationTitle ?: "",
                    contentText = shortDescription ?: "",
                    bigContentTitle = notificationTitle ?: "",
                    bigText = longDescription ?: ""
                )
            )
        }

        return notificationVo
    }


}

internal data class NotificationVo(
    val id: String,
    val notificationChannelVo: NotificationChannelVo,
    val notificationBasicVo: NotificationBasicVo,
    val bigTextVo: BigTextVo? = null,
    val bigPictureVo: BigPictureVo? = null,
    val inboxStyleVo: InBoxStyleVo? = null,
    val actions: List<NotificationActionVo>? = null
) {
    fun getNotificationBodyActionVo(): NotificationActionVo {
        val deeplink = notificationBasicVo.deeplink
        return NotificationActionVo(link = deeplink, notificationId = id, notificationActionType = NotificationActionType.BODY)
    }
}

internal data class NotificationActionVo(
    val actionId: String?="",
    val title: String ="",
    val link: String? = null,
    val iconDrawableName: String? = null,
    val notificationId: String,
    val notificationActionType: NotificationActionType? = null
) : Serializable

internal enum class NotificationActionType {
    BODY, BUTTON
}

internal data class NotificationChannelVo(
    val id: String,
    val name: String,
    val description: String,
    val showBadge: Boolean,
    val channelLockScreenVisibility: NotificationChannelVisibility = NotificationChannelVisibility.PUBLIC,
    val channelImportance: NotificationChannelImportance = NotificationChannelImportance.HIGH,
    val channelSound:String?
)

internal enum class NotificationChannelVisibility {
    PUBLIC, PRIVATE, SECRET
}

internal enum class NotificationChannelImportance {
    HIGH, LOW, MAX, MIN, DEFAULT
}

internal enum class NotificationPriority {
    HIGH, LOW, MAX, MIN, DEFAULT
}

internal data class NotificationBasicVo(
    val priority: NotificationPriority,
    val smallIconDrawableName: String? = null,
    // #000000
    val color: String? = null,
    val contentTitle: String,
    val subText: String? = null,
    val contentText: String,
    val tickerText: String,
    val largeIconUrl: String? = null,
    val deeplink: String? = null,
    val sound: String? = null,

    val category: String? = null,

    val group: String? = null,
    val groupSubText: String? = null,
    val groupShowWhenTimeStamp: Boolean? = null,
    val groupWhenTimeStamp: Long? = null,

    val sortKey: String? = null,

    val onGoing: Boolean? = null,
    val autoCancel: Boolean? = null,

    val timeoutAfter: Long? = null,

    val showWhenTimeStamp: Boolean? = null,
    val whenTimeStamp: Long? = null,

    val localOnly: Boolean? = null

)

internal data class BigTextVo(
    val title: String? = null,
    val contentText: String? = null,
    val summaryText: String? = null,
    val bigContentTitle: String? = null,
    val bigText: String? = null
)

internal data class BigPictureVo(
    val bigContentTitle: String? = null,
    val summaryText: String? = null,
    val bigPictureUrl: String? = null,
    val largeIconUrl: String? = null
)

internal data class InBoxStyleVo(
    val bigContentTitle: String? = null,
    val summaryText: String? = null,
    val lines: List<String>? = null
)
