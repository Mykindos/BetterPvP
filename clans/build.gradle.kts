import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}


val shadowJar: ShadowJar by tasks
shadowJar.apply {
    dependencies {
        exclude(project(":core"))
        exclude(dependency("com.google.inject:guice:5.1.0"))
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
}

dependencies {
    // https://mvnrepository.com/artifact/com.google.inject/guice
    implementation("com.google.inject:guice:5.1.0")
    implementation("org.reflections:reflections:0.10.2")
    implementation(project(":core"))

    compileOnly("io.papermc.paper:paper-api:1.19-R0.1-SNAPSHOT")
    compileOnly("org.projectlombok:lombok:1.18.24")

    annotationProcessor("org.projectlombok:lombok:1.18.24")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testCompileOnly("org.projectlombok:lombok:1.18.24")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.24")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}