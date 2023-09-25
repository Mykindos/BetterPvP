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
include("progression")
include("progression")

if(File("./events/").exists()) {
    include("events")
}
