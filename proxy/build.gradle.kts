plugins {
    `java-library`
}

version = "1.0.0"
group = "me.mykindos.betterpvp.proxy"
description = "Velocity proxy admission layer for BetterPvP orchestration"

dependencies {
    compileOnly(libs.velocity.api)
    annotationProcessor(libs.velocity.api)
    implementation(project(":orchestration"))
    implementation(libs.jackson.core)
    implementation(libs.jackson.annotations)
    implementation(libs.jackson.databind)
    implementation("org.tomlj:tomlj:1.1.1")
}
