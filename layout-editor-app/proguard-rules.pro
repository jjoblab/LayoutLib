# layout-editor-app/proguard-rules.pro — règles R8 pour le build release
# @author jo@Dev
# @since 3.0
#
# Contexte : le mini-layoutlib instancie les vues du XML de layout par
# réflexion (ViewFactory : Class.forName(name) + Constructor.newInstance).
# R8 ne voit pas ces usages statiquement, il faut donc conserver les
# constructeurs publics des familles de vues utilisées.
#
# Ces règles sont câblées dans build.gradle.kts (proguardFiles) mais la
# minification reste désactivée : à réactiver (isMinifyEnabled = true)
# seulement après vérification du rendu sur un vrai appareil, car aucun
# test d'intégration runtime ne couvre encore la réflexion de ViewFactory.

# --- Vues instanciées par réflexion (ViewFactory) ------------------------
# Les widgets android.* viennent de la plateforme (jamais renommés),
# mais leurs constructeurs View(Context, AttributeSet) doivent exister
# dans le bytecode packagé — les bibliothèques androidx/material, elles,
# sont processées par R8.
-keep class androidx.** { public <init>(android.content.Context, android.util.AttributeSet); }
-keep class androidx.** { public <init>(android.content.Context); }
-keep class com.google.android.material.** { public <init>(android.content.Context, android.util.AttributeSet); }
-keep class com.google.android.material.** { public <init>(android.content.Context); }

# --- Éditeur externe (jo.codeeditor, JitPack) ---------------------------
# Bibliothèque tierce : on ne contrôle pas ses usages réflexifs internes.
-keep class jo.codeeditor.** { *; }

# --- Mini-layoutlib ------------------------------------------------------
# Les namespaces de vues personnalisées référencés dans les XML de démo
# sont résolus par réflexion ; on garde les noms de classes.
-keep class jo.layoutlib.** { public <init>(...); }
-keepclassmembers class jo.layoutlib.** { public <init>(...); }

# R8 coupe les stacks traces ; on les affiche dans CrashActivity
# (debug uniquement) — garder les numéros de lignes, sans intrusif.
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
