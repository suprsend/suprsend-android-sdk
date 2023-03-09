import java.util.Locale
import org.codehaus.groovy.runtime.ProcessGroovyMethods

object Deps {

    //Sdk Details
    const val SDK_PACKAGE_NAME = "app.suprsend"
    private const val MAJOR_VERSION = 1
    private const val MINOR_VERSION = 0
    private const val PATCH_VERSION = 2
    const val SNAPSHOT = -1
    val BUILD_TYPE = BuildType.NATIVE
    private const val ISPROD = false
    const val RUN_LIB = false

    const val SDK_VERSION_CODE = MAJOR_VERSION * 1000 + (MINOR_VERSION * 100) + PATCH_VERSION
    var SDK_VERSION_NAME = if (SNAPSHOT != -1) {
        if (RUN_LIB)
            "$MAJOR_VERSION.${MINOR_VERSION}.${PATCH_VERSION}.${SNAPSHOT}" + "-LOCAL"
        else
            "$MAJOR_VERSION.${MINOR_VERSION}.${PATCH_VERSION}.${SNAPSHOT}" + "-SNAPSHOT"
    } else
        "$MAJOR_VERSION.${MINOR_VERSION}.${PATCH_VERSION}"

    //App Details
    var APP_VERSION_CODE = (10000 * MAJOR_VERSION) + (1000 * MINOR_VERSION) + (100 * PATCH_VERSION) + SNAPSHOT

    var APP_VERSION_NAME = if (ISPROD) "$SDK_VERSION_NAME-Prod" else "$SDK_VERSION_NAME-Stag"

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
        const val targetSdk = 33
        const val compileSdk = 33
        const val buildToolsVersion = "33.0.0"
    }

    object Publication {
        const val GROUP = "com.suprsend"
        var VERSION = SDK_VERSION_NAME

        const val PUBLISH_GROUP_ID = "com.suprsend"
        var PUBLISH_ARTIFACT_ID = BUILD_TYPE.name.toLowerCase(Locale.getDefault())
        var PUBLISH_ARTIFACT_VERSION = SDK_VERSION_NAME
        const val POM_NAME = "suprsend"
        var POM_DESCRIPTION = "Suprsend Android SDK release from commit id : ${"git rev-parse HEAD".execute().text().trim()}"
        const val POM_URL = "https://github.com/suprsend/suprsend-android-sdk"
        const val POM_LICENCE_NAME = "The Apache Software License, Version 2.0"
        const val POM_LICENCE_URL = "http://www.apache.org/licenses/LICENSE-2.0.txt"
        const val POM_DEVELOPER_NAME = "SuprSend Team"
        const val POM_DEVELOPER_EMAIL = "developers@suprsend.com"
        const val POM_SCM_CONNECTION = "scm:git@github.com:suprsend/suprsend-android-sdk.git"
        const val POM_SCM_DEV_CONNECTION = "scm:git@github.com:suprsend/suprsend-android-sdk.git"
        const val POM_SCM_URL = "https://github.com/suprsend/suprsend-android-sdk"

        const val OSSRH_USERNAME = "XXXX"
        const val OSSRH_PASSWORD = "XXXX"
    }

    object JetBrains {
        object Kotlin {
            const val VERSION = "1.3.72"
        }
    }
    object Firebase {
        const val messaging = "20.2.4"
    }
    const val material = "1.4.0"
}

fun String.execute(): Process = ProcessGroovyMethods.execute(this)
fun Process.text(): String = ProcessGroovyMethods.getText(this)
