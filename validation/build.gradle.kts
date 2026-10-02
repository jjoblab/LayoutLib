// validation/build.gradle.kts
// Module de validation — comparaison avec layoutlib original
// @author jo@Dev

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "jo.layoutlib.validation"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
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
    testImplementation(libs.assertj.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
