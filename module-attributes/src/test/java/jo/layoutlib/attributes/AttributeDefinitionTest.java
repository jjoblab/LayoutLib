package jo.layoutlib.attributes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires du {@link AttributeDefinition}.
 *
 * @author jo@Dev
 */
@DisplayName("AttributeDefinition — définition d'attribut")
class AttributeDefinitionTest {

    private AttributeDefinition attr;

    @BeforeEach
    void setUp() {
        attr = new AttributeDefinition("cornerRadius",
                new AttributeFormat[]{AttributeFormat.DIMENSION});
    }

    @Nested
    @DisplayName("Validation de valeurs")
    class ValueValidation {

        @Test
        @DisplayName("valide une dimension correcte")
        void shouldValidateCorrectDimension() {
            assertThat(attr.isValidValue("16dp")).isTrue();
            assertThat(attr.isValidValue("8sp")).isTrue();
            assertThat(attr.isValidValue("100px")).isTrue();
        }

        @Test
        @DisplayName("rejette une dimension invalide")
        void shouldRejectInvalidDimension() {
            assertThat(attr.isValidValue("abc")).isFalse();
            assertThat(attr.isValidValue("16")).isFalse();
        }

        @Test
        @DisplayName("valide une couleur pour un attribut color")
        void shouldValidateColorForColorAttr() {
            AttributeDefinition colorAttr = new AttributeDefinition("bg",
                    new AttributeFormat[]{AttributeFormat.COLOR});
            assertThat(colorAttr.isValidValue("#FF6750A4")).isTrue();
            assertThat(colorAttr.isValidValue("@color/foo")).isTrue();
            assertThat(colorAttr.isValidValue("not_a_color")).isFalse();
        }

        @Test
        @DisplayName("valide un booléen")
        void shouldValidateBoolean() {
            AttributeDefinition boolAttr = new AttributeDefinition("enabled",
                    new AttributeFormat[]{AttributeFormat.BOOLEAN});
            assertThat(boolAttr.isValidValue("true")).isTrue();
            assertThat(boolAttr.isValidValue("false")).isTrue();
            assertThat(boolAttr.isValidValue("1")).isTrue();
            assertThat(boolAttr.isValidValue("maybe")).isFalse();
        }

        @Test
        @DisplayName("valide un entier")
        void shouldValidateInteger() {
            AttributeDefinition intAttr = new AttributeDefinition("max",
                    new AttributeFormat[]{AttributeFormat.INTEGER});
            assertThat(intAttr.isValidValue("42")).isTrue();
            assertThat(intAttr.isValidValue("-10")).isTrue();
            assertThat(intAttr.isValidValue("3.14")).isFalse();
        }

        @Test
        @DisplayName("accepte n'importe quelle valeur si pas de format défini")
        void shouldAcceptAnyValueIfNoFormat() {
            AttributeDefinition anyAttr = new AttributeDefinition("x", new AttributeFormat[0]);
            assertThat(anyAttr.isValidValue("anything")).isTrue();
        }
    }

    @Nested
    @DisplayName("Enums")
    class Enums {

        @Test
        @DisplayName("récupère la valeur d'un enum")
        void shouldGetEnumValue() {
            AttributeDefinition enumAttr = new AttributeDefinition("gravity",
                    new AttributeFormat[]{AttributeFormat.ENUM});
            enumAttr.addEnumValue("textStart", 1);
            enumAttr.addEnumValue("textEnd", 2);
            assertThat(enumAttr.getEnumValue("textStart")).isEqualTo(1);
            assertThat(enumAttr.getEnumValue("textEnd")).isEqualTo(2);
            assertThat(enumAttr.getEnumValue("unknown")).isNull();
        }

        @Test
        @DisplayName("valide une valeur d'enum connue")
        void shouldValidateKnownEnum() {
            AttributeDefinition enumAttr = new AttributeDefinition("gravity",
                    new AttributeFormat[]{AttributeFormat.ENUM});
            enumAttr.addEnumValue("top", 1);
            assertThat(enumAttr.isValidValue("top")).isTrue();
            assertThat(enumAttr.isValidValue("bottom")).isFalse();
        }
    }

    @Nested
    @DisplayName("Flags")
    class Flags {

        @Test
        @DisplayName("récupère la valeur combinée d'un flag unique")
        void shouldGetSingleFlagValue() {
            AttributeDefinition flagAttr = new AttributeDefinition("flags",
                    new AttributeFormat[]{AttributeFormat.FLAG});
            flagAttr.addFlagValue("bold", 1);
            flagAttr.addFlagValue("italic", 2);
            assertThat(flagAttr.getFlagValue("bold")).isEqualTo(1);
            assertThat(flagAttr.getFlagValue("italic")).isEqualTo(2);
        }

        @Test
        @DisplayName("combine plusieurs flags avec |")
        void shouldCombineMultipleFlags() {
            AttributeDefinition flagAttr = new AttributeDefinition("flags",
                    new AttributeFormat[]{AttributeFormat.FLAG});
            flagAttr.addFlagValue("bold", 1);
            flagAttr.addFlagValue("italic", 2);
            flagAttr.addFlagValue("underline", 4);
            assertThat(flagAttr.getFlagValue("bold|italic")).isEqualTo(3);
            assertThat(flagAttr.getFlagValue("bold|italic|underline")).isEqualTo(7);
        }

        @Test
        @DisplayName("retourne null pour un flag inconnu")
        void shouldReturnNullForUnknownFlag() {
            AttributeDefinition flagAttr = new AttributeDefinition("flags",
                    new AttributeFormat[]{AttributeFormat.FLAG});
            flagAttr.addFlagValue("bold", 1);
            assertThat(flagAttr.getFlagValue("unknown")).isNull();
        }
    }

    @Nested
    @DisplayName("Méthodes utilitaires")
    class UtilityMethods {

        @Test
        @DisplayName("acceptsFormat retourne true pour un format accepté")
        void shouldReturnTrueForAcceptedFormat() {
            assertThat(attr.acceptsFormat(AttributeFormat.DIMENSION)).isTrue();
            assertThat(attr.acceptsFormat(AttributeFormat.COLOR)).isFalse();
        }

        @Test
        @DisplayName("getFormatsString produit la chaîne correcte")
        void shouldProduceFormatsString() {
            AttributeDefinition multi = new AttributeDefinition("x",
                    new AttributeFormat[]{AttributeFormat.REFERENCE, AttributeFormat.COLOR});
            assertThat(multi.getFormatsString()).isEqualTo("reference|color");
        }
    }
}
