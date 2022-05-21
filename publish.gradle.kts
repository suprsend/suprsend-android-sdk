plugins {
    id("kotlin-android")
    id("maven-publish")
    id("signing")
}

//publishing {
//    publications {
//        // Creates a Maven publication called "release".
//        create<MavenPublication>("release") {
//            // Applies the component for the release build variant.
//            from(components["release"])
//
//            // You can then customize attributes of the publication as shown below.
//            groupId = Deps.PUBLISH_GROUP_ID
//            artifactId = Deps.PUBLISH_ARTIFACT_ID
//            version = Deps.PUBLISH_ARTIFACT_VERSION
//
////                artifact sourcesJar
//
//            pom {
//                name.set(Deps.POM_NAME)
//                description.set(Deps.POM_DESCRIPTION)
//                url.set(Deps.POM_URL)
//                setPackaging("aar")
//                licenses {
//                    license {
//                        name = POM_LICENCE_NAME
//                        url = POM_LICENCE_URL
//                    }
//                }
//                developers {
//                    developer {
//                        id = POM_DEVELOPER_ID
//                        name = POM_DEVELOPER_NAME
//                        email = POM_DEVELOPER_EMAIL
//                    }
//                    // Add all other devs here...
//                }
//                // Version control info - if you're using GitHub, follow the format as seen here
//                scm {
//                    connection = POM_SCM_CONNECTION
//                    developerConnection = POM_SCM_DEV_CONNECTION
//                    url = POM_SCM_URL
//                }
//            }
//        }
//    }
//    // The repository to publish to, Sonatype/MavenCentral
//    repositories {
//        maven {
//            // This is an arbitrary name, you may also use "mavencentral" or
//            // any other name that's descriptive for you
//            name = 'mavencentral'
//            url = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
////                url = "https://s01.oss.sonatype.org/content/repositories/snapshots/"
//            credentials {
//                username = Deps.OSSRH_USERNAME
//                password = Deps.OSSRH_PASSWORD
//            }
//        }
//    }
//}
//
//signing {
//    sign(publishing.publications)
//}


afterEvaluate {
    publishing {
        publications {
            // Creates a Maven publication called "release".
            create<MavenPublication>("release") {
                // Applies the component for the release build variant.
                from(components["release"])

                // You can then customize attributes of the publication as shown below.
                groupId = Deps.PUBLISH_GROUP_ID
                artifactId = Deps.PUBLISH_ARTIFACT_ID
                version = Deps.PUBLISH_ARTIFACT_VERSION

//                artifact sourcesJar

                pom {
                    name.set(Deps.POM_NAME)
                    description.set(Deps.POM_DESCRIPTION)
                    url.set(Deps.POM_URL)
                    setPackaging("aar")
                    licenses {
                        license {
                            name.set(Deps.POM_LICENCE_NAME)
                            url.set(Deps.POM_LICENCE_URL)
                        }
                    }
                    developers {
                        developer {
                            id.set(Deps.POM_DEVELOPER_ID)
                            name.set(Deps.POM_DEVELOPER_NAME)
                            email.set(Deps.POM_DEVELOPER_EMAIL)
                        }
                        // Add all other devs here...
                    }
                    // Version control info - if you're using GitHub, follow the format as seen here
                    scm {
                        connection.set(Deps.POM_SCM_CONNECTION)
                        developerConnection.set(Deps.POM_SCM_DEV_CONNECTION)
                        url.set(Deps.POM_SCM_URL)
                    }
                }
            }
        }
        // The repository to publish to, Sonatype/MavenCentral
        repositories {
            maven {
                // This is an arbitrary name, you may also use "mavencentral" or
                // any other name that's descriptive for you
                name = "mavencentral"
                setUrl("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
//                url = "https://s01.oss.sonatype.org/content/repositories/snapshots/"
                credentials {
                    username = Deps.OSSRH_USERNAME
                    password = Deps.OSSRH_PASSWORD
                }
            }
        }
    }
}
signing {
    sign(publishing.publications)
}
