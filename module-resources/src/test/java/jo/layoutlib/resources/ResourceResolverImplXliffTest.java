package jo.layoutlib.resources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de résolution des chaînes preview avec exemples XLIFF (v3.3).
 *
 * <p>Valide le pipeline complet : capture des {@code <xliff:g id/example>}
 * dans les {@code <string>} et les items de {@code <plurals>} → substitution
 * des exemples par {@code getPreviewString}/{@code getPreviewQuantityString}
 * (chaînage des {@code @string/} avec héritage des spans de la cible, repli
 * de qualifier, catégorie CLDR + repli {@code other}), et le contrat
 * device-fidèle : {@code getString} laisse les placeholders intacts.</p>
 *
 * @author jo@Dev
 * @since 3.3
 */
@DisplayName("ResourceResolverImpl — preview lisible <xliff:g>")
class ResourceResolverImplXliffTest {

    private static final String XLIFF_NS =
            " xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\"";

    private ResourceTable table;
    private ResourceResolverImpl resolver;

    @BeforeEach
    void setUp() {
        table = new ResourceTable();

        // values/strings.xml — défaut (anglais par convention du projet test)
        ResourceFileParser defParser = new ResourceFileParser(table, ResourceQualifier.DEFAULT);
        defParser.parse("<resources" + XLIFF_NS + ">"
                + "<string name=\"songs\">%1$d songs by "
                + "<xliff:g id=\"artist\" example=\"Adele\">%2$s</xliff:g>"
                + "</string>"
                + "<string name=\"plain\">Aucun placeholder</string>"
                + "<string name=\"no_example\">"
                + "<xliff:g id=\"count\">%1$d</xliff:g> songs"
                + "</string>"
                + "<string name=\"wrapped\">"
                + "<xliff:g id=\"count\" example=\"7\">%1$d</xliff:g> songs by "
                + "<xliff:g id=\"artist\" example=\"Adele\">%2$s</xliff:g>"
                + "</string>"
                + "<string name=\"alias\">@string/wrapped</string>"
                + "<plurals name=\"item_count\">"
                + "<item quantity=\"one\">"
                + "<xliff:g id=\"count\" example=\"1\">%d</xliff:g> item"
                + "</item>"
                + "<item quantity=\"other\">"
                + "<xliff:g id=\"count\" example=\"7\">%d</xliff:g> items"
                + "</item>"
                + "</plurals>"
                + "<string-array name=\"labels\">"
                + "<item>%1$d by <xliff:g id=\"artist\" example=\"Adele\">%2$s"
                + "</xliff:g>!</item>"
                + "</string-array>"
                + "</resources>");

        // values-fr/strings.xml — traduction avec ses propres exemples
        ResourceFileParser frParser = new ResourceFileParser(
                table, ResourceQualifier.forLanguage("fr"));
        frParser.parse("<resources" + XLIFF_NS + ">"
                + "<string name=\"songs\">%1$d chansons de "
                + "<xliff:g id=\"artist\" example=\"Adèle\">%2$s</xliff:g>"
                + "</string>"
                + "<plurals name=\"item_count\">"
                + "<item quantity=\"one\">"
                + "<xliff:g id=\"count\" example=\"1\">%d</xliff:g> élément"
                + "</item>"
                + "<item quantity=\"many\">"
                + "<xliff:g id=\"count\" example=\"2000000\">%d</xliff:g> millions d'éléments"
                + "</item>"
                + "<item quantity=\"other\">"
                + "<xliff:g id=\"count\" example=\"7\">%d</xliff:g> éléments"
                + "</item>"
                + "</plurals>"
                + "</resources>");

        resolver = new ResourceResolverImpl(table);
    }

    @Nested
    @DisplayName("getPreviewString — chaînes")
    class PreviewString {

        @Test
        @DisplayName("l'exemple remplace le placeholder, l'autre reste visible")
        void shouldSubstituteExampleOnly() {
            assertThat(resolver.getPreviewString("@string/songs"))
                    .isEqualTo("%1$d songs by Adele");
        }

        @Test
        @DisplayName("chaîne sans segment xliff → identique à getString")
        void shouldReturnRawStringWithoutSpans() {
            assertThat(resolver.getPreviewString("@string/plain"))
                    .isEqualTo("Aucun placeholder")
                    .isEqualTo(resolver.getString("@string/plain"));
        }

        @Test
        @DisplayName("segment sans example → placeholder intact")
        void shouldKeepPlaceholderWhenNoExample() {
            assertThat(resolver.getPreviewString("@string/no_example"))
                    .isEqualTo("%1$d songs");
        }

        @Test
        @DisplayName("plusieurs segments substitués en cascade")
        void shouldSubstituteSeveralExamples() {
            assertThat(resolver.getPreviewString("@string/wrapped"))
                    .isEqualTo("7 songs by Adele");
        }

