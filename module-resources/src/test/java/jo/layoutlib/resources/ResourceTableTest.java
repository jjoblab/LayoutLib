package jo.layoutlib.resources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires du {@link ResourceTable}.
 *
 * @author jo@Dev
 */
@DisplayName("ResourceTable — stockage des resources")
class ResourceTableTest {

    private ResourceTable table;

    @BeforeEach
    void setUp() {
        table = new ResourceTable();
    }

    @Nested
    @DisplayName("Stockage et récupération")
    class StorageAndRetrieval {

        @Test
        @DisplayName("stocke et récupère une couleur")
        void shouldStoreAndRetrieveColor() {
            table.putColor("red", "#FFFF0000", ResourceQualifier.DEFAULT);
            assertThat(table.getColor("red", ResourceQualifier.DEFAULT))
                    .isEqualTo("#FFFF0000");
        }

        @Test
        @DisplayName("stocke et récupère une chaîne")
        void shouldStoreAndRetrieveString() {
            table.putString("hello", "Hello", ResourceQualifier.DEFAULT);
            assertThat(table.getString("hello", ResourceQualifier.DEFAULT))
                    .isEqualTo("Hello");
        }

        @Test
        @DisplayName("stocke et récupère une dimension")
        void shouldStoreAndRetrieveDimen() {
            table.putDimen("margin", "16dp", ResourceQualifier.DEFAULT);
            assertThat(table.getDimen("margin", ResourceQualifier.DEFAULT))
                    .isEqualTo("16dp");
        }

        @Test
        @DisplayName("stocke et récupère un entier")
        void shouldStoreAndRetrieveInteger() {
            table.putInteger("max", 42, ResourceQualifier.DEFAULT);
            assertThat(table.getInteger("max", ResourceQualifier.DEFAULT))
                    .isEqualTo(42);
        }

