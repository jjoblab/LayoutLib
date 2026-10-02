# LayoutLib

> Bibliothèque mobile-first de rendu de layouts Android, inspirée du `layoutlib` d'Android Studio mais allégée pour fonctionner on-device.
>
> **Auteur :** `jo@Dev` (toutes les Javadoc)
> **Langue des Javadoc :** Français
> **Version :** 2.0.0 — Alignée sur l'architecture AOSP layoutlib (416 classes)
> **Cible :** Android minSdk 24, compileSdk 34, Java 11
> **Référence AOSP :** https://android.googlesource.com/platform/frameworks/layoutlib

## Mission

Android Studio utilise `layoutlib` (~85 000 lignes, ~400 classes) pour afficher un rendu fidèle des layouts XML dans l'éditeur visuel. Mais `layoutlib` dépend de `java.awt` (desktop only), embarque 50 MB de natives par ABI et nécessite ~80 MB de resources du SDK. Aucune app mobile publique ne l'utilise on-device.

**LayoutLib** couvre 95 % des cas d'usage avec ~23 000 lignes au lieu de 85 000, dans une architecture 6 modules indépendants testables séparément.

## Architecture — 6 modules

```
LayoutLib/
├── module-inflater/          ← Module 1 : XML parser → View tree          ✅ Livré
├── module-resources/         ← Module 2 : @color/, @string/, @dimen/      ✅ Livré
├── module-drawables/         ← Module 3 : <shape>, <selector>, <vector>   ✅ Livré
├── module-themes/            ← Module 4 : themes.xml, ?attr/              ✅ Livré
├── module-attributes/        ← Module 5 : <declare-styleable>, app:*      ✅ Livré
├── module-layout/            ← Module 6 : measure/layout engine           ✅ Livré
├── validation/               ← Tests de comparaison avec layoutlib        ✅ Livré
└── layout-editor-app/        ← App d'édition visuelle de layouts (jo.layoutlib.editor)
```

Package principal : `jo.layoutlib` (un sous-package par module : `jo.layoutlib.inflater`, `jo.layoutlib.resources`, `jo.layoutlib.drawables`, `jo.layoutlib.themes`, `jo.layoutlib.attributes`, `jo.layoutlib.layout`, `jo.layoutlib.validation`).

## Statut de la livraison

| Module | Statut | Classes | Inspiré de l'AOSP |
|--------|--------|---------|-------------------|
| Module 1 — Inflater | ✅ Livré | 87 classes | bridge/impl, bridge/android, bridge/util, bridge/bars, bridge/binding, bridge/view |
| Module 2 — Resources | ✅ Livré | 105 classes | layoutlib-api (ResourceValue, ResourceNamespace, RenderResources) |
| Module 3 — Drawables | ✅ Livré | 78 classes | drawable delegates (AdaptiveIconDrawable_Delegate, NinePatchDrawable_Delegate, etc.) |
| Module 4 — Themes | ✅ Livré | 48 classes | StyleResourceValue, AttrResourceValue, RenderResources |
| Module 5 — Attributes | ✅ Livré | 48 classes | AttributeFormat, AttrResourceValueImpl |
| Module 6 — Layout | ✅ Livré | 39 classes | RenderSessionImpl, Cassowary solver |
| Validation | ✅ Livré | 11 classes | LayoutTestHarness (50 layouts) |
| **Total** | | **416 classes** | **21 715 lignes** |

**Référence AOSP consultée :** https://android.googlesource.com/platform/frameworks/layoutlib (327 fichiers, 43 597 lignes) + tools/base/layoutlib-api (77 fichiers, 9 108 lignes)

**Total tests : 323 cas** (240 JVM + 51 instrumentés + 32 validation)

## Démarrage rapide

### Configuration Gradle

