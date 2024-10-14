package com.crakac.blemessaging.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

@Suppress("unused")
class AndroidLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
                apply("org.jetbrains.kotlin.android")
                apply("com.google.devtools.ksp")
                apply("com.google.dagger.hilt.android")
            }
            androidLibrary {
                setupAndroid()
            }
            dependencies {
                // Use Java11 API in Android 11 and lower
                add("coreLibraryDesugaring", libs.library("androidDesugarJdkLibs"))

                implementation(libs.library("androidx-core-ktx"))
                implementation(libs.library("androidx-collection-ktx"))
                implementation(libs.library("kotlinx-coroutines-android"))
                implementation(libs.library("timber"))

                testImplementation(libs.library("junit"))
                testImplementation(libs.library("turbine"))
                testImplementation(libs.library("kotlinx-coroutines-test"))
                testImplementation(libs.library("kotest-assertions-core"))
                testImplementation(libs.library("hilt-androidTesting"))

                androidTestImplementation(libs.library("androidx-junit"))
                androidTestImplementation(libs.library("androidx-espresso-core"))

                implementation(libs.library("hilt-android"))
                ksp(libs.library("hilt-compiler"))
                kspTest(libs.library("hilt-compiler"))
            }
        }
    }
}
