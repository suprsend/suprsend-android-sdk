plugins {
    id("com.android.library")
    id("kotlin-android")
    id("maven-publish")
}

android {
    compileSdkVersion(Deps.Android.compileSdk)
    buildToolsVersion(Deps.Android.buildToolsVersion)

    defaultConfig {
        minSdkVersion(Deps.Android.minSdk)
        targetSdkVersion(Deps.Android.targetSdk)
        versionCode = Deps.APP_VERSION_CODE
        versionName = Deps.APP_VERSION_NAME

        buildConfigField("String", "SS_SDK_VERSION_CODE", "\"${Deps.SDK_VERSION_CODE}\"")
        buildConfigField("String", "SS_SDK_VERSION_NAME", "\"${Deps.SDK_VERSION_NAME}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        getByName("release") {
            minifyEnabled(false)
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
}

dependencies {

    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.3.72")
    implementation("com.google.firebase:firebase-messaging:20.2.4")
    implementation(files("libs/MiPush_SDK_Client_4_8_3.jar"))
    implementation("com.googlecode.libphonenumber:libphonenumber:8.12.38")

    testImplementation("junit:junit:4.+")

    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")

}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                // Configure the publication here
                artifactId = Deps.Publication.ARTIFACT_ID
                group = Deps.Publication.GROUP
                version = Deps.Publication.VERSION
                from(components["release"])
            }
            repositories {
                maven {
                    url = uri("https://jitpack.io")
                    credentials {
                        username = Deps.JITPACK_TOKEN
                    }
                }
            }
        }
    }
}