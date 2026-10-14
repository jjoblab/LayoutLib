# Module 2 — Resources

> Résolution des références `@color/`, `@string/`, `@dimen/`, `@integer/`, `@bool/`, `@layout/`, `@drawable/`.
>
> @author jo@Dev
> **Statut :** ✅ Livré (Phase 2)

## Responsabilité

Parser les fichiers XML de resources du projet Android (`res/values/*.xml`) et exposer une API de résolution des références avec support des qualifiers (langue/région, jour/nuit, portrait/paysage, niveau d'API, dimensions d'écran sw/w/h…dp) et des références chainables.

## Classes

| Classe | Lignes | Rôle |
|--------|--------|------|
| `ResourceResolver` | ~80 | Interface principale (contrat) |
| `ResourceResolverImpl` | ~370 | Implémentation avec cache LRU + scan dossier |
| `ResourceTable` | ~210 | Stockage indexé par nom + qualifier |
| `ResourceFileParser` | ~200 | Parser XML multi-types (colors/strings/dimens/...) |
| `ResourceQualifier` | ~490 | Représentation + compatibilité qualifiers (langue, sw/w/h, nuit, land, API) |
| `ResourceCache` | ~100 | Cache LRU générique |
| `DimensionConverter` | ~150 | Conversion dp/sp/px/mm/in/pt → pixels |
| `ColorParser` | ~170 | Parsing couleurs hex + nommées |
| `ResourceException` | ~60 | Exception unchecked |
| **Total** | **~1 530** | |

## API publique

```java
// Construction depuis un dossier res/
ResourceResolver resolver = new ResourceResolverImpl("/path/to/res");

// Configuration du mode
resolver.setNightMode(true);
resolver.setLandscape(false);
resolver.setApiLevel(31);
resolver.setLanguage("fr");        // values-fr/ (repli auto sur values/)
resolver.setLocale("fr", "FR");    // variante régionale values-fr-rFR/

// Résolution
Integer color = resolver.getColor("@color/primary");
String text = resolver.getString("@string/hello");
Float dimen = resolver.getDimension("@dimen/margin");
Integer number = resolver.getInteger("@integer/max");
Boolean enabled = resolver.getBoolean("@bool/enabled");
String layoutXml = resolver.getLayout("@layout/login_form");
String drawablePath = resolver.getDrawablePath("@drawable/icon");
```

## Fonctionnalités supportées

- ✅ Parsing `colors.xml`, `strings.xml`, `dimens.xml`, `integers.xml`, `bools.xml`
- ✅ Support des qualifiers `values-night/`, `values-land/`, `values-v31/`
- ✅ Support des qualifiers de langue `values-fr/`, `values-en/`, `values-fr-rFR/` (et BCP47 `b+fr`) avec repli automatique sur `values/`
- ✅ Support des qualifiers de dimensions d'écran `values-sw600dp/`, `values-w600dp/`, `values-h720dp/` (ADR 0116 — L1) : sémantique de seuil Android (un dossier ne sert que les écrans atteignant sa dimension), bascule via `setSmallestScreenWidth(int)` / `setScreenWidth(int)` / `setScreenHeight(int)`, repli automatique sur `values/` — les dossiers tablette ne concourent plus pour la config téléphone
- ✅ Priorité AOSP : locale > région > sw > w > h > mode nuit > orientation > niveau d'API
- ✅ Résolution chainable : `<color name="primary">@color/purple_500</color>`
- ✅ Cache LRU (512 entrées par défaut, configurable)
- ✅ Couleurs hex 8/6/4/3 chiffres + couleurs nommées (red, blue, transparent, ...)
- ✅ Dimensions : `dp`, `dip`, `sp`, `px`, `mm`, `in`, `pt`
- ✅ Booléens : `true`/`false`/`1`/`0` (insensible à la casse)
- ✅ Invalidation automatique du cache au changement de mode
- ✅ Gestion des resources manquantes (return `null` + pas d'exception)
- ✅ Profondeur de chaînage limitée à 5 (anti-boucle)

## Tests

| Type | Fichier | Nombre de cas |
|------|---------|---------------|
| JVM | `ColorParserTest` | 18 |
| JVM | `ResourceQualifierTest` | 12 |
| JVM | `ResourceQualifierLocaleTest` | 26 |
| JVM | `ResourceQualifierScreenSizeTest` (L1) | 20 |
| JVM | `DimensionConverterTest` | 14 |
| JVM | `ResourceFileParserTest` | 13 |
| JVM | `ResourceResolverImplTest` | 20 |
| JVM | `ResourceResolverImplLocaleTest` | 15 |
| JVM | `ResourceResolverImplScreenSizeTest` (L1) | 15 |
| JVM | `ResourceCacheTest` | 9 |
| JVM | `ResourceTableTest` | 11 |
| JVM | `ResourceStringArrayTest` | 6 |
| Instrumenté | `ResourceResolverImplInstrumentedTest` | 10 |
| **Total** | | **189 cas** |

## Exécution

```bash
# Tests JVM
./gradlew :module-resources:test

# Tests instrumentés
./gradlew :module-resources:connectedAndroidTest
```

## Référence layoutlib

- `com.android.layoutlib.bridge.android.BridgeContext`
- `com.android.ide.common.resources.ResourceResolver`

## Limitations v1.0

- ❌ Pas de support des `<plurals>` (les `<string-array>` sont exposés)
- ❌ Pas de résolution des références `?attr/foo` (gérées par Module 4 — Themes)
