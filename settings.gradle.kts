plugins {
    // 让 Gradle 能自动下载缺失的 JDK toolchain（如 JDK 25）
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "MarkNote"
