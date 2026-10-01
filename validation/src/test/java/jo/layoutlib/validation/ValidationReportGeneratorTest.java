package jo.layoutlib.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires du {@link ValidationReportGenerator}.
 *
 * @author jo@Dev
 */
@DisplayName("ValidationReportGenerator — génération de rapport HTML")
class ValidationReportGeneratorTest {

    @Nested
    @DisplayName("Génération HTML")
    class HtmlGeneration {

        @Test
        @DisplayName("génère un HTML non vide pour une liste de résultats")
        void shouldGenerateNonEmptyHtml() {
            List<LayoutTestResult> results = new ArrayList<>();
            results.add(new LayoutTestResult("01_test", "<xml/>", true,
                    50, 30, 5, 2, null, 1080, 1920));
            results.add(new LayoutTestResult("02_test", "<xml/>", true,
                    100, 50, 3, 1, null, 1080, 1920));
            String html = ValidationReportGenerator.generateHtml(results);
            assertThat(html).isNotEmpty();
            assertThat(html).contains("<html");
            assertThat(html).contains("</html>");
        }

        @Test
        @DisplayName("contient les statistiques globales")
        void shouldContainGlobalStats() {
            List<LayoutTestResult> results = new ArrayList<>();
            results.add(new LayoutTestResult("01_test", "<xml/>", true,
                    50, 30, 5, 2, null, 1080, 1920));
            String html = ValidationReportGenerator.generateHtml(results);
            assertThat(html).contains("Statistiques globales");
            assertThat(html).contains("Total tests");
            assertThat(html).contains("Succès");
        }

        @Test
        @DisplayName("contient le tableau détaillé")
        void shouldContainDetailedTable() {
            List<LayoutTestResult> results = new ArrayList<>();
            results.add(new LayoutTestResult("01_test", "<xml/>", true,
                    50, 30, 5, 2, null, 1080, 1920));
            String html = ValidationReportGenerator.generateHtml(results);
            assertThat(html).contains("Détail par layout");
            assertThat(html).contains("01_test");
        }

        @Test
        @DisplayName("contient la section par catégorie")
        void shouldContainByCategorySection() {
            List<LayoutTestResult> results = new ArrayList<>();
            results.add(new LayoutTestResult("01_test", "<xml/>", true,
                    50, 30, 5, 2, null, 1080, 1920));
            String html = ValidationReportGenerator.generateHtml(results);
            assertThat(html).contains("Par catégorie");
        }

        @Test
        @DisplayName("met en évidence les échecs")
        void shouldHighlightFailures() {
            List<LayoutTestResult> results = new ArrayList<>();
            results.add(new LayoutTestResult("01_test", "<xml/>", true,
                    50, 30, 5, 2, null, 1080, 1920));
            results.add(new LayoutTestResult("02_test", "<invalid", false,
                    10, 0, 0, 0, "XML malformé", 0, 0));
            String html = ValidationReportGenerator.generateHtml(results);
            assertThat(html).contains("class='fail'");
            assertThat(html).contains("XML malformé");
        }
    }

    @Nested
    @DisplayName("Cas limites")
    class EdgeCases {

        @Test
        @DisplayName("retourne un HTML minimal pour une liste vide")
        void shouldReturnMinimalHtmlForEmptyList() {
            String html = ValidationReportGenerator.generateHtml(new ArrayList<>());
            assertThat(html).contains("Aucun résultat");
        }

        @Test
        @DisplayName("retourne un HTML minimal pour null")
        void shouldReturnMinimalHtmlForNull() {
            String html = ValidationReportGenerator.generateHtml(null);
            assertThat(html).contains("Aucun résultat");
        }
    }

    @Nested
    @DisplayName("Contenu du rapport")
    class ReportContent {

        @Test
        @DisplayName("inclut le taux de succès")
        void shouldIncludeSuccessRate() {
            List<LayoutTestResult> results = new ArrayList<>();
            results.add(new LayoutTestResult("01_test", "<xml/>", true,
                    50, 30, 5, 2, null, 1080, 1920));
            results.add(new LayoutTestResult("02_test", "<xml/>", false,
                    10, 0, 0, 0, "Erreur", 0, 0));
            String html = ValidationReportGenerator.generateHtml(results);
            assertThat(html).contains("50.0%");
        }

        @Test
        @DisplayName("inclut le nombre total de vues")
        void shouldIncludeTotalViews() {
            List<LayoutTestResult> results = new ArrayList<>();
            results.add(new LayoutTestResult("01_test", "<xml/>", true,
                    50, 30, 5, 2, null, 1080, 1920));
            results.add(new LayoutTestResult("02_test", "<xml/>", true,
                    50, 30, 3, 1, null, 1080, 1920));
            String html = ValidationReportGenerator.generateHtml(results);
            assertThat(html).contains("Vues créées");
            assertThat(html).contains("8");  // 5 + 3
        }
    }
}
