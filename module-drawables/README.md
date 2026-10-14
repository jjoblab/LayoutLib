# Module 3 — Drawables

> Parsing et rendu des drawables XML : `<shape>`, `<selector>`, `<vector>`, `<layer-list>`, `<ripple>`, `<inset>`, `<bitmap>`, `<color>`, `<adaptive-icon>`.
>
> @author jo@Dev
> **Statut :** ✅ Livré (Phase 3) — shape, selector, vector, color + composites (ADR 0116 P0/L3) et adaptive icons (P4/L6)

## Responsabilité

Transformer un XML de drawable ou une référence `@drawable/foo` en un objet `android.graphics.drawable.Drawable`.

## Classes

| Classe | Lignes | Rôle |
|--------|--------|------|
| `DrawableResolver` | ~30 | Interface (contrat) |
| `DrawableResolverImpl` | ~900 | Implémentation avec cache + détection type + création des drawables |
| `ShapeParser` + `ShapeConfig` | ~470 | Parser et POJO pour `<shape>` |
| `SelectorParser` + `SelectorConfig` | ~330 | Parser et POJO pour `<selector>` |
| `VectorParser` + `VectorConfig` | ~440 | Parser et POJO pour `<vector>` |
| `LayerListParser` + `LayerListConfig` | ~330 | Parser et POJO pour `<layer-list>` (P0/L3) |
| `RippleParser` + `RippleConfig` | ~200 | Parser et POJO pour `<ripple>` (P0/L3) |
| `InsetParser` + `InsetConfig` | ~260 | Parser et POJO pour `<inset>` (P0/L3) |
| `BitmapParser` + `BitmapConfig` | ~250 | Parser et POJO pour `<bitmap>` (P0/L3) |
| `AdaptiveIconParser` + `AdaptiveIconConfig` | ~330 | Parser et POJO pour `<adaptive-icon>` (P4/L6) |
| `DrawableXmlFragments` | ~150 | Re-sérialisation XML inline pour la récursion (P0/L3) |
| `DrawableAttributeParser` | ~95 | Utilitaire (angles, types) |
| `ShapeType` | ~25 | Enums (ShapeType, GradientType, GradientAngle) |
| `DrawableException` | ~50 | Exception unchecked |

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

### ✅ Layer-list (LayerDrawable) — P0/L3
- Items par référence `@drawable/`/`@color/`/littéral, ou drawable inline
- Offsets/insets par item (`left/top/right/bottom`) + gravité
- Résolution récursive via re-sérialisation XML inline

### ✅ Ripple (RippleDrawable) — P0/L3
- Couleur du ripple + contenu/masque optionnels
- Rendu statique = frame au repos

### ✅ Inset (InsetDrawable) — P0/L3
- Insets absolus ou fractionnaires (`10%`)
- Drawable encapsulé par référence ou inline

### ✅ Bitmap (BitmapDrawable) — P0/L3
- Gravité, tile mode (`clamp/repeat/mirror`), anti-alias
- Repli ColorDrawable quand l'image n'est pas résolvable

### ✅ Adaptive-icon — P4/L6
- Couches `<background>`/`<foreground>`/`<monochrome>` par référence ou inline
- API 33+ : `AdaptiveIconDrawable` à trois couches ; API 26-32 : deux couches ;
  API 24-25 et JVM : repli `LayerDrawable` (superposition statique)

## Tests

| Type | Fichier | Nombre de cas |
|------|---------|---------------|
| JVM | `ShapeParserTest` | 17 |
| JVM | `SelectorParserTest` | 12 |
| JVM | `VectorParserTest` | 9 |
| JVM | `DrawableResolverImplTest` + `DrawableResolverCreationTest` | 15 |
| JVM | `LayerListParserTest` (P0) | 15 |
| JVM | `RippleParserTest` (P0) | 12 |
| JVM | `InsetParserTest` (P0) | 11 |
| JVM | `BitmapParserTest` (P0) | 19 |
| JVM | `CompositeDrawablesCreationTest` (P0) | 26 |
| JVM | `AdaptiveIconParserTest` (P4) | 10 |
| JVM | `AdaptiveIconCreationTest` (P4) | 9 |
| Instrumenté | `DrawableResolverImplInstrumentedTest` | 10 |
| **Total** | | **155 JVM + 10 instrumentés** |

## Architecture

```
XML source
    │
    ▼
DrawableResolverImpl.parse(xml)
    │
    ├─ detectRootTag(xml) → "shape" / "selector" / "vector" / "layer-list" /
    │                       "ripple" / "inset" / "bitmap" / "adaptive-icon" / "color"
    │
    ├─ shapeParser.parse(xml) → ShapeConfig (POJO)
    ├─ selectorParser.parse(xml) → SelectorConfig (POJO)
    ├─ vectorParser.parse(xml) → VectorConfig (POJO)
    ├─ layerListParser / rippleParser / insetParser / bitmapParser / adaptiveIconParser
    │
    └─ createShapeDrawable(config, context) → GradientDrawable
       createSelectorDrawable(config, context) → StateListDrawable
       createVectorDrawable(config, context) → VectorDrawable
       createColorDrawable(config) → ColorDrawable
       createLayerListDrawable / createRippleDrawable / createInsetDrawable /
       createBitmapDrawable / createAdaptiveIconDrawable → composites
```

La séparation **parsing → POJO → Drawable Android** permet de tester la logique de parsing en JVM pure (sans device Android).

## Exécution

```bash
./gradlew :module-drawables:test
./gradlew :module-drawables:connectedAndroidTest
```

## Référence layoutlib

- `com.android.layoutlib.bridge.impl.ResourceHelper.getDrawable()`

## Limitations

- ❌ Pas de support des animated-vector (ADR 0116 P5/L5 — première frame uniquement en preview statique)
- ❌ StateListDrawable avec drawables enfants (ADR 0116 L4 — à clarifier)
- ❌ VectorDrawable complet (pathData rendering) — à brancher avec VectorDrawableCompat
