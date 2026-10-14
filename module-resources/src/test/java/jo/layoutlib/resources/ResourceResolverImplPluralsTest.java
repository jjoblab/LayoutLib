package jo.layoutlib.resources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Tests de résolution des pluriels {@code <plurals>} (v3.2).
 *
 * <p>Valide le pipeline complet : parsing {@code @plurals/} → sélection de
 * la catégorie CLDR selon la langue courante ({@code setLanguage}), repli
 * {@code other} puis {@code null}, résolution des items chainables
 * {@code @string/} et {@code @plurals/}, formatage tolérant
 * {@code String.format}, copie défensive de l'introspection
 * {@code getPlurals} et invalidation du cache au changement de langue.</p>
 *
 * @author jo@Dev
 * @since 3.2
 */
@DisplayName("ResourceResolverImpl — pluriels <plurals>")
class ResourceResolverImplPluralsTest {

    private ResourceTable table;
    private ResourceResolverImpl resolver;

    @BeforeEach
    void setUp() {
        table = new ResourceTable();

        // values/strings.xml — défaut (anglais par convention du projet test)
        ResourceFileParser defParser = new ResourceFileParser(table, ResourceQualifier.DEFAULT);
        defParser.parse("<resources>"
                + "<plurals name=\"item_count\">"
                + "<item quantity=\"one\">%d item</item>"
                + "<item quantity=\"other\">%d items</item>"
                + "</plurals>"
                + "<plurals name=\"minimal\">"
                + "<item quantity=\"one\">seul</item>"
                + "</plurals>"
                + "<plurals name=\"linked\">"
                + "<item quantity=\"one\">@string/one_item</item>"
                + "<item quantity=\"other\">@string/many_items</item>"
                + "</plurals>"
                + "<plurals name=\"relay\">"
                + "<item quantity=\"other\">@plurals/item_count</item>"
                + "</plurals>"
                + "<string name=\"one_item\">un élément</string>"
                + "<string name=\"many_items\">plusieurs éléments</string>"
                + "</resources>");

        // values-fr/strings.xml — traduction complète + many CLDR
        ResourceFileParser frParser = new ResourceFileParser(
                table, ResourceQualifier.forLanguage("fr"));
        frParser.parse("<resources>"
                + "<plurals name=\"item_count\">"
                + "<item quantity=\"one\">%d élément</item>"
                + "<item quantity=\"many\">%d millions d'éléments</item>"
                + "<item quantity=\"other\">%d éléments</item>"
                + "</plurals>"
                + "</resources>");

        resolver = new ResourceResolverImpl(table);
    }

    @Nested
    @DisplayName("Sélection de catégorie CLDR")
    class CategorySelection {

        @Test
        @DisplayName("anglais : 1 → one, 2 → other")
        void shouldSelectOneAndOtherInEnglish() {
            assertThat(resolver.getQuantityString("@plurals/item_count", 1, 1))
                    .isEqualTo("1 item");
            assertThat(resolver.getQuantityString("@plurals/item_count", 2, 2))
                    .isEqualTo("2 items");
        }

        @Test
        @DisplayName("français : 0 et 1 → one, 2 → other (values-fr sélectionné)")
        void shouldFollowFrenchRuleAfterSetLanguage() {
            resolver.setLanguage("fr");
            assertThat(resolver.getQuantityString("@plurals/item_count", 0, 0))
                    .isEqualTo("0 élément");
            assertThat(resolver.getQuantityString("@plurals/item_count", 1, 1))
                    .isEqualTo("1 élément");
            assertThat(resolver.getQuantityString("@plurals/item_count", 2, 2))
                    .isEqualTo("2 éléments");
        }

        @Test
        @DisplayName("français : 2000000 → many (millions)")
        void shouldSelectManyForExactMillionsInFrench() {
            resolver.setLanguage("fr");
            assertThat(resolver.getQuantityString("@plurals/item_count", 2_000_000, 2000000))
                    .isEqualTo("2000000 millions d'éléments");
        }

        @Test
        @DisplayName("le nom nu est accepté en plus de @plurals/")
        void shouldAcceptBareNameReference() {
            assertThat(resolver.getQuantityString("item_count", 1, 1))
                    .isEqualTo("1 item");
        }

        @Test
        @DisplayName("bascule de langue : la sélection suit (cache invalidé)")
        void shouldFollowLanguageSwitch() {
            assertThat(resolver.getQuantityString("@plurals/item_count", 2, 2))
                    .isEqualTo("2 items");
            resolver.setLanguage("fr");
            assertThat(resolver.getQuantityString("@plurals/item_count", 2, 2))
                    .isEqualTo("2 éléments");
            resolver.setLanguage("en");
            assertThat(resolver.getQuantityString("@plurals/item_count", 2, 2))
                    .isEqualTo("2 items");
        }
    }

    @Nested
    @DisplayName("Replis sémantiques")
    class Fallbacks {

        @Test
        @DisplayName("catégorie exacte manquante : repli sur other")
        void shouldFallBackToOtherWhenCategoryMissing() {
            // Russe : 5 → many, absent du plurals défaut → other
            resolver.setLanguage("ru");
            assertThat(resolver.getQuantityString("@plurals/item_count", 5, 5))
                    .isEqualTo("5 items");
        }

        @Test
        @DisplayName("catégorie one manquante : repli sur other")
        void shouldFallBackToOtherWhenOneMissing() {
            // Chinois : 1 → other (pas de one en zh)
            resolver.setLanguage("zh");
            assertThat(resolver.getQuantityString("@plurals/item_count", 1, 1))
                    .isEqualTo("1 items");
        }

