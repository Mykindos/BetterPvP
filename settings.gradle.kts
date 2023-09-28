pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://papermc.io/repo/repository/maven-public/")
    }
}
rootProject.name = "BetterPvP"

include("clans")
include("core")
include("lunar")
include("champions")
include("shops")

if(File("./events/").exists()) {
    include("events")
}
