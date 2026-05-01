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
        // এই লাইনটি অবশ্যই লাগবে
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "NeoConnect"
include(":app")