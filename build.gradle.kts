plugins {
    java
    id("java")
    id("java-library")
    id("com.gradleup.shadow") version "9.6.1"
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

group = "ca.anthonyting.personalplugins"
version = "1.0"

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

dependencies {
    // Paper API (Provided by server at runtime)
    compileOnly("io.papermc.paper:paper-api:26.3.build.+")
    testCompileOnly("io.papermc.paper:paper-api:26.3.build.+")

    // Compile & Shaded dependencies
    implementation("net.java.dev.jna:jna:5.12.1")
    implementation("org.jetbrains:annotations:22.0.0")

    // Local JAR (replaces Maven systemPath)
    implementation(files("build-resources/EmojiChat.jar"))

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:4.2.0")
    testImplementation("org.powermock:powermock-api-mockito2:2.0.9")
    testImplementation("org.powermock:powermock-module-junit4:2.0.9")
}

// Replicates maven-shade-plugin minimization for JNA
tasks.shadowJar {
    minimize {
        include(dependency("net.java.dev.jna:jna"))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks {
    runServer {
        minecraftVersion("26.3")
    }
}
