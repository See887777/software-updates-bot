import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val detektVersion = "1.23.3" // IMPORTANT don't forget to update plugin version too
val prjJavaVersion = JavaVersion.VERSION_17

plugins {
    val kotlinVersion = "1.9.20"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    kotlin("kapt") version kotlinVersion
    id("org.springframework.boot") version "3.1.5"
    id("io.spring.dependency-management") version "1.1.3"
    id("com.github.ben-manes.versions") version "0.49.0"
    id("project-report") // https://docs.gradle.org/current/userguide/project_report_plugin.html
    id("io.gitlab.arturbosch.detekt") version "1.23.3" // IMPORTANT set it to detektVersion's value
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
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.jayway.jsonpath:json-path:2.8.0")
    implementation("org.jsoup:jsoup:1.16.2") // https://jsoup.org/news/
    implementation("commons-io:commons-io:2.15.0")
    implementation("org.apache.commons:commons-lang3:3.13.0")
    implementation("com.rometools:rome:2.1.0") // https://github.com/rometools/rome/releases
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.retry:spring-retry:2.0.4")
    kapt("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:$detektVersion")
}

detekt {
    toolVersion = detektVersion
    config.setFrom("./detekt-config.yml")
    buildUponDefaultConfig = true
    ignoreFailures = false
}

configurations.matching { it.name == "detekt" }.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion("1.9.10") // IMPORTANT update if failed with "detekt was compiled with Kotlin XX but is currently running with YY"
        }
    }
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
    if (listOf("RELEASE", "FINAL", "GA").any { version.uppercase().endsWith(it) }) {
        return false
    }
    return listOf("alpha", "Alpha", "ALPHA", "b", "beta", "Beta", "BETA", "rc", "RC", "M", "EA", "pr", "atlassian").any {
        "(?i).*[.-]${it}[.\\d-]*$".toRegex().matches(version)
    }
}
