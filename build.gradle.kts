plugins {
    id("io.papermc.paperweight.userdev") version "1.5.5" apply false
}

subprojects.forEach { subProject ->
    run {
        if (!subProject.name.equals("private")) {
            subProject.plugins.apply("io.papermc.paperweight.userdev")
        }
    }
}

