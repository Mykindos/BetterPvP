import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    java apply true
    `java-gradle-plugin` apply true
    `version-catalog` apply true
    kotlin("jvm") version libs.versions.kotlin apply true
    id("com.gradleup.shadow") version "8.3.3" apply false // Building fat jar
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
        maven {
          url =  uri("http://mykindos.me:8081/repository/maven-public/")
            isAllowInsecureProtocol = true
        }
    }

    // Set java language version
    plugins.apply("java")
    plugins.apply("org.inferred.processors")
    plugins.apply("com.gradleup.shadow")
    plugins.apply("org.jetbrains.kotlin.jvm")
    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }

    // Shadow
    tasks.withType<ShadowJar>().configureEach {
        relocate("com.github.benmanes.caffeine", "me.mykindos.betterpvp.caffeine")
        archiveBaseName.set(project.name)
        archiveVersion.set("")
        archiveClassifier.set("")
        destinationDirectory.set(file("$rootDir/build/"))
        mergeServiceFiles()
    }

    tasks.assemble.configure {

        dependsOn(tasks.withType<ShadowJar>())
    }

    // Change output jar location
    //tasks.named("assemble").configure {
    //    tasks.findByName("reobfJar")?.let { reobf ->
    //        this@configure.dependsOn(reobf)
    //    }
    //}


    // Make tests use JUnit
    tasks {
        test {
            useJUnitPlatform()
        }
    }
}


