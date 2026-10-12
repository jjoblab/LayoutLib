# LayoutLib — Build instructions

## Architecture du projet

Un **seul build Gradle** (`LayoutLib/`) : 6 modules de rendu, le module `validation` et l'app `layout-editor-app`.

L'éditeur de code [`jjoblab/code-editor`](https://github.com/jjoblab/code-editor) est une **dépendance Maven** récupérée via JitPack — il n'y a plus de composite build ni de dossier `code-editor-lib-dir` à placer à côté du projet.

```kotlin
// settings.gradle.kts — dépôt JitPack
maven {
    url = uri("https://jitpack.io")
    content { includeGroupByRegex("com\\.github\\.jjoblab.*") }
}

// layout-editor-app/build.gradle.kts
val codeEditorVersion = "3.49.2"   // tag Git de jjoblab/code-editor (aligné CodeIDE)
dependencies {
    implementation("com.github.jjoblab.code-editor:cel-ui:$codeEditorVersion")
}
```

`cel-ui` expose `cel-core` et `cel-lsp-api` en `api` : `EditorView`, `EditorSession` et `EditorDocument` sont disponibles sans déclaration supplémentaire. Pour changer de version, modifier uniquement `codeEditorVersion`.

**Remarques**
- Au premier build, JitPack construit la bibliothèque à la demande depuis le tag : la première résolution peut être lente ou échouer (timeout) ; relancer le build.
- **Coordonnée confirmée** : le dépôt étant multi-modules, JitPack publie sous le groupe `com.github.jjoblab.code-editor` (vérifié : `https://jitpack.io/com/github/jjoblab/code-editor/cel-ui/3.49.2/`).
- Alternative : GitHub Packages (`jo.codeeditor:cel-ui:3.49.2`, dépôt `https://maven.pkg.github.com/jjoblab/code-editor`) — nécessite `gpr.user` / `gpr.key` dans `~/.gradle/gradle.properties`.

## Outils requis

Environnement **aligné sur CodeIDE** (décision propriétaire 2026-10-13) : les
 deux dépôts partagent le même outillage (JDK, Gradle, AGP) — démon Gradle et
caches partagés.

- **OpenJDK 21** (Temurin recommandé) — `java -version` doit afficher 21.x
- **Android SDK** avec :
  - `platform-tools`
  - `platforms;android-37` (mineure 37.2 — auto-installée par AGP si les licences sont acceptées)
  - `build-tools` (la version requise par AGP 9.4.1 est auto-installée)
  - `cmdline-tools;latest`
- **Gradle 9.7.1** (le wrapper `./gradlew` est inclus)
- **AGP 9.4.1**
- Accès réseau à `jitpack.io` au premier build (voir « Dépendance code-editor »)

## Installation rapide (Linux)

```bash
# 1. Temurin JDK 21
# (CodeIDE utilise l'API « latest » d'Adoptium ; URL de release fixe :)
curl -fsSL -o jdk21.tar.gz \
  "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.12%2B1/OpenJDK21U-jdk_x64_linux_hotspot_21.0.12_1.tar.gz"
tar -xzf jdk21.tar.gz
export JAVA_HOME=$(pwd)/jdk-21.0.12+1

# 2. Android cmdline-tools
curl -fsSL -o cmdline-tools.zip \
  "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
unzip -q cmdline-tools.zip
mkdir -p android-sdk/cmdline-tools
mv cmdline-tools android-sdk/cmdline-tools/latest
export ANDROID_HOME=$(pwd)/android-sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "platform-tools" "platforms;android-37" "build-tools;36.0.0"

# 3. Variables d'environnement
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH
```

## Structure des dossiers

```
LayoutLib/
├── settings.gradle.kts
├── module-inflater/         # RenderService, BridgeInflater, OverlayView, etc.
├── module-resources/        # ResourceResolverImpl, ResourceTable
├── module-drawables/        # parsers de drawables
├── module-themes/           # parsers de thèmes
├── module-attributes/       # parsers d'attributs
├── module-layout/           # engine de layout + cassowary
├── validation/              # tests de comparaison
└── layout-editor-app/       # ← app principale
    └── src/main/java/jo/layoutlib/editor/
        ├── MainActivity.java        # Intégration EditorView + RenderService
        └── PaletteAdapter.java      # Snippets XML pour le popup FAB
```

