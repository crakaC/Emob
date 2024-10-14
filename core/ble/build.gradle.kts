plugins {
    id("com.crakac.android.library")
}

android {
    namespace = "com.crakac.blemessaging.ble"
}

dependencies {
    implementation(projects.core.utils)
}