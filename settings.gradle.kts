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
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
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
include(":validation")
include(":layout-editor-app")
