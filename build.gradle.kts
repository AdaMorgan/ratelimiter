plugins {
    id("java")
}

group = "netty"

apply(from = "gradle/dependencies.gradle")

repositories {
    mavenCentral()
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "main.Server"
    }
}

tasks.withType<Jar>() {
    exclude("META-INF/BC2048KE.RSA", "META-INF/BC2048KE.SF", "META-INF/BC2048KE.DSA")
}

tasks.withType<Jar> {
    from(configurations.runtimeClasspath.get().filter {
        it.exists()
    }.map {
        if (it.isDirectory)
            it
        else
            zipTree(it)
    })
}

tasks.withType<Copy> {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.test {
    useJUnitPlatform()
}