        @Test
        @DisplayName("chaînage @string/ : les spans de la cible sont appliqués")
        void shouldApplyTargetSpansThroughChain() {
            assertThat(resolver.getPreviewString("@string/alias"))
                    .isEqualTo("7 songs by Adele");
        }

        @Test
        @DisplayName("values-fr après setLanguage : exemple localisé")
        void shouldFollowQualifier() {
            resolver.setLanguage("fr");
            assertThat(resolver.getPreviewString("@string/songs"))
                    .isEqualTo("%1$d chansons de Adèle");
            resolver.setLanguage("en");
            assertThat(resolver.getPreviewString("@string/songs"))
                    .isEqualTo("%1$d songs by Adele");
        }

        @Test
        @DisplayName("référence inconnue ou null → null")
        void shouldReturnNullWhenUnresolvable() {
            assertThat(resolver.getPreviewString("@string/inconnu")).isNull();
            assertThat(resolver.getPreviewString(null)).isNull();
        }

        @Test
        @DisplayName("contrat device-fidèle : getString laisse les placeholders")
        void shouldKeepGetStringDeviceFaithful() {
            assertThat(resolver.getString("@string/wrapped"))
                    .isEqualTo("%1$d songs by %2$s");
        }
    }

    @Nested
    @DisplayName("getPreviewQuantityString — pluriels")
    class PreviewQuantityString {

        @Test
        @DisplayName("anglais : 1 → one, 2 → other, exemples substitués")
        void shouldSelectCategoryAndSubstitute() {
            assertThat(resolver.getPreviewQuantityString("@plurals/item_count", 1))
                    .isEqualTo("1 item");
            assertThat(resolver.getPreviewQuantityString("@plurals/item_count", 2))
                    .isEqualTo("7 items");
        }

        @Test
        @DisplayName("français : 0/1 → one, 2 → other, 2 000 000 → many")
        void shouldFollowFrenchRules() {
            resolver.setLanguage("fr");
            assertThat(resolver.getPreviewQuantityString("@plurals/item_count", 0))
                    .isEqualTo("1 élément");
            assertThat(resolver.getPreviewQuantityString("@plurals/item_count", 2))
                    .isEqualTo("7 éléments");
            assertThat(resolver.getPreviewQuantityString("@plurals/item_count", 2_000_000))
                    .isEqualTo("2000000 millions d'éléments");
        }

        @Test
        @DisplayName("le nom nu est accepté en plus de @plurals/")
        void shouldAcceptBareName() {
            assertThat(resolver.getPreviewQuantityString("item_count", 2))
                    .isEqualTo("7 items");
        }

        @Test
        @DisplayName("plurals sans xliff → texte brut (pattern intact)")
        void shouldReturnRawTextWithoutSpans() {
            ResourceFileParser parser = new ResourceFileParser(table, ResourceQualifier.DEFAULT);
            parser.parse("<resources>"
                    + "<plurals name=\"brut\">"
                    + "<item quantity=\"other\">%d éléments</item>"
                    + "</plurals></resources>");
            assertThat(resolver.getPreviewQuantityString("@plurals/brut", 5))
                    .isEqualTo("%d éléments");
        }

        @Test
        @DisplayName("plurals inconnu ou null → null")
        void shouldReturnNullWhenUnresolvable() {
            assertThat(resolver.getPreviewQuantityString("@plurals/inconnu", 1)).isNull();
            assertThat(resolver.getPreviewQuantityString(null, 1)).isNull();
        }
    }

    @Nested
    @DisplayName("Capture du parser — introspection")
    class ParserCapture {

        @Test
        @DisplayName("la table expose les spans capturés (string + plurals)")
        void shouldExposeCapturedSpans() {
            assertThat(table.hasStringXliffSpans("songs")).isTrue();
            assertThat(table.hasStringXliffSpans("plain")).isFalse();
            assertThat(table.getStringXliffSpans("songs", ResourceQualifier.DEFAULT))
                    .hasSize(1);
            List<XliffSpan> spans = table.getStringXliffSpans("songs", ResourceQualifier.DEFAULT);
            assertThat(spans.get(0).getId()).isEqualTo("artist");
            assertThat(spans.get(0).getExample()).isEqualTo("Adele");
            assertThat(spans.get(0).getPlaceholder()).isEqualTo("%2$s");
            assertThat(table.hasPluralsXliffSpans("item_count")).isTrue();
            assertThat(table.stringXliffCount()).isEqualTo(3);
            assertThat(table.pluralsXliffCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("string-array : le texte après une balise inline est conservé (bug v3.2 corrigé), placeholder intact")
        void shouldKeepTextAfterInlineTagInArrayItems() {
            List<String> labels = resolver.getStringArray("@array/labels");
            // Le « ! » qui suit </xliff:g> était perdu avant v3.3 ; le
            // placeholder reste brut (device-fidèle — pas de capture xliff
            // dans les items de string-array, par design).
            assertThat(labels).containsExactly("%1$d by %2$s!");
        }
    }
}
