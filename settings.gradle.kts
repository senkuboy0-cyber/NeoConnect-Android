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
        // JitPack লাগবে না, কারণ stream-webrtc-android Maven Central এ আছে
    }
}

rootProject.name = "NeoConnect"
include(":app")