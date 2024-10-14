package com.crakac.blemessaging.convention

import com.android.build.api.dsl.CommonExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestedExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun Project.androidLibrary(action: LibraryExtension.() -> Unit) {
    extensions.configure(action)
}

fun Project.android(action: TestedExtension.() -> Unit) {
    extensions.configure(action)
}

fun Project.setupAndroid() {
    android {
        setCompileSdkVersion(35)

        defaultConfig {
            minSdk = 31
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
            isCoreLibraryDesugaringEnabled = true
        }

        tasks.withType<KotlinCompile>().configureEach {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
            }
        }

        testOptions {
            unitTests {
                isIncludeAndroidResources = true
            }
        }

        (this as CommonExtension<*, *, *, *, *, *>).lint {
            // shell friendly
            val filename = displayName.replace(":", "_").replace("[\\s']".toRegex(), "")

            xmlReport = true
            xmlOutput =
                rootProject.layout.buildDirectory.file("lint-reports/lint-results-${filename}.xml")
                    .get().asFile

            htmlReport = true
            htmlOutput =
                rootProject.layout.buildDirectory.file("lint-reports/lint-results-${filename}.html")
                    .get().asFile

            // for now
            sarifReport = false
        }
    }
}
