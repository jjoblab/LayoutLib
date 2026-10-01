package jo.layoutlib.resources;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitaires du {@link ColorParser}.
 *
 * <p>Valide le parsing des couleurs Android dans tous les formats supportés :
 * hex 8/6/4/3 chiffres, couleurs nommées du framework, et cas d'erreur.</p>
 *
 * @author jo@Dev
 */
@DisplayName("ColorParser — parsing des couleurs Android")
class ColorParserTest {

    @Nested
    @DisplayName("Couleurs hexadécimales")
    class HexColors {

        @Test
        @DisplayName("parse #AARRGGBB (8 chiffres)")
        void shouldParseArgbHex() {
            assertThat(ColorParser.parse("#FF6750A4")).isEqualTo(0xFF6750A4);
        }

        @Test
        @DisplayName("parse #RRGGBB (6 chiffres, alpha = 0xFF)")
        void shouldParseRgbHex() {
            assertThat(ColorParser.parse("#6750A4")).isEqualTo(0xFF6750A4);
        }

        @Test
        @DisplayName("parse #ARGB compact (4 chiffres)")
        void shouldParseCompactArgb() {
            assertThat(ColorParser.parse("#F675"))
                    .isEqualTo(0xFF667755);
        }

        @Test
        @DisplayName("parse #RGB compact (3 chiffres)")
        void shouldParseCompactRgb() {
            assertThat(ColorParser.parse("#675"))
                    .isEqualTo(0xFF667755);
        }

        @Test
        @DisplayName("parse #00000000 (transparent)")
        void shouldParseTransparent() {
            assertThat(ColorParser.parse("#00000000")).isEqualTo(0x00000000);
        }
    }

    @Nested
    @DisplayName("Couleurs nommées")
    class NamedColors {

        @ParameterizedTest
        @CsvSource({
                "red, -65536",
                "green, -16711936",
                "blue, -16776961",
                "black, -16777216",
                "white, -1",
                "transparent, 0",
                "yellow, -256",
                "cyan, -16711681"
        })
        @DisplayName("parse les couleurs nommées courantes")
        void shouldParseNamedColors(String name, int expected) {
            assertThat(ColorParser.parse(name)).isEqualTo(expected);
        }

        @Test
        @DisplayName("accepte les noms en majuscules (insensible à la casse)")
        void shouldAcceptUppercaseNames() {
            assertThat(ColorParser.parse("RED")).isEqualTo(0xFFFF0000);
        }
    }

    @Nested
    @DisplayName("Cas d'erreur")
    class ErrorCases {

        @Test
        @DisplayName("rejette une couleur null")
        void shouldRejectNull() {
            assertThatThrownBy(() -> ColorParser.parse(null))
                    .isInstanceOf(ResourceException.class);
        }

        @Test
        @DisplayName("rejette une couleur vide")
        void shouldRejectEmpty() {
            assertThatThrownBy(() -> ColorParser.parse(""))
                    .isInstanceOf(ResourceException.class);
        }

        @Test
        @DisplayName("rejette une référence @color/ non résolue")
        void shouldRejectUnresolvedReference() {
            assertThatThrownBy(() -> ColorParser.parse("@color/foo"))
                    .isInstanceOf(ResourceException.class);
        }

        @Test
        @DisplayName("rejette un hex invalide")
        void shouldRejectInvalidHex() {
            assertThatThrownBy(() -> ColorParser.parse("#XYZYXZ"))
                    .isInstanceOf(ResourceException.class);
        }

        @Test
        @DisplayName("rejette une couleur nommée inconnue")
        void shouldRejectUnknownNamed() {
            assertThatThrownBy(() -> ColorParser.parse("supercolor"))
                    .isInstanceOf(ResourceException.class);
        }
    }

    @Nested
    @DisplayName("isColor")
    class IsColorMethod {

        @ParameterizedTest
        @ValueSource(strings = {"#FF0000", "#FF6750A4", "red", "blue", "#F675"})
        @DisplayName("retourne true pour les couleurs valides")
        void shouldReturnTrueForValidColors(String value) {
            assertThat(ColorParser.isColor(value)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "invalid", "@color/foo", "#XYZ"})
        @DisplayName("retourne false pour les couleurs invalides")
        void shouldReturnFalseForInvalidColors(String value) {
            assertThat(ColorParser.isColor(value)).isFalse();
        }
    }
}
