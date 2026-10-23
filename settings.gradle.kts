// settings.gradle.kts — Configuration racine du projet LayoutLib
// @author jo@Dev

rootProject.name = "LayoutLib"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // Aligné sur CodeIDE : aucun dépôt déclaré au niveau projet (mode
    // strict) — la résolution passe intégralement par ce bloc.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // code-editor (github.com/jjoblab/code-editor) est publié via JitPack.
        maven {
            url = uri("https://jitpack.io")
            content { includeGroupByRegex("com\\.github\\.jjoblab.*") }
        }
    }
}

include(":module-inflater")
include(":module-resources")
include(":module-drawables")
include(":module-themes")
include(":module-attributes")
include(":module-layout")
include(":module-compose")
include(":validation")
include(":layout-editor-app")
