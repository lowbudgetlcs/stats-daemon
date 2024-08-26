group = "com.lowbudgetlcs"
version = "0.0.1"

plugins {
    kotlin("jvm") version "2.0.0"
    id("application")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.10"
    id("app.cash.sqldelight") version "2.0.2"
    id("com.gradleup.shadow") version "8.3.0"
}

application {
    mainClass.set("$group.MainKt")
}

repositories {
    google()
    mavenCentral()
    maven("https://jitpack.io")
}

kotlin {
    jvmToolchain(17)
}

sqldelight {
    databases {
        create("Database") {
            packageName.set("$group")
            dialect("app.cash.sqldelight:postgresql-dialect:2.0.2")
            srcDirs.setFrom("src/main/sqldelight")
            deriveSchemaFromMigrations.set(true)
        }
    }
}

dependencies {
    implementation("com.rabbitmq:amqp-client:5.21.0")

    implementation("com.github.stelar7:R4J:master-SNAPSHOT")

    implementation("app.cash.sqldelight:jdbc-driver:2.0.2")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.postgresql:postgresql:42.7.2")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0-RC")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.2")

    implementation("ch.qos.logback:logback-classic:1.4.14")
}
