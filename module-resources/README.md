# Module 2 — Resources

> Résolution des références `@color/`, `@string/`, `@dimen/`, `@integer/`, `@bool/`, `@array/` (string-arrays + **integer-arrays typés** v3.3), `@plurals/`, `@layout/`, `@drawable/` + segments **`<xliff:g id/example>`** (v3.3).
>
> @author jo@Dev
> **Statut :** ✅ Livré (Phase 2)

## Responsabilité

Parser les fichiers XML de resources du projet Android (`res/values/*.xml`) et exposer une API de résolution des références avec support des qualifiers (langue/région, jour/nuit, portrait/paysage, niveau d'API, dimensions d'écran sw/w/h…dp) et des références chainables.

## Classes

| Classe | Lignes | Rôle |
|--------|--------|------|
| `ResourceResolver` | ~265 | Interface principale (contrat) |
| `ResourceResolverImpl` | ~960 | Implémentation avec cache LRU + scan dossier |
| `ResourceTable` | ~560 | Stockage indexé par nom + qualifier |
| `ResourceFileParser` | ~570 | Parser XML multi-types (colors/strings/dimens/plurals/arrays/xliff/...) |
| `ResourceQualifier` | ~580 | Représentation + compatibilité qualifiers (langue, sw/w/h, nuit, land, API) |
| `PluralRules` | ~240 | Règles de pluriel CLDR (sélection one/other/few/many/zero/two) |
| `XliffSpan` | ~115 | Segment `<xliff:g>` capturé (id/example/placeholder, v3.3) |
| `XliffText` | ~125 | Texte riche + substitution des exemples (v3.3) |
| `ResourceCache` | ~100 | Cache LRU générique |
| `DimensionConverter` | ~150 | Conversion dp/sp/px/mm/in/pt → pixels |
| `ColorParser` | ~150 | Parsing couleurs hex + nommées |
| `ResourceException` | ~70 | Exception unchecked |
| **Total** | **~3 980** | |

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
List<String> items = resolver.getStringArray("@array/planets");
List<Integer> ratios = resolver.getIntegerArray("@array/ratios");   // <integer-array> (v3.3)
String layoutXml = resolver.getLayout("@layout/login_form");
String drawablePath = resolver.getDrawablePath("@drawable/icon");

// Pluriels (v3.2) — catégorie CLDR de la langue courante
String label = resolver.getQuantityString("@plurals/songs", 42, 42); // "42 chansons"
Map<String, String> itemsByCategory = resolver.getPlurals("@plurals/songs");

// Preview lisible (v3.3) — exemples <xliff:g example="…"> substitués
String readable = resolver.getPreviewString("@string/songs");       // "7 chansons — Adèle"
String quantity = resolver.getPreviewQuantityString("@plurals/songs", 7);
```

## Fonctionnalités supportées

- ✅ Parsing `colors.xml`, `strings.xml`, `dimens.xml`, `integers.xml`, `bools.xml`
- ✅ Support des `<string-array>` (`@array/`) et des `<plurals>` (`@plurals/`, v3.2)
- ✅ **Integer-arrays typés** (v3.3) : `<integer-array>` parsé en entiers, `getIntegerArray(ref)` avec repli de qualifier (`values-fr/` bat `values/`), indépendant des string-arrays du même espace de noms `@array/` (un `<string-array>` vu par le typé → `null`, jamais d'exception), items malformés neutralisés en `0` (longueur préservée — indices stables pour l'introspection), copie défensive des listes retournées
- ✅ **Segments `<xliff:g id/example>`** (v3.3) : attributs `id`/`example` capturés dans les `<string>` et les items de `<plurals>` (par catégorie CLDR) — le texte brut reste device-fidèle (placeholder interne, comme aapt2) ; `getPreviewString(ref)` / `getPreviewQuantityString(ref, quantity)` substituent chaque `example` à son placeholder (remplacement littéral, comme le layout editor d'Android Studio — « 7 chansons — Adèle » au lieu de « %1$d chansons — %2$s »), les placeholders sans exemple restent visibles ; chaînage `@string/` avec héritage des spans de la cible ; namespace XLIFF standard (`urn:oasis:names:tc:xliff:document:1.2`) ou tout namespace contenant `xliff`
- ✅ **Pluriels CLDR** (v3.2) : `getQuantityString(ref, quantity, args…)` sélectionne l'item `one`/`other`/`few`/`many`/`zero`/`two` selon les règles Unicode CLDR de la langue courante (fr, en, de, es, it, pt, nl, ru, uk, sr, hr, cs, sk, pl, ar, he, lt, lv, zh, ja, ko, vi, th, id…), repli `other` puis `null`, items chainables `@string/`/`@plurals/`, formatage `String.format` tolérant (pattern invalide → texte brut), introspection `getPlurals(ref)` ; `getPlurals` est exposé aussi pour les éditeurs qui choisissent eux-mêmes la catégorie
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
| JVM | `ColorParserTest` | 28 |
| JVM | `PluralRulesTest` (v3.2) | 21 |
| JVM | `ResourceQualifierTest` | 13 |
| JVM | `ResourceQualifierLocaleTest` | 26 |
| JVM | `ResourceQualifierScreenSizeTest` (L1) | 20 |
| JVM | `DimensionConverterTest` | 16 |
| JVM | `ResourceFileParserTest` (+14 xliff/integer-array v3.3) | 36 |
| JVM | `ResourceResolverImplTest` | 25 |
| JVM | `ResourceResolverImplLocaleTest` | 15 |
| JVM | `ResourceResolverImplScreenSizeTest` (L1) | 15 |
| JVM | `ResourceResolverImplPluralsTest` (v3.2) | 20 |
| JVM | `ResourceResolverImplIntegerArrayTest` (v3.3) | 10 |
| JVM | `ResourceResolverImplXliffTest` (v3.3) | 15 |
| JVM | `XliffSpanTest` (v3.3) | 15 |
| JVM | `ResourceCacheTest` | 9 |
| JVM | `ResourceTableTest` (+8 v3.3) | 19 |
| JVM | `ResourceStringArrayTest` | 6 |
| JVM | `api.ResourceUrlTest` | 17 |
| Instrumenté | `ResourceResolverImplInstrumentedTest` | 10 |
| **Total** | | **336 cas** (326 JVM mesurés + 10 instrumentés) |

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

- ❌ Pas de résolution des références `?attr/foo` (gérées par Module 4 — Themes)
- ℹ️ Les quantités des pluriels sont entières (l'API Android est `getQuantityString(int, int)`) : les règles CLDR décimales (ex. `1,5` en fr) ne s'appliquent pas
- ℹ️ Les items de `<integer-array>` sont des littéraux : une référence `@integer/` dans un item est neutralisée en `0` (philosophie placeholder — longueur préservée)
- ℹ️ La substitution des exemples xliff ne couvre pas les items de `<string-array>` (seuls `<string>` et les items de `<plurals>` portent des spans capturés) ; le texte interne des `<xliff:g>` d'un item reste toutefois conservé (device-fidèle)
