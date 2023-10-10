import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    kotlin("jvm") version "1.7.10"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("org.flywaydb.flyway") version "9.0.1"
    `maven-publish`
}

version = 1.0

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}


val shadowJar: ShadowJar by tasks
shadowJar.apply {
    mergeServiceFiles()
}

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


repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    maven{ url = uri("https://repo.dmulloy2.net/repository/public/") }
    maven(url = "https://mvn.lumine.io/repository/maven-public/")
}

dependencies {
    paperweight.paperDevBundle("1.20.2-R0.1-SNAPSHOT")

    implementation("org.flywaydb:flyway-core:9.0.4")
    implementation("org.flywaydb:flyway-mysql:9.0.4")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    compileOnly("io.papermc.paper:paper-api:1.20.2-R0.1-SNAPSHOT")
    compileOnly("org.projectlombok:lombok:1.18.26")
    compileOnly("com.comphenix.protocol:ProtocolLib:5.1.0")
    compileOnly("io.lumine:Mythic-Dist:5.3.5")

    implementation("com.google.inject:guice:5.1.0")
    implementation("org.reflections:reflections:0.10.2")

    annotationProcessor("org.projectlombok:lombok:1.18.26")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testCompileOnly("org.projectlombok:lombok:1.18.26")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.26")
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}