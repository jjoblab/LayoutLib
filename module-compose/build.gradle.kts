// module-compose/build.gradle.kts
// v3.7.0 — S9 du plan PE14 (étude § 5.4) : le SPIKE Compose C0.
//
// « Interpréter une @Composable triviale sur l'arbre résolu, la composer
// dans un ComposeView DANS la session LayoutLib existante (décor off par
// défaut), mesurer latence + mémoire » — le SEUL module Kotlin du dépôt :
// la composition exige le compilateur Compose (porté par le plugin
// compose, ALIGNÉ sur le Kotlin built-in d'AGP 9 — 2.2.10 embarqué par
// l'AGP, jamais de plugin kotlin-android versionné : miroir ADR 0007
// de CodeIDE). Le runtime est EMBARQUÉ ICI (miroir PC1 : le pack preview
// de CodeIDE l'embarquera pour :preview à C1) — le MOTEUR
// (module-inflater) reste SANS AndroidX Compose : il instancie
// `ComposeView` par RÉFLEXION comme toute vue, la session C0 la branche.
//
// @author jo@Dev
// @since 3.7.0

plugins {
    alias(libs.plugins.android.library)
    // Kotlin BUILT-IN d'AGP 9 : AUCUN plugin kotlin-android à appliquer —
    // l'extension `kotlin` est déjà posée par l'AGP (miroir ADR 0007 de
    // CodeIDE, convention `codeide.android.library`). Reste le
    // compilateur Compose — MÊME version que le Kotlin built-in (2.2.10).
    id("org.jetbrains.kotlin.plugin.compose") version libs.versions.kotlin.get()
}

android {
    namespace = "jo.layoutlib.compose"
    compileSdk = 37
    compileSdkMinor = 2

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    // La session C0 conduit LE moteur (inflater + LayoutEngine) — le
    // module-inflater en api : le harnais de l'app démo n'a rien à savoir.
    api(project(":module-inflater"))
    implementation(project(":module-layout"))

    // Le runtime Compose embarqué — versions par le BOM uniquement.
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)

    // Le harnais d'activité (ViewTreeLifecycleOwner/SavedStateRegistry
    // pour la composition dans le ComposeView attaché).
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Tests JVM — l'INTERPRÉTEUR est pur (chaînes), testé sans Android.
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.assertj.core)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
