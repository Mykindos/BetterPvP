pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://papermc.io/repo/repository/maven-public/")
    }
}

var enableEventsModule = File("./events/").exists()

rootProject.name = "BetterPvP"

include("clans")
include("core")
include("lunar")
include("champions")
include("shops")

if(enableEventsModule) {
    include("events")
}
