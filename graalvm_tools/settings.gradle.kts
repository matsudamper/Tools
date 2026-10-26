rootProject.name = "graalvm_tools"

pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        maven {
            url = uri("https://maven.matsudamper.net/releases")
            content {
                includeModule("net.matsudamper", "command")
            }
        }
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}