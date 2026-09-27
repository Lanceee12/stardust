plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    application
}

val configuredBackendBuildDir = providers.environmentVariable("STARDUST_BACKEND_BUILD_DIR").orNull
if (configuredBackendBuildDir != null) {
    layout.buildDirectory.set(file(configuredBackendBuildDir))
} else if (rootDir.absolutePath.contains(':')) {
    layout.buildDirectory.set(file("${System.getProperty("java.io.tmpdir")}/stardust-backend-build"))
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:2.3.12")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.12")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.12")
    implementation("io.ktor:ktor-client-cio-jvm:2.3.12")
    implementation("org.xerial:sqlite-jdbc:3.49.1.0")
    implementation("ch.qos.logback:logback-classic:1.5.6")
}

application { mainClass.set("com.stardust.backend.ApplicationKt") }