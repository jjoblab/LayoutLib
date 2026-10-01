package jo.layoutlib.inflater.bridge.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitaires du {@link ResourceHelper}.
 *
 * @author jo@Dev
 */
@DisplayName("ResourceHelper — utilitaire de résolution de resources")
class ResourceHelperTest {

    @Nested
    @DisplayName("Couleurs")
    class Colors {

        @Test
        @DisplayName("parse une couleur hex 8 chiffres")
        void shouldParseArgbHex() {
            assertThat(ResourceHelper.getColor("#FF6750A4")).isEqualTo(0xFF6750A4);
        }

        @Test
        @DisplayName("parse une couleur hex 6 chiffres")
        void shouldParseRgbHex() {
            assertThat(ResourceHelper.getColor("#6750A4")).isEqualTo(0xFF6750A4);
        }

        @Test
        @DisplayName("parse une couleur nommée")
        void shouldParseNamedColor() {
            assertThat(ResourceHelper.getColor("red")).isEqualTo(0xFFFF0000);
            assertThat(ResourceHelper.getColor("transparent")).isEqualTo(0x00000000);
        }

        @Test
        @DisplayName("rejette une couleur invalide")
        void shouldRejectInvalidColor() {
            assertThatThrownBy(() -> ResourceHelper.getColor("invalid"))
                    .isInstanceOf(jo.layoutlib.resources.ResourceException.class);
        }

        @Test
        @DisplayName("isColor retourne true pour une couleur valide")
        void shouldReturnTrueForValidColor() {
            assertThat(ResourceHelper.isColor("#FF0000")).isTrue();
            assertThat(ResourceHelper.isColor("red")).isTrue();
        }

        @Test
        @DisplayName("isColor retourne false pour une couleur invalide")
        void shouldReturnFalseForInvalidColor() {
            assertThat(ResourceHelper.isColor("invalid")).isFalse();
            assertThat(ResourceHelper.isColor("")).isFalse();
        }
    }

    @Nested
    @DisplayName("Booléens")
    class Booleans {

        @Test
        @DisplayName("parse true/false")
        void shouldParseTrueFalse() {
            assertThat(ResourceHelper.getBoolean("true")).isTrue();
            assertThat(ResourceHelper.getBoolean("false")).isFalse();
        }

        @Test
        @DisplayName("parse 1/0")
        void shouldParseOneZero() {
            assertThat(ResourceHelper.getBoolean("1")).isTrue();
            assertThat(ResourceHelper.getBoolean("0")).isFalse();
        }

        @Test
        @DisplayName("rejette un booléen invalide")
        void shouldRejectInvalidBoolean() {
            assertThatThrownBy(() -> ResourceHelper.getBoolean("maybe"))
                    .isInstanceOf(jo.layoutlib.resources.ResourceException.class);
        }
    }

    @Nested
    @DisplayName("Entiers et flottants")
    class Numbers {

        @Test
        @DisplayName("parse un entier positif")
        void shouldParsePositiveInt() {
            assertThat(ResourceHelper.getInteger("42")).isEqualTo(42);
        }

        @Test
        @DisplayName("parse un entier négatif")
        void shouldParseNegativeInt() {
            assertThat(ResourceHelper.getInteger("-10")).isEqualTo(-10);
        }

        @Test
        @DisplayName("rejette un entier invalide")
        void shouldRejectInvalidInt() {
            assertThatThrownBy(() -> ResourceHelper.getInteger("abc"))
                    .isInstanceOf(jo.layoutlib.resources.ResourceException.class);
        }

        @Test
        @DisplayName("parse un flottant")
        void shouldParseFloat() {
            assertThat(ResourceHelper.getFloat("3.14")).isEqualTo(3.14f);
        }
    }

    @Nested
    @DisplayName("Références")
    class References {

        @Test
        @DisplayName("isReference retourne true pour @color/foo")
        void shouldReturnTrueForReference() {
            assertThat(ResourceHelper.isReference("@color/foo")).isTrue();
            assertThat(ResourceHelper.isReference("?attr/colorPrimary")).isTrue();
        }

        @Test
        @DisplayName("isReference retourne false pour une valeur littérale")
        void shouldReturnFalseForLiteral() {
            assertThat(ResourceHelper.isReference("#FF0000")).isFalse();
            assertThat(ResourceHelper.isReference("42")).isFalse();
        }
    }
}
