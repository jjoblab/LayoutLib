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
