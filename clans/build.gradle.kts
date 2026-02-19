project.ext["jooqPackageName"] = "me.mykindos.betterpvp.clans.database.jooq"
project.ext["jooqOutputDir"] = "src/main/java"

plugins {
    id("org.flywaydb.flyway")
    id("io.papermc.paperweight.userdev")
    id("jooqdynamic")
}

version = "1.0.0"
group = "me.mykindos.betterpvp.clans"
description = "Clans plugin for BetterPvP"

dependencies {
    compileOnly(libs.bundles.paper)
    paperweight.paperDevBundle(libs.versions.paper)
    implementation(libs.reflections)

    compileOnly(project(":core"))
    compileOnly(project(":progression"))
    compileOnly(project(":champions"))
    compileOnly(project(":shops"))
    compileOnly(project(":private:dungeons"))
    compileOnly(libs.mineplex)
    compileOnly(libs.packetevents)
    compileOnly(libs.modelengine)

    annotationProcessor(libs.lombok)
    compileOnly(libs.lombok)
}

paperweight {
    reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION
}