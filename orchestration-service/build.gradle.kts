plugins {
    application
}

version = "1.0.0"
group = "me.mykindos.betterpvp.orchestration.service"
description = "Standalone orchestration service for queueing and admission"

dependencies {
    implementation(project(":orchestration"))
    implementation(libs.jackson.core)
    implementation(libs.jackson.annotations)
    implementation(libs.jackson.databind)
    implementation("org.tomlj:tomlj:1.1.1")
}

application {
    mainClass.set("me.mykindos.betterpvp.orchestration.service.OrchestrationServiceApplication")
}
