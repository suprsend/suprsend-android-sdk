plugins {
    id("com.android.library")
    id("kotlin-android")
    id("maven-publish")
    id("signing")
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    testOptions{
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

afterEvaluate {
    publishing {
        publications {
            // Creates a Maven publication called "release".
            create<MavenPublication>("release") {
                // Applies the component for the release build variant.
                from(components["release"])

                // You can then customize attributes of the publication as shown below.
                groupId = Deps.Publication.PUBLISH_GROUP_ID
                artifactId = Deps.Publication.PUBLISH_ARTIFACT_ID
                version = Deps.Publication.PUBLISH_ARTIFACT_VERSION

//                artifact sourcesJar

                pom {
                    name.set(Deps.Publication.POM_NAME)
                    description.set(Deps.Publication.POM_DESCRIPTION)
                    url.set(Deps.Publication.POM_URL)
                    setPackaging("aar")
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
                        // Add all other devs here...
                    }
                    // Version control info - if you're using GitHub, follow the format as seen here
                    scm {
                        connection.set(Deps.Publication.POM_SCM_CONNECTION)
                        developerConnection.set(Deps.Publication.POM_SCM_DEV_CONNECTION)
                        url.set(Deps.Publication.POM_SCM_URL)
                    }
                }
            }
        }
        // The repository to publish to, Sonatype/MavenCentral
        repositories {
            maven {
                name = "mavencentral"
                if(Deps.SNAPSHOT != -1){
                    setUrl("https://s01.oss.sonatype.org/content/repositories/snapshots/")
                }else{
                    setUrl("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                }
                credentials {
                    username = Deps.Publication.OSSRH_USERNAME
                    password = Deps.Publication.OSSRH_PASSWORD
                }
            }
        }
    }
}
signing {
    sign(publishing.publications)
}


//apply {
//    from("$rootDir/publish.gradle")
//}