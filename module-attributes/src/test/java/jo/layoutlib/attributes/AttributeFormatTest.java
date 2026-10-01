package jo.layoutlib.attributes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires du {@link AttributeFormat}.
 *
 * @author jo@Dev
 */
@DisplayName("AttributeFormat — formats d'attributs")
class AttributeFormatTest {

    @Nested
    @DisplayName("fromXmlName")
    class FromXmlName {

        @Test
        @DisplayName("retourne DIMENSION pour 'dimension'")
        void shouldReturnDimension() {
            assertThat(AttributeFormat.fromXmlName("dimension"))
                    .isEqualTo(AttributeFormat.DIMENSION);
        }

        @Test
        @DisplayName("retourne COLOR pour 'color'")
        void shouldReturnColor() {
            assertThat(AttributeFormat.fromXmlName("color"))
                    .isEqualTo(AttributeFormat.COLOR);
        }

        @Test
        @DisplayName("retourne null pour un format inconnu")
        void shouldReturnNullForUnknown() {
            assertThat(AttributeFormat.fromXmlName("unknown")).isNull();
        }

        @Test
        @DisplayName("retourne null pour null ou vide")
        void shouldReturnNullForNullOrEmpty() {
            assertThat(AttributeFormat.fromXmlName(null)).isNull();
            assertThat(AttributeFormat.fromXmlName("")).isNull();
        }
    }

    @Nested
    @DisplayName("parseFormats")
    class ParseFormats {

        @Test
        @DisplayName("parse un format unique")
        void shouldParseSingleFormat() {
            AttributeFormat[] formats = AttributeFormat.parseFormats("dimension");
            assertThat(formats).hasSize(1);
            assertThat(formats[0]).isEqualTo(AttributeFormat.DIMENSION);
        }

        @Test
        @DisplayName("parse plusieurs formats séparés par |")
        void shouldParseMultipleFormats() {
            AttributeFormat[] formats = AttributeFormat.parseFormats("reference|color");
            assertThat(formats).hasSize(2);
            assertThat(formats[0]).isEqualTo(AttributeFormat.REFERENCE);
            assertThat(formats[1]).isEqualTo(AttributeFormat.COLOR);
        }

        @Test
        @DisplayName("retourne un tableau vide pour null ou vide")
        void shouldReturnEmptyForNullOrEmpty() {
            assertThat(AttributeFormat.parseFormats(null)).isEmpty();
            assertThat(AttributeFormat.parseFormats("")).isEmpty();
        }
    }

    @Nested
    @DisplayName("getXmlName")
    class GetXmlName {

        @Test
        @DisplayName("retourne le nom XML correct")
        void shouldReturnCorrectXmlName() {
            assertThat(AttributeFormat.DIMENSION.getXmlName()).isEqualTo("dimension");
            assertThat(AttributeFormat.COLOR.getXmlName()).isEqualTo("color");
            assertThat(AttributeFormat.ENUM.getXmlName()).isEqualTo("enum");
        }
    }
}
