package jo.layoutlib.resources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests du support {@code <string-array>} / {@code @array/} (module
 * Resources) : table, résolution chainable et parsing de fichiers
 * {@code res/values/}.
 *
 * @author jo@Dev
 * @since 1.0
 */
@DisplayName("String-arrays @array/ — table, resolver et parser")
class ResourceStringArrayTest {

    private ResourceTable table;
    private ResourceResolverImpl resolver;

    @BeforeEach
    void setUp() {
        table = new ResourceTable();
        table.putString("planet_mars", "Mars", ResourceQualifier.DEFAULT);
        table.putStringArray("planets", Arrays.asList(
                "Mercure", "Vénus", "Terre", "@string/planet_mars"),
                ResourceQualifier.DEFAULT);
        resolver = new ResourceResolverImpl(table);
    }

    @Test
    @DisplayName("putStringArray/getStringArray stocke et restitue une copie")
    void tableStoresAndReturnsArray() {
        List<String> stored = table.getStringArray("planets", ResourceQualifier.DEFAULT);

        assertThat(stored).containsExactly("Mercure", "Vénus", "Terre", "@string/planet_mars");
        // copie défensive : modifier le retour ne corrompt pas la table
        stored.set(0, "MODIFIÉ");
        assertThat(table.getStringArray("planets", ResourceQualifier.DEFAULT).get(0))
                .isEqualTo("Mercure");
        assertThat(table.hasStringArray("planets")).isTrue();
        assertThat(table.stringArrayCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("getStringArray retourne null pour un nom inconnu")
    void tableReturnsNullForUnknown() {
        assertThat(table.getStringArray("inconnu", ResourceQualifier.DEFAULT)).isNull();
        assertThat(table.hasStringArray("inconnu")).isFalse();
    }

    @Test
    @DisplayName("resolver résout @array/ et les @string/ contenus")
    void resolverResolvesArrayAndInnerStrings() {
        List<String> values = resolver.getStringArray("@array/planets");

        assertThat(values).containsExactly("Mercure", "Vénus", "Terre", "Mars");
    }

    @Test
    @DisplayName("resolver retourne null pour une référence non-@array/")
    void resolverRejectsNonArrayReference() {
        assertThat(resolver.getStringArray("@string/planet_mars")).isNull();
        assertThat(resolver.getStringArray(null)).isNull();
        assertThat(resolver.getStringArray("planets")).isNull();
    }

    @Test
    @DisplayName("ResourceFileParser parse un <string-array> complet")
    void parserParsesStringArray() {
        ResourceTable parsed = new ResourceTable();
        ResourceFileParser parser = new ResourceFileParser(parsed, ResourceQualifier.DEFAULT);

        parser.parse(
                "<resources>\n"
                        + "  <string name=\"app\">Layout Editor</string>\n"
                        + "  <string-array name=\"couleurs\">\n"
                        + "    <item>Rouge</item>\n"
                        + "    <item>Vert clair</item>\n"
                        + "    <item>@string/app</item>\n"
                        + "  </string-array>\n"
                        + "</resources>");

        assertThat(parsed.hasStringArray("couleurs")).isTrue();
        assertThat(parsed.stringCount()).isEqualTo(1);

        ResourceResolverImpl parsedResolver = new ResourceResolverImpl(parsed);
        assertThat(parsedResolver.getStringArray("@array/couleurs"))
                .containsExactly("Rouge", "Vert clair", "Layout Editor");
    }

    @Test
    @DisplayName("le cache renvoie des copies indépendantes")
    void cacheReturnsIndependentCopies() {
        List<String> first = resolver.getStringArray("@array/planets");
        List<String> second = resolver.getStringArray("@array/planets");

        assertThat(first).isNotSameAs(second);
        assertThat(first).isEqualTo(second);
    }
}
