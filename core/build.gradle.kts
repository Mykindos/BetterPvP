plugins {
    id("org.flywaydb.flyway")
    id("io.papermc.paperweight.userdev")
    id("nu.studer.jooq")
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

    paperweight.paperDevBundle(libs.versions.paper)
    implementation(libs.reflections)
    implementation(libs.hikari.cp)
    implementation(libs.reflection.remapper)
    api(libs.sidebar.api)
    runtimeOnly(libs.sidebar.impl)
    runtimeOnly(libs.sidebar.packetevents)

    api(libs.jooq)
    api(libs.jooq.meta)
    jooqGenerator(libs.jooq.meta.extensions)
    jooqGenerator("org.postgresql:postgresql:42.7.4")
    implementation("org.postgresql:postgresql:42.7.4")

    api(libs.prettytime)
    api(libs.bundles.data)
    api(libs.bundles.utils)

    api(libs.mini.placeholders)
    api(libs.caffeine)

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


jooq {
    version.set("3.19.15")

    configurations {
        create("main") {
            generateSchemaSourceOnCompilation.set(false)
            jooqConfiguration.apply {
                jdbc.apply {
                    driver = "org.postgresql.Driver"
                    url = "jdbc:postgresql://localhost:5002/betterpvp"
                    user = "user"
                    password = "BetterPvP123!"
                }
                generator.apply {
                    name = "org.jooq.codegen.DefaultGenerator"
                    database.apply {
                        name = "org.jooq.meta.postgres.PostgresDatabase"
                        inputSchema = "public"
                        includes = ".*"
                        excludes = ".*_\\d+|.*_\\d+_\\d+"  // Excludes partition tables like table_0, table_1_2, etc.
                        // Force TINYINT UNSIGNED to be treated as INTEGER
                        withForcedTypes(
                            org.jooq.meta.jaxb.ForcedType().apply {
                                setName("INTEGER")
                                setIncludeTypes("SMALLINT")
                            }
                        )
                    }
                    generate.apply {
                        isDeprecated = false
                        isRecords = true
                        isImmutablePojos = false
                        isFluentSetters = true
                    }
                    target.apply {
                        packageName = "me.mykindos.betterpvp.core.database.jooq"
                        directory = "build/generated-src/jooq/main"
                    }
                }
            }
        }
    }
}
