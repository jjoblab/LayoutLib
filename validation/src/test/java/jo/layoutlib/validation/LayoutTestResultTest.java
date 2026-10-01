package jo.layoutlib.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires du {@link LayoutTestResult}.
 *
 * @author jo@Dev
 */
@DisplayName("LayoutTestResult — résultat de test")
class LayoutTestResultTest {

    @Nested
    @DisplayName("Constructeurs et getters")
    class ConstructorsAndGetters {

        @Test
        @DisplayName("construit un résultat de succès")
        void shouldConstructSuccessResult() {
            LayoutTestResult result = new LayoutTestResult(
                    "test_01", "<xml/>", true,
                    50, 30, 5, 2, null, 1080, 1920);
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getInflationTimeMs()).isEqualTo(50);
            assertThat(result.getLayoutTimeMs()).isEqualTo(30);
            assertThat(result.getTotalTimeMs()).isEqualTo(80);
            assertThat(result.getViewCount()).isEqualTo(5);
            assertThat(result.getMaxDepth()).isEqualTo(2);
            assertThat(result.getMeasuredWidth()).isEqualTo(1080);
            assertThat(result.getMeasuredHeight()).isEqualTo(1920);
            assertThat(result.getErrorMessage()).isNull();
        }

        @Test
        @DisplayName("construit un résultat d'échec")
        void shouldConstructFailureResult() {
            LayoutTestResult result = new LayoutTestResult(
                    "test_02", "<invalid", false,
                    10, 0, 0, 0, "XML malformé", 0, 0);
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).isEqualTo("XML malformé");
            assertThat(result.getTotalTimeMs()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("Méthodes utilitaires")
    class UtilityMethods {

        @Test
        @DisplayName("meetsPerformanceTarget retourne true si < 200 ms")
        void shouldReturnTrueWhenUnderThreshold() {
            LayoutTestResult result = new LayoutTestResult(
                    "test", "<xml/>", true, 50, 30, 1, 1, null, 100, 100);
            assertThat(result.meetsPerformanceTarget()).isTrue();
        }

        @Test
        @DisplayName("meetsPerformanceTarget retourne false si >= 200 ms")
        void shouldReturnFalseWhenAtOrAboveThreshold() {
            LayoutTestResult result = new LayoutTestResult(
                    "test", "<xml/>", true, 100, 100, 1, 1, null, 100, 100);
            assertThat(result.meetsPerformanceTarget()).isFalse();
        }

        @Test
        @DisplayName("toString produit une chaîne pour un succès")
        void shouldProduceStringForSuccess() {
            LayoutTestResult result = new LayoutTestResult(
                    "test_01", "<xml/>", true, 50, 30, 5, 2, null, 1080, 1920);
            String s = result.toString();
            assertThat(s).contains("test_01");
            assertThat(s).contains("OK");
            assertThat(s).contains("5 vues");
        }

        @Test
        @DisplayName("toString produit une chaîne pour un échec")
        void shouldProduceStringForFailure() {
            LayoutTestResult result = new LayoutTestResult(
                    "test_02", "<xml", false, 10, 0, 0, 0, "Erreur", 0, 0);
            String s = result.toString();
            assertThat(s).contains("ECHEC");
            assertThat(s).contains("Erreur");
        }
    }
}
