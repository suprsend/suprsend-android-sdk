import java.io.File
import java.security.MessageDigest

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
    // Task to create sources JAR
    val sourcesJar by tasks.creating(Jar::class) {
        archiveClassifier.set("sources")
        from(android.sourceSets.getByName("main").java.srcDirs)
        // Kotlin sources are included in java.srcDirs for Android projects
    }

    // Task to create javadoc JAR (empty for Android/Kotlin, but required by Maven Central)
    val javadocJar by tasks.creating(Jar::class) {
        archiveClassifier.set("javadoc")
        // For Android/Kotlin libraries, javadoc might be empty or minimal
        // You can add actual javadoc generation here if needed
    }
    
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

                // Add sources and javadoc artifacts
                artifact(sourcesJar)
                artifact(javadocJar)

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
        // The repository to publish to, Maven Central Portal (using new staging API)
        repositories {
            maven {
                name = "mavencentral"
                url = uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
                credentials {
                    username = project.findProperty("mavenCentralUsername") as String?
                        ?: throw GradleException("mavenCentralUsername not found in gradle.properties")
                    password = project.findProperty("mavenCentralPassword") as String?
                        ?: throw GradleException("mavenCentralPassword not found in gradle.properties")
                }
            }
        }
    }
    
    signing {
        sign(publishing.publications) // picks from gradle.properties signing.keyId, signing.password, signing.secretKeyRingFile
    }
    
    // Task to print publication details for bundle script
    tasks.register("printPublicationInfo") {
        doLast {
            val publication = publishing.publications.getByName("release") as org.gradle.api.publish.maven.MavenPublication
            println("GROUP_ID=${publication.groupId}")
            println("ARTIFACT_ID=${publication.artifactId}")
            println("VERSION=${publication.version}")
        }
    }
    
    // Generate all checksums (MD5, SHA1, SHA256, SHA512) for published artifacts and .asc files
    tasks.named("publishToMavenLocal").configure {
        doLast {
            val publication = publishing.publications.getByName("release") as org.gradle.api.publish.maven.MavenPublication
            val groupId = publication.groupId.replace(".", "/")
            val artifactId = publication.artifactId
            val version = publication.version
            val baseDir = File(System.getProperty("user.home"), ".m2/repository/$groupId/$artifactId/$version")
            
            fun generateChecksums(file: File, fileName: String) {
                val fileBytes = file.readBytes()
                val checksums = mapOf(
                    "md5" to MessageDigest.getInstance("MD5"),
                    "sha1" to MessageDigest.getInstance("SHA-1"),
                    "sha256" to MessageDigest.getInstance("SHA-256"),
                    "sha512" to MessageDigest.getInstance("SHA-512")
                )
                
                checksums.forEach { entry ->
                    val algorithm = entry.key
                    val digest = entry.value
                    digest.update(fileBytes)
                    val hash = digest.digest().joinToString("") { byte ->
                        val byteValue = byte.toInt() and 0xff
                        String.format("%02x", byteValue)
                    }
                    val checksumFile = File(baseDir, "$fileName.$algorithm")
                    checksumFile.writeText(hash)
                }
            }
            
            if (baseDir.exists()) {
                val files = baseDir.listFiles()
                if (files != null) {
                    for (file in files) {
                        val fileName = file.name
                        if (file.isFile &&
                            !fileName.endsWith(".md5") && 
                            !fileName.endsWith(".sha1") && 
                            !fileName.endsWith(".sha256") && 
                            !fileName.endsWith(".sha512")) {
                            generateChecksums(file, fileName)
                        }
                    }
                }
            }
        }
    }
}