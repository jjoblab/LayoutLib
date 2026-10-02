// module-inflater/build.gradle.kts
// Module 1 — Inflater : XML parser → View tree
// @author jo@Dev

plugins {
    id("com.android.library")
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

    // AndroidX core
    implementation("androidx.core:core:1.13.1")
    implementation("androidx.annotation:annotation:1.8.0")

    // Tests unitaires JVM (JUnit 5)
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testImplementation("org.assertj:assertj-core:3.26.0")
    testImplementation("net.sf.kxml:kxml2:2.3.0")
    testImplementation("org.mockito:mockito-core:5.14.2")

    // Tests instrumentés Android
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:runner:1.5.2")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
