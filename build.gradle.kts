import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    java apply true
    `java-gradle-plugin` apply true
    `version-catalog` apply true
    kotlin("jvm") version libs.versions.kotlin apply true
    id("com.gradleup.shadow") version "9.0.2" apply false // Building fat jar
    id("org.inferred.processors") version "3.7.0" apply false  // Annotation processing
    id("io.papermc.paperweight.userdev") version libs.versions.paperweight apply false // NMS Paper
    id("org.flywaydb.flyway") version "11.11.1" apply false // Flyway
    id("org.sonarqube") version "6.2.0.5505" apply true
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {

    mavenLocal()
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
        maven("https://repo.spongepowered.org/maven/")
        maven("https://repo.opencollab.dev/maven-releases/")
        maven("https://repo.md-5.net/repository/public/")
        maven("https://jitpack.io")
        maven("https://repo.viaversion.com")
        maven("https://repo.opencollab.dev/main/")
        maven("https://repo.codemc.io/repository/maven-releases/")
        maven("https://repo.codemc.io/repository/maven-snapshots/")
        maven("https://repo.opencollab.dev/main/")
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

    tasks.withType<JavaCompile>().configureEach {
        options.compilerArgs.add("-Xlint:deprecation")
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            freeCompilerArgs += "-Xlint:deprecation"
        }
    }

    // Make tests use JUnit
    tasks {
        test {
            useJUnitPlatform()
        }


    }



}

sonar {
    properties {
        property("sonar.projectKey", "Mykindos_BetterPvP")
        property("sonar.organization", "mykindos")
    }
}


