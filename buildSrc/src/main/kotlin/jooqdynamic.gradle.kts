plugins {
    id("nu.studer.jooq")
}

val jooqVersion = "3.19.15"

// Extract tables from migrations
val migrationsDir = file("src/main/resources/${project.name}-migrations/postgres")
val extractedTables = SqlMigrationParser.extractTableNames(migrationsDir)
val dynamicIncludes = SqlMigrationParser.generateIncludes(extractedTables)

println("jOOQ: Extracted tables for ${project.name}: $extractedTables")

dependencies {
    // Get the version catalog
    val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")

    add("api", libs.findLibrary("jooq").get())
    add("api", libs.findLibrary("jooq.meta").get())
    add("jooqGenerator", libs.findLibrary("jooq.meta.extensions").get())
    add("jooqGenerator", "org.postgresql:postgresql:42.7.4")
    add("implementation", "org.postgresql:postgresql:42.7.4")
}

jooq {
    version.set(jooqVersion)

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
                        includes = dynamicIncludes
                        excludes = ".*_\\d+|.*_\\d+_\\d+"
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
                        packageName = "me.mykindos.betterpvp.${project.name}.database.jooq"
                        directory = "src/main/java"
                    }
                }
            }
        }
    }
}