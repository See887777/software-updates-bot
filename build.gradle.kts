import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val detektVersion = "1.22.0" // don't forget to update plugin version too
val prjJavaVersion = JavaVersion.VERSION_17

plugins {
    val kotlinVersion = "1.7.21"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    kotlin("kapt") version kotlinVersion
    id("org.springframework.boot") version "2.7.6"
    id("io.spring.dependency-management") version "1.1.0"
    id("com.github.ben-manes.versions") version "0.44.0"
    id("project-report") // https://docs.gradle.org/current/userguide/project_report_plugin.html
    id("io.gitlab.arturbosch.detekt") version "1.22.0"
    id("biz.lermitage.oga") version "1.1.1"
}

group = "biz.lermitage"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = prjJavaVersion
java.targetCompatibility = prjJavaVersion

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.google.code.gson:gson:2.10")
    implementation("com.jayway.jsonpath:json-path:2.7.0")
    implementation("org.jsoup:jsoup:1.15.3")
    implementation("commons-io:commons-io:2.11.0")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("com.rometools:rome:1.18.0")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.retry:spring-retry:2.0.0")
    kapt("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:$detektVersion")
}

detekt {
    toolVersion = detektVersion
    config = files("./detekt-config.yml")
    buildUponDefaultConfig = true
    ignoreFailures = false
}

tasks {
    withType<Test> {
        useJUnitPlatform()
    }
    withType<KotlinCompile> {
        kotlinOptions {
            javaParameters = true
            freeCompilerArgs = listOf("-Xjsr305=strict"/*, "-Xuse-k2"*/)
            jvmTarget = prjJavaVersion.toString()
        }
    }
    withType<DependencyUpdatesTask> {
        checkForGradleUpdate = true
        gradleReleaseChannel = "current"
        revision = "release"
        rejectVersionIf {
            isNonStable(candidate.version)
        }
        outputFormatter = closureOf<com.github.benmanes.gradle.versions.reporter.result.Result> {
            unresolved.dependencies.removeIf { it.group.toString() == "org.jetbrains.kotlin" }
            com.github.benmanes.gradle.versions.reporter.PlainTextReporter(project, revision, gradleReleaseChannel)
                .write(System.out, this)
        }
    }
    withType<Detekt> {
        jvmTarget = prjJavaVersion.toString()
        reports {
            html.required.set(false)
            xml.required.set(false)
            txt.required.set(false)
        }
    }
}

fun isNonStable(version: String): Boolean {
    if (listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().endsWith(it) }) {
        return false
    }
    return listOf("alpha", "Alpha", "ALPHA", "b", "beta", "Beta", "BETA", "rc", "RC", "M", "EA", "pr", "atlassian").any {
        "(?i).*[.-]${it}[.\\d-]*$".toRegex().matches(version)
    }
}
