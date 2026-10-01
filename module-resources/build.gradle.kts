// module-resources/build.gradle.kts
// Module 2 — Resources : résolution @color/, @string/, @dimen/, etc.
// @author jo@Dev

plugins {
    id("com.android.library")
}

android {
    namespace = "jo.layoutlib.resources"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
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
    implementation("androidx.annotation:annotation:1.8.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.assertj:assertj-core:3.26.0")
    testImplementation("net.sf.kxml:kxml2:2.3.0")
    testImplementation("androidx.annotation:annotation:1.8.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
