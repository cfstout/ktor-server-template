import org.gradle.api.JavaVersion.VERSION_11
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    application
    kotlin("jvm") version "1.3.70"
    id("org.jmailen.kotlinter") version "2.3.2"
    id("org.flywaydb.flyway") version "6.4.2"
    id("nu.studer.jooq") version "4.1"
}

repositories {
    jcenter()
}

val dbUser by extra { "cfstout" }
val dbPw by extra { "password" }
val dbUrl by extra { "jdbc:postgresql://localhost:5432/cfstout" }

apply(from = "jooq.gradle")

flyway {
    url = dbUrl
    user = dbUser
    password = dbPw
    validateMigrationNaming = true
}

val deps by extra {
    mapOf(
        "hikari" to "3.4.2",
        "konfig" to "1.6.10.0",
        "junit" to "5.6.2",
        "ktor" to "1.3.2",
        "logback" to "1.2.3",
        "postgres" to "42.2.12"
    )
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation("ch.qos.logback", "logback-classic", deps["logback"])
    implementation("com.natpryce", "konfig", deps["konfig"])
    implementation("com.zaxxer", "HikariCP", deps["hikari"])
    implementation("io.ktor", "ktor-jackson", "${deps["ktor"]}")
    implementation("io.ktor", "ktor-server-netty", deps["ktor"])
    implementation("org.jooq", "jooq")
    runtimeOnly("org.postgresql", "postgresql", deps["postgres"])

    jooqRuntime("org.postgresql", "postgresql", deps["postgres"])

    testImplementation("org.junit.jupiter", "junit-jupiter-api", deps["junit"])
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", deps["junit"])
    testImplementation("io.ktor", "ktor-server-tests", deps["ktor"])
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }

    (run) {
        args = listOf("config")
    }

    test {
        useJUnitPlatform()
    }
}

java {
    sourceCompatibility = VERSION_11
}

application {
    mainClassName = "io.github.cfstout.ktor.Server"
}
