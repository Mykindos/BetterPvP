description = "Convention, architecture and translation checks across the BetterPvP modules"

val checkedModules = listOf(
    ":core", ":clans", ":champions", ":shops", ":progression", ":game", ":hub", ":lunar",
    ":orchestration", ":orchestration-service", ":proxy",
)

dependencies {
    testImplementation(libs.junit.jupiter)
    testImplementation("com.tngtech.archunit:archunit:1.5.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // Lets ArchUnit resolve Bukkit and Adventure types, such as Listener, that the checked classes refer to
    testRuntimeOnly(libs.paper.api)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}

tasks.shadowJar {
    enabled = false
}

tasks.test {
    useJUnitPlatform()
    checkedModules.forEach { dependsOn("$it:classes") }
    inputs.files(checkedModules.map { project(it).fileTree("src/main") })
    val updateBaseline = providers.gradleProperty("updateBaseline").isPresent
    systemProperty("conventions.root", rootDir.absolutePath)
    systemProperty("conventions.modules", checkedModules.joinToString(",") { it.removePrefix(":") })
    systemProperty("conventions.updateBaseline", updateBaseline.toString())
    if (updateBaseline) {
        systemProperty("archunit.freeze.store.default.allowStoreCreation", "true")
        systemProperty("archunit.freeze.store.default.allowStoreUpdate", "true")
        systemProperty("archunit.freeze.refreeze", "true")
        outputs.upToDateWhen { false }
    }
}
