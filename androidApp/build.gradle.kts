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
            buildConfigField("String", "XIAOMI_APP_ID", "\"${Deps.XIAOMI_APP_ID}\"")
            buildConfigField("String", "XIAOMI_APP_KEY", "\"${Deps.XIAOMI_APP_KEY}\"")
            buildConfigField("String", "SS_TOKEN", "\"${Deps.SS_TOKEN}\"")
            buildConfigField("String", "SS_SECRET", "\"${Deps.SS_SECRET}\"")
            buildConfigField("String", "MX_TOKEN", "\"${Deps.MX_TOKEN}\"")
            buildConfigField("String", "SS_API_BASE_URL", "\"${Deps.SS_API_BASE_URL}\"")
            versionNameSuffix = "(d)"
            isDebuggable = true
            isCrunchPngs = false
            isMinifyEnabled = false
        }
        getByName("release") {
            buildConfigField("String", "XIAOMI_APP_ID", "\"${Deps.XIAOMI_APP_ID}\"")
            buildConfigField("String", "XIAOMI_APP_KEY", "\"${Deps.XIAOMI_APP_KEY}\"")
            buildConfigField("String", "SS_TOKEN", "\"${Deps.SS_TOKEN}\"")
            buildConfigField("String", "SS_SECRET", "\"${Deps.SS_SECRET}\"")
            buildConfigField("String", "MX_TOKEN", "\"${Deps.MX_TOKEN}\"")
            buildConfigField("String", "SS_API_BASE_URL", "\"${Deps.SS_API_BASE_URL}\"")
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
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${Deps.JetBrains.Kotlin.VERSION}")
//    implementation(Deps.AndroidX.CORE_KTX)
    implementation("androidx.appcompat:appcompat:1.3.1")
    implementation("com.google.android.material:material:1.4.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.0")
    implementation("com.google.firebase:firebase-crashlytics:18.2.1")

    if (Deps.RUN_LIB) {
        implementation(project(":library"))
        println("Using shared library")
    }else{
        implementation("${Deps.Publication.GROUP}:${Deps.Publication.ARTIFACT_ID}:${Deps.Publication.VERSION}")
        println("Using remote library")
    }

//    implementation(files("libs/library-debug.aar"))
    implementation("com.google.firebase:firebase-messaging:20.2.4")


    implementation("com.mixpanel.android:mixpanel-android:5.9.1")

    implementation("com.github.bumptech.glide:glide:4.12.0")
    kapt("com.github.bumptech.glide:compiler:4.12.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}