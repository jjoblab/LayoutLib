# Module 1 — Inflater

> XML parser → View tree. Module fondation de LayoutLib.
>
> @author jo@Dev
> **Statut :** ✅ Livré (Phase 1)

## Responsabilité

Transformer un XML de layout Android en un arbre de `android.view.View`. C'est le point d'entrée de toute inflation.

## Classes

| Classe | Lignes | Rôle |
|--------|--------|------|
| `MiniLayoutLib` | ~140 | Façade publique |
| `BridgeInflater` | ~370 | Inflater principal récursif |
| `XmlPreprocessor` | ~190 | Normalisation XML avant parsing |
| `ViewTagRegistry` | ~210 | Mapping tags → classes |
| `ViewFactory` | ~170 | Instanciation par réflexion |
| `ViewStubController` | ~100 | Gestion ViewStub paresseux |
| `InflateException` | ~80 | Exception unchecked |
| **Total** | **~1 260** | |

## API publique

```java
// Usage simple
MiniLayoutLib layoutLib = new MiniLayoutLib(context);
View root = layoutLib.inflate(xml);

// Usage avancé
BridgeInflater inflater = new BridgeInflater(context, customRegistry);
inflater.setResourceResolver(resolver);
inflater.setStrictMode(true);
View root = inflater.inflate(xml, parentViewGroup);
```

## Fonctionnalités supportées

- ✅ Parsing XML via `XmlPullParser` (namespace-aware)
- ✅ Création de vues par réflexion `(Context)` / `(Context, AttributeSet)` / `()`
- ✅ Résolution des tags framework Android (`TextView`, `Button`, `LinearLayout`, …)
- ✅ Résolution des tags AndroidX/Material pleinement qualifiés
- ✅ Pré-processing : `tools:text` → `android:text` (7 attributs convertis)
- ✅ Pré-processing : `*Start` → `*Left`, `*End` → `*Right`
- ✅ Support `<include layout="@layout/foo" />` (récursif)
- ✅ Support `<merge>` (enfants dans le parent)
- ✅ Support `<ViewStub>` (lazy via `ViewStubController`)
- ✅ Application des attributs de base (id, visibility, padding, layout_*)
- ✅ Index des vues par id (`findViewById` sur la façade)
- ✅ Cache des constructeurs résolus

## Tests

| Type | Fichier | Nombre de cas |
|------|---------|---------------|
| JVM | `XmlPreprocessorTest` | 14 |
| JVM | `ViewTagRegistryTest` | 18 |
| JVM | `BridgeInflaterTest` | 8 |
| JVM | `InflateExceptionTest` | 4 |
| Instrumenté | `BridgeInflaterInstrumentedTest` | 10 |
| **Total** | | **54 cas** |

## Exécution

```bash
# Tests JVM
./gradlew :module-inflater:test

# Tests instrumentés
./gradlew :module-inflater:connectedAndroidTest
```

## Limitations v1.0

- ❌ Les attributs custom `app:*` sont ignorés (gérés par Module 5 à venir)
- ❌ Les références `?attr/foo` ne sont pas résolues (gérées par Module 4)
- ❌ Les drawables `@drawable/foo` ne sont pas résolus (gérés par Module 3)
- ❌ Le background n'est pas appliqué (nécessite Module 3)
- ❌ Pas de mesure/layout (géré par Module 6)
