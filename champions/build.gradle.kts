plugins {
    id("org.flywaydb.flyway")
    id("io.papermc.paperweight.userdev")
    id("jooqdynamic")
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
    compileOnly(project(":shops"))
    compileOnly(libs.packetevents)
    compileOnly(libs.modelengine)

    annotationProcessor(libs.lombok)
    compileOnly(libs.lombok)

    testImplementation(libs.bundles.test)
    testImplementation(project(":core"))
    testImplementation("org.mockito:mockito-core:5.23.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

paperweight {
    reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION
}