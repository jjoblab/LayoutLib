// layout-editor-app/build.gradle.kts
// @author jo@Dev

plugins {
    id("com.android.application")
}

// Version (tag Git) de jjoblab/code-editor
val codeEditorVersion = "v3.41.0"

android {
    namespace = "jo.layoutlib.editor"
    compileSdk = 34

    defaultConfig {
        applicationId = "jo.layoutlib.editor"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release { isMinifyEnabled = false }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
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
    implementation("com.github.jjoblab.code-editor:cel-ui:$codeEditorVersion")

    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core:1.13.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
}
