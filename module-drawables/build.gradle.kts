// module-drawables/build.gradle.kts
// Module 3 — Drawables : parsing <shape>, <selector>, <vector>, etc.
// @author jo@Dev

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "jo.layoutlib.drawables"
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
    api(project(":module-resources"))
    implementation(libs.androidx.annotation)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.assertj.core)
    testImplementation(libs.kxml2)
    androidTestImplementation(libs.androidx.test.ext.junit)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
