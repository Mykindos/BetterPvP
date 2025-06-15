plugins {
    id("org.flywaydb.flyway")
    id("io.papermc.paperweight.userdev")
    `maven-publish`
    id("jooqdynamic")
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

    paperweight.paperDevBundle(libs.versions.paper)
    implementation(libs.reflections)
    implementation(libs.hikari.cp)
    implementation(libs.reflection.remapper)
    api(libs.sidebar.api)
    runtimeOnly(libs.sidebar.impl)
    runtimeOnly(libs.sidebar.packetevents)

    api(libs.prettytime)
    api(libs.bundles.data)
    api(libs.bundles.utils)

    api(libs.mini.placeholders)
    api(libs.caffeine)
    compileOnly(libs.nexo)

    compileOnly(libs.mineplex)
    compileOnly(libs.lombok)
    compileOnly(libs.mythic)
    compileOnly(libs.modelengine)
    compileOnly(libs.protocollib)
    compileOnly(libs.bundles.paper)

    annotationProcessor(libs.lombok)
    testImplementation(libs.bundles.test)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

paperweight {
    reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION
    addServerDependencyTo = configurations.named(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME).map { setOf(it) }
}

publishing {
    publications {
        create<MavenPublication>("publishCore") {
            groupId = "me.mykindos.betterpvp"
            artifactId = "core"
            version = "1.0"
            artifact(tasks.getByName("jar"))
        }
    }
}