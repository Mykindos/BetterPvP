import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import io.papermc.paperweight.tasks.RemapJar

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    java apply true
    `java-gradle-plugin` apply true
    `version-catalog` apply true
    kotlin("jvm") version libs.versions.kotlin apply true
    id("com.github.johnrengelman.shadow") version "7.1.2" apply false // Building fat jar
    id("org.inferred.processors") version "3.7.0" apply false  // Annotation processing
    id("io.papermc.paperweight.userdev") version libs.versions.paperweight apply false // NMS Paper
    id("org.flywaydb.flyway") version libs.versions.flyway apply false // Flyway
}

repositories {
    mavenCentral()
}

subprojects {
    if (project.name == "private") {
        return@subprojects
    }

    repositories {
        gradlePluginPortal()
        mavenLocal()
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        maven("https://mvn.lumine.io/repository/maven-public/")
        maven("https://repo.xenondevs.xyz/releases")
        maven("https://repo.dmulloy2.net/repository/public/")
        maven("https://repo.spongepowered.org/maven/")
        maven("https://maven.aestrus.io/releases")
        maven("https://repo.opencollab.dev/maven-releases/")
    }

    // Set java language version
    plugins.apply("java")
    plugins.apply("org.inferred.processors")
    plugins.apply("com.github.johnrengelman.shadow")
    plugins.apply("org.jetbrains.kotlin.jvm")
    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    }

    // Shadow
    tasks.withType<ShadowJar>().all {
        mergeServiceFiles()
    }

    // Change output jar location
    tasks.named("assemble").configure {
        tasks.findByName("reobfJar")?.let { reobf ->
            this@configure.dependsOn(reobf)
        }
    }

    tasks.withType<RemapJar> {
        outputJar.set(file("$rootDir/build/${project.name}.jar"))
    }

    // Make tests use JUnit
    tasks {
        test {
            useJUnitPlatform()
        }
    }
}