Dans le `settings.gradle.kts` du projet hôte (le dossier `LayoutLib/` est placé à la racine de l'hôte) :

```kotlin
listOf(
    "module-inflater", "module-resources", "module-drawables",
    "module-themes", "module-attributes", "module-layout"
).forEach {
    include(":$it")
    project(":$it").projectDir = file("LayoutLib/$it")
}
```

### Utilisation depuis Kotlin/Java

```java
import jo.layoutlib.inflater.MiniLayoutLib;
import jo.layoutlib.resources.ResourceResolverImpl;
import jo.layoutlib.themes.ThemeResolverImpl;
import jo.layoutlib.drawables.DrawableResolverImpl;
import jo.layoutlib.layout.LayoutEngineImpl;

// Configuration
MiniLayoutLib layoutLib = new MiniLayoutLib(context);
ResourceResolverImpl resources = new ResourceResolverImpl("/path/to/res");
ThemeResolverImpl themes = new ThemeResolverImpl(resources, dimensionConverter);
themes.registerThemesFile(themesXml);
themes.setTheme("Theme.Material3.DayNight");

// Inflation
View root = layoutLib.inflate(xml);

// Mesure + layout
LayoutEngineImpl engine = new LayoutEngineImpl();
engine.render(root, 1080, 1920);
```

## Convention de code

Toutes les classes Java respectent les conventions suivantes :

- **Auteur :** toutes les Javadoc portent `@author jo@Dev`
- **Langue :** toutes les Javadoc sont en français
- **Depuis :** tag `@since 1.0` indiquant la version d'introduction
- **Exceptions :** les erreurs métier héritent d'exceptions unchecked propres à chaque module
- **Nullité :** les paramètres null sont rejetés par `IllegalArgumentException` sauf indication contraire
- **Séparation parsing/logique :** les parsers produisent des POJOs (testables en JVM pure), convertis ensuite en objets Android

## Build & tests

```bash
# Variables d'environnement
export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=/path/to/android-sdk

# Tests JVM de tous les modules
./gradlew testAllModules

# Tests JVM d'un module spécifique
./gradlew :module-inflater:test
./gradlew :module-resources:test
./gradlew :module-drawables:test
./gradlew :module-themes:test
./gradlew :module-attributes:test

# Tests instrumentés (nécessite émulateur/device)
./gradlew :module-inflater:connectedAndroidTest
./gradlew :module-resources:connectedAndroidTest
./gradlew :module-drawables:connectedAndroidTest
./gradlew :module-themes:connectedAndroidTest
./gradlew :module-layout:connectedAndroidTest

# APK de l'app d'édition
./gradlew :layout-editor-app:assembleDebug
```

## Dépendance code-editor

`layout-editor-app` utilise l'éditeur de code [`jjoblab/code-editor`](https://github.com/jjoblab/code-editor) (`EditorView`, `EditorSession`) comme **dépendance Maven** (JitPack), et non plus comme composite build :

```kotlin
// layout-editor-app/build.gradle.kts
val codeEditorVersion = "v3.41.0"
dependencies {
    implementation("com.github.jjoblab.code-editor:cel-ui:$codeEditorVersion")
}
```

Le dépôt JitPack est déclaré dans `settings.gradle.kts`. Les détails de build sont dans [`BUILD-INSTRUCTIONS.md`](./BUILD-INSTRUCTIONS.md).

## Documentation

- [`ARCHITECTURE.md`](./ARCHITECTURE.md) — Détail des modules et de leurs interactions
- [`BUILD-INSTRUCTIONS.md`](./BUILD-INSTRUCTIONS.md) — Outils requis, build de l'APK, points d'entrée
- `module-*/README.md` — Documentation de chaque module
- [`validation/README.md`](./validation/README.md) — Catalogue des 50 layouts de test

## Référence AOSP

Le code s'inspire (sans copier) du layoutlib original :

- Repo : https://android.googlesource.com/platform/frameworks/layoutlib/
- Mirror : https://github.com/aosp-mirror/platform_frameworks_base (dossier `tools/layoutlib/`)
- layoutlib-api : https://android.googlesource.com/platform/tools/base/+/master/layoutlib-api/

## Licence

Projet interne — aucun droit accordé sans autorisation explicite.
