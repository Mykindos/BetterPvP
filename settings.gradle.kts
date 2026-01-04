@file:Suppress("UnstableApiUsage")


// Projects
rootProject.name = "BetterPvP"
include(":clans")
include(":core")
include(":lunar")
include(":champions")
include(":shops")
include(":progression")
include(":game")

if (File("./private/").exists()) {
    include(":private:events")
    include(":private:dungeons")
    include(":private:store")
    include(":private:compatability")
    include(":private:mineplex")
}

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        maven("https://mvn.lumine.io/repository/maven-public/")
        maven("https://repo.xenondevs.xyz/releases")
        maven("https://repo.md-5.net/content/groups/public/")
        maven("https://repo.codemc.io/repository/maven-releases/")
        maven("https://jitpack.io")
        maven {
            url =  uri("http://mykindos.me:8081/repository/maven-public/")
            isAllowInsecureProtocol = true
        }
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            // Versions
            version("kotlin", "2.1.0")
            version("paper", "1.21.8-R0.1-SNAPSHOT")
            version("paperweight", "2.0.0-SNAPSHOT")
            version("jackson", "2.17.2")
            version("mineplex", "1.21.4")
            version("sidebar", "2.4.1")
            version("mockbukkit", "4.72.8")
            version("junit", "5.13.0-M2")
            version("jooq", "3.19.3")
            version("postgres", "42.7.4")

            // Library - PostgreSQL
            library("postgres", "org.postgresql", "postgresql").versionRef("postgres")

            // Library - jOOQ
            library("jooq", "org.jooq", "jooq").versionRef("jooq")
            library("jooq-codegen", "org.jooq", "jooq-codegen").versionRef("jooq")
            library("jooq-meta", "org.jooq", "jooq-meta").versionRef("jooq")
            library("jooq-meta-extensions", "org.jooq", "jooq-meta-extensions").versionRef("jooq")

            // Library - Mineplex SDK
            library("mineplex", "com.mineplex.studio.sdk", "sdk").versionRef("mineplex")

            // Library - reflection mapper
            library("reflection-remapper", "xyz.jpenilla:reflection-remapper:0.1.3")

            // Library - lombok
            library("lombok", "org.projectlombok", "lombok").version("1.18.34")

            // Library - Kotlin
            library("kotlin-stdlib", "org.jetbrains.kotlin", "kotlin-stdlib").versionRef("kotlin")
            library("kotlin-reflect", "org.jetbrains.kotlin", "kotlin-reflect").versionRef("kotlin")
            plugin("kotlin", "org.jetbrains.kotlin.jvm").versionRef("kotlin")

            // Library - Tests
            library("junit-jupiter", "org.junit.jupiter","junit-jupiter").versionRef("junit")
            library("reflections", "org.reflections:reflections:0.10.2")
            library("mockbukkit", "org.mockbukkit.mockbukkit", "mockbukkit-v1.21").versionRef("mockbukkit")

            // Library - Paper
            library("paper-api", "io.papermc.paper", "paper-api").versionRef("paper")
            plugin("paperweight-userdev", "io.papermc.paperweight.userdev").versionRef("paperweight")

            // Library - Kyori Adventure (does not come with Paper)
            library("mini-placeholders", "io.github.miniplaceholders:miniplaceholders-api:3.1.0")

            // Library - Jackson
            library("jackson-core", "com.fasterxml.jackson.core", "jackson-core").versionRef("jackson")
            library("jackson-annotations", "com.fasterxml.jackson.core", "jackson-annotations").versionRef("jackson")
            library("jackson-databind", "com.fasterxml.jackson.core", "jackson-databind").versionRef("jackson")

            // Libraries - Data Management
            library("jedis", "redis.clients:jedis:7.2.0")
            library("flyway-core", "org.flywaydb", "flyway-core").version("11.13.0")
            library("flyway-mysql", "org.flywaydb", "flyway-mysql").version("11.13.0")
            library("flyway-postgres", "org.flywaydb", "flyway-database-postgresql").version("11.15.0")
            library("hikari-cp", "com.zaxxer", "HikariCP").version("5.1.0")

            // Libraries - Utilities
            library("persistent-data-types", "com.jeff-media", "MorePersistentDataTypes").version("2.4.0")
            library("glowapi", "fr.skytasul", "glowingentities").version("1.4.8")
            library("commons-text", "org.apache.commons", "commons-text").version("1.10.0")
            library("commons-lang3", "org.apache.commons", "commons-lang3").version("3.12.0")
            library("commons-math3", "org.apache.commons", "commons-math3").version("3.6.1")
            library("annotations", "org.jetbrains", "annotations").version("24.0.1")
            library("jsr305", "com.google.code.findbugs", "jsr305").version("3.0.2")
            library("caffeine", "com.github.ben-manes.caffeine", "caffeine").version("3.1.8")
            library("okhttp", "com.squareup.okhttp3", "okhttp").version("4.10.0")
            library("prettytime", "org.ocpsoft.prettytime", "prettytime").version("5.0.4.Final")
            library("zip4j", "net.lingala.zip4j", "zip4j").version("2.11.5")

            // Library - Mapper
            library("mapper", "com.github.braulio-dev", "Mapper").version("1.0.7")

            // Library - UI
            library("sidebar-api", "net.megavex", "scoreboard-library-api").versionRef("sidebar")
            library("sidebar-impl", "net.megavex", "scoreboard-library-implementation").versionRef("sidebar")
            library("sidebar-packetevents", "net.megavex", "scoreboard-library-packetevents").versionRef("sidebar")

            // Library - WorldEdit
            library("fawe", "com.fastasyncworldedit", "FastAsyncWorldEdit-Core").version("2.8.4")
            library("fawebukkit", "com.fastasyncworldedit", "FastAsyncWorldEdit-Bukkit").version("2.8.4")

            // Library - Mythic
            library("mythic", "io.lumine", "Mythic-Dist").version("5.9.5")

            // Library - Nexo
            library("nexo", "com.nexomc", "nexo").version("1.7.1")

            // Library - Oraxen
            library("oraxen", "io.th0rgal", "oraxen").version("1.200.0")

            library("modelengine", "com.ticxo.modelengine", "ModelEngine").version("R4.0.9")

            // Library - McPets
            library("mcpets", "fr.nocsy", "mcpets").version("4.1.6-SNAPSHOT")

            // Library - Protocol
            library("libsdisguises", "me.libraryaddict.disguises", "libsdisguises").version("11.0.13")
            library("packetevents", "com.github.retrooper", "packetevents-spigot").version("2.11.0")

            // Library - Mixins
            library("mixin", "org.spongepowered", "mixin").version("0.8.7")

            // Library - Guice
            library("guice", "com.google.inject", "guice").version("7.0.0")

            // Bundled Libraries
            bundle("kotlin", listOf("kotlin-stdlib", "kotlin-reflect"))
            bundle("test", listOf("junit-jupiter", "mockbukkit"))
            bundle("paper", listOf("paper-api"))
            bundle("utils",
                listOf("commons-text",
                    "persistent-data-types",
                    "glowapi",
                    "commons-lang3",
                    "commons-math3",
                    "annotations",
                    "jsr305",
                    "jackson-core",
                    "jackson-annotations",
                    "jackson-databind",
                    "guice",
                    "reflections",
                    "okhttp",
                    "zip4j"))
            bundle("data", listOf("jedis", "flyway-core", "flyway-mysql", "flyway-postgres"))
            bundle("mixins", listOf("mixin"))
        }
    }
}
