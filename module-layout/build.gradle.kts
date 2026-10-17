// module-layout/build.gradle.kts
// Module 6 — Layout engine : measure/layout algorithms
// @author jo@Dev

plugins {
    id("maven-publish")
    alias(libs.plugins.android.library)
}

android {
    // Publication : variante unique release (AGP 9 — le composant
    // « release » de maven-publish n'existe qu'une fois la variante
    // déclarée ici).
    publishing {
        singleVariant("release")
    }
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
    testImplementation(libs.mockito.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
}

tasks.withType<Test> {
    useJUnitPlatform()
}


// Publication GitHub Packages (miroir codeide-tooling, mise en place du
// 2026-10-17 — PLAN_SEPARATION_DEPOTS.md §9) : composant release,
// coordonnées com.github.jjoblab.LayoutLib:<module> — les MÊMES coordonnées que la
// consommation composite de CodeIDE (substitution, version déclarée
// ignorée en dev ; l'épinglage = version du catalogue + tag du dépôt).


// Publication GitHub Packages (miroir codeide-tooling, mise en place du
// 2026-10-17 — PLAN_SEPARATION_DEPOTS.md §9) : composant RELEASE (AAR),
// coordonnées com.github.jjoblab.LayoutLib:<module> — les MÊMES coordonnées que la
// consommation composite de CodeIDE (substitution, version déclarée
// ignorée en dev ; l'épinglage = version du catalogue + tag du dépôt).
// AGP 9 : le composant « release » n'est créé qu'en afterEvaluate — la
// publication est donc déclarée DANS afterEvaluate (accès direct =
// « SoftwareComponent with name 'release' not found »).
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["release"])
            }
        }
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/jjoblab/LayoutLib")
                credentials {
                    username = System.getenv("GITHUB_ACTOR")
                    password = System.getenv("GITHUB_TOKEN")
                }
            }
        }
    }
}
