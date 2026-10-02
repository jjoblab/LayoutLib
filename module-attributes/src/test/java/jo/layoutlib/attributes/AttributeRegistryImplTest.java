package jo.layoutlib.attributes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import jo.layoutlib.attributes.api.AttributeDefinitionImpl;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires du {@link AttributeRegistryImpl}.
 *
 * @author jo@Dev
 */
@DisplayName("AttributeRegistryImpl — registre des attributs custom")
class AttributeRegistryImplTest {

    private AttributeRegistryImpl registry;

    @BeforeEach
    void setUp() {
        registry = new AttributeRegistryImpl();
    }

    @Nested
    @DisplayName("Enregistrement")
    class Registration {

        @Test
        @DisplayName("parse un declare-styleable simple")
        void shouldParseSimpleStyleable() {
            registry.registerAttrsFile(
                    "<resources>"
                            + "<declare-styleable name=\"MaterialButton\">"
                            + "<attr name=\"cornerRadius\" format=\"dimension\"/>"
                            + "</declare-styleable>"
                            + "</resources>");
            assertThat(registry.getAttributeCount()).isEqualTo(1);
            assertThat(registry.getStyleableCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("parse plusieurs styleables")
        void shouldParseMultipleStyleables() {
            registry.registerAttrsFile(
                    "<resources>"
                            + "<declare-styleable name=\"A\">"
                            + "<attr name=\"x\" format=\"dimension\"/>"
                            + "</declare-styleable>"
                            + "<declare-styleable name=\"B\">"
                            + "<attr name=\"y\" format=\"color\"/>"
                            + "</declare-styleable>"
                            + "</resources>");
            assertThat(registry.getStyleableCount()).isEqualTo(2);
            assertThat(registry.getAttributeCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("parse un attribut avec enum")
        void shouldParseAttrWithEnum() {
            registry.registerAttrsFile(
                    "<resources>"
                            + "<declare-styleable name=\"MyView\">"
                            + "<attr name=\"gravity\" format=\"enum\">"
                            + "<enum name=\"top\" value=\"1\"/>"
                            + "<enum name=\"bottom\" value=\"2\"/>"
                            + "</attr>"
                            + "</declare-styleable>"
                            + "</resources>");
            AttributeDefinitionImpl attr = registry.getAttributeDefinition("gravity");
            assertThat(attr).isNotNull();
            assertThat(attr.getEnumValue("top")).isEqualTo(1);
            assertThat(attr.getEnumValue("bottom")).isEqualTo(2);
        }

        @Test
        @DisplayName("parse un attribut avec flags")
        void shouldParseAttrWithFlags() {
            registry.registerAttrsFile(
                    "<resources>"
                            + "<declare-styleable name=\"TextView\">"
                            + "<attr name=\"textStyle\" format=\"flag\">"
                            + "<flag name=\"bold\" value=\"1\"/>"
                            + "<flag name=\"italic\" value=\"2\"/>"
                            + "</attr>"
                            + "</declare-styleable>"
                            + "</resources>");
            AttributeDefinitionImpl attr = registry.getAttributeDefinition("textStyle");
            assertThat(attr).isNotNull();
            assertThat(attr.getFlagValue("bold|italic")).isEqualTo(3);
        }

        @Test
        @DisplayName("parse un flag en hex (0x...)")
        void shouldParseHexFlagValue() {
            registry.registerAttrsFile(
                    "<resources>"
                            + "<declare-styleable name=\"V\">"
                            + "<attr name=\"flags\" format=\"flag\">"
                            + "<flag name=\"x\" value=\"0x10\"/>"
                            + "</attr>"
                            + "</declare-styleable>"
                            + "</resources>");
            AttributeDefinitionImpl attr = registry.getAttributeDefinition("flags");
            assertThat(attr.getFlagValue("x")).isEqualTo(16);
        }

        @Test
        @DisplayName("parse un attribut global (hors styleable)")
        void shouldParseGlobalAttr() {
            registry.registerAttrsFile(
                    "<resources>"
                            + "<attr name=\"customAttr\" format=\"string\"/>"
                            + "</resources>");
            assertThat(registry.isKnownAttribute("customAttr")).isTrue();
        }
    }

    @Nested
    @DisplayName("Recherche")
    class Lookup {

        @Test
        @DisplayName("isKnownAttribute retourne true pour un attribut connu")
        void shouldReturnTrueForKnownAttr() {
            registry.registerAttrsFile(
                    "<resources>"
                            + "<declare-styleable name=\"V\">"
                            + "<attr name=\"foo\" format=\"dimension\"/>"
                            + "</declare-styleable>"
                            + "</resources>");
            assertThat(registry.isKnownAttribute("foo")).isTrue();
            assertThat(registry.isKnownAttribute("bar")).isFalse();
        }

        @Test
        @DisplayName("getAttributeFormat retourne la chaîne de formats")
        void shouldReturnFormatsString() {
            registry.registerAttrsFile(
                    "<resources>"
                            + "<declare-styleable name=\"V\">"
                            + "<attr name=\"foo\" format=\"reference|color\"/>"
                            + "</declare-styleable>"
                            + "</resources>");
            assertThat(registry.getAttributeFormat("foo")).isEqualTo("reference|color");
        }

        @Test
        @DisplayName("getAttributeFormat retourne null pour un attribut inconnu")
        void shouldReturnNullForUnknownAttr() {
            assertThat(registry.getAttributeFormat("unknown")).isNull();
        }
    }

    @Nested
    @DisplayName("Cas limites")
    class EdgeCases {

        @Test
        @DisplayName("ignore un XML null ou vide")
        void shouldIgnoreNullOrEmptyXml() {
            registry.registerAttrsFile(null);
            registry.registerAttrsFile("");
            assertThat(registry.getAttributeCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("ignore un styleable sans nom")
        void shouldIgnoreStyleableWithoutName() {
            registry.registerAttrsFile(
                    "<resources><declare-styleable><attr name=\"x\" format=\"string\"/></declare-styleable></resources>");
            assertThat(registry.getStyleableCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("clear vide le registre")
        void shouldClearRegistry() {
            registry.registerAttrsFile(
                    "<resources><declare-styleable name=\"V\"><attr name=\"x\" format=\"string\"/></declare-styleable></resources>");
            registry.clear();
            assertThat(registry.getAttributeCount()).isEqualTo(0);
            assertThat(registry.getStyleableCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Statistiques")
    class Statistics {

        @Test
        @DisplayName("getAttributeCount retourne le bon nombre")
        void shouldReturnCorrectAttributeCount() {
            registry.registerAttrsFile(
                    "<resources>"
                            + "<declare-styleable name=\"V\">"
                            + "<attr name=\"a\" format=\"string\"/>"
                            + "<attr name=\"b\" format=\"color\"/>"
                            + "</declare-styleable>"
                            + "</resources>");
            assertThat(registry.getAttributeCount()).isEqualTo(2);
        }
    }
}