        @Test
        @DisplayName("stocke et récupère un booléen")
        void shouldStoreAndRetrieveBoolean() {
            table.putBoolean("enabled", true, ResourceQualifier.DEFAULT);
            assertThat(table.getBoolean("enabled", ResourceQualifier.DEFAULT))
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("Résolution par qualifier")
    class QualifierBasedResolution {

        @Test
        @DisplayName("retourne la valeur par défaut si pas de variantes")
        void shouldReturnDefaultValueIfNoVariants() {
            table.putColor("primary", "#FF6750A4", ResourceQualifier.DEFAULT);
            ResourceQualifier nightTarget = new ResourceQualifier(true, false, 1);
            // Pas de variante nuit → retourne la valeur DEFAULT (compatible)
            assertThat(table.getColor("primary", nightTarget)).isEqualTo("#FF6750A4");
        }

        @Test
        @DisplayName("retourne la variante nuit en mode nuit")
        void shouldReturnNightVariantInNightMode() {
            table.putColor("primary", "#FF6750A4", ResourceQualifier.DEFAULT);
            ResourceQualifier nightQ = new ResourceQualifier(true, false, 1);
            table.putColor("primary", "#FFD0BCFF", nightQ);

            ResourceQualifier nightTarget = new ResourceQualifier(true, false, 1);
            assertThat(table.getColor("primary", nightTarget)).isEqualTo("#FFD0BCFF");
        }

        @Test
        @DisplayName("retourne la variante API 31 si target API >= 31")
        void shouldReturnV31VariantForApi31Target() {
            table.putColor("bg", "#FF000000", ResourceQualifier.DEFAULT);
            table.putColor("bg", "#FFFFFFFF", new ResourceQualifier(false, false, 31));

            ResourceQualifier target31 = new ResourceQualifier(false, false, 31);
            assertThat(table.getColor("bg", target31)).isEqualTo("#FFFFFFFF");

            ResourceQualifier target24 = new ResourceQualifier(false, false, 24);
            assertThat(table.getColor("bg", target24)).isEqualTo("#FF000000");
        }
    }

    @Nested
    @DisplayName("Méthodes utilitaires")
    class UtilityMethods {

        @Test
        @DisplayName("hasColor retourne true pour une couleur existante")
        void shouldReturnTrueForExistingColor() {
            table.putColor("red", "#FFFF0000", ResourceQualifier.DEFAULT);
            assertThat(table.hasColor("red")).isTrue();
            assertThat(table.hasColor("blue")).isFalse();
        }

        @Test
        @DisplayName("colorCount retourne le nombre de couleurs")
        void shouldReturnColorCount() {
            table.putColor("red", "#FFFF0000", ResourceQualifier.DEFAULT);
            table.putColor("blue", "#FF0000FF", ResourceQualifier.DEFAULT);
            assertThat(table.colorCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("clear vide toutes les tables")
        void shouldClearAllTables() {
            table.putColor("red", "#FFFF0000", ResourceQualifier.DEFAULT);
            table.putString("hello", "Hello", ResourceQualifier.DEFAULT);
            table.clear();
            assertThat(table.colorCount()).isEqualTo(0);
            assertThat(table.stringCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Integer-arrays et segments xliff (v3.3)")
    class IntegerArrayAndXliff {

        @Test
        @DisplayName("stocke et récupère un integer-array")
        void shouldStoreAndRetrieveIntegerArray() {
            table.putIntegerArray("ratios", java.util.Arrays.asList(2, 3, 5),
                    ResourceQualifier.DEFAULT);
            assertThat(table.getIntegerArray("ratios", ResourceQualifier.DEFAULT))
                    .containsExactly(2, 3, 5);
        }

        @Test
        @DisplayName("integer-array : repli de qualifier (fr ciblé, défaut sinon)")
        void shouldFallbackIntegerArrayByQualifier() {
            table.putIntegerArray("ratios", java.util.Arrays.asList(2, 3),
                    ResourceQualifier.DEFAULT);
            table.putIntegerArray("ratios", java.util.Arrays.asList(4, 9),
                    ResourceQualifier.forLanguage("fr"));
            assertThat(table.getIntegerArray("ratios", ResourceQualifier.forLanguage("fr")))
                    .containsExactly(4, 9);
            assertThat(table.getIntegerArray("ratios", ResourceQualifier.DEFAULT))
                    .containsExactly(2, 3);
        }

        @Test
        @DisplayName("integer-array null stocké comme liste vide, copie défensive")
        void shouldDefensivelyCopyIntegerArrays() {
            java.util.List<Integer> values = new java.util.ArrayList<>();
            values.add(1);
            table.putIntegerArray("nums", values, ResourceQualifier.DEFAULT);
            values.set(0, 999);
            assertThat(table.getIntegerArray("nums", ResourceQualifier.DEFAULT))
                    .containsExactly(1);
            java.util.List<Integer> got = table.getIntegerArray("nums", ResourceQualifier.DEFAULT);
            got.set(0, 777);
            assertThat(table.getIntegerArray("nums", ResourceQualifier.DEFAULT))
                    .containsExactly(1);
            table.putIntegerArray("nul", null, ResourceQualifier.DEFAULT);
            assertThat(table.getIntegerArray("nul", ResourceQualifier.DEFAULT)).isEmpty();
        }

        @Test
        @DisplayName("hasIntegerArray / integerArrayCount")
        void shouldSupportIntegerArrayIntrospection() {
            table.putIntegerArray("nums", java.util.Arrays.asList(1),
                    ResourceQualifier.DEFAULT);
            assertThat(table.hasIntegerArray("nums")).isTrue();
            assertThat(table.hasIntegerArray("other")).isFalse();
            assertThat(table.integerArrayCount()).isEqualTo(1);
            assertThat(table.hasStringArray("nums")).isFalse();
        }

        @Test
        @DisplayName("stocke et récupère les spans xliff d'une chaîne (repli qualifier)")
        void shouldStoreAndRetrieveStringXliffSpans() {
            table.putStringXliffSpans("songs",
                    java.util.Collections.singletonList(
                            new XliffSpan("artist", "Adele", "%2$s")),
                    ResourceQualifier.DEFAULT);
            table.putStringXliffSpans("songs",
                    java.util.Collections.singletonList(
                            new XliffSpan("artist", "Adèle", "%2$s")),
                    ResourceQualifier.forLanguage("fr"));
            assertThat(table.getStringXliffSpans("songs",
                    ResourceQualifier.forLanguage("fr")).get(0).getExample())
                    .isEqualTo("Adèle");
            assertThat(table.getStringXliffSpans("songs",
                    ResourceQualifier.DEFAULT).get(0).getExample())
                    .isEqualTo("Adele");
            assertThat(table.getStringXliffSpans("inconnu", ResourceQualifier.DEFAULT))
                    .isNull();
        }

        @Test
        @DisplayName("spans xliff : copie défensive des listes retournées")
        void shouldDefensivelyCopyXliffSpans() {
            java.util.List<XliffSpan> spans = new java.util.ArrayList<>();
            spans.add(new XliffSpan("a", "1", "%d"));
            table.putStringXliffSpans("s", spans, ResourceQualifier.DEFAULT);
            spans.set(0, new XliffSpan("x", "x", "x"));
            assertThat(table.getStringXliffSpans("s", ResourceQualifier.DEFAULT)
                    .get(0).getId()).isEqualTo("a");
            java.util.List<XliffSpan> got =
                    table.getStringXliffSpans("s", ResourceQualifier.DEFAULT);
            got.clear();
            assertThat(table.getStringXliffSpans("s", ResourceQualifier.DEFAULT))
                    .hasSize(1);
        }

        @Test
        @DisplayName("stocke et récupère les spans xliff d'un pluriel (copie profonde)")
        void shouldStoreAndRetrievePluralsXliffSpans() {
            java.util.Map<String, java.util.List<XliffSpan>> byCategory =
                    new java.util.LinkedHashMap<>();
            byCategory.put("one", java.util.Collections.singletonList(
                    new XliffSpan("count", "1", "%d")));
            byCategory.put("other", java.util.Collections.singletonList(
                    new XliffSpan("count", "7", "%d")));
            table.putPluralsXliffSpans("items", byCategory, ResourceQualifier.DEFAULT);
            java.util.Map<String, java.util.List<XliffSpan>> got =
                    table.getPluralsXliffSpans("items", ResourceQualifier.DEFAULT);
            assertThat(got).containsOnlyKeys("one", "other");
            got.get("one").clear();
            got.put("many", java.util.Collections.emptyList());
            assertThat(table.getPluralsXliffSpans("items", ResourceQualifier.DEFAULT))
                    .containsOnlyKeys("one", "other");
            assertThat(table.hasPluralsXliffSpans("items")).isTrue();
            assertThat(table.pluralsXliffCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("clear vide aussi les integer-arrays et les spans xliff")
        void shouldClearNewTables() {
            table.putIntegerArray("nums", java.util.Arrays.asList(1),
                    ResourceQualifier.DEFAULT);
            table.putStringXliffSpans("s", java.util.Collections.singletonList(
                    new XliffSpan("a", "1", "%d")), ResourceQualifier.DEFAULT);
            table.putPluralsXliffSpans("p",
                    java.util.Collections.singletonMap("other",
                            java.util.Collections.singletonList(
                                    new XliffSpan("a", "1", "%d"))),
                    ResourceQualifier.DEFAULT);
            table.clear();
            assertThat(table.integerArrayCount()).isEqualTo(0);
            assertThat(table.stringXliffCount()).isEqualTo(0);
            assertThat(table.pluralsXliffCount()).isEqualTo(0);
            assertThat(table.hasIntegerArray("nums")).isFalse();
            assertThat(table.hasStringXliffSpans("s")).isFalse();
            assertThat(table.hasPluralsXliffSpans("p")).isFalse();
        }
    }
}
