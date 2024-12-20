package com.crakac.blemessaging.convention

import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.kotlin.dsl.DependencyHandlerScope

fun DependencyHandlerScope.ksp(
    artifact: MinimalExternalModuleDependency,
) {
    add("ksp", artifact)
}

fun DependencyHandlerScope.kspTest(
    artifact: MinimalExternalModuleDependency,
) {
    add("kspTest", artifact)
}
