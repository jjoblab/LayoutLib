package jo.layoutlib.resources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de résolution des integer-arrays {@code <integer-array>} (v3.3).
 *
 * <p>Valide le pipeline complet : parsing {@code @array/} typé entier →
 * sélection par qualifier avec repli ({@code values-fr/} bat
 * {@code values/}), indépendance face aux {@code <string-array>} du même
 * espace de noms, neutralisation des items malformés en {@code 0} (longueur
 * préservée), copie défensive des listes retournées.</p>
 *
 * @author jo@Dev
 * @since 3.3
 */
@DisplayName("ResourceResolverImpl — integer-arrays <integer-array>")
class ResourceResolverImplIntegerArrayTest {

    private ResourceTable table;
    private ResourceResolverImpl resolver;

    @BeforeEach
    void setUp() {
        table = new ResourceTable();

        // values/arrays.xml — défaut
        ResourceFileParser defParser = new ResourceFileParser(table, ResourceQualifier.DEFAULT);
        defParser.parse("<resources>"
                + "<integer-array name=\"ratios\">"
                + "<item>2</item><item>3</item><item>5</item>"
                + "</integer-array>"
                + "<integer-array name=\"durations\">"
                + "<item>150</item><item>300</item>"
                + "</integer-array>"
                + "<integer-array name=\"poison\">"
                + "<item>1</item><item>abc</item><item></item><item>-7</item>"
                + "</integer-array>"
                + "<integer-array name=\"empty\"></integer-array>"
                + "<string-array name=\"labels\">"
                + "<item>un</item><item>deux</item>"
                + "</string-array>"
                + "</resources>");

        // values-fr/arrays.xml — traduction partielle (ratios seulement)
        ResourceFileParser frParser = new ResourceFileParser(
                table, ResourceQualifier.forLanguage("fr"));
        frParser.parse("<resources>"
                + "<integer-array name=\"ratios\">"
                + "<item>4</item><item>9</item>"
                + "</integer-array>"
                + "</resources>");

        resolver = new ResourceResolverImpl(table);
    }

    @Test
    @DisplayName("@array/ratios résout la liste d'entiers du défaut")
    void shouldResolveIntegerArray() {
        List<Integer> ratios = resolver.getIntegerArray("@array/ratios");
        assertThat(ratios).containsExactly(2, 3, 5);
    }

    @Test
    @DisplayName("le nom nu est accepté en plus de @array/")
    void shouldAcceptBareName() {
        assertThat(resolver.getIntegerArray("durations")).containsExactly(150, 300);
    }

    @Test
    @DisplayName("values-fr l'emporte après setLanguage, avec repli sur values/")
    void shouldFollowQualifierWithFallback() {
        resolver.setLanguage("fr");
        assertThat(resolver.getIntegerArray("@array/ratios")).containsExactly(4, 9);
        // traduction partielle : durations retombe sur values/
        assertThat(resolver.getIntegerArray("@array/durations")).containsExactly(150, 300);
        // et retour à la défaut après bascule (cache invalidé)
        resolver.setLanguage("en");
        assertThat(resolver.getIntegerArray("@array/ratios")).containsExactly(2, 3, 5);
    }

    @Test
    @DisplayName("items malformés neutralisés en 0, longueur préservée")
    void shouldNeutralizeMalformedItems() {
        assertThat(resolver.getIntegerArray("@array/poison"))
                .containsExactly(1, 0, 0, -7);
    }

    @Test
    @DisplayName("array vide → liste vide (pas null)")
    void shouldReturnEmptyListForEmptyArray() {
        assertThat(resolver.getIntegerArray("@array/empty")).isEmpty();
    }

    @Test
    @DisplayName("référence inconnue ou null → null, jamais d'exception")
    void shouldReturnNullForUnknownOrNull() {
        assertThat(resolver.getIntegerArray("@array/inconnu")).isNull();
        assertThat(resolver.getIntegerArray(null)).isNull();
        assertThat(resolver.getIntegerArray("")).isNull();
    }

    @Test
    @DisplayName("un <string-array> du même espace de noms → null côté typé")
    void shouldNotSeeStringArrays() {
        assertThat(resolver.getIntegerArray("@array/labels")).isNull();
        // et réciproquement : getStringArray ne voit pas les integer-arrays
        assertThat(resolver.getStringArray("@array/ratios")).isNull();
    }

    @Test
    @DisplayName("copie défensive : muter la liste retournée n'altère pas le resolver")
    void shouldReturnDefensiveCopies() {
        List<Integer> first = resolver.getIntegerArray("@array/ratios");
        first.set(0, 999);
        first.remove(2);
        List<Integer> second = resolver.getIntegerArray("@array/ratios");
        assertThat(second).containsExactly(2, 3, 5);
    }

    @Test
    @DisplayName("référence d'un autre type (@string/) → null")
    void shouldRejectForeignReference() {
        assertThat(resolver.getIntegerArray("@string/ratios")).isNull();
    }

    @Test
    @DisplayName("introspection : la table expose has/count")
    void shouldSupportIntrospection() {
        assertThat(table.hasIntegerArray("ratios")).isTrue();
        assertThat(table.hasIntegerArray("inconnu")).isFalse();
        assertThat(table.integerArrayCount()).isEqualTo(4);
        assertThat(table.getIntegerArray("ratios", ResourceQualifier.DEFAULT))
                .containsExactly(2, 3, 5);
    }
}
