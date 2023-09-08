import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val commonsCodecVersion: String by project
val httpClientVersion: String by project
val lombokVersion: String by project
val logbackVersion: String by project
val jacksonVersion: String by project

val mvnUsername: String = findProperty("MVN_USERNAME") as String
val mvnPwd: String = findProperty("MVN_PWD") as String

plugins {
    id("maven-publish")
    id("signing")
    kotlin("jvm") version "1.7.21"
}

group = "net.fallingangel"
version = "0.0.6"

repositories {
    mavenCentral()
}

dependencies {
    api("org.apache.httpcomponents:httpmime:$httpClientVersion")

    implementation("commons-codec:commons-codec:$commonsCodecVersion")
    implementation("org.apache.httpcomponents:httpclient:$httpClientVersion")
    implementation("org.projectlombok:lombok:$lombokVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    testImplementation(kotlin("test"))
}

java {
    withJavadocJar()
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

publishing {
    publications {
        create<MavenPublication>("HttpUtil") {
            from(components["java"])
            groupId = "net.fallingangel"
            artifactId = "http-util"
            version = "0.0.6"

            pom {
                name.set("HttpUtil")
                description.set("Simple chain call wrapping based on HttpClient")
                developers {
                    developer {
                        id.set("fallenagnel")
                        name.set("杜海蛟")
                        email.set("the.fallenangel.965@gmail.com")
                    }
                }
                licenses {
                    name.set("GNU General Public License v3.0")
                    url.set("https://github.com/ClearPlume/http-util/blob/master/LICENSE")
                }
                scm {
                    connection.set("scm:git:git://github.com/ClearPlume/http-util.git")
                    developerConnection.set("scm:git:ssh://github.com/ClearPlume/http-util.git")
                    url.set("https://github.com/ClearPlume/http-util")
                }
            }
        }
    }

    repositories {
        mavenCentral {
            url = uri("https://s01.oss.sonatype.org/content/repositories/releases/")
            authentication {
                create<BasicAuthentication>("basic")
            }
            credentials {
                username = mvnUsername
                password = mvnPwd
            }
        }
    }
}

signing {
    sign(publishing.publications["HttpUtil"])
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
}
