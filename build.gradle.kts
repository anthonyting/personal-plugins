plugins {
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
    
    // Compile & Shaded dependencies
    implementation("net.java.dev.jna:jna:5.19.1")
    implementation("org.jetbrains:annotations:26.1.0")

    // Local JAR (replaces Maven systemPath)
    // implementation(files("build-resources/EmojiChat.jar"))

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
    testImplementation("org.mockito:mockito-core:5.24.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.3")
    testRuntimeOnly("io.papermc.paper:paper-api:26.3.build.+")
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

tasks.test {
    useJUnitPlatform()
}

tasks {
    runServer {
        minecraftVersion("26.3")
    }
}

configurations {
    testCompileClasspath.get().extendsFrom(compileOnly.get())
}
