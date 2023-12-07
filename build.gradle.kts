import io.papermc.paperweight.tasks.RemapJar

plugins {
    id("io.papermc.paperweight.userdev") version "1.5.5" apply false
}

subprojects {
    if (name != "private") {
        plugins.apply("io.papermc.paperweight.userdev")
    }

    plugins.withType<JavaPlugin> {
        (tasks.findByName("reobfJar") as? RemapJar)?.apply {
            outputJar.set(file("$rootDir/build/${project.name}.jar"))
        }
    }
}


