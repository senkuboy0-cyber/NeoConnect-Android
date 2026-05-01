pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // এই লাইনটি যোগ করতে হবে WebRTC এর জন্য
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "NeoConnect"
include(":app")