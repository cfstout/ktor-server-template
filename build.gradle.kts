import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.3.70"
}

repositories {
    jcenter()
}

val deps by extra {
    mapOf(
        "ktor" to "1.3.2",
        "logback" to "1.2.3"
    )
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation("ch.qos.logback", "logback-classic", deps["logback"])
    implementation("io.ktor", "ktor-jackson", "${deps["ktor"]}")
    implementation("io.ktor", "ktor-server-netty", deps["ktor"])
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }
}