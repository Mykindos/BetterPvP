@file:Suppress("UnstableApiUsage")

// Projects
rootProject.name = "BetterPvP"
include(":clans")
include(":core")
include(":lunar")
include(":champions")
include(":shops")
include(":progression")

if (File("./private/").exists()) {
    include(":private:events")
    include(":private:dungeons")
    include(":private:blocking-mixins")
    include(":private:blocking")
    include(":private:store")
    include(":private:compatability")
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
        maven("https://repo.dmulloy2.net/repository/public/")
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
            version("kotlin", "1.9.20")
            version("paper", "1.21.1-R0.1-SNAPSHOT")
            version("paperweight", "1.7.2")
            version("jackson", "2.17.2")
            version("lombok", "1.18.34")
            version("flyway", "9.0.4")
            version("mineplex", "1.3.12")

            // Library - Mineplex SDK
            library("mineplex", "com.mineplex.studio.sdk", "com.mineplex.studio.sdk").versionRef("mineplex")

            // Library - reflection mapper
            library("reflection-remapper", "xyz.jpenilla:reflection-remapper:0.1.1")

            // Library - lombok
            library("lombok", "org.projectlombok", "lombok").versionRef("lombok")

            // Library - Kotlin
            library("kotlin-stdlib", "org.jetbrains.kotlin", "kotlin-stdlib").versionRef("kotlin")
            library("kotlin-reflect", "org.jetbrains.kotlin", "kotlin-reflect").versionRef("kotlin")
            plugin("kotlin", "org.jetbrains.kotlin.jvm").versionRef("kotlin")

            // Library - Tests
            library("junit-jupiter", "org.junit.jupiter:junit-jupiter:5.9.0")
            library("reflections", "org.reflections:reflections:0.10.2")

            // Library - Paper
            library("paper-api", "io.papermc.paper", "paper-api").versionRef("paper")
            plugin("paperweight-userdev", "io.papermc.paperweight.userdev").versionRef("paperweight")

            // Library - Kyori Adventure (does not come with Paper)
            library("adventure-text-minimessage", "net.kyori:adventure-text-minimessage:4.14.0")
            library("adventure-platform-bukkit", "net.kyori:adventure-platform-bukkit:4.3.1")
            library("mini-placeholders", "io.github.miniplaceholders:miniplaceholders-api:2.2.2")

            // Library - Jackson
            library("jackson-core", "com.fasterxml.jackson.core", "jackson-core").versionRef("jackson")
            library("jackson-annotations", "com.fasterxml.jackson.core", "jackson-annotations").versionRef("jackson")
            library("jackson-databind", "com.fasterxml.jackson.core", "jackson-databind").versionRef("jackson")

            // Libraries - Data Management
            library("jedis", "redis.clients:jedis:4.3.1")
            library("flyway-core", "org.flywaydb", "flyway-core").versionRef("flyway")
            library("flyway-mysql", "org.flywaydb", "flyway-mysql").versionRef("flyway")
            library("hikari-cp", "com.zaxxer", "HikariCP").version("5.1.0")

            // Libraries - Utilities
            library("commons-text", "org.apache.commons", "commons-text").version("1.10.0")
            library("commons-lang3", "org.apache.commons", "commons-lang3").version("3.12.0")
            library("commons-math3", "org.apache.commons", "commons-math3").version("3.6.1")
            library("annotations", "org.jetbrains", "annotations").version("24.0.1")
            library("jsr305", "com.google.code.findbugs", "jsr305").version("3.0.2")
            library("caffeine", "com.github.ben-manes.caffeine", "caffeine").version("3.1.8")
            library("okhttp", "com.squareup.okhttp3", "okhttp").version("4.10.0")
            library("prettytime", "org.ocpsoft.prettytime", "prettytime").version("5.0.4.Final")

            // Library - UI
            library("sidebar", "me.catcoder", "bukkit-sidebar").version("1.0.0-SNAPSHOT")

            // Library - WorldEdit
            library("fawe", "com.fastasyncworldedit", "FastAsyncWorldEdit-Core").version("2.8.4")
            library("fawebukkit", "com.fastasyncworldedit", "FastAsyncWorldEdit-Bukkit").version("2.8.4")

            // Library - Mythic
            library("mythic", "io.lumine", "Mythic-Dist").version("5.3.5")
            library("mythicdungeons", "net.playavalon", "MythicDungeons").version("1.3.0-SNAPSHOT")
            library("modelengine", "com.ticxo.modelengine", "ModelEngine").version("R4.0.4")

            // Library - Protocol
            library("protocollib", "com.comphenix.protocol", "ProtocolLib").version("5.1.0")
            library("libsdisguises", "LibsDisguises", "LibsDisguises").version("10.0.40")

            // Library - Mixins
            library("ignite", "space.vectrix.ignite", "ignite-api").version("0.8.1")
            library("mixin", "org.spongepowered", "mixin").version("0.8.5")

            // Library - Guice
            library("guice", "com.google.inject", "guice").version("7.0.0")

            // Bundled Libraries
            bundle("kotlin", listOf("kotlin-stdlib", "kotlin-reflect"))
            bundle("test", listOf("junit-jupiter"))
            bundle("kyori-adventure", listOf("adventure-platform-bukkit", "adventure-text-minimessage"))
            bundle("paper", listOf("paper-api"))
            bundle("utils",
                listOf("commons-text",
                    "commons-lang3",
                    "commons-math3",
                    "annotations",
                    "jsr305",
                    "jackson-core",
                    "jackson-annotations",
                    "jackson-databind",
                    "guice",
                    "reflections",
                    "okhttp"))
            bundle("data", listOf("jedis", "flyway-core", "flyway-mysql"))
            bundle("mixins", listOf("ignite", "mixin"))
        }
    }
}