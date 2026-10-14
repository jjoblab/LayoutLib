package jo.layoutlib.resources;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests des qualifiers de dimensions d'écran (ADR 0116 — L1) :
 * {@code values-sw600dp/}, {@code values-w600dp/}, {@code values-h720dp/}.
 *
 * <p>Valide le parse des dossiers, la sémantique de seuil (un dossier ne
 * sert que les écrans atteignant sa dimension), l'ordre de priorité AOSP
 * (locale &gt; région &gt; sw &gt; w &gt; h &gt; nuit &gt; land &gt; API)
 * et l'identité des qualifiers — l'ancien comportement classait
 * {@code values-sw600dp} en DEFAULT, provoquant un écrasement non
 * déterministe avec {@code values/}.</p>
 *
 * @author jo@Dev
 * @since 3.1
 */
@DisplayName("ResourceQualifier — dimensions d'écran sw/w/h (L1)")
class ResourceQualifierScreenSizeTest {

    @Nested
    @DisplayName("Parse des dossiers")
    class ParseFolders {

        @Test
        @DisplayName("parse values-sw600dp → smallestScreenWidth=600")
        void parsesSw600dp() {
            ResourceQualifier q = ResourceQualifier.parse("values-sw600dp");
            assertThat(q.getSmallestScreenWidth()).isEqualTo(600);
            assertThat(q.getScreenWidth()).isZero();
            assertThat(q.getScreenHeight()).isZero();
            assertThat(q.hasScreenSizeQualifier()).isTrue();
        }

        @Test
        @DisplayName("parse values-w600dp → screenWidth=600")
        void parsesW600dp() {
            ResourceQualifier q = ResourceQualifier.parse("values-w600dp");
            assertThat(q.getScreenWidth()).isEqualTo(600);
            assertThat(q.getSmallestScreenWidth()).isZero();
        }

        @Test
        @DisplayName("parse values-h720dp → screenHeight=720")
        void parsesH720dp() {
            ResourceQualifier q = ResourceQualifier.parse("values-h720dp");
            assertThat(q.getScreenHeight()).isEqualTo(720);
        }

        @Test
        @DisplayName("parse values-w600dp-h720dp → les deux seuils")
        void parsesWidthAndHeight() {
            ResourceQualifier q = ResourceQualifier.parse("values-w600dp-h720dp");
            assertThat(q.getScreenWidth()).isEqualTo(600);
            assertThat(q.getScreenHeight()).isEqualTo(720);
        }

        @Test
        @DisplayName("parse combiné : values-fr-sw600dp-night-v31")
        void parsesCombinedQualifiers() {
            ResourceQualifier q = ResourceQualifier.parse("values-fr-sw600dp-night-v31");
            assertThat(q.getLanguage()).isEqualTo("fr");
            assertThat(q.getSmallestScreenWidth()).isEqualTo(600);
            assertThat(q.isNight()).isTrue();
            assertThat(q.getApiLevel()).isEqualTo(31);
        }

        @Test
        @DisplayName("values/sw600dp (sans dp) reste ignoré silencieusement")
        void malformedDpIsIgnored() {
            assertThat(ResourceQualifier.parse("values-sw600"))
                    .isEqualTo(ResourceQualifier.DEFAULT);
            assertThat(ResourceQualifier.parse("values-swXXXdp"))
                    .isEqualTo(ResourceQualifier.DEFAULT);
        }

        @Test
        @DisplayName("un qualifier de dimension n'est PLUS confondu avec DEFAULT")
        void swQualifierIsNotDefault() {
            // Le bug L1 : values-sw600dp était parsé en DEFAULT et entrait
            // en collision avec values/ dans la table
            assertThat(ResourceQualifier.parse("values-sw600dp"))
                    .isNotEqualTo(ResourceQualifier.DEFAULT);
        }

        @Test
        @DisplayName("toString restitue le dossier (values-sw600dp)")
        void toStringRoundTrip() {
            assertThat(ResourceQualifier.parse("values-sw600dp").toString())
                    .isEqualTo("values-sw600dp");
            assertThat(ResourceQualifier.parse("values-w600dp-h720dp").toString())
                    .isEqualTo("values-w600dp-h720dp");
        }
    }

    @Nested
    @DisplayName("Compatibilité (sémantique de seuil)")
    class Compatibility {

        @Test
        @DisplayName("sw600dp est écarté pour une cible téléphone (DEFAULT)")
        void sw600IncompatibleWithPhone() {
            ResourceQualifier sw600 = ResourceQualifier.parse("values-sw600dp");
            assertThat(sw600.isCompatibleWith(ResourceQualifier.DEFAULT)).isFalse();
        }

        @Test
        @DisplayName("sw600dp est compatible avec les cibles sw600 et au-delà")
        void sw600CompatibleWithTablets() {
            ResourceQualifier sw600 = ResourceQualifier.parse("values-sw600dp");
            ResourceQualifier sw600Target = new ResourceQualifier(
                    false, false, 1, null, null, 600, 0, 0);
            ResourceQualifier sw800Target = new ResourceQualifier(
                    false, false, 1, null, null, 800, 0, 0);
            assertThat(sw600.isCompatibleWith(sw600Target)).isTrue();
            assertThat(sw600.isCompatibleWith(sw800Target)).isTrue();
        }

        @Test
        @DisplayName("sw720dp est écarté pour une cible sw600")
        void biggerSwIncompatibleWithSmallerTarget() {
            ResourceQualifier sw720 = ResourceQualifier.parse("values-sw720dp");
            ResourceQualifier sw600Target = new ResourceQualifier(
                    false, false, 1, null, null, 600, 0, 0);
            assertThat(sw720.isCompatibleWith(sw600Target)).isFalse();
        }

