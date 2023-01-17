import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val httpClientVersion: String by project
val lombokVersion: String by project
val logbackVersion: String by project
val jacksonVersion: String by project

plugins {
    kotlin("jvm") version "1.7.21"
    application
}

group = "top.fallenangel.tools"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.httpcomponents:httpclient:$httpClientVersion")
    implementation("org.apache.httpcomponents:httpmime:$httpClientVersion")
    implementation("org.projectlombok:lombok:$lombokVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}
