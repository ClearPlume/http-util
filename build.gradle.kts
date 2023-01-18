import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val commonsCodecVersion: String by project
val httpClientVersion: String by project
val lombokVersion: String by project
val logbackVersion: String by project
val jacksonVersion: String by project

plugins {
    id("maven-publish")
    kotlin("jvm") version "1.7.21"
    application
}

group = "top.clearplume.httputil"
version = "0.0.1"

publishing {
    publications {
        create<MavenPublication>("HttpUtil") {
            from(components["kotlin"])
            groupId = "top.clearplume.httputil"
            artifactId = "http-util"
            version = "0.0.1"

            pom {
                name.set("HttpUtil")
                description.set("Simple chain call wrapping based on HttpClient")
                developers {
                    developer {
                        name.set("fallenagnel")
                        email.set("the.fallenangel.965@gmail.com")
                    }
                }
                licenses {
                    name.set("GNU General Public License v3.0")
                    url.set("https://www.gnu.org/licenses/gpl-3.0.txt")
                }
            }
        }
    }

    repositories {
        mavenCentral {
            authentication {
                create<BasicAuthentication>("basic")
            }
            credentials {
                username = System.getenv("MVN_USERNAME")
                password = System.getenv("MVN_PWD")
            }
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("commons-codec:commons-codec:$commonsCodecVersion")
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
