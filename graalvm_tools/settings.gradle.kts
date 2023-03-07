rootProject.name = "graalvm_tools"

pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        maven {
            url = uri("https://maven.pkg.github.com/matsudamper/kotlin-shell-native")
            val properties = java.util.Properties()

            try {
                properties.load(java.io.FileInputStream(File(rootProject.projectDir, "local.properties")))
            } catch (_: java.io.IOException) {
            }
            val propertyToken = properties["GITHUB_TOKEN"]?.toString()

            credentials {
                username = "matsudamper"
                password = if (propertyToken == null || propertyToken == "") {
                    System.getenv("GITHUB_TOKEN")
                } else {
                    propertyToken
                }
            }
            content {
                includeModule("net.matsudamper", "command")
            }
        }
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}