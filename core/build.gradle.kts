import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java

    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("org.flywaydb.flyway") version "9.0.1"
}

version = 1.0

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

val shadowJar: ShadowJar by tasks
shadowJar.apply {
    mergeServiceFiles()
}


repositories {
    mavenCentral()
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    maven{ url = uri("https://repo.dmulloy2.net/repository/public/") }
}

dependencies {
    paperweightDevelopmentBundle("io.papermc.paper:dev-bundle:1.19.1-R0.1-SNAPSHOT")

    implementation("org.flywaydb:flyway-core:9.0.1")
    implementation("org.flywaydb:flyway-mysql:9.0.1")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("com.squareup.okhttp3:okhttp:4.5.0")

    compileOnly("io.papermc.paper:paper-api:1.19-R0.1-SNAPSHOT")
    compileOnly("org.projectlombok:lombok:1.18.24")
    compileOnly("com.comphenix.protocol:ProtocolLib:4.8.0")

    implementation("com.google.inject:guice:5.1.0")
    implementation("org.reflections:reflections:0.10.2")

    annotationProcessor("org.projectlombok:lombok:1.18.24")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
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