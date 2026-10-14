# Module 5 — Attributes

> Parsing de `<declare-styleable>` et application des attributs custom `app:*`.
>
> @author jo@Dev
> **Statut :** ✅ Livré (Phase 5)

## Classes

| Classe | Lignes | Rôle |
|--------|--------|------|
| `AttributeRegistry` | ~45 | Interface |
| `AttributeRegistryImpl` | ~230 | Implémentation avec parser XML |
| `AttributeDefinition` | ~200 | POJO d'un attribut (nom, formats, enums, flags) |
| `AttributeFormat` | ~85 | Enum des 10 formats supportés |
| `AttributeException` | ~40 | Exception |
| **Total** | **~600** | |

## API publique

```java
AttributeRegistry registry = new AttributeRegistryImpl();
registry.registerAttrsFile(attrsXml);

// Vérification
boolean known = registry.isKnownAttribute("cornerRadius");
String format = registry.getAttributeFormat("iconGravity");  // "enum"

// Définition complète
AttributeDefinition attr = registry.getAttributeDefinition("iconGravity");
Integer enumValue = attr.getEnumValue("textStart");  // 1
Integer flagValue = attr.getFlagValue("bold|italic");  // 3 (OR)
```

## Fonctionnalités

- ✅ Parsing `<declare-styleable name="...">` avec enfants `<attr>`
- ✅ Parsing des attributs globaux (hors styleable)
- ✅ 10 formats supportés : dimension, color, reference, string, integer, float, boolean, fraction, enum, flag
- ✅ Support des enums (`<enum name="..." value="..."/>`)
- ✅ Support des flags combinables par `|` (avec valeurs hex `0x...`)
- ✅ Validation de valeurs selon les formats déclarés
- ✅ Héritage implicite via le nom du styleable parent

## Tests

| Type | Fichier | Cas |
|------|---------|-----|
| JVM | `AttributeFormatTest` | 8 |
| JVM | `AttributeDefinitionTest` | 14 |
| JVM | `AttributeRegistryImplTest` | 11 |
| JVM | `CustomAttributeApplierTest` (P3/L8) | 18 |
| JVM | `AttributeRegistryImplCustomAttributesTest` (P3/L8) | 8 |
| **Total** | | **59 cas** |

## Application des attributs app: (ADR 0116 — P3/L8)

`AttributeRegistryImpl.applyCustomAttributes(view, parser, context)` est
invoquée par le `BridgeInflater` pour chaque vue créée (registre par défaut
installé par le `RenderService`, remplaçable par
`RenderService.setAttributeRegistry(...)`) :

- énumère les attributs du namespace `app:` (res-auto) sur le tag courant ;
- **attributs connus** Material/AndroidX (elevation, backgroundTint,
  card*, contentPadding*, cornerRadius, tint…) appliqués via les setters
  natifs des vues framework (un `CardView` prévisualisé = `FrameLayout`) ;
- **attributs sans setter natif** (layout_behavior, fabSize, strokeWidth…)
  reconnus et neutralisés — plus d'avertissement parasite ;
- **attributs inconnus** (vues custom, DataBinding) : repli réflexion sur les
  `set<Name>(String/int/float/boolean/...)` avec cache — comme l'AOSP
  layoutlib pour les vues custom ;
- les `app:layout_constraint*` restent au solver Cassowary (P2/L9).

## Limitations v1.0

- ✅ ~~`applyCustomAttributes()` est un stub~~ — implémenté (P3/L8) :
  application typée + repli réflexion, câblée au pipeline
- ❌ Pas de résolution des références `@color/`, `@dimen/` dans les valeurs d'attributs
- ❌ Pas de support des attributs hérités via `parent` sur les styleables
