import org.gradle.api.JavaVersion.VERSION_11
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    application
    kotlin("jvm") version "1.3.70"
    id("org.jmailen.kotlinter") version "2.3.2"
}

repositories {
    jcenter()
}

val deps by extra {
    mapOf(
        "konfig" to "1.6.10.0",
        "ktor" to "1.3.2",
        "logback" to "1.2.3"
    )
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation("ch.qos.logback", "logback-classic", deps["logback"])
    implementation("com.natpryce", "konfig", deps["konfig"])
    implementation("io.ktor", "ktor-jackson", "${deps["ktor"]}")
    implementation("io.ktor", "ktor-server-netty", deps["ktor"])
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }

    (run) {
        args = listOf("config")
    }
}

java {
    sourceCompatibility = VERSION_11
}

application {
    mainClassName = "io.github.cfstout.ktor.Server"
}
