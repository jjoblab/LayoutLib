package jo.layoutlib.inflater;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de {@link RenderService#isXmlWellFormed(String)}.
 *
 * <p>Régression du bug historique : des heuristiques fragiles (comptage des
 * {@code "} sur tout le texte, test {@code contains("=")}) rejetaient à tort
 * du XML valide — par exemple
 * {@code <TextView android:text='Dis "bonjour' />} (guillemet dans une valeur
 * entre apostrophes, nombre impair de {@code "}).</p>
 *
 * <p>La validation s'appuie exclusivement sur {@code XmlPullParser}. Note :
 * le parseur (kxml2, aussi bien en JVM qu'on-device) exige que les préfixes
 * soient déclarés ({@code xmlns:android}) et se montre tolérant sur du texte
 * placé avant la racine — les cas de test reflètent ce comportement réel.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
@DisplayName("RenderService — validation XML (isXmlWellFormed)")
class RenderServiceXmlValidationTest {

    /** xmlns:android, comme dans tout layout réel édité par l'app. */
    private static final String NS =
            " xmlns:android=\"http://schemas.android.com/apk/res/android\"";

    @Nested
    @DisplayName("XML valide (y compris cas rejetés à tort avant le correctif)")
    class ValidXml {

        @Test
        @DisplayName("guillemet dans un attribut entre apostrophes (l'exemple du bug)")
        void acceptsDoubleQuoteInsideSingleQuotedAttribute() {
            assertThat(RenderService.isXmlWellFormed(
                    "<TextView" + NS + " android:text='Dis \"bonjour' />")).isTrue();
        }

        @Test
        @DisplayName("guillemet dans un commentaire (nombre impair)")
        void acceptsOddDoubleQuotesInComment() {
            assertThat(RenderService.isXmlWellFormed(
                    "<LinearLayout><!-- elle a dit \"bonjour --><TextView/>"
                            + "</LinearLayout>")).isTrue();
        }

        @Test
        @DisplayName("plusieurs guillemets dans un commentaire")
        void acceptsDoubleQuotesInComment() {
            assertThat(RenderService.isXmlWellFormed(
                    "<Root><!-- a \"b\" c \"d --><Item/></Root>")).isTrue();
        }

        @Test
        @DisplayName("document avec prolog XML et espaces de nom")
        void acceptsNamespacedDocument() {
            assertThat(RenderService.isXmlWellFormed(
                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<LinearLayout" + NS + " android:orientation=\"vertical\">\n"
                            + "  <TextView android:text=\"Hello\"/>\n"
                            + "</LinearLayout>")).isTrue();
        }

        @Test
        @DisplayName("valeur d'attribut contenant une entité échappée")
        void acceptsEscapedEntityInAttribute() {
            assertThat(RenderService.isXmlWellFormed(
                    "<TextView" + NS + " android:text=\"a &lt; b &amp;&amp; c\"/>"))
                    .isTrue();
        }

        @Test
        @DisplayName("préfixe non déclaré → rejet propre (pas d'exception)")
        void rejectsUndefinedPrefixWithoutThrowing() {
            // kxml2 lève RuntimeException("Undefined Prefix") : le validateur
            // doit retourner false sans jamais lever.
            assertThat(RenderService.isXmlWellFormed(
                    "<TextView android:text='Dis \"bonjour' />")).isFalse();
        }
    }

    @Nested
    @DisplayName("XML réellement malformé")
    class MalformedXml {

        @ParameterizedTest
        @ValueSource(strings = {
                // balise intérieure non fermée
                "<LinearLayout><TextView></LinearLayout>",
                // racine non fermée
                "<LinearLayout><TextView/></LinearLayout",
                // attribut sans valeur
                "<TextView android:text= />",
                // attribut sans guillemets
                "<TextView android:text=hello />",
                // valeur non fermée (en cours de frappe)
                "<TextView android:text=\"hello />",
                // chevron ouvrant en trop
                "<<TextView/>",
                // tag fermant sans ouvrant
                "</LinearLayout>"
        })
        @DisplayName("rejette : {0}")
        void rejectsMalformedXml(String xml) {
            assertThat(RenderService.isXmlWellFormed(xml)).isFalse();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t\n"})
        @DisplayName("rejette null / vide / whitespace")
        void rejectsNullOrEmpty(String xml) {
            assertThat(RenderService.isXmlWellFormed(xml)).isFalse();
        }
    }
}
