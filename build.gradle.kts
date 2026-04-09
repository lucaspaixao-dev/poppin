import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask

plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.spring") version "2.2.0"
    kotlin("plugin.jpa") version "2.2.0"
    id("org.springframework.boot") version "4.0.5"
    id("io.spring.dependency-management") version "1.1.7"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
    id("org.jetbrains.kotlinx.kover") version "0.9.1"
}

group = "io.github.lucaspaixaodev"
version = "0.0.1-SNAPSHOT"
description = "poppin"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    all {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
    }
    // Detekt 1.23.8 was compiled with Kotlin 2.0.21. The Kotlin 2.2.0 plugin upgrades kotlin-stdlib
    // in all configurations via conflict resolution, breaking detekt's internal version check.
    // Using eachDependency (runs after conflict resolution) to pin Kotlin back to 2.0.21 for
    // detekt's isolated worker classpath only.
    matching { it.name.startsWith("detekt") }.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin") {
                useVersion("2.0.21")
            }
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-log4j2")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.springframework.boot:spring-boot-flyway")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("com.google.firebase:firebase-admin:9.4.3")
    implementation("org.springframework.boot:spring-boot-starter-data-neo4j")
    implementation("tools.jackson.module:jackson-module-kotlin")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter:2.0.4")
    testImplementation("org.testcontainers:testcontainers-postgresql:2.0.4")
    testImplementation("org.testcontainers:testcontainers-neo4j:2.0.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

springBoot {
    mainClass.set("io.github.lucaspaixaodev.poppin.PoppinApplicationKt")
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    environment("FIREBASE_AUTH_EMULATOR_HOST", "localhost:9099")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$projectDir/config/detekt.yml")
    baseline = file("$projectDir/config/baseline.xml")
}

tasks.withType<Detekt>().configureEach {
    reports {
        html.required.set(true)
        sarif.required.set(true)
    }
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = "1.8"
}
tasks.withType<DetektCreateBaselineTask>().configureEach {
    jvmTarget = "1.8"
}

kover {
    reports {
        filters {
            excludes {
                classes(
                    "*.PoppinApplicationKt",
                    "*.infrastructure.config.*",
                    "*.infrastructure.input.rest.*.request.*",
                    "*.infrastructure.input.rest.*.response.*",
                    "*Entity",
                    "*Embeddable",
                    "*.infrastructure.output.graph.*",
                    "*.infrastructure.output.database.neo4j.*",
                    "*.infrastructure.output.authentication.*",
                )
            }
        }
        verify {
            rule("Minimum line coverage must be at least 90%") {
                bound {
                    minValue = 90
                }
            }
        }
    }
}
