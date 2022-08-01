pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://papermc.io/repo/repository/maven-public/")
    }
}


rootProject.name = "BetterPvP"

include("clans")
include("core")

