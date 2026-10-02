# Architecture — LayoutLib

> Détail des modules et de leurs interactions. Package principal : `jo.layoutlib`.
>
> @author jo@Dev

## Vue d'ensemble

LayoutLib est organisé en 6 modules Gradle indépendants (+ `validation` et l'app `layout-editor-app`), chacun avec une responsabilité claire. Les dépendances entre modules sont strictement orientées (pas de cycles) et minimisent le couplage.

```
layout-editor-app ──► module-inflater, module-resources, module-drawables,
        │             module-themes, module-attributes, module-layout
        └──► com.github.jjoblab.code-editor:cel-ui  (EditorView + EditorSession)

module-inflater   ──► module-resources, module-attributes, module-layout
module-drawables  ──► module-resources
module-themes     ──► module-resources
module-attributes ──► module-resources
module-layout     (autonome)
validation        ──► tous les modules
```

## Module 1 — Inflater

**Statut :** ✅ Livré

**Package :** `jo.layoutlib.inflater`

### Classes

| Classe | Rôle |
|--------|------|
| `MiniLayoutLib` | Façade publique — point d'entrée unique |
| `BridgeInflater` | Inflater principal — orchestre parsing, création, attachement |
| `XmlPreprocessor` | Normalise le XML (suppression `xmlns:tools`, conversion RTL, etc.) |
| `ViewTagRegistry` | Map tags courts → classes Design/Natives |
| `ViewFactory` | Instanciation par réflexion (cache de constructeurs) |
| `ViewStubController` | Gestion des `<ViewStub>` paresseux |
| `InflateException` | Exception unchecked pour erreurs d'inflation |

### Algorithmes clés

#### Pré-traitement XML (`XmlPreprocessor.preprocess`)

1. Suppression de `xmlns:tools="http://schemas.android.com/tools"`
2. Conversion `tools:text` → `android:text` (et 6 autres attributs `tools:*`)
3. Conversion `layout_marginStart` → `layout_marginLeft`
4. Conversion `layout_marginEnd` → `layout_marginRight`
5. Conversion `paddingStart` → `paddingLeft`
6. Conversion `paddingEnd` → `paddingRight`

#### Résolution de tags (`ViewTagRegistry.resolveClassName`)

```
1. Si tag contient '.'  → retourner tel quel (FQN)
2. Si tag spécial       → retourner null (include/merge/requestFocus)
3. Si preferDesign && classe Design existe → retourner classe Design
4. Sinon si classe native existe → retourner classe native
5. Sinon si commence par majuscule → android.widget.{tag}
6. Sinon → InflateException
```

#### Création par réflexion (`ViewFactory.resolveConstructor`)

Ordre de recherche du constructeur :
1. `(Context)` — préféré
2. `(Context, AttributeSet)` — fallback
3. `()` — dernier recours

Le constructeur trouvé est mis en cache pour éviter la réflexion répétée.

#### Inflation récursive (`BridgeInflater.parseElement`)

```
parseElement(parser, parent):
  tag = parser.getName()
  if tag == "include":
    return parseInclude(parser, parent)        // récursif via layout référencé
  if tag == "merge":
    parseMerge(parser, parent)                  // enfants directement dans parent
    return null
  if tag == "requestFocus":
    return null                                 // tag vide
  
  view = viewFactory.createView(tag)
  applyBaseAttributes(view, parser)             // id, visibility, padding, etc.
  
  if view instanceof ViewGroup:
    parseChildren(parser, view)                 // récursif sur chaque enfant
  else:
    skipChildren(parser)
  
  if parent != null:
    parent.addView(view, produceLayoutParams(parent, parser))
  
  return view
```

## Modules 2 à 6

Tous livrés ; le détail des classes, de l'API et des limitations est dans le `README.md` de chaque module.

| Module | Package | Rôle | Interface principale |
|--------|---------|------|----------------------|
| `module-resources` | `jo.layoutlib.resources` | Résolution `@color/`, `@string/`, `@dimen/`, … | `ResourceResolver` (`ResourceResolverImpl`) |
| `module-drawables` | `jo.layoutlib.drawables` | Parsing `<shape>`, `<selector>`, `<vector>`, … | `DrawableResolver` (`DrawableResolverImpl`) |
| `module-themes` | `jo.layoutlib.themes` | `themes.xml`, `styles.xml`, résolution `?attr/` | `ThemeResolver` (`ThemeResolverImpl`) |
| `module-attributes` | `jo.layoutlib.attributes` | `<declare-styleable>`, formats d'attributs, `app:*` | `AttributeRegistry` |
| `module-layout` | `jo.layoutlib.layout` | Algorithmes measure/layout | `LayoutEngine` (`LayoutEngineImpl`) |

## Application — layout-editor-app

**Package :** `jo.layoutlib.editor` (namespace et `applicationId`)

Éditeur visuel de layouts : `MainActivity` relie `RenderService` (rendu automatique debouncé) à l'éditeur de code `EditorView`/`EditorSession`.

L'éditeur de code est la bibliothèque externe [`jjoblab/code-editor`](https://github.com/jjoblab/code-editor), consommée comme **dépendance Maven** (`implementation("com.github.jjoblab.code-editor:cel-ui:<version>")`) — pas de composite build. Ses packages (`jo.codeeditor.view`, `jo.codeeditor.session`, `jo.codeeditor.document`, `jo.codeeditor.view.chrome`) restent inchangés.

## Stratégie de tests

| Type | Localisation | Framework | Quantité cible |
|------|--------------|-----------|----------------|
| Unitaires JVM | `src/test/java/` | JUnit 5 + AssertJ | 20+ par module |
| Instrumentés | `src/androidTest/java/` | AndroidX Test + JUnit 4 | 10+ par module |
| Intégration | `validation/` | JUnit 5 | 50 layouts |

### Convention de nommage

- Classes de test : `{ClasseÀTester}Test`
- Méthodes de test : `should{ExpectedBehavior}` (en anglais pour lisibilité)
- Tests paramétrés via `@ParameterizedTest` + `@CsvSource` / `@ValueSource`
- Tests groupés par thème via `@Nested` + `@DisplayName`

## Métriques cibles

| Métrique | Cible |
|----------|-------|
| Couverture layouts courants | 95 % |
| Perf inflation (50 éléments) | < 200 ms |
| Taille code ajouté APK | < 500 KB |
| Tests en échec | 0 |
| Similarité rendu vs layoutlib | ≥ 95 % |
