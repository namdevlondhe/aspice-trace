import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.getSupportedKotlinVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.detekt)
    application
}

group = "dev.namdev"
version = "0.1.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}

application {
    mainClass.set("dev.namdev.aspicetrace.cli.MainKt")
    applicationName = "aspice-trace"
}

dependencies {
    implementation(libs.clikt)
    testImplementation(libs.junit)
    testImplementation(kotlin("test-junit"))
}

tasks.test {
    // SampleProjectTest reads these, so a change to them must re-run the tests.
    inputs.files(fileTree("samples"), fileTree("docs/sample-report"))
        .withPropertyName("sampleFiles")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("config/detekt/detekt.yml"))
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = "17"
    reports {
        html.required.set(true)
        xml.required.set(false)
    }
}

// detekt 1.23.7 is built against Kotlin 2.0.10; keep its own classpath on that version.
configurations.matching { it.name == "detekt" }.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(getSupportedKotlinVersion())
        }
    }
}

val mainClassName = "dev.namdev.aspicetrace.cli.MainKt"

// Dogfooding: the tool traces its own requirements (docs/requirements.md) and fails the build on any gap.
tasks.register<JavaExec>("traceSelf") {
    group = "verification"
    description = "Traces aspice-trace's own requirements and fails on any gap."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set(mainClassName)
    args(
        projectDir.path,
        "--pattern", "TRC-[A-Z]+-\\d{3}",
        "--exclude", "samples/**",
        "--exclude", "docs/sample-report/**",
        "--out", layout.buildDirectory.dir("trace-self").get().asFile.path,
        "--link-base", "https://github.com/namdevlondhe/aspice-trace/blob/main/",
        "--fail-on-gaps",
    )
}

// Regenerates the committed example report in docs/sample-report from samples/hmi-demo.
tasks.register<JavaExec>("sampleReport") {
    group = "documentation"
    description = "Regenerates docs/sample-report from samples/hmi-demo."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set(mainClassName)
    args(
        file("samples/hmi-demo").path,
        "--out", file("docs/sample-report").path,
        "--link-base", "https://github.com/namdevlondhe/aspice-trace/blob/main/samples/hmi-demo/",
    )
}
