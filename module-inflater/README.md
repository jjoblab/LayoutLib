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
| `BridgeInflater` | ~1 560 | Inflater principal récursif (contraintes P2, app:* P3, Factory2/LayoutTransition P6) |
| `XmlPreprocessor` | ~190 | Normalisation XML avant parsing |
| `ViewTagRegistry` | ~210 | Mapping tags → classes |
| `ViewFactory` | ~170 | Instanciation par réflexion |
| `ViewStubController` | ~100 | Gestion ViewStub paresseux |
| `ViewTreeObservers` | ~60 | Accès sûr au ViewTreeObserver (P6/L11) |
| `InflateException` | ~80 | Exception unchecked |

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

// Hook AOSP d'interception de création de vues (ADR 0116 — P6/L10)
inflater.setFactory2(new LayoutInflater.Factory2() {
    @Override
    public View onCreateView(View parent, String name, Context ctx,
                             AttributeSet attrs) {
        return "com.projet.CustomView".equals(name)
                ? new CustomView(ctx) : null; // null → création par défaut
    }
    @Override
    public View onCreateView(String name, Context ctx, AttributeSet attrs) {
        return null;
    }
});
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
- ✅ Attributs `app:*` appliqués via le registre du Module 5 (ADR 0116 — P3/L8, propagés aux `<include>`)
- ✅ **Hook `LayoutInflater.Factory2`** (ADR 0116 — P6/L10) : interception de la création des vues avant le défaut, sémantique AOSP (retour null → défaut, une seule factory, exception propagée en `InflateException`), propagation aux `<include>`, forward `RenderService.setFactory2(...)`
- ✅ **`android:animateLayoutChanges`** consommé (ADR 0116 — P6/L11) : `LayoutTransition` installé sur les ViewGroup (no-op visuel en preview statique), jamais d'exception, compteur `getAppliedLayoutTransitionCount()`
- ✅ **Accès sûr au `ViewTreeObserver`** via `ViewTreeObservers.of(view)` (ADR 0116 — P6/L11) : null-safe, `isAlive` vérifié, exceptions neutralisées — retourne `null` en JVM/preview

## Tests

| Type | Fichier | Nombre de cas |
|------|---------|---------------|
| JVM | `XmlPreprocessorTest` | 18 |
| JVM | `ViewTagRegistryTest` | 17 |
| JVM | `BridgeInflaterTest` | 10 |
| JVM | `InflateExceptionTest` | 4 |
| JVM | `BridgeInflaterConstraintCaptureTest` (P2) | 4 |
| JVM | `BridgeInflaterCustomAttributesTest` (P3) | 8 |
| JVM | `BridgeInflaterFactory2Test` (P6) | 10 |
| JVM | `BridgeInflaterLayoutTransitionTest` (P6) | 6 |
| JVM | `ViewTreeObserversTest` (P6) | 6 |
| JVM | `AttributeApplierDrawableTest` | 13 |
| JVM | `RenderServiceXmlValidationTest` | 17 |
| JVM | `RenderServiceWiringTest` | 16 |
| JVM | `RenderServiceConstraintTest` (P2) | 4 |
| JVM | `ModuleIntegrationTest` | 22 |
| Instrumenté | `BridgeInflaterInstrumentedTest` | 10 |
| **Total** | | **160 JVM + 10 instrumentés** |

## Exécution

```bash
# Tests JVM
./gradlew :module-inflater:test

# Tests instrumentés
./gradlew :module-inflater:connectedAndroidTest
```

## Limitations résiduelles

- ❌ Les animations de layout ne sont pas **rendues** en preview statique (`animateLayoutChanges` consommé, `LayoutTransition` installé mais sans effet visuel — ADR 0116 P6/L11)
- ❌ Les callbacks runtime du `ViewTreeObserver` ne sont pas distribués (API framework cachée — accès sûr fourni via `ViewTreeObservers`)
- ℹ️ Historique v1.0 résolu : `app:*` (P3/L8), `?attr/` (Module 4), `@drawable/`/background (Module 3), mesure/layout (Module 6)
