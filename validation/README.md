# Validation — Comparaison avec layoutlib original

> Module dédié aux tests d'intégration comparant le rendu de LayoutLib avec le layoutlib original.
>
> @author jo@Dev
> **Statut :** ✅ Livré (Phase 6 — Validation)

## Classes

| Classe | Lignes | Rôle |
|--------|--------|------|
| `LayoutTestHarness` | ~200 | Exécute les tests (inflation + measure + layout) |
| `LayoutTestCatalog` | ~280 | Catalogue des 50 layouts XML de test |
| `LayoutTestResult` | ~150 | POJO résultat (temps, vues, erreurs) |
| `LayoutTestCase` | ~80 | POJO cas de test (nom + XML + catégorie) |
| `ValidationReportGenerator` | ~200 | Génère un rapport HTML des résultats |
| `ValidationException` | ~30 | Exception |
| `ReportMain` | ~120 | Programme CLI pour générer le catalogue |
| **Total** | **~1 060** | |

## Catalogue des 50 layouts

Le catalogue est divisé en 8 catégories couvrant tous les cas d'usage de LayoutLib :

| Catégorie | Nombre | Description |
|-----------|--------|-------------|
| `basic` | 10 | Vues simples (TextView, Button, EditText, ...) |
| `nested` | 10 | Hiérarchies imbriquées (LinearLayout, FrameLayout, ScrollView) |
| `resources` | 5 | Résolution @color/, @string/, @dimen/ |
| `drawables` | 8 | Drawables XML (shapes, gradients) |
| `theme` | 4 | Résolution ?attr/ et DayNight |
| `special` | 5 | Tags spéciaux (<include>, <merge>, <ViewStub>, tools:*, RTL) |
| `material` | 3 | Composants AndroidX/Material (CardView, ConstraintLayout) |
| `complex` | 5 | Écrans complets (login, dashboard, settings, profile, list_item) |
| **Total** | **50** | |

## API publique

```java
// Création du harness
LayoutTestHarness harness = new LayoutTestHarness(context);
harness.setDefaultDimensions(1080, 1920);

// Exécution d'un test unique
LayoutTestResult result = harness.runTest("my_test", xml);
if (result.isSuccess()) {
    System.out.println("OK : " + result.getViewCount() + " vues en "
            + result.getTotalTimeMs() + " ms");
}

// Exécution de tous les tests du catalogue
List<LayoutTestCase> tests = LayoutTestCatalog.getAllTests();
List<LayoutTestResult> results = harness.runTests(tests);

// Génération du rapport HTML
String html = ValidationReportGenerator.generateHtml(results);
Files.write(Paths.get("report.html"), html.getBytes());
```

## Tests

| Type | Fichier | Cas |
|------|---------|-----|
| JVM | `LayoutTestCatalogTest` | 8 |
| JVM | `LayoutTestResultTest` | 6 |
| JVM | `ValidationReportGeneratorTest` | 7 |
| Instrumenté | `LayoutTestHarnessInstrumentedTest` | 11 |
| **Total** | | **32 cas** |

## Métriques cibles

| Métrique | Cible | Méthode |
|----------|-------|---------|
| Taux de succès | ≥ 95% | Comptage des LayoutTestResult.isSuccess() |
| Temps moyen par layout | < 200 ms | `getTotalTimeMs()` moyenné |
| Temps max | < 500 ms | `getTotalTimeMs()` maximum |
| Taille APK ajoutée | < 500 KB | `apkanalyzer` sur l'AAR |
| Similarité vs layoutlib | ≥ 95% | Comparaison bitmap via Paparazzi (à brancher) |

## Exécution

```bash
# Tests JVM
./gradlew :validation:test

# Tests instrumentés (nécessite émulateur/device)
./gradlew :validation:connectedAndroidTest
```

## Limitations v1.0

- ❌ La comparaison bitmap avec Paparazzi n'est pas encore branchée (nécessite un setup desktop séparé)
- ❌ Le rapport HTML dynamique (avec résultats réels) doit être généré sur device
- ❌ Pas de tests de performance automatisés (les mesures sont collectées mais pas validées par seuil)
