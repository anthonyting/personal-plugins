plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.6.1"
    id("xyz.jpenilla.run-paper") version "3.1.0"
    // Provides Mojang-mapped Paper internals (NMS) for version-specific server integrations.
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.24"
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
    paperweight.paperDevBundle("26.3.build.+")

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

paperweight.reobfArtifactConfiguration =
    io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

// Replicates maven-shade-plugin minimization for JNA
tasks.shadowJar {
    minimize {
        include(dependency("net.java.dev.jna:jna"))
    }

    from(rootProject.file("LICENSE")) {
        into("META-INF")
        rename { "LICENSE-PersonalPlugins.txt" }
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
