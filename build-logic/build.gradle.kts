import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

group = "com.crakac.blemessaging.buildlogic"

repositories {
    google {
        content {
            includeGroupByRegex("com\\.android.*")
            includeGroupByRegex("com\\.google.*")
            includeGroupByRegex("androidx.*")
        }
    }
    mavenCentral()
    gradlePluginPortal()
}

// We are using JDK 17 for build process but we are targeting JDK 11 for the app
// If we use jvmToolchain, we need to install JDK 11
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = "17"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(libs.bundles.plugins)
    // https://github.com/google/dagger/issues/3068#issuecomment-1470534930
    implementation(libs.javapoet)
}

gradlePlugin {
    plugins {
        // Primitives
        register("androidLibrary") {
            id = "com.crakac.android.library"
            implementationClass = "com.crakac.blemessaging.convention.AndroidLibraryPlugin"
        }
    }
}
