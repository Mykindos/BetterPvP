plugins {
    `java-library`
    `maven-publish`
}

version = "1.0.0"
group = "me.mykindos.betterpvp.structure"
description = "The .structure file format: blocks and Mapper datapoints in one file"

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "me.mykindos.betterpvp"
            artifactId = "structure-format"
            version = "1.0"
            from(components["java"])
        }
    }
}

// Everything here is provided by the server or the plugin that uses the format, so the jar holds only this
// module's own classes.
dependencies {
    compileOnly(libs.bundles.paper)
    compileOnly(libs.mapper)
    compileOnly(libs.fawe)
    compileOnly(libs.fawebukkit) {
        exclude(group = "org.lz4", module = "lz4-java")
    }
    compileOnly(libs.jackson.databind)
    compileOnly(libs.jackson.annotations)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.bundles.test)
    testImplementation(libs.mapper)
    testImplementation(libs.jackson.databind)
    testImplementation(libs.jackson.annotations)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
    testImplementation("org.mockito:mockito-core:5.23.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