        @Test
        @DisplayName("other manquant aussi : null, jamais d'exception")
        void shouldReturnNullWhenOtherAlsoMissing() {
            resolver.setLanguage("ru");
            assertThat(resolver.getQuantityString("@plurals/minimal", 5, 5)).isNull();
            assertThatCode(() -> resolver.getQuantityString("@plurals/minimal", 5, 5))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("référence inconnue : null")
        void shouldReturnNullForUnknownReference() {
            assertThat(resolver.getQuantityString("@plurals/inconnu", 1, 1)).isNull();
        }

        @Test
        @DisplayName("référence null ou vide : null")
        void shouldReturnNullForNullReference() {
            assertThat(resolver.getQuantityString(null, 1)).isNull();
            assertThat(resolver.getQuantityString("", 1)).isNull();
        }

        @Test
        @DisplayName("plurals vide (items sans quantity) : null")
        void shouldReturnNullForEmptyPlurals() {
            ResourceFileParser parser = new ResourceFileParser(table, ResourceQualifier.DEFAULT);
            parser.parse("<resources><plurals name=\"vide\">"
                    + "<item>sans quantité</item>"
                    + "</plurals></resources>");
            assertThat(resolver.getQuantityString("@plurals/vide", 1, 1)).isNull();
        }
    }

    @Nested
    @DisplayName("Items chainables et formatage")
    class ChainingAndFormatting {

        @Test
        @DisplayName("item @string/ : résolu récursivement")
        void shouldResolveStringReferenceItems() {
            assertThat(resolver.getQuantityString("@plurals/linked", 1))
                    .isEqualTo("un élément");
            assertThat(resolver.getQuantityString("@plurals/linked", 2))
                    .isEqualTo("plusieurs éléments");
        }

        @Test
        @DisplayName("item @plurals/ : relais vers un autre pluriel")
        void shouldResolvePluralsRelayItems() {
            assertThat(resolver.getQuantityString("@plurals/relay", 1, 1))
                    .isEqualTo("1 item");
            assertThat(resolver.getQuantityString("@plurals/relay", 2, 2))
                    .isEqualTo("2 items");
        }

        @Test
        @DisplayName("sans arguments de format : pattern laissé intact (comme Android)")
        void shouldKeepPatternWhenNoFormatArgs() {
            assertThat(resolver.getQuantityString("@plurals/item_count", 2))
                    .isEqualTo("%d items");
        }

        @Test
        @DisplayName("pattern %1$s + argument chaîne : formaté")
        void shouldFormatStringPattern() {
            ResourceFileParser parser = new ResourceFileParser(table, ResourceQualifier.DEFAULT);
            parser.parse("<resources><plurals name=\"greeting\">"
                    + "<item quantity=\"one\">Bonjour %1$s</item>"
                    + "<item quantity=\"other\">Bonjour %1$s et %1$s</item>"
                    + "</plurals></resources>");
            assertThat(resolver.getQuantityString("@plurals/greeting", 1, "monde"))
                    .isEqualTo("Bonjour monde");
            assertThat(resolver.getQuantityString("@plurals/greeting", 2, "monde"))
                    .isEqualTo("Bonjour monde et monde");
        }

        @Test
        @DisplayName("pattern invalide : texte brut, jamais d'exception")
        void shouldDegradeToRawTextForInvalidPattern() {
            ResourceFileParser parser = new ResourceFileParser(table, ResourceQualifier.DEFAULT);
            parser.parse("<resources><plurals name=\"poison\">"
                    + "<item quantity=\"other\">100% des %z éléments</item>"
                    + "</plurals></resources>");
            assertThatCode(() ->
                    resolver.getQuantityString("@plurals/poison", 2, 2))
                    .doesNotThrowAnyException();
            assertThat(resolver.getQuantityString("@plurals/poison", 2, 2))
                    .isEqualTo("100% des %z éléments");
        }
    }

    @Nested
    @DisplayName("Introspection getPlurals")
    class Introspection {

        @Test
        @DisplayName("renvoie les items catégorie → texte du qualifier courant")
        void shouldReturnItemsForCurrentQualifier() {
            resolver.setLanguage("fr");
            Map<String, String> items = resolver.getPlurals("@plurals/item_count");
            assertThat(items)
                    .containsEntry("one", "%d élément")
                    .containsEntry("many", "%d millions d'éléments")
                    .containsEntry("other", "%d éléments")
                    .hasSize(3);
        }

        @Test
        @DisplayName("ordre de déclaration préservé")
        void shouldPreserveDeclarationOrder() {
            resolver.setLanguage("fr");
            assertThat(resolver.getPlurals("@plurals/item_count").keySet())
                    .containsExactly("one", "many", "other");
        }

        @Test
        @DisplayName("copie défensive : muter le retour n'affecte pas le resolver")
        void shouldReturnDefensiveCopy() {
            Map<String, String> items = resolver.getPlurals("@plurals/item_count");
            items.put("one", "HACKED");
            items.remove("other");
            assertThat(resolver.getPlurals("@plurals/item_count"))
                    .containsEntry("one", "%d item")
                    .containsEntry("other", "%d items");
        }

        @Test
        @DisplayName("référence inconnue : null")
        void shouldReturnNullForUnknownPlurals() {
            assertThat(resolver.getPlurals("@plurals/inconnu")).isNull();
        }
    }
}
