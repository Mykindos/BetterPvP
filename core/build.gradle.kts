plugins {
    id("org.flywaydb.flyway")
    id("io.papermc.paperweight.userdev")
    `maven-publish`
}

version = "1.0.0"
group = "me.mykindos.betterpvp.core"
description = "Core plugin for BetterPvP"

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "me.mykindos.betterpvp"
            artifactId = "core"
            version = "1.0"
            from(components["java"])
        }
    }
}

dependencies {
    compileOnly(libs.mythic)
    compileOnly(libs.modelengine)
    compileOnly(libs.protocollib)
    compileOnly(libs.bundles.paper)
    paperweight.paperDevBundle(libs.versions.paper)
    implementation(libs.reflections)
    implementation(libs.hikari.cp)
    implementation(libs.reflection.remapper)
    compileOnly(libs.mineplex)

    api(libs.prettytime)
    api(libs.bundles.data)
    api(libs.bundles.utils)
    api(libs.sidebar)
    api(libs.mini.placeholders)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testImplementation(libs.bundles.test)
}

paperweight {
    reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION
}