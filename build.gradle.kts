// build.gradle.kts — Configuration racine du projet LayoutLib
// @author jo@Dev

plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.application) apply false
}

// Tâche utilitaire : exécute les tests JVM de tous les modules
tasks.register("testAllModules") {
    group = "verification"
    description = "Exécute les tests unitaires (JVM) de tous les modules"
    dependsOn(
        ":module-inflater:test",
        ":module-resources:test",
        ":module-drawables:test",
        ":module-themes:test",
        ":module-attributes:test",
        ":module-layout:test",
        ":validation:test"
    )
}

// Publication GitHub Packages (mise en place du 2026-10-17, miroir des
// dépôts codeide-tooling / codeide-lsp — docs CodeIDE
// PLAN_SEPARATION_DEPOTS.md §9) : coordonnées com.github.jjoblab.LayoutLib:<module>,
// les MÊMES coordonnées que la consommation composite de CodeIDE
// (substitution settings.gradle.kts — la version déclarée est ignorée
// en dev, l'épinglage = version du catalogue + tag du dépôt).
val proprietesVersion = java.util.Properties().apply {
    rootProject.file("version.properties").inputStream().use { load(it) }
}

allprojects {
    group = "com.github.jjoblab.LayoutLib"
    version = proprietesVersion.getProperty("VERSION_NAME")
}
