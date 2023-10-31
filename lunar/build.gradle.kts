plugins {
    id("io.papermc.paperweight.userdev")
}

version = "1.0.0"
group = "me.mykindos.betterpvp.lunar"
description = "Lunar Client API-accessor plugin for BetterPvP"

repositories {
    maven { url = uri("https://repo.lunarclient.dev") }
}

dependencies {
    compileOnly(libs.bundles.paper)
    paperweight.paperDevBundle(libs.versions.paper)
    implementation(libs.reflections)

    compileOnly("com.lunarclient:apollo-api:1.0.0")
    compileOnly(libs.libsdisguises)
    compileOnly(libs.mythic)
    compileOnly(project(":core"))

    annotationProcessor(libs.lombok)
    compileOnly(libs.lombok)
}