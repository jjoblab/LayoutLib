package jo.layoutlib.resources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de résolution des resources par qualifiers de langue (ADR 0116 —
 * P1/L2).
 *
 * <p>Valide le basculement {@code values-fr/} → {@code values-en/} via
 * {@link ResourceResolverImpl#setLanguage(String)} : sélection localisée,
 * repli automatique sur {@code values/} quand la traduction manque,
 * invalidation du cache, combinaison avec le mode nuit, variantes
 * régionales, et scan réel d'un dossier {@code res/} localisé.</p>
 *
 * @author jo@Dev
 * @since 3.0
 */
@DisplayName("ResourceResolverImpl — qualifiers de langue (P1/L2)")
class ResourceResolverImplLocaleTest {

    private ResourceTable table;
    private ResourceResolverImpl resolver;

    @BeforeEach
    void setUp() {
        table = new ResourceTable();

        // values/strings.xml — défaut (anglais par convention du projet test)
        ResourceFileParser defParser = new ResourceFileParser(table, ResourceQualifier.DEFAULT);
        defParser.parse("<resources>"
                + "<string name=\"app_name\">My App</string>"
                + "<string name=\"save\">Save</string>"
                + "<string name=\"count\">%d items</string>"
                + "<color name=\"accent\">#FF6750A4</color>"
                + "</resources>");

        // values-fr/strings.xml — traduction partielle
        ResourceFileParser frParser = new ResourceFileParser(
                table, ResourceQualifier.forLanguage("fr"));
        frParser.parse("<resources>"
                + "<string name=\"app_name\">Mon App</string>"
                + "<string name=\"save\">Enregistrer</string>"
                + "</resources>");

        // values-en/strings.xml — identique au défaut (langue explicite)
        ResourceFileParser enParser = new ResourceFileParser(
                table, ResourceQualifier.forLanguage("en"));
        enParser.parse("<resources>"
                + "<string name=\"app_name\">My App (EN)</string>"
                + "</resources>");

        // values-fr-night/ — variante nuit française
        ResourceFileParser frNightParser = new ResourceFileParser(
                table, new ResourceQualifier(true, false, 1, "fr", null));
        frNightParser.parse("<resources>"
                + "<color name=\"accent\">#FFD0BCFF</color>"
                + "</resources>");

        // values-fr-rFR/ — variante régionale
        ResourceFileParser frFrParser = new ResourceFileParser(
                table, ResourceQualifier.forLanguage("fr", "FR"));
        frFrParser.parse("<resources>"
                + "<string name=\"save\">Sauvegarder</string>"
                + "</resources>");

        resolver = new ResourceResolverImpl(table);
        resolver.setDimensionConverter(new DimensionConverter(2.0f, 1.0f, 320f));
    }

    @Nested
    @DisplayName("Sélection de langue")
    class LanguageSelection {

        @Test
        @DisplayName("sans locale, la valeur par défaut est retournée")
        void defaultWithoutLocale() {
            assertThat(resolver.getString("@string/app_name")).isEqualTo("My App");
        }

        @Test
        @DisplayName("setLanguage(\"fr\") sélectionne la traduction française")
        void frenchSelection() {
            resolver.setLanguage("fr");
            assertThat(resolver.getString("@string/app_name")).isEqualTo("Mon App");
            assertThat(resolver.getString("@string/save")).isEqualTo("Enregistrer");
        }

        @Test
        @DisplayName("setLanguage(\"en\") sélectionne la variante anglaise")
        void englishSelection() {
            resolver.setLanguage("en");
            assertThat(resolver.getString("@string/app_name")).isEqualTo("My App (EN)");
            // count n'existe qu'au défaut → repli
            assertThat(resolver.getString("@string/count")).isEqualTo("%d items");
        }

        @Test
        @DisplayName("repli automatique sur values/ quand la traduction manque")
        void fallbackWhenTranslationMissing() {
            resolver.setLanguage("fr");
            // count n'est pas traduit en fr → défaut
            assertThat(resolver.getString("@string/count")).isEqualTo("%d items");
        }

        @Test
        @DisplayName("une langue inconnue retombe entièrement sur le défaut")
        void unknownLanguageFallsBack() {
            resolver.setLanguage("de");
            assertThat(resolver.getString("@string/app_name")).isEqualTo("My App");
            assertThat(resolver.getString("@string/save")).isEqualTo("Save");
        }

        @Test
        @DisplayName("setLanguage(null) revient à la locale par défaut")
        void nullLanguageResetsToDefault() {
            resolver.setLanguage("fr");
            resolver.setLanguage(null);
            assertThat(resolver.getString("@string/app_name")).isEqualTo("My App");
        }

        @Test
        @DisplayName("le cache est invalidé après un changement de langue")
        void cacheInvalidatedOnLanguageChange() {
            assertThat(resolver.getString("@string/app_name")).isEqualTo("My App");
            resolver.setLanguage("fr");
            assertThat(resolver.getString("@string/app_name")).isEqualTo("Mon App");
            resolver.setLanguage("en");
            assertThat(resolver.getString("@string/app_name")).isEqualTo("My App (EN)");
        }
    }

    @Nested
    @DisplayName("Langue combinée aux autres qualifiers")
    class LanguageWithOtherQualifiers {

        @Test
        @DisplayName("values-fr-night l'emporte sur values-fr en nuit française")
        void frenchNightBeatsFrench() {
            resolver.setNightMode(true);
            resolver.setLanguage("fr");
            assertThat(resolver.getColor("@color/accent")).isEqualTo(0xFFD0BCFF);
        }

        @Test
        @DisplayName("values-fr (jour) l'emporte en jour français")
        void frenchDayValueInDayMode() {
            resolver.setNightMode(false);
            resolver.setLanguage("fr");
            assertThat(resolver.getColor("@color/accent")).isEqualTo(0xFF6750A4);
        }

        @Test
        @DisplayName("la nuit sans locale retombe sur le défaut")
        void nightWithoutLocaleFallsBack() {
            resolver.setNightMode(true);
            // Pas de values-night pur dans la table → défaut
            assertThat(resolver.getColor("@color/accent")).isEqualTo(0xFF6750A4);
        }
    }

    @Nested
    @DisplayName("Variantes régionales")
    class RegionalVariants {

        @Test
        @DisplayName("setLocale(\"fr\", \"FR\") sélectionne values-fr-rFR")
        void regionalVariantSelected() {
            resolver.setLocale("fr", "FR");
            assertThat(resolver.getString("@string/save")).isEqualTo("Sauvegarder");
        }

        @Test
        @DisplayName("setLanguage(\"fr\") sans région ignore values-fr-rFR")
        void regionalVariantRequiresRegion() {
            resolver.setLanguage("fr");
            assertThat(resolver.getString("@string/save")).isEqualTo("Enregistrer");
        }

        @Test
        @DisplayName("une autre région retombe sur values-fr")
        void otherRegionFallsBackToLanguage() {
            resolver.setLocale("fr", "BE");
            assertThat(resolver.getString("@string/save")).isEqualTo("Enregistrer");
        }
    }

    @Nested
    @DisplayName("Scan d'un dossier res/ localisé")
    class LocalizedFolderScan {

        @Test
        @DisplayName("scanne values/, values-fr/ et values-en/ et permet le basculement")
        void scansLocalizedValuesFolders(@TempDir Path resDir) throws IOException {
            writeFolder(resDir, "values",
                    "<resources><string name=\"greet\">Hello</string></resources>");
            writeFolder(resDir, "values-fr",
                    "<resources><string name=\"greet\">Bonjour</string></resources>");
            writeFolder(resDir, "values-en",
                    "<resources><string name=\"greet\">Hello (EN)</string></resources>");

            ResourceResolverImpl folderResolver =
                    new ResourceResolverImpl(resDir.toFile().getAbsolutePath());

            // Défaut
            assertThat(folderResolver.getString("@string/greet")).isEqualTo("Hello");
            // Français
            folderResolver.setLanguage("fr");
            assertThat(folderResolver.getString("@string/greet")).isEqualTo("Bonjour");
            // Anglais
            folderResolver.setLanguage("en");
            assertThat(folderResolver.getString("@string/greet")).isEqualTo("Hello (EN)");
            // Retour au défaut
            folderResolver.setLanguage(null);
            assertThat(folderResolver.getString("@string/greet")).isEqualTo("Hello");
        }

        @Test
        @DisplayName("les dossiers non-langue (values-night) continuent de fonctionner")
        void nightFoldersStillScan(@TempDir Path resDir) throws IOException {
            writeFolder(resDir, "values",
                    "<resources><string name=\"greet\">Hello</string></resources>");
            writeFolder(resDir, "values-night",
                    "<resources><string name=\"greet\">Good evening</string></resources>");
            writeFolder(resDir, "values-fr",
                    "<resources><string name=\"greet\">Bonjour</string></resources>");
            writeFolder(resDir, "values-fr-night",
                    "<resources><string name=\"greet\">Bonsoir</string></resources>");

            ResourceResolverImpl folderResolver =
                    new ResourceResolverImpl(resDir.toFile().getAbsolutePath());

            assertThat(folderResolver.getString("@string/greet")).isEqualTo("Hello");
            folderResolver.setNightMode(true);
            assertThat(folderResolver.getString("@string/greet")).isEqualTo("Good evening");
            folderResolver.setLanguage("fr");
            assertThat(folderResolver.getString("@string/greet")).isEqualTo("Bonsoir");
        }
    }

    /**
     * Écrit un dossier de resources localisé avec un strings.xml.
     *
     * @param resDir   la racine res/
     * @param folder   le nom du dossier (ex. {@code "values-fr"})
     * @param contents le XML des resources
     */
    private static void writeFolder(Path resDir, String folder, String contents)
            throws IOException {
        File dir = resDir.resolve(folder).toFile();
        if (!dir.mkdirs() && !dir.exists()) {
            throw new IOException("Impossible de créer " + dir);
        }
        Files.write(dir.toPath().resolve("strings.xml"),
                contents.getBytes(StandardCharsets.UTF_8));
    }
}
