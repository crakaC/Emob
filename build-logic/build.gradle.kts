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
