plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}
apply {
    from("$rootDir/ktlint.gradle")
}
android {

    compileSdkVersion(Deps.Android.compileSdk)
    buildToolsVersion(Deps.Android.buildToolsVersion)

    defaultConfig {
        applicationId = "${Deps.SDK_PACKAGE_NAME}.android"
        minSdkVersion(Deps.Android.minSdk)
        targetSdkVersion(Deps.Android.targetSdk)
        versionCode = Deps.APP_VERSION_CODE
        versionName = Deps.APP_VERSION_NAME
        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        getByName("debug") {
            storeFile = file("../debug.jks")
            storePassword = "debugdebug"
            keyAlias = "debug"
            keyPassword = "debugdebug"
        }
        create("release") {
            storeFile = file("../debug.jks")
            storePassword = "debugdebug"
            keyAlias = "debug"
            keyPassword = "debugdebug"
        }
    }
    buildFeatures {
        dataBinding = true
    }

    buildTypes {
        getByName("debug") {
            addBuildConfigFields()
            versionNameSuffix = "(d)"
            isDebuggable = true
            isCrunchPngs = false
            isMinifyEnabled = false
        }
        getByName("release") {
            addBuildConfigFields()
            signingConfig = signingConfigs.getByName("release")
            isDebuggable = false
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
//    packagingOptions {
//        exclude("META-INF/ktor-client-core.kotlin_module")
//        exclude("META-INF/ktor-io.kotlin_module")
//        exclude("META-INF/ktor-http.kotlin_module")
//        exclude("META-INF/ktor-http-cio.kotlin_module")
//        exclude("META-INF/ktor-utils.kotlin_module")
//        exclude("META-INF/ktor-client-serialization.kotlin_module")
//        exclude("META-INF/ktor-client-logging.kotlin_module")
//        exclude("META-INF/ktor-client-json.kotlin_module")
//        exclude("META-INF/kotlinx-serialization-runtime.kotlin_module")
//    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))

    implementation("org.jetbrains.kotlin:kotlin-stdlib:${Deps.JetBrains.Kotlin.VERSION}")
//    implementation(Deps.AndroidX.CORE_KTX)
    implementation("androidx.appcompat:appcompat:1.3.1")
    implementation("com.google.android.material:material:${Deps.material}")
    implementation("androidx.constraintlayout:constraintlayout:2.1.0")
    implementation("com.google.firebase:firebase-crashlytics:18.2.1")

    if (Deps.RUN_LIB) {
        implementation(project(":library"))
        println("Using shared library")
    } else {
        val dependency = "${Deps.Publication.GROUP}:${Deps.Publication.PUBLISH_ARTIFACT_ID}:${Deps.Publication.VERSION}"
        implementation(dependency)
        println("Using remote library - $dependency")
    }

//    implementation(files("libs/library-debug.aar"))
    implementation("com.google.firebase:firebase-messaging:${Deps.Firebase.messaging}")

    implementation("com.mixpanel.android:mixpanel-android:5.9.1")

    implementation("com.github.bumptech.glide:glide:4.12.0")
    kapt("com.github.bumptech.glide:compiler:4.12.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.7")
    implementation("com.github.angads25:toggle:1.1.0")
    implementation ("io.noties.markwon:core:4.6.2")
    implementation ("io.noties.markwon:html:4.6.2")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}

fun com.android.build.gradle.internal.dsl.BuildType.addBuildConfigFields() {
    buildConfigField("String", "XIAOMI_APP_ID", "\"${Deps.XIAOMI_APP_ID}\"")
    buildConfigField("String", "XIAOMI_APP_KEY", "\"${Deps.XIAOMI_APP_KEY}\"")
    buildConfigField("String", "SS_BASE_URL", "\"${Deps.SS_BASE_URL}\"")
    buildConfigField("String", "SS_INBOX_BASE_URL", "\"${Deps.SS_INBOX_BASE_URL}\"")
    buildConfigField("String", "SS_INBOX_SOCKET_URL", "\"${Deps.SS_INBOX_SOCKET_URL}\"")
    buildConfigField("String", "SS_INBOX_SUBSCRIBER_ID", "\"${Deps.SS_INBOX_SUBSCRIBER_ID}\"")
    buildConfigField("String", "SS_TOKEN", "\"${Deps.SS_TOKEN}\"")
    buildConfigField("String", "SS_SECRET", "\"${Deps.SS_SECRET}\"")
    buildConfigField("String", "SS_TENANT_ID", "\"${Deps.SS_TENANT_ID}\"")
    buildConfigField("String", "MX_TOKEN", "\"${Deps.MX_TOKEN}\"")
}