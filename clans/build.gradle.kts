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
    compileOnly(libs.mapper)
    compileOnly(libs.packetevents)
    compileOnly(libs.nexo)
    compileOnly(libs.modelengine)

    annotationProcessor(libs.lombok)
    compileOnly(libs.lombok)

    testImplementation(libs.bundles.test)
    testImplementation(project(":core"))
    testImplementation(libs.mapper)
    testImplementation("org.mockito:mockito-core:5.23.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.23.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

paperweight {
    reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION
}

// jooqdynamic excludes org.jooq from this module's runtimeOnly/runtimeClasspath (it ships only via :core's shaded
// jar at real server runtime), and that exclusion also taints testRuntimeClasspath since it extends testRuntimeOnly
// -> runtimeOnly. Tests that touch classes referencing jOOQ types (e.g. IslandInstanceRepository) still need those
// classes to *load*, so they are resolved into a detached configuration and appended straight to the test task's
// classpath, bypassing the excluding hierarchy entirely.
val jooqTestRuntime: Configuration = configurations.detachedConfiguration(
    project.dependencies.create(
        extensions.getByType<VersionCatalogsExtension>().named("libs")
            .findLibrary("jooq").orElseThrow().get().toString()))

tasks.test {
    classpath += jooqTestRuntime
}
