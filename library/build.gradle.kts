import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.vanniktech.maven.publish")
}

android {
    namespace = "app.suprsend"
    compileSdk = Deps.Android.compileSdk

    defaultConfig {
        minSdk = Deps.Android.minSdk

        buildConfigField("String", "SS_SDK_VERSION_CODE", "\"${Deps.SDK_VERSION_CODE}\"")
        buildConfigField("String", "SS_SDK_VERSION_NAME", "\"${Deps.SDK_VERSION_NAME}\"")
        buildConfigField("String", "SS_SDK_TYPE", "\"${Deps.BUILD_TYPE.name}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
        apiVersion = "1.4" // Can only use stdlib APIs from Kotlin 1.4
        languageVersion = "1.4" // Can only use language features from Kotlin 1.4
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    compileOnly(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar","*.aar"))))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${Deps.JetBrains.Kotlin.VERSION}")
    implementation("com.google.firebase:firebase-messaging:${Deps.Firebase.messaging}")
    implementation("com.googlecode.libphonenumber:libphonenumber:8.12.38")
    implementation("androidx.core:core:1.1.0")
    implementation ("io.socket:socket.io-client:2.0.0") {
        exclude("org.json","json")
    }

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20230227")
    testImplementation("org.robolectric:robolectric:4.10.3")

    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")

}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    coordinates(
        groupId = Deps.Publication.PUBLISH_GROUP_ID,
        artifactId = Deps.Publication.PUBLISH_ARTIFACT_ID,
        version = Deps.Publication.PUBLISH_ARTIFACT_VERSION
    )

    pom {
        name.set(Deps.Publication.POM_NAME)
        description.set(Deps.Publication.POM_DESCRIPTION)
        url.set(Deps.Publication.POM_URL)

        licenses {
            license {
                name.set(Deps.Publication.POM_LICENCE_NAME)
                url.set(Deps.Publication.POM_LICENCE_URL)
            }
        }
        developers {
            developer {
                name.set(Deps.Publication.POM_DEVELOPER_NAME)
                email.set(Deps.Publication.POM_DEVELOPER_EMAIL)
            }
        }
        scm {
            connection.set(Deps.Publication.POM_SCM_CONNECTION)
            developerConnection.set(Deps.Publication.POM_SCM_DEV_CONNECTION)
            url.set(Deps.Publication.POM_SCM_URL)
        }
    }
}