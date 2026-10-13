# Module 6 — Layout Engine

> Wrapper autour de `View.measure()` et `View.layout()` avec gestion fine des `MeasureSpec` + résolution de contraintes ConstraintLayout via le solver Cassowary.
>
> @author jo@Dev
> **Statut :** ✅ Livré (Phase 6 — Cassowary câblé en 3.0, ADR 0116 P2/L9)

## Classes

| Classe | Lignes | Rôle |
|--------|--------|------|
| `LayoutEngine` | ~50 | Interface |
| `LayoutEngineImpl` | ~170 | Implémentation avec MeasureSpec builder |
| `ConstraintSpec` | ~600 | Instantané des attributs `app:layout_constraint*` (ancres, biais, ratios, chaînes, guideline/barrier) |
| `ConstraintSolver` | ~1400 | Résolution ConstraintLayout : système Cassowary + repli legacy |
| `cassowary/*` | ~700 | Solver Cassowary (variables, expressions, contraintes, forces) |
| `LayoutException` | ~15 | Exception |
| **Total** | **~2900** | |

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

// Contraintes ConstraintLayout (solver Cassowary)
ConstraintSolver solver = new ConstraintSolver();
solver.registerView(vue, "vue_a", attributesMap);  // map capturée à l'inflation
boolean cassowary = solver.solve(1080, 1920);      // true si Cassowary a abouti
int[] bornes = solver.getSolvedBounds(vue);        // {left, top, right, bottom}
```

## Fonctionnalités

- ✅ Construction de `MeasureSpec` intelligente selon les `LayoutParams`
- ✅ Support des 3 modes : `EXACTLY`, `AT_MOST`, `UNSPECIFIED`
- ✅ Gestion `MATCH_PARENT` → `EXACTLY`
- ✅ Gestion `WRAP_CONTENT` → `AT_MOST`
- ✅ Gestion dimensions fixes → `EXACTLY`
- ✅ Configuration densité + fontScale + xdpi
- ✅ Configuration des `DisplayMetrics` d'un contexte
- ✅ Résolution ConstraintLayout via Cassowary (3.0) : ancres
  parent/vue/guideline/barrier, biais, pourcentages, ratios
  (`16:9`, `W,16:9`, `H,1:2`), chaînes (`spread`/`spread_inside`/`packed`/
  pondérée), guidelines (`begin`/`end`/`percent`), barrières (2 passes),
  `match_constraint` (0dp)
- ✅ Repli automatique sur l'algorithme historique si le système échoue
  (contraintes circulaires, valeurs non finies) — le rendu n'est jamais
  pire qu'avant

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
| JVM | `cassowary/SolverTest` | 8 |
| JVM | `ConstraintSolverCassowaryTest` | 32 |
| Instrumenté | `LayoutEngineImplInstrumentedTest` | 11 |
| **Total** | | **51 cas** |

## Limitations

- ❌ Pas de hook `LayoutInflater.Factory2`
- ❌ Pas de gestion `LayoutTransition` (animations de layout)
- ❌ Pas de gestion `ViewTreeObserver` (callbacks)
- ⚠️ Baseline ConstraintLayout : approximée (centre) au lieu du
  `baseline` typographique réel
- ⚠️ Le solver Cassowary est une relaxation min-norm (Gauss-Seidel),
  pas le simplexe modifié de l'AOSP — convergence géométrique, repli
  legacy en cas d'échec
