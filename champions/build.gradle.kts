import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("org.sonarqube") version "4.2.1.3168"
}

version = 1.0

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

sonar {
    properties {
        property("sonar.projectKey", "Mykindos_BetterPvP_AYsY-joZlXMIEAL7aBHC")
        property("sonar.projectName", "BetterPvP")
    }
}


val shadowJar: ShadowJar by tasks
shadowJar.apply {
    dependencies {
        exclude(project(":core"))
        exclude(dependency("com.google.inject:guice:5.1.0"))
    }
    mergeServiceFiles()
}

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    maven{ url = uri("https://repo.dmulloy2.net/repository/public/") }
    maven{ url = uri("https://repo.md-5.net/content/groups/public/") }
}

dependencies {
    paperweight.paperDevBundle("1.20.1-R0.1-SNAPSHOT")

    implementation("com.google.inject:guice:5.1.0")
    implementation("org.reflections:reflections:0.10.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.3")

    compileOnly(project(":core"))
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
    compileOnly("org.projectlombok:lombok:1.18.24")
    compileOnly("com.comphenix.protocol:ProtocolLib:4.6.1-SNAPSHOT")
    compileOnly("LibsDisguises:LibsDisguises:10.0.31")

    annotationProcessor("org.projectlombok:lombok:1.18.24")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testCompileOnly("org.projectlombok:lombok:1.18.24")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.24")
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}