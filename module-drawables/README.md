# Module 3 — Drawables

> Parsing et rendu des drawables XML : `<shape>`, `<selector>`, `<vector>`, `<layer-list>`, `<ripple>`, `<inset>`, `<bitmap>`, `<color>`.
>
> @author jo@Dev
> **Statut :** ✅ Livré (Phase 3) — shape, selector, vector, color fully implemented

## Responsabilité

Transformer un XML de drawable ou une référence `@drawable/foo` en un objet `android.graphics.drawable.Drawable`.

## Classes

| Classe | Lignes | Rôle |
|--------|--------|------|
| `DrawableResolver` | ~30 | Interface (contrat) |
| `DrawableResolverImpl` | ~280 | Implémentation avec cache + détection type |
| `ShapeParser` + `ShapeConfig` | ~470 | Parser et POJO pour `<shape>` |
| `SelectorParser` + `SelectorConfig` | ~330 | Parser et POJO pour `<selector>` |
| `VectorParser` + `VectorConfig` | ~440 | Parser et POJO pour `<vector>` |
| `DrawableAttributeParser` | ~95 | Utilitaire (angles, types) |
| `ShapeType` | ~25 | Enums (ShapeType, GradientType, GradientAngle) |
| `DrawableException` | ~50 | Exception unchecked |
| **Total** | **~1 720** | |

## API publique

```java
// Sans dépendances
DrawableResolver resolver = new DrawableResolverImpl();

// Avec résolveur de resources et convertisseur
DrawableResolver resolver = new DrawableResolverImpl(resourceResolver, dimensionConverter);

// Résolution depuis une référence @drawable/
Drawable d = resolver.resolve("@drawable/my_bg", context);

// Parsing direct d'un XML inline
Drawable d = resolver.parse("<shape>...</shape>", context);

// Récupération de la config (POJO) pour inspection
Object config = resolver.parseConfig(xml);  // ShapeConfig, SelectorConfig, VectorConfig
```

## Fonctionnalités supportées

### ✅ Shape (GradientDrawable)
- Types : `rectangle`, `oval`, `line`, `ring`
- `<solid>` couleur unie
- `<gradient>` avec startColor, centerColor, endColor, angle, type (linear/radial/sweep), gradientRadius
- `<corners>` avec radius global + radii individuels (topLeft, topRight, bottomLeft, bottomRight)
- `<stroke>` avec width, color, dashWidth, dashGap
- `<padding>` 4 côtés
- `<size>` (taille intrinsèque)

### ✅ Selector (StateListDrawable)
- Items avec états : `state_pressed`, `state_enabled`, `state_focused`, `state_checked`, `state_selected`, `state_window_focused`, `state_checkable`
- Item par défaut (sans état)
- Algorithme de matching suivant la logique Android

### ✅ Vector (VectorDrawable)
- Dimensions (width, height) et viewport
- Tint, tintMode, alpha, autoMirrored
- `<group>` avec rotation, pivot, scale, translate
- `<path>` avec fillColor, strokeColor, strokeWidth, fillAlpha, strokeAlpha, strokeLineCap, strokeLineJoin, strokeMiterLimit, fillType, pathData
- Groupes imbriqués récursivement

### ✅ Color (ColorDrawable)
- Tag `<color>` avec attribut `android:color`

### 🚧 Layer-list, Ripple, Inset, Bitmap
- Détection du type implémentée
- Création du Drawable Android à implémenter dans une prochaine version

## Tests

| Type | Fichier | Nombre de cas |
|------|---------|---------------|
| JVM | `ShapeParserTest` | 16 |
| JVM | `SelectorParserTest` | 11 |
| JVM | `VectorParserTest` | 9 |
| JVM | `DrawableResolverImplTest` | 7 |
| Instrumenté | `DrawableResolverImplInstrumentedTest` | 10 |
| **Total** | | **53 cas** |

## Architecture

```
XML source
    │
    ▼
DrawableResolverImpl.parse(xml)
    │
    ├─ detectRootTag(xml) → "shape" / "selector" / "vector" / ...
    │
    ├─ shapeParser.parse(xml) → ShapeConfig (POJO)
    ├─ selectorParser.parse(xml) → SelectorConfig (POJO)
    ├─ vectorParser.parse(xml) → VectorConfig (POJO)
    │
    └─ createShapeDrawable(config, context) → GradientDrawable
       createSelectorDrawable(config, context) → StateListDrawable (TODO)
       createVectorDrawable(config, context) → VectorDrawable (TODO)
       createColorDrawable(config) → ColorDrawable
```

La séparation **parsing → POJO → Drawable Android** permet de tester la logique de parsing en JVM pure (sans device Android).

## Exécution

```bash
./gradlew :module-drawables:test
./gradlew :module-drawables:connectedAndroidTest
```

## Référence layoutlib

- `com.android.layoutlib.bridge.impl.ResourceHelper.getDrawable()`

## Limitations v1.0

- ❌ `<layer-list>`, `<ripple>`, `<inset>`, `<bitmap>` détectés mais pas créés
- ❌ StateListDrawable avec drawables enfants (nécessite résolution récursive)
- ❌ VectorDrawable complet (pathData rendering) — à brancher avec VectorDrawableCompat
- ❌ Pas de support des animated-vector
- ❌ Pas de support des adaptive icons
