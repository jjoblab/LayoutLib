// build.gradle.kts — Configuration racine du projet LayoutLib
// @author jo@Dev

plugins {
    id("com.android.library") version "8.5.0" apply false
    id("com.android.application") version "8.5.0" apply false
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
        ":module-layout:test"
    )
}
