// layout-editor-app/build.gradle.kts
// @author jo@Dev

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "jo.layoutlib.editor"
    compileSdk = 34

    defaultConfig {
        applicationId = "jo.layoutlib.editor"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // R8 désactivé pour l'instant : ViewFactory instancie les vues
            // par réflexion (Class.forName), ce qu'R8 ne voit pas — les
            // règles nécessaires sont prêtes dans proguard-rules.pro. Pour
            // activer la minification : isMinifyEnabled = true (puis vérifier
            // le rendu sur un vrai appareil avant distribution).
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // BuildConfig n'est plus généré globalement (gradle.properties) : seul
    // le module qui l'utilise réellement (CrashActivity) le demande.
    buildFeatures {
        buildConfig = true
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
    implementation(project(":module-inflater"))
    implementation(project(":module-resources"))
    implementation(project(":module-drawables"))
    implementation(project(":module-themes"))
    implementation(project(":module-attributes"))
    implementation(project(":module-layout"))

    // code-editor (github.com/jjoblab/code-editor) — EditorView + EditorSession.
    // Dépôt multi-modules : JitPack publie sous com.github.jjoblab.code-editor.
    // cel-ui expose cel-core et cel-lsp-api en `api` (transitif).
    implementation(libs.code.editor)

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core)
    implementation(libs.androidx.recyclerview)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    // Tests unitaires JVM (XmlMutator, UndoRedoManager)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
