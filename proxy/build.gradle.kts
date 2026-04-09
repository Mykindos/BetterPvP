plugins {
    `java-library`
}

version = "1.0.0"
group = "me.mykindos.betterpvp.proxy"
description = "Velocity proxy admission layer for BetterPvP orchestration"

dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.5.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.5.0-SNAPSHOT")
    implementation(project(":orchestration"))
    implementation(libs.jackson.core)
    implementation(libs.jackson.annotations)
    implementation(libs.jackson.databind)
    implementation("org.tomlj:tomlj:1.1.1")
}
