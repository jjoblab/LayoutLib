// validation/build.gradle.kts
// Module de validation — comparaison avec layoutlib original
// @author jo@Dev

plugins {
    id("com.android.library")
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

    implementation("androidx.annotation:annotation:1.8.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.assertj:assertj-core:3.26.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