## Build de l'APK

```bash
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew :layout-editor-app:assembleDebug
```

L'APK debug sera généré dans :
```
layout-editor-app/build/outputs/apk/debug/layout-editor-app-debug.apk
```

## Build de tous les modules (tests inclus)

```bash
./gradlew build
```

## Points d'entrée principaux

### Rendu (modules LayoutLib)
- `jo.layoutlib.inflater.RenderService` — rendu XML avec debounce
- `jo.layoutlib.inflater.BridgeInflater` — inflation XML → View tree
- `jo.layoutlib.inflater.ViewInfoCollector` — collecte hiérarchie
- `jo.layoutlib.inflater.OverlayView` — overlay design/blueprint (8 handles, drag-to-move, drag-to-resize)
- `jo.layoutlib.inflater.BlueprintListView` — liste de bp-box (mode Blueprint)
- `jo.layoutlib.inflater.ComponentPalettePopup` — popup palette (FAB)
- `jo.layoutlib.inflater.ColorSwatchPopup` — popup color picker
- `jo.layoutlib.resources.ResourceResolverImpl` — résolution @color/@string/@dimen

### code-editor (dépendance externe `jjoblab/code-editor`)
- `jo.codeeditor.view.EditorView` — éditeur Canvas (gutter, folds, IME, scroll)
- `jo.codeeditor.session.EditorSession` — session (texte, undo/redo, listeners)
- `jo.codeeditor.document.EditorDocument` — document (Rope-based)
- `jo.codeeditor.view.chrome.EditorTheme` — thème (31 couleurs)

### layout-editor-app
- `jo.layoutlib.editor.MainActivity` — activité principale (intégration)
- `jo.layoutlib.editor.PaletteAdapter` — snippets XML pour le popup FAB

## Connexion RenderService (pattern canonique)

Extrait de `MainActivity.setupRenderService()` :

```java
float density   = getResources().getDisplayMetrics().density;
float fontScale = getResources().getDisplayMetrics().scaledDensity;
float xdpi      = getResources().getDisplayMetrics().xdpi;

renderService = new RenderService(this);
renderService.setDebounceMs(400);
renderService.setTargetDimensions(widthPixels, heightPixels / 2);
renderService.setDimensionConverter(new DimensionConverter(density, fontScale, xdpi));

// ═══ CRITIQUE : ResourceResolver pour résoudre @color/@string/@dimen ═══
ResourceTable table = new ResourceTable();
ResourceResolverImpl resourceResolver = new ResourceResolverImpl(table);
resourceResolver.setDimensionConverter(new DimensionConverter(density, fontScale, xdpi));
renderService.setResourceResolver(resourceResolver);

renderService.setRenderCallback(new RenderService.RenderCallback() {
    @Override
    public void onRenderSuccess(View root, long timeMs, int viewCount, int width, int height) {
        runOnUiThread(() -> {
            previewContainer.removeAllViews();
            previewContainer.addView(root);
            // ... overlay, status, etc.
        });
    }
    @Override public void onRenderError(String message, Throwable cause) { /* ... */ }
    @Override public void onXmlInvalid(String message) { /* no-op : keep last valid render */ }
});
```

## Rendu automatique

Le rendu est **automatique** : chaque modification du texte dans `EditorView`
déclenche via `session.setOnTextEditListener()` un `renderService.requestRender(xml)`
debouncé à 400ms. Le XML est validé avant rendu (si invalide, on garde le dernier
rendu valide — comme Android Studio).

## Dépendances principales

- AGP 9.4.1
- Gradle 9.7.1
- AndroidX appcompat 1.8.0
- AndroidX recyclerview 1.4.0
- Material Components 1.14.0
- AndroidX constraintlayout 2.1.4
- compileSdk 37.2, minSdk 24, targetSdk 34
