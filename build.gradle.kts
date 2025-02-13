// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        google()
        mavenCentral()
        maven(url = "https://kotlin.bintray.com/kotlinx")
        maven(url = "https://maven.google.com")
        maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.8.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Deps.JetBrains.Kotlin.VERSION}")
        classpath ("com.google.gms:google-services:4.4.2")
        classpath ("com.google.firebase:firebase-crashlytics-gradle:3.0.2")
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
//        jcenter() // Warning: this repository is going to shut down soon
        maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
}

tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}