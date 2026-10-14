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

    /** Namespace XLIFF standard d'Android pour les tests <xliff:g> (v3.3). */
    private static final String XLIFF_NS =
            " xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\"";

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
        @DisplayName("les plurals atterrissent dans leur propre table, pas parmi les chaînes")
        void shouldStorePluralsInTheirOwnTable() {
            parser.parse("<resources>"
                    + "<string name=\"hello\">Hello</string>"
                    + "<string-array name=\"items\"><item>A</item><item>B</item></string-array>"
                    + "<plurals name=\"count\"><item quantity=\"one\">un</item></plurals>"
                    + "</resources>");
            assertThat(table.stringCount()).isEqualTo(1);
            assertThat(table.getString("hello", ResourceQualifier.DEFAULT)).isEqualTo("Hello");
            assertThat(table.pluralsCount()).isEqualTo(1);
            assertThat(table.getPlurals("count", ResourceQualifier.DEFAULT))
                    .containsEntry("one", "un");
        }
    }

    @Nested
    @DisplayName("Parsing de <plurals>")
    class PluralsParsing {

        @Test
        @DisplayName("parse les items par catégorie CLDR")
        void shouldParsePluralItemsByCategory() {
            parser.parse("<resources><plurals name=\"songs\">"
                    + "<item quantity=\"one\">%d chanson</item>"
                    + "<item quantity=\"other\">%d chansons</item>"
                    + "</plurals></resources>");
            assertThat(table.getPlurals("songs", ResourceQualifier.DEFAULT))
                    .containsEntry("one", "%d chanson")
                    .containsEntry("other", "%d chansons")
                    .hasSize(2);
        }

        @Test
        @DisplayName("préserve l'ordre de déclaration des items")
        void shouldPreserveItemOrder() {
            parser.parse("<resources><plurals name=\"ordered\">"
                    + "<item quantity=\"many\">A</item>"
                    + "<item quantity=\"one\">B</item>"
                    + "<item quantity=\"other\">C</item>"
                    + "</plurals></resources>");
            assertThat(table.getPlurals("ordered", ResourceQualifier.DEFAULT).keySet())
                    .containsExactly("many", "one", "other");
        }

        @Test
        @DisplayName("ignore les items sans attribut quantity")
        void shouldIgnoreItemsWithoutQuantity() {
            parser.parse("<resources><plurals name=\"partial\">"
                    + "<item>orphan</item>"
                    + "<item quantity=\"\">vide</item>"
                    + "<item quantity=\"other\">garde</item>"
                    + "</plurals></resources>");
            assertThat(table.getPlurals("partial", ResourceQualifier.DEFAULT))
                    .hasSize(1)
                    .containsEntry("other", "garde");
        }

        @Test
        @DisplayName("catégorie dupliquée : le dernier item gagne")
        void shouldLetLastDuplicateWin() {
            parser.parse("<resources><plurals name=\"dup\">"
                    + "<item quantity=\"other\">premier</item>"
                    + "<item quantity=\"other\">second</item>"
                    + "</plurals></resources>");
            assertThat(table.getPlurals("dup", ResourceQualifier.DEFAULT))
                    .containsEntry("other", "second")
                    .hasSize(1);
        }

        @Test
        @DisplayName("aplatit les balises inline du texte (comme <string>)")
        void shouldFlattenInlineTags() {
            parser.parse("<resources><plurals name=\"rich\">"
                    + "<item quantity=\"other\">%d <b>éléments</b> clés</item>"
                    + "</plurals></resources>");
            assertThat(table.getPlurals("rich", ResourceQualifier.DEFAULT))
                    .containsEntry("other", "%d éléments clés");
        }

        @Test
        @DisplayName("stocke les catégories inconnues de CLDR sans échouer")
        void shouldStoreUnknownCategories() {
            parser.parse("<resources><plurals name=\"weird\">"
                    + "<item quantity=\"lots\">beaucoup</item>"
                    + "</plurals></resources>");
            assertThat(table.getPlurals("weird", ResourceQualifier.DEFAULT))
                    .containsEntry("lots", "beaucoup");
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
    @DisplayName("Parsing des segments <xliff:g>")
    class XliffParsing {

        @Test
        @DisplayName("capture id/example, le texte brut garde le placeholder")
        void shouldCaptureXliffAttributes() {
            parser.parse("<resources" + XLIFF_NS + ">"
                    + "<string name=\"songs\">%1$d chansons — "
                    + "<xliff:g id=\"artist\" example=\"Adèle\">%2$s</xliff:g>"
                    + "</string></resources>");
            assertThat(table.getString("songs", ResourceQualifier.DEFAULT))
                    .isEqualTo("%1$d chansons — %2$s");
            java.util.List<XliffSpan> spans =
                    table.getStringXliffSpans("songs", ResourceQualifier.DEFAULT);
            assertThat(spans).hasSize(1);
            assertThat(spans.get(0).getId()).isEqualTo("artist");
            assertThat(spans.get(0).getExample()).isEqualTo("Adèle");
            assertThat(spans.get(0).getPlaceholder()).isEqualTo("%2$s");
        }

        @Test
        @DisplayName("plusieurs segments capturés dans l'ordre")
        void shouldCaptureSeveralSpansInOrder() {
            parser.parse("<resources" + XLIFF_NS + ">"
                    + "<string name=\"full\">"
                    + "<xliff:g id=\"count\" example=\"7\">%1$d</xliff:g> chansons de "
                    + "<xliff:g id=\"artist\" example=\"Adèle\">%2$s</xliff:g>"
                    + "</string></resources>");
            java.util.List<XliffSpan> spans =
                    table.getStringXliffSpans("full", ResourceQualifier.DEFAULT);
            assertThat(spans).hasSize(2);
            assertThat(spans.get(0).getId()).isEqualTo("count");
            assertThat(spans.get(1).getId()).isEqualTo("artist");
        }

        @Test
        @DisplayName("segment sans example → span capturé, attribut null")
        void shouldCaptureSpanWithoutExample() {
            parser.parse("<resources" + XLIFF_NS + ">"
                    + "<string name=\"count_only\">"
                    + "<xliff:g id=\"count\">%1$d</xliff:g> items"
                    + "</string></resources>");
            java.util.List<XliffSpan> spans =
                    table.getStringXliffSpans("count_only", ResourceQualifier.DEFAULT);
            assertThat(spans).hasSize(1);
            assertThat(spans.get(0).getExample()).isNull();
            assertThat(spans.get(0).hasExample()).isFalse();
        }

        @Test
        @DisplayName("préfixe non déclaré → XML invalide rejeté (comme aapt2)")
        void shouldRejectUndeclaredPrefix() {
            // aapt2 exige la déclaration xmlns:xliff ; un préfixe non lié
            // est un XML invalide — rejeté comme tout XML malformé.
            assertThatThrownBy(() -> parser.parse("<resources>"
                    + "<string name=\"songs\">%1$d chansons de "
                    + "<xliff:g id=\"artist\" example=\"Adèle\">%2$s</xliff:g>"
                    + "</string></resources>"))
                    .isInstanceOf(ResourceException.class);
        }

        @Test
        @DisplayName("chaîne sans xliff → aucune entrée dans la table des spans")
        void shouldNotStoreSpansForPlainString() {
            parser.parse("<resources" + XLIFF_NS + ">"
                    + "<string name=\"plain\">Hello</string></resources>");
            assertThat(table.hasStringXliffSpans("plain")).isFalse();
            assertThat(table.stringXliffCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("balise inline non-xliff traversée sans capture")
        void shouldTraverseNonXliffInlineTags() {
            parser.parse("<resources" + XLIFF_NS + ">"
                    + "<string name=\"rich\"><b>Gras</b> et %d</string></resources>");
            assertThat(table.getString("rich", ResourceQualifier.DEFAULT))
                    .isEqualTo("Gras et %d");
            assertThat(table.hasStringXliffSpans("rich")).isFalse();
        }

        @Test
        @DisplayName("spans des items de <plurals> capturés par catégorie")
        void shouldCapturePluralsSpansByCategory() {
            parser.parse("<resources" + XLIFF_NS + ">"
                    + "<plurals name=\"items\">"
                    + "<item quantity=\"one\">"
                    + "<xliff:g id=\"count\" example=\"1\">%d</xliff:g> item"
                    + "</item>"
                    + "<item quantity=\"other\">"
                    + "<xliff:g id=\"count\" example=\"7\">%d</xliff:g> items"
                    + "</item>"
                    + "</plurals></resources>");
            assertThat(table.getPlurals("items", ResourceQualifier.DEFAULT))
                    .containsEntry("one", "%d item")
                    .containsEntry("other", "%d items");
            java.util.Map<String, java.util.List<XliffSpan>> byCategory =
                    table.getPluralsXliffSpans("items", ResourceQualifier.DEFAULT);
            assertThat(byCategory).containsKeys("one", "other");
            assertThat(byCategory.get("one").get(0).getExample()).isEqualTo("1");
            assertThat(byCategory.get("other").get(0).getExample()).isEqualTo("7");
        }

        @Test
        @DisplayName("plurals mixte : seul l'item avec xliff porte des spans")
        void shouldStoreSpansOnlyForRichItems() {
            parser.parse("<resources" + XLIFF_NS + ">"
                    + "<plurals name=\"mix\">"
                    + "<item quantity=\"one\">seul</item>"
                    + "<item quantity=\"other\">"
                    + "<xliff:g id=\"count\" example=\"7\">%d</xliff:g> items"
                    + "</item>"
                    + "</plurals></resources>");
            java.util.Map<String, java.util.List<XliffSpan>> byCategory =
                    table.getPluralsXliffSpans("mix", ResourceQualifier.DEFAULT);
            assertThat(byCategory).containsOnlyKeys("other");
        }
    }

    @Nested
    @DisplayName("Parsing de <integer-array>")
    class IntegerArrayParsing {

        @Test
        @DisplayName("parse les items en entiers")
        void shouldParseIntegerArrayItems() {
            parser.parse("<resources><integer-array name=\"ratios\">"
                    + "<item>2</item><item>3</item><item>5</item>"
                    + "</integer-array></resources>");
            assertThat(table.getIntegerArray("ratios", ResourceQualifier.DEFAULT))
                    .containsExactly(2, 3, 5);
        }

        @Test
        @DisplayName("items malformés ou vides neutralisés en 0 (longueur préservée)")
        void shouldNeutralizeMalformedItems() {
            parser.parse("<resources><integer-array name=\"poison\">"
                    + "<item>1</item><item>abc</item><item></item><item>-7</item>"
                    + "</integer-array></resources>");
            assertThat(table.getIntegerArray("poison", ResourceQualifier.DEFAULT))
                    .containsExactly(1, 0, 0, -7);
        }

        @Test
        @DisplayName("espaces autour des valeurs tolérés")
        void shouldTrimItemValues() {
            parser.parse("<resources><integer-array name=\"spaced\">"
                    + "<item>  16  </item><item>\n24\n</item>"
                    + "</integer-array></resources>");
            assertThat(table.getIntegerArray("spaced", ResourceQualifier.DEFAULT))
                    .containsExactly(16, 24);
        }

        @Test
        @DisplayName("array vide → liste vide")
        void shouldReturnEmptyArray() {
            parser.parse("<resources><integer-array name=\"empty\">"
                    + "</integer-array></resources>");
            assertThat(table.getIntegerArray("empty", ResourceQualifier.DEFAULT)).isEmpty();
        }

        @Test
        @DisplayName("integer-array et string-array sont indépendants")
        void shouldKeepArrayTypesSeparate() {
            parser.parse("<resources>"
                    + "<integer-array name=\"nums\"><item>1</item></integer-array>"
                    + "<string-array name=\"labels\"><item>un</item></string-array>"
                    + "</resources>");
            assertThat(table.integerArrayCount()).isEqualTo(1);
            assertThat(table.stringArrayCount()).isEqualTo(1);
            assertThat(table.hasIntegerArray("labels")).isFalse();
            assertThat(table.hasStringArray("nums")).isFalse();
        }

        @Test
        @DisplayName("compte comme un élément parsé")
        void shouldCountAsParsedElement() {
            int count = parser.parse("<resources>"
                    + "<integer-array name=\"nums\"><item>1</item><item>2</item></integer-array>"
                    + "</resources>");
            assertThat(count).isEqualTo(1);
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
