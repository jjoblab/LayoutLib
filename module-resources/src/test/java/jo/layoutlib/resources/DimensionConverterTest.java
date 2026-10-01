package jo.layoutlib.resources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitaires du {@link DimensionConverter}.
 *
 * @author jo@Dev
 */
@DisplayName("DimensionConverter — conversion dp/sp/px vers pixels")
class DimensionConverterTest {

    private DimensionConverter converter;

    @BeforeEach
    void setUp() {
        // density=2.0 (xhdpi), fontScale=1.0, xdpi=320
        converter = new DimensionConverter(2.0f, 1.0f, 320f);
    }

    @Nested
    @DisplayName("Conversions d'unités")
    class UnitConversions {

        @Test
        @DisplayName("convertit les dp en pixels (density=2.0)")
        void shouldConvertDp() {
            assertThat(converter.toPixels("16dp")).isEqualTo(32.0f);
            assertThat(converter.toPixels("8dp")).isEqualTo(16.0f);
        }

        @Test
        @DisplayName("convertit les dip (alias de dp)")
        void shouldConvertDip() {
            assertThat(converter.toPixels("16dip")).isEqualTo(32.0f);
        }

        @Test
        @DisplayName("convertit les sp avec fontScale=1.0")
        void shouldConvertSp() {
            assertThat(converter.toPixels("14sp")).isEqualTo(28.0f);
        }

        @Test
        @DisplayName("convertit les px tels quels")
        void shouldConvertPx() {
            assertThat(converter.toPixels("100px")).isEqualTo(100.0f);
        }

        @Test
        @DisplayName("convertit les mm")
        void shouldConvertMm() {
            // 1 mm = 320 / 25.4 ≈ 12.598 pixels
            assertThat(converter.toPixels("1mm")).isCloseTo(12.598f, within(0.01f));
        }

        @Test
        @DisplayName("convertit les in (pouces)")
        void shouldConvertIn() {
            assertThat(converter.toPixels("1in")).isEqualTo(320.0f);
        }

        @Test
        @DisplayName("convertit les pt (points)")
        void shouldConvertPt() {
            // 1 pt = 320 / 72 ≈ 4.444 pixels
            assertThat(converter.toPixels("1pt")).isCloseTo(4.444f, within(0.01f));
        }

        @Test
        @DisplayName("traite une valeur sans unité comme des pixels")
        void shouldTreatNoUnitAsPixels() {
            assertThat(converter.toPixels("42")).isEqualTo(42.0f);
        }
    }

    @Nested
    @DisplayName("Avec fontScale personnalisé")
    class CustomFontScale {

        @Test
        @DisplayName("fontScale=1.5 multiplie les sp")
        void shouldScaleSpWithFontScale() {
            DimensionConverter bigFont = new DimensionConverter(2.0f, 1.5f, 320f);
            // 14sp × 2.0 × 1.5 = 42
            assertThat(bigFont.toPixels("14sp")).isEqualTo(42.0f);
        }

        @Test
        @DisplayName("fontScale n'affecte pas les dp")
        void fontScaleShouldNotAffectDp() {
            DimensionConverter bigFont = new DimensionConverter(2.0f, 1.5f, 320f);
            assertThat(bigFont.toPixels("16dp")).isEqualTo(32.0f);
        }
    }

    @Nested
    @DisplayName("Cas limites et erreurs")
    class EdgeCases {

        @Test
        @DisplayName("retourne 0 pour null ou vide")
        void shouldReturnZeroForNullOrEmpty() {
            assertThat(converter.toPixels(null)).isEqualTo(0f);
            assertThat(converter.toPixels("")).isEqualTo(0f);
        }

        @Test
        @DisplayName("rejette une valeur numérique invalide")
        void shouldRejectInvalidNumber() {
            assertThatThrownBy(() -> converter.toPixels("abcpx"))
                    .isInstanceOf(ResourceException.class);
        }

        @Test
        @DisplayName("rejette une unité inconnue")
        void shouldRejectUnknownUnit() {
            assertThatThrownBy(() -> converter.toPixels("16foo"))
                    .isInstanceOf(ResourceException.class)
                    .hasMessageContaining("Unité inconnue");
        }

        @Test
        @DisplayName("isValidDimension retourne true pour une dimension valide")
        void shouldReturnTrueForValidDimension() {
            assertThat(converter.isValidDimension("16dp")).isTrue();
        }

        @Test
        @DisplayName("isValidDimension retourne false pour une dimension invalide")
        void shouldReturnFalseForInvalidDimension() {
            assertThat(converter.isValidDimension("abc")).isFalse();
        }

        @Test
        @DisplayName("toPixelsInt arrondit correctement")
        void shouldRoundToInt() {
            // 1mm = 12.598 → 13
            assertThat(converter.toPixelsInt("1mm")).isEqualTo(13);
        }
    }

    private static org.assertj.core.data.Offset<Float> within(float tolerance) {
        return org.assertj.core.data.Offset.offset(tolerance);
    }
}
