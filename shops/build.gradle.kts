plugins {
    id("org.flywaydb.flyway")
    id("io.papermc.paperweight.userdev")
}

version = "1.0.0"
group = "me.mykindos.betterpvp.shops"
description = "Shops plugin for BetterPvP"

dependencies {
    compileOnly(libs.bundles.paper)
    paperweight.paperDevBundle(libs.versions.paper)
    implementation(libs.reflections)

    compileOnly(project(":core"))
    compileOnly(libs.libsdisguises)
    compileOnly(libs.mythic)

    annotationProcessor(libs.lombok)
    compileOnly(libs.lombok)
}

paperweight {
    reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION
}