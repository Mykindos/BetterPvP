plugins {
    id("org.flywaydb.flyway")
    id("io.papermc.paperweight.userdev")
}

version = "1.0.0"
group = "me.mykindos.betterpvp.champions"
description = "Champions plugin for BetterPvP"

dependencies {
    compileOnly(libs.bundles.paper)
    paperweight.paperDevBundle(libs.versions.paper)
    implementation(libs.reflections)

    compileOnly(libs.libsdisguises)
    compileOnly(project(":core"))
    compileOnly(project(":progression"))
    compileOnly(libs.protocollib)

    annotationProcessor(libs.lombok)
    compileOnly(libs.lombok)
}

paperweight {
    reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION
}