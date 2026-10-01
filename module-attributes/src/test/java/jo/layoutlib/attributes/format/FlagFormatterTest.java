package jo.layoutlib.attributes.format;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitaires du {@link FlagFormatter}.
 *
 * @author jo@Dev
 */
@DisplayName("FlagFormatter — formatage de flags combinables")
class FlagFormatterTest {

    private FlagFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new FlagFormatter();
        formatter.addValue("normal", 0);
        formatter.addValue("bold", 1);
        formatter.addValue("italic", 2);
        formatter.addValue("underline", 4);
    }

    @Nested
    @DisplayName("Formatage")
    class Formatting {

        @Test
        @DisplayName("format un flag unique")
        void shouldFormatSingleFlag() {
            assertThat(formatter.format("bold")).isEqualTo(1);
            assertThat(formatter.format("italic")).isEqualTo(2);
        }

        @Test
        @DisplayName("format des flags combinés avec |")
        void shouldFormatCombinedFlags() {
            assertThat(formatter.format("bold|italic")).isEqualTo(3);
            assertThat(formatter.format("bold|italic|underline")).isEqualTo(7);
        }

        @Test
        @DisplayName("format normal retourne 0")
        void shouldReturnZeroForNormal() {
            assertThat(formatter.format("normal")).isEqualTo(0);
        }

        @Test
        @DisplayName("rejette un flag inconnu")
        void shouldRejectUnknownFlag() {
            assertThatThrownBy(() -> formatter.format("unknown"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejette une expression vide")
        void shouldRejectEmptyExpression() {
            assertThatThrownBy(() -> formatter.format(""))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> formatter.format(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("isValid retourne true pour un flag valide")
        void shouldReturnTrueForValidFlag() {
            assertThat(formatter.isValid("bold")).isTrue();
            assertThat(formatter.isValid("bold|italic")).isTrue();
        }

        @Test
        @DisplayName("isValid retourne false pour un flag inconnu")
        void shouldReturnFalseForUnknownFlag() {
            assertThat(formatter.isValid("unknown")).isFalse();
            assertThat(formatter.isValid("bold|unknown")).isFalse();
        }
    }

    @Nested
    @DisplayName("Décomposition")
    class Decomposition {

        @Test
        @DisplayName("decompose 0 retourne [normal]")
        void shouldDecomposeZero() {
            assertThat(formatter.decompose(0)).containsExactly("normal");
        }

        @Test
        @DisplayName("decompose 1 retourne [bold]")
        void shouldDecomposeBold() {
            assertThat(formatter.decompose(1)).containsExactly("bold");
        }

        @Test
        @DisplayName("decompose 3 retourne [bold, italic]")
        void shouldDecomposeBoldItalic() {
            assertThat(formatter.decompose(3))
                    .containsExactlyInAnyOrder("bold", "italic");
        }

        @Test
        @DisplayName("decompose 7 retourne [bold, italic, underline]")
        void shouldDecomposeAll() {
            assertThat(formatter.decompose(7))
                    .containsExactlyInAnyOrder("bold", "italic", "underline");
        }
    }
}
