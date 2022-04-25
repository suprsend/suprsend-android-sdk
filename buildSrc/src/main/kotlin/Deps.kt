object Deps {

    const val MAJOR_VERSION = 0
    const val MINOR_VERSION = 1

    //Sdk Details
    const val SDK_PACKAGE_NAME = "app.suprsend"
    const val SDK_VERSION_CODE = 10
    const val SDK_BETA = 10
    const val SDK_VERSION_NAME = "$MAJOR_VERSION.${MINOR_VERSION}Beta$SDK_BETA"

    //App Details
    const val APP_BETA = 13
    const val APP_VERSION_CODE = 13
    const val ISPROD = false
    const val RUN_LIB = false

    var APP_VERSION_NAME = if (ISPROD) {
        "$SDK_VERSION_NAME Prod B$APP_BETA"
    } else {
        "$SDK_VERSION_NAME Stag B$APP_BETA"
    }

    //Prod
    var SS_API_BASE_URL = if (ISPROD) "https://hub.suprsend.com" else "https://collector-staging.suprsend.workers.dev"

    var SS_TOKEN = "XXXX"
    var SS_SECRET = "XXXX"

    const val XIAOMI_APP_ID = "XXXX"
    const val XIAOMI_APP_KEY = "XXXX"

    const val OPPO_APP_KEY = "XXXX"
    const val OPPO_APP_SECRET = "XXXX"

    //Todo : Add jitpack publishing
    const val MX_TOKEN = "XXXX"
    const val JITPACK_TOKEN = "XXXX"

    object Android {
        const val minSdk = 19
        const val targetSdk = 32
        const val compileSdk = 32
        const val buildToolsVersion = "32.0.0"
    }

    object Publication {
        const val ARTIFACT_ID = "suprsend-android-sdk"
        const val GROUP = "com.github.suprsend"
        const val VERSION = SDK_VERSION_NAME
    }

    object JetBrains {
        object Kotlin {
            const val VERSION = "1.3.72"
        }
    }
}