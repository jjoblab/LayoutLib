// module-inflater/build.gradle.kts
// Module 1 — Inflater : XML parser → View tree
// @author jo@Dev

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "jo.layoutlib.inflater"
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
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    // Dépendances vers les autres modules du mini-layoutlib
    api(project(":module-resources"))
    api(project(":module-attributes"))
    api(project(":module-layout"))
    // Drawables : AttributeApplier expose DrawableResolver dans son API publique
    api(project(":module-drawables"))
    // Themes : AttributeApplier expose ThemeResolver dans son API publique
    api(project(":module-themes"))

    // AndroidX core
    implementation(libs.androidx.core)
    implementation(libs.androidx.annotation)

    // Tests unitaires JVM (JUnit 5)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.kxml2)
    testImplementation(libs.mockito.core)

    // Tests instrumentés Android
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.androidx.test.runner)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
