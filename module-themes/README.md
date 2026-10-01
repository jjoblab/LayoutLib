# Module 4 — Themes

> Parsing des thèmes et styles, résolution `?attr/` et `?android:attr/`, support DayNight.
>
> @author jo@Dev
> **Statut :** ✅ Livré (Phase 4)

## Classes

| Classe | Lignes | Rôle |
|--------|--------|------|
| `ThemeResolver` | ~50 | Interface |
| `ThemeResolverImpl` | ~280 | Implémentation avec héritage + cache |
| `ThemeFileParser` | ~170 | Parser XML de themes.xml/styles.xml |
| `StyleDefinition` | ~120 | POJO représentant un style |
| `ThemeException` | ~50 | Exception |
| **Total** | **~670** | |

## API publique

```java
ThemeResolver resolver = new ThemeResolverImpl(resourceResolver, dimensionConverter);

// Enregistrement des fichiers
resolver.registerThemesFile(themesXml);          // mode jour
resolver.registerThemesFile(themesNightXml, true); // mode nuit
resolver.registerStylesFile(stylesXml);

// Configuration
resolver.setTheme("Theme.MyApp");
resolver.setNightMode(true);

// Résolution
Object value = resolver.resolveAttr("?attr/colorPrimary");
Integer color = resolver.getColorAttr("colorPrimary");
Float dimen = resolver.getDimensionAttr("buttonHeight");
String parent = resolver.getParentStyle("Theme.MyApp");
```

## Fonctionnalités

- ✅ Parsing `themes.xml` et `styles.xml`
- ✅ Héritage explicite (`parent="..."`) et implicite (par point du nom)
- ✅ Résolution `?attr/foo`
- ✅ Résolution `?android:attr/foo`
- ✅ Mode nuit (DayNight) avec variantes `values-night/`
- ✅ Cache des attributs résolus
- ✅ Anti-boucle d'héritage (profondeur max 20)
- ✅ Résolution récursive via `ResourceResolver` (@color/, @dimen/, etc.)
- ✅ Conversion automatique des valeurs littérales (hex → Integer, "16dp" → Float)

## Tests

| Type | Fichier | Cas |
|------|---------|-----|
| JVM | `ThemeFileParserTest` | 9 |
| JVM | `ThemeResolverImplTest` | 14 |
| Instrumenté | `ThemeResolverImplInstrumentedTest` | 10 |
| **Total** | | **33 cas** |

## Limitations v1.0

- ❌ Pas de support des qualifiers `values-land/themes.xml`
- ❌ Pas de résolution `?attr/` vers des drawables (limité à colors/dimens/strings)
- ❌ Pas d'application directe du thème à une vue (via `obtainStyledAttributes`)
