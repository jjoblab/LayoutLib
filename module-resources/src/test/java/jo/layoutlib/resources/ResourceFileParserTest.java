package jo.layoutlib.resources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitaires du {@link ResourceFileParser}.
 *
 * <p>Valide le parsing des fichiers XML de resources (colors.xml, strings.xml,
 * dimens.xml, integers.xml, bools.xml) et l'insertion dans la
 * {@link ResourceTable}.</p>
 *
 * @author jo@Dev
 */
@DisplayName("ResourceFileParser — parsing des XML de resources")
class ResourceFileParserTest {

    private ResourceTable table;
    private ResourceFileParser parser;

    @BeforeEach
    void setUp() {
        table = new ResourceTable();
        parser = new ResourceFileParser(table, ResourceQualifier.DEFAULT);
    }

    @Nested
    @DisplayName("Parsing de colors.xml")
    class ColorsParsing {

        @Test
        @DisplayName("parse une couleur simple")
        void shouldParseSimpleColor() {
            parser.parse("<resources><color name=\"red\">#FF0000</color></resources>");
            assertThat(table.getColor("red", ResourceQualifier.DEFAULT))
                    .isEqualTo("#FF0000");
        }

        @Test
        @DisplayName("parse plusieurs couleurs")
        void shouldParseMultipleColors() {
            parser.parse("<resources>"
                    + "<color name=\"red\">#FF0000</color>"
                    + "<color name=\"green\">#00FF00</color>"
                    + "<color name=\"blue\">#0000FF</color>"
                    + "</resources>");
            assertThat(table.colorCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("supporte les références chainables")
        void shouldSupportChainableReferences() {
            parser.parse("<resources>"
                    + "<color name=\"primary\">@color/purple_500</color>"
                    + "</resources>");
            // La valeur stockée est la référence elle-même (résolution au moment du getColor)
            assertThat(table.hasColor("primary")).isTrue();
        }
    }

    @Nested
    @DisplayName("Parsing de strings.xml")
    class StringsParsing {

        @Test
        @DisplayName("parse une chaîne simple")
        void shouldParseSimpleString() {
            parser.parse("<resources><string name=\"hello\">Hello World</string></resources>");
            assertThat(table.getString("hello", ResourceQualifier.DEFAULT))
                    .isEqualTo("Hello World");
        }

        @Test
        @DisplayName("parse une chaîne avec espaces")
        void shouldParseStringWithSpaces() {
            parser.parse("<resources><string name=\"greeting\">  Bonjour le monde  </string></resources>");
            assertThat(table.getString("greeting", ResourceQualifier.DEFAULT))
                    .isEqualTo("Bonjour le monde");  // trim appliqué
        }

        @Test
        @DisplayName("ignore les string-array et plurals")
        void shouldIgnoreStringArraysAndPlurals() {
            parser.parse("<resources>"
                    + "<string name=\"hello\">Hello</string>"
                    + "<string-array name=\"items\"><item>A</item><item>B</item></string-array>"
                    + "<plurals name=\"count\"><item quantity=\"one\">un</item></plurals>"
                    + "</resources>");
            assertThat(table.stringCount()).isEqualTo(1);
            assertThat(table.getString("hello", ResourceQualifier.DEFAULT)).isEqualTo("Hello");
        }
    }

    @Nested
    @DisplayName("Parsing de dimens.xml")
    class DimensParsing {

        @Test
        @DisplayName("parse une dimension")
        void shouldParseDimen() {
            parser.parse("<resources><dimen name=\"margin\">16dp</dimen></resources>");
            assertThat(table.getDimen("margin", ResourceQualifier.DEFAULT)).isEqualTo("16dp");
        }

        @Test
        @DisplayName("parse plusieurs dimensions")
        void shouldParseMultipleDimens() {
            parser.parse("<resources>"
                    + "<dimen name=\"small\">8dp</dimen>"
                    + "<dimen name=\"medium\">16dp</dimen>"
                    + "<dimen name=\"large\">24dp</dimen>"
                    + "</resources>");
            assertThat(table.dimenCount()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Parsing de integers.xml et bools.xml")
    class IntegerAndBoolParsing {

        @Test
        @DisplayName("parse un entier")
        void shouldParseInteger() {
            parser.parse("<resources><integer name=\"max_items\">42</integer></resources>");
            assertThat(table.getInteger("max_items", ResourceQualifier.DEFAULT)).isEqualTo(42);
        }

        @Test
        @DisplayName("parse un booléen true")
        void shouldParseBooleanTrue() {
            parser.parse("<resources><bool name=\"enabled\">true</bool></resources>");
            assertThat(table.getBoolean("enabled", ResourceQualifier.DEFAULT)).isTrue();
        }

        @Test
        @DisplayName("parse un booléen false")
        void shouldParseBooleanFalse() {
            parser.parse("<resources><bool name=\"disabled\">false</bool></resources>");
            assertThat(table.getBoolean("disabled", ResourceQualifier.DEFAULT)).isFalse();
        }

        @Test
        @DisplayName("rejette un booléen invalide")
        void shouldRejectInvalidBoolean() {
            assertThatThrownBy(() ->
                    parser.parse("<resources><bool name=\"x\">maybe</bool></resources>"))
                    .isInstanceOf(ResourceException.class);
        }

        @Test
        @DisplayName("rejette un entier invalide")
        void shouldRejectInvalidInteger() {
            assertThatThrownBy(() ->
                    parser.parse("<resources><integer name=\"x\">abc</integer></resources>"))
                    .isInstanceOf(ResourceException.class);
        }
    }

    @Nested
    @DisplayName("Cas limites")
    class EdgeCases {

        @Test
        @DisplayName("retourne 0 pour un XML vide")
        void shouldReturnZeroForEmptyXml() {
            assertThat(parser.parse("")).isEqualTo(0);
            assertThat(parser.parse(null)).isEqualTo(0);
        }

        @Test
        @DisplayName("ignore les éléments sans attribut name")
        void shouldIgnoreElementsWithoutName() {
            parser.parse("<resources><color>#FF0000</color></resources>");
            assertThat(table.colorCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("ignore les tags inconnus")
        void shouldIgnoreUnknownTags() {
            parser.parse("<resources>"
                    + "<unknown name=\"x\">value</unknown>"
                    + "<color name=\"red\">#FF0000</color>"
                    + "</resources>");
            assertThat(table.colorCount()).isEqualTo(1);
        }
    }
}
