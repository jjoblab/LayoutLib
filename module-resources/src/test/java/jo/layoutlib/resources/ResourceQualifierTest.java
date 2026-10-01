package jo.layoutlib.resources;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitaires du {@link ResourceQualifier}.
 *
 * <p>Valide le parsing des noms de dossiers de resources, la compatibilité
 * entre qualifiers, et le calcul du score de spécificité.</p>
 *
 * @author jo@Dev
 */
@DisplayName("ResourceQualifier — qualifiers de dossiers")
class ResourceQualifierTest {

    @Nested
    @DisplayName("Parsing des noms de dossiers")
    class FolderNameParsing {

        @Test
        @DisplayName("parse 'values' en DEFAULT")
        void shouldParseValuesAsDefault() {
            ResourceQualifier q = ResourceQualifier.parse("values");
            assertThat(q).isEqualTo(ResourceQualifier.DEFAULT);
            assertThat(q.isNight()).isFalse();
            assertThat(q.isLandscape()).isFalse();
            assertThat(q.getApiLevel()).isEqualTo(1);
        }

        @Test
        @DisplayName("parse 'values-night'")
        void shouldParseNightQualifier() {
            ResourceQualifier q = ResourceQualifier.parse("values-night");
            assertThat(q.isNight()).isTrue();
            assertThat(q.isLandscape()).isFalse();
        }

        @Test
        @DisplayName("parse 'values-land'")
        void shouldParseLandQualifier() {
            ResourceQualifier q = ResourceQualifier.parse("values-land");
            assertThat(q.isLandscape()).isTrue();
        }

        @Test
        @DisplayName("parse 'values-v31'")
        void shouldParseApiQualifier() {
            ResourceQualifier q = ResourceQualifier.parse("values-v31");
            assertThat(q.getApiLevel()).isEqualTo(31);
        }

        @Test
        @DisplayName("parse 'values-night-land-v31' (combiné)")
        void shouldParseCombinedQualifier() {
            ResourceQualifier q = ResourceQualifier.parse("values-night-land-v31");
            assertThat(q.isNight()).isTrue();
            assertThat(q.isLandscape()).isTrue();
            assertThat(q.getApiLevel()).isEqualTo(31);
        }
    }

    @Nested
    @DisplayName("Compatibilité")
    class Compatibility {

        @Test
        @DisplayName("DEFAULT est compatible avec n'importe quel target")
        void defaultShouldBeCompatibleWithAnyTarget() {
            ResourceQualifier target = new ResourceQualifier(true, true, 31);
            assertThat(ResourceQualifier.DEFAULT.isCompatibleWith(target)).isTrue();
        }

        @Test
        @DisplayName("night qualifier n'est pas compatible avec un target en mode jour")
        void nightQualifierNotCompatibleWithDayTarget() {
            ResourceQualifier night = new ResourceQualifier(true, false, 1);
            ResourceQualifier dayTarget = new ResourceQualifier(false, false, 1);
            assertThat(night.isCompatibleWith(dayTarget)).isFalse();
        }

        @Test
        @DisplayName("v31 qualifier n'est pas compatible avec un target API 24")
        void v31QualifierNotCompatibleWithApi24Target() {
            ResourceQualifier v31 = new ResourceQualifier(false, false, 31);
            ResourceQualifier target = new ResourceQualifier(false, false, 24);
            assertThat(v31.isCompatibleWith(target)).isFalse();
        }
    }

    @Nested
    @DisplayName("Score de spécificité")
    class SpecificityScore {

        @Test
        @DisplayName("night a un score supérieur à DEFAULT")
        void nightShouldScoreHigherThanDefault() {
            ResourceQualifier target = new ResourceQualifier(true, false, 1);
            int defaultScore = ResourceQualifier.DEFAULT.specificityScore(target);
            int nightScore = new ResourceQualifier(true, false, 1).specificityScore(target);
            assertThat(nightScore).isGreaterThan(defaultScore);
        }

        @Test
        @DisplayName("v31 a un score supérieur à v21 (API plus élevé)")
        void higherApiShouldScoreHigher() {
            ResourceQualifier target = new ResourceQualifier(false, false, 31);
            int v21Score = new ResourceQualifier(false, false, 21).specificityScore(target);
            int v31Score = new ResourceQualifier(false, false, 31).specificityScore(target);
            assertThat(v31Score).isGreaterThan(v21Score);
        }
    }

    @Nested
    @DisplayName("Cas d'erreur")
    class ErrorCases {

        @Test
        @DisplayName("rejette un niveau d'API < 1")
        void shouldRejectInvalidApiLevel() {
            assertThatThrownBy(() -> new ResourceQualifier(false, false, 0))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Égalité et toString")
    class EqualsAndToString {

        @Test
        @DisplayName("deux qualifiers identiques sont égaux")
        void equalQualifiersShouldBeEqual() {
            ResourceQualifier q1 = new ResourceQualifier(true, false, 31);
            ResourceQualifier q2 = new ResourceQualifier(true, false, 31);
            assertThat(q1).isEqualTo(q2);
            assertThat(q1.hashCode()).isEqualTo(q2.hashCode());
        }

        @Test
        @DisplayName("toString produit le nom du dossier")
        void toStringShouldProduceFolderName() {
            assertThat(new ResourceQualifier(true, false, 1).toString())
                    .isEqualTo("values-night");
            assertThat(new ResourceQualifier(true, true, 31).toString())
                    .isEqualTo("values-night-land-v31");
        }
    }
}
