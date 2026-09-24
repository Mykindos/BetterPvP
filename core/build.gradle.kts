import org.gradle.api.artifacts.result.ResolvedDependencyResult

plugins {
    id("org.flywaydb.flyway")
    id("io.papermc.paperweight.userdev")
    `maven-publish`
    id("jooqdynamic")
    id("me.champeau.jmh")
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
    implementation(project(":orchestration"))
    implementation(libs.reflections)
    implementation(libs.hikari.cp)
    implementation(libs.jedis)
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
    compileOnly(libs.oraxen)

    compileOnly(libs.lombok)
    compileOnly(libs.mythic)
    compileOnly(libs.mapper)
    compileOnly(libs.modelengine)
    compileOnly(libs.packetevents)
    compileOnly(libs.packetevents)
    compileOnly(libs.bundles.paper)
    compileOnly(libs.fawe)
    compileOnly(libs.fawebukkit) {
        exclude(group = "org.lz4", module = "lz4-java")
    }

    annotationProcessor(libs.lombok)
    testImplementation(libs.bundles.test)
    // Mapper is compileOnly for the plugin (it is a server dependency), but the schematic tests build real regions.
    testImplementation(libs.mapper)
    testImplementation("org.mockito:mockito-core:5.23.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.23.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Libraries Paper downloads at startup (see CoreLoader) instead of shading them into the jar
val paperLibrary: Configuration by configurations.creating {
    isTransitive = false
    shouldResolveConsistentlyWith(configurations.compileClasspath.get())
}

dependencies {
    paperLibrary(libs.jooq)
    paperLibrary(libs.jooq.meta)
    paperLibrary(libs.jackson.core)
    paperLibrary(libs.jackson.annotations)
    paperLibrary(libs.jackson.databind)
    paperLibrary(libs.guice)
    paperLibrary(libs.commons.text)
    paperLibrary(libs.commons.math3)
    paperLibrary(libs.jexl3)
    paperLibrary(libs.okhttp)
    paperLibrary(libs.prettytime)
    paperLibrary(libs.zip4j)
    paperLibrary(libs.json)
}

// The plugin jar's classpath. Kept apart from runtimeClasspath so tests and benchmarks still get every library.
val shadedRuntimeClasspath: Configuration by configurations.creating {
    isCanBeConsumed = false
    extendsFrom(configurations.implementation.get(), configurations.runtimeOnly.get())
    attributes.addAllLater(configurations.runtimeClasspath.get().attributes)
    paperLibrary.dependencies.forEach { exclude(group = it.group!!, module = it.name) }
    // Provided by the server, which plugin classloaders always ask first
    exclude(group = "com.google.guava")
    exclude(group = "com.google.code.gson")
}

tasks.shadowJar {
    configurations.set(listOf(shadedRuntimeClasspath))
}

val generatePaperLibraries by tasks.registering {
    val coordinates = paperLibrary.incoming.resolutionResult.rootComponent.map { root ->
        root.dependencies.filterIsInstance<ResolvedDependencyResult>()
            .filterNot { it.isConstraint }
            .map { it.selected.moduleVersion.toString() }
            .sorted()
    }
    val outputDir = layout.buildDirectory.dir("generated/paper-libraries")
    inputs.property("coordinates", coordinates)
    outputs.dir(outputDir)
    doLast {
        outputDir.get().file("paper-libraries.txt").asFile.writeText(coordinates.get().joinToString("\n"))
    }
}

sourceSets.main {
    resources.srcDir(generatePaperLibraries)
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
