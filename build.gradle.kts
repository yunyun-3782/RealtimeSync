plugins {
    java
}

group = "com.caellab.realtimesync"
version = "1.2.1"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to version)
    }
}

tasks.jar {
    archiveFileName.set("RealtimeSync-${version}.jar")
    destinationDirectory.set(file("/tmp/rs-build/out"))
}
