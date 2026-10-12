// validation/build.gradle.kts
// Module de validation — comparaison avec layoutlib original
// @author jo@Dev

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "jo.layoutlib.validation"
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
    api(project(":module-inflater"))
    api(project(":module-resources"))
    api(project(":module-drawables"))
    api(project(":module-themes"))
    api(project(":module-attributes"))
    api(project(":module-layout"))

    implementation(libs.androidx.annotation)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.assertj.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
