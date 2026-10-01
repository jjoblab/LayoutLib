# Module 6 — Layout Engine

> Wrapper autour de `View.measure()` et `View.layout()` avec gestion fine des `MeasureSpec`.
>
> @author jo@Dev
> **Statut :** ✅ Livré (Phase 6)

## Classes

| Classe | Lignes | Rôle |
|--------|--------|------|
| `LayoutEngine` | ~50 | Interface |
| `LayoutEngineImpl` | ~170 | Implémentation avec MeasureSpec builder |
| `LayoutException` | ~15 | Exception |
| **Total** | **~235** | |

## API publique

```java
LayoutEngine engine = new LayoutEngineImpl();
engine.setDensity(2.0f);
engine.setFontScale(1.0f);
engine.setXdpi(320f);

// Mesurer
engine.measure(rootView, 1080, 1920);

// Positionner
engine.layout(rootView, 0, 0, 1080, 1920);

// Mesurer + positionner en une fois
engine.render(rootView, 1080, 1920);

// Configurer les DisplayMetrics d'un contexte
engine.configureDisplayMetrics(context.getResources().getDisplayMetrics());
```

## Fonctionnalités

- ✅ Construction de `MeasureSpec` intelligente selon les `LayoutParams`
- ✅ Support des 3 modes : `EXACTLY`, `AT_MOST`, `UNSPECIFIED`
- ✅ Gestion `MATCH_PARENT` → `EXACTLY`
- ✅ Gestion `WRAP_CONTENT` → `AT_MOST`
- ✅ Gestion dimensions fixes → `EXACTLY`
- ✅ Configuration densité + fontScale + xdpi
- ✅ Configuration des `DisplayMetrics` d'un contexte

## Algorithme de MeasureSpec

```
buildMeasureSpec(targetSize, lp, isWidth):
  if targetSize <= 0:
    return UNSPECIFIED(0)
  if lp == null:
    return EXACTLY(targetSize)
  size = isWidth ? lp.width : lp.height
  if size == MATCH_PARENT:
    return EXACTLY(targetSize)
  if size == WRAP_CONTENT:
    return AT_MOST(targetSize)
  return EXACTLY(size)  // dimension fixe
```

## Tests

| Type | Fichier | Cas |
|------|---------|-----|
| Instrumenté | `LayoutEngineImplInstrumentedTest` | 11 |
| **Total** | | **11 cas** |

## Limitations v1.0

- ❌ Pas de hook `LayoutInflater.Factory2`
- ❌ Pas de gestion `LayoutTransition` (animations de layout)
- ❌ Pas de gestion `ViewTreeObserver` (callbacks)
- ❌ Pas de solver Cassowary pour ConstraintLayout
- ❌ Tests JVM purs non fournis (nécessitent des vues Android réelles)
