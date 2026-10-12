// module-layout/build.gradle.kts
// Module 6 — Layout engine : measure/layout algorithms
// @author jo@Dev

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "jo.layoutlib.layout"
    compileSdk = 37
    compileSdkMinor = 2

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests { isReturnDefaultValues = true }
    }
}

dependencies {
    implementation(libs.androidx.annotation)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.assertj.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
