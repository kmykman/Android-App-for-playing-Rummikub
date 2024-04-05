pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://chaquo.com/maven") }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "Rummikub"
include(":app")
include(":libBT")
project(":libBT").projectDir = File(".\\libBT")
include(":openCV")
project(":openCV").projectDir = File(".\\opencv")