        @Test
        @DisplayName("w600dp / h720dp suivent la même sémantique")
        void widthAndHeightThresholds() {
            ResourceQualifier w600 = ResourceQualifier.parse("values-w600dp");
            ResourceQualifier h720 = ResourceQualifier.parse("values-h720dp");
            assertThat(w600.isCompatibleWith(new ResourceQualifier(
                    false, false, 1, null, null, 0, 599, 0))).isFalse();
            assertThat(w600.isCompatibleWith(new ResourceQualifier(
                    false, false, 1, null, null, 0, 600, 0))).isTrue();
            assertThat(h720.isCompatibleWith(new ResourceQualifier(
                    false, false, 1, null, null, 0, 0, 720))).isTrue();
            assertThat(h720.isCompatibleWith(new ResourceQualifier(
                    false, false, 1, null, null, 0, 0, 719))).isFalse();
        }

        @Test
        @DisplayName("DEFAULT (sans dimension) reste compatible avec toute cible")
        void defaultCompatibleWithEverything() {
            ResourceQualifier tablet = new ResourceQualifier(
                    false, false, 1, null, null, 600, 600, 720);
            assertThat(ResourceQualifier.DEFAULT.isCompatibleWith(tablet)).isTrue();
        }
    }

    @Nested
    @DisplayName("Spécificité (ordre AOSP)")
    class Specificity {

        @Test
        @DisplayName("sw600dp score 3600 : au-dessus du mode nuit (1000), sous la région (5000)")
        void swScoreBetweenRegionAndNight() {
            ResourceQualifier sw600 = ResourceQualifier.parse("values-sw600dp");
            ResourceQualifier target = new ResourceQualifier(
                    false, false, 1, null, null, 720, 0, 0);
            // 3000 (palier sw) + 600 (seuil) + 1 (api par défaut du qualifier)
            int swScore = sw600.specificityScore(target);
            assertThat(swScore).isEqualTo(3601);
            // Sur une cible nuit, le qualifier nuit marque 1000 + 1 (api)
            int nightScore = new ResourceQualifier(true, false, 1)
                    .specificityScore(new ResourceQualifier(
                            true, false, 1, null, null, 720, 0, 0));
            assertThat(swScore).isGreaterThan(nightScore);
            assertThat(swScore).isLessThan(
                    new ResourceQualifier(false, false, 1, "fr", "FR")
                            .specificityScore(new ResourceQualifier(
                                    false, false, 1, "fr", "FR")));
        }

        @Test
        @DisplayName("entre deux seuils compatibles, le plus proche de l'écran gagne")
        void closestThresholdWins() {
            ResourceQualifier sw600 = ResourceQualifier.parse("values-sw600dp");
            ResourceQualifier sw720 = ResourceQualifier.parse("values-sw720dp");
            ResourceQualifier target = new ResourceQualifier(
                    false, false, 1, null, null, 720, 0, 0);
            assertThat(sw720.specificityScore(target))
                    .isGreaterThan(sw600.specificityScore(target));
        }

        @Test
        @DisplayName("w (2000+) et h (1500+) restent sous sw (3000+), au-dessus de nuit")
        void wAndHOrdering() {
            ResourceQualifier target = new ResourceQualifier(
                    false, false, 1, null, null, 720, 720, 720);
            int swScore = ResourceQualifier.parse("values-sw600dp").specificityScore(target);
            int wScore = ResourceQualifier.parse("values-w600dp").specificityScore(target);
            int hScore = ResourceQualifier.parse("values-h720dp").specificityScore(target);
            int nightScore = new ResourceQualifier(true, false, 1)
                    .specificityScore(new ResourceQualifier(
                            true, false, 1, null, null, 720, 720, 720));
            assertThat(swScore).isGreaterThan(wScore);
            assertThat(wScore).isGreaterThan(hScore);
            assertThat(hScore).isGreaterThan(nightScore);
        }

        @Test
        @DisplayName("un seuil non atteint ne marque pas de point")
        void unmetThresholdScoresNothing() {
            ResourceQualifier sw600 = ResourceQualifier.parse("values-sw600dp");
            // cible téléphone : le seuil n'ajoute rien — seul l'api par
            // défaut (+1) du qualifier marque, comme pour DEFAULT
            assertThat(sw600.specificityScore(ResourceQualifier.DEFAULT)).isEqualTo(1);
            assertThat(ResourceQualifier.DEFAULT.specificityScore(
                    ResourceQualifier.DEFAULT)).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Constructeur 8 arguments")
    class Constructor {

        @Test
        @DisplayName("dimensions négatives rejetées")
        void negativeDimensionsRejected() {
            assertThatThrownBy(() -> new ResourceQualifier(
                    false, false, 1, null, null, -1, 0, 0))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new ResourceQualifier(
                    false, false, 1, null, null, 0, -600, 0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("equals/hashCode intègrent les dimensions")
        void equalsAndHashCodeIncludeDimensions() {
            ResourceQualifier a = new ResourceQualifier(false, false, 1, null, null, 600, 0, 0);
            ResourceQualifier b = new ResourceQualifier(false, false, 1, null, null, 600, 0, 0);
            ResourceQualifier c = new ResourceQualifier(false, false, 1, null, null, 720, 0, 0);
            assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
            assertThat(a).isNotEqualTo(c);
        }

        @Test
        @DisplayName("compareTo ordonne par dimensions décroissantes")
        void compareToOrdersByDimensions() {
            ResourceQualifier sw600 = new ResourceQualifier(false, false, 1, null, null, 600, 0, 0);
            ResourceQualifier sw720 = new ResourceQualifier(false, false, 1, null, null, 720, 0, 0);
            assertThat(sw720.compareTo(sw600)).isNegative();
        }
    }
}
