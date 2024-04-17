package app.suprsend.base

internal object SSConstants {

    const val DEFAULT_BASE_API_URL = "https://hub.suprsend.com"
    const val DEFAULT_INBOX_BASE_API_URL = "https://inboxs.live"
    const val DEFAULT_INBOX_SOCKET_API_URL = "https://betainbox.suprsend.com"
    const val IMAGE_KIT_BASE_PATH = "https://ik.imagekit.io/l0quatz6utm"

    const val NOTIFICATION_PAYLOAD = "supr_send_n_pl"
    const val EVENT_KEY_MAX_LENGTH = 120
    const val EVENT_VALUE_MAX_LENGTH = 512
    const val FLUSH_EVENT_PAYLOAD_SIZE = 20L
    const val PERIODIC_FLUSH_EVENT_IN_SEC = 60

    // Config Helper Keys
    const val CONFIG_API_KEY = "api_key"
    const val CONFIG_API_SECRET = "app_secret"
    const val CONFIG_API_BASE_URL = "api_base_url"
    const val CONFIG_INBOX_API_BASE_URL = "inbox_api_base_url"
    const val CONFIG_INBOX_SOCKET_BASE_URL = "inbox_socket_base_url"
    const val CONFIG_USER_ID = "user_id"
    const val CONFIG_IS_APP_INSTALLED = "is_app_launched" // Mistakenly value was places wrong in ver 1 keep this as it is to avoid wrong stats
    const val CONFIG_APP_LAUNCH_TIME = "app_launch_time"
    const val CONFIG_FCM_PUSH_TOKEN = "fcm_push_token"
    const val CONFIG_XIAOMI_PUSH_TOKEN = "xiaomi_push_token"
    const val CONFIG_IOS_PUSH_TOKEN = "ios_push_token"
    const val CONFIG_DEVICE_ID = "device_id"
    const val CONFIG_NOTIFICATION_GROUP_SHOWN = "notification_group_shown_%s"

    // Properties

    const val APP_VERSION_STRING= "\$app_version_string"
    const val APP_BUILD_NUMBER= "\$app_build_number"
    const val OS= "\$os"
    const val MANUFACTURER= "\$manufacturer"
    const val BRAND= "\$brand"
    const val MODEL= "\$model"
    const val SS_SDK_VERSION_CODE= "\$ss_sdk_version_code"
    const val SS_SDK_VERSION= "\$ss_sdk_version"
    const val NETWORK= "\$network"
    const val CONNECTED= "\$connected"

    const val IDENTIFIED_ID = "\$identified_id"
    const val ANONYMOUS_ID = "\$anon_id"
    const val DISTINCT_ID = "distinct_id"
    const val INSERT_ID = "\$insert_id"
    const val IDENTIFY = "\$identify"
    const val TIME = "\$time"
    const val PUSH_ANDROID_TOKEN = "\$androidpush"
    const val PUSH_IOS_TOKEN = "\$iospush"
    const val PUSH_VENDOR = "\$pushvendor"
    const val PUSH_VENDOR_FCM = "fcm"
    const val PUSH_VENDOR_XIAOMI = "xiaomi"
    const val PUSH_VENDOR_APNS = "apns"
    const val DEVICE_ID = "\$device_id"
    const val SET = "\$set"
    const val SET_ONCE = "\$set_once"
    const val ADD = "\$add"
    const val APPEND = "\$append"
    const val REMOVE = "\$remove"
    const val UNSET = "\$unset"
    const val EMAIL = "\$email"
    const val SMS = "\$sms"
    const val WHATS_APP = "\$whatsapp"
    const val PREFERRED_LANGUAGE = "\$preferred_language"

    const val ENV = "env"
    const val PROPERTIES = "properties"
    const val EVENT = "event"

    // System Event
    const val S_EVENT_APP_INSTALLED = "\$app_installed"
    const val S_EVENT_APP_LAUNCHED = "\$app_launched"
    const val S_EVENT_NOTIFICATION_DELIVERED = "\$notification_delivered"
    const val S_EVENT_NOTIFICATION_CLICKED = "\$notification_clicked"
    const val S_EVENT_NOTIFICATION_DISMISS = "\$notification_dismiss"
    const val S_EVENT_USER_LOGIN = "\$user_login"
    const val S_EVENT_USER_LOGOUT = "\$user_logout"
    const val S_EVENT_PURCHASE_MADE = "\$purchase_made"
    const val S_EVENT_NOTIFICATION_SUBSCRIBED = "\$notification_subscribed"
    const val S_EVENT_NOTIFICATION_UNSUBSCRIBED = "\$notification_unsubscribed"
    const val S_EVENT_PAGE_VISITED = "\$page_visited"

    const val TAG_VALIDATION = "validation"
    const val TAG_SUPRSEND = "suprsend"

    const val SS_SHARED_PREFERENCE_KEY = "ss_pref"
    const val SP_USER_PREFERENCES = "user_preferences"
}
