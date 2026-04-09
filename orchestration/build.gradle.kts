plugins {
    `java-library`
}

version = "1.0.0"
group = "me.mykindos.betterpvp.orchestration"
description = "Shared orchestration contracts for queueing, admission, and future matchmaking"

dependencies {
    api(libs.jackson.core)
    api(libs.jackson.annotations)
    api(libs.jackson.databind)
}
