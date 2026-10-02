package jo.layoutlib.editor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires JVM du {@link XmlMutator} (point 9 : l'app n'avait
 * aucun test). Les mutations sont purement textuelles et ne requièrent
 * ni Android ni XmlPullParser, donc exécutables sur la JVM.
 *
 * @author jo@Dev
 * @since 3.0
 */
@DisplayName("XmlMutator — mutation d'attributs par id")
class XmlMutatorTest {

    private static final String XML =
            "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    android:layout_width=\"match_parent\"\n"
            + "    android:layout_height=\"wrap_content\">\n"
            + "    <TextView\n"
            + "        android:id=\"@+id/label\"\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:text=\"Bonjour\" />\n"
            + "    <Button\n"
            + "        android:id=\"@id/submit\"\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\" />\n"
            + "</LinearLayout>";

    @Nested
    @DisplayName("setAttributeById")
    class SetAttribute {

        @Test
        @DisplayName("remplace la valeur d'un attribut existant (@+id/)")
        void replacesExistingAttributeValue() {
            String out = XmlMutator.setAttributeById(
                    XML, "label", "text", "Salut");

            assertThat(out).contains("android:text=\"Salut\"");
            assertThat(out).doesNotContain("android:text=\"Bonjour\"");
            // Le reste du document doit rester intact.
            assertThat(out).contains("android:id=\"@+id/label\"");
            assertThat(out).contains("</LinearLayout>");
        }

        @Test
        @DisplayName("insère l'attribut s'il est absent du tag")
        void insertsMissingAttribute() {
            String out = XmlMutator.setAttributeById(
                    XML, "submit", "android:text", "Valider");

            assertThat(out).contains("android:text=\"Valider\"");
            assertThat(out).contains("android:id=\"@id/submit\"");
            // L'insertion préserve la fermeture auto-fermante « /> ».
            assertThat(out).contains("android:text=\"Valider\" />");
        }

        @Test
        @DisplayName("préfixe android: est ajouté si omis")
        void prefixesAndroidNamespace() {
            String out = XmlMutator.setAttributeById(
                    XML, "label", "textSize", "18sp");

            assertThat(out).contains("android:textSize=\"18sp\"");
        }

        @Test
        @DisplayName("l'id @id/ (sans +) est aussi reconnu")
        void recognizesIdWithoutPlus() {
            String out = XmlMutator.setAttributeById(
                    XML, "submit", "text", "OK");

            assertThat(out).contains("android:text=\"OK\"");
        }

        @Test
        @DisplayName("échappe les caractères XML de la valeur")
        void escapesXmlCharacters() {
            String out = XmlMutator.setAttributeById(
                    XML, "label", "text", "<a & \"b\">");

            assertThat(out).contains("android:text=\"&lt;a &amp; &quot;b&quot;&gt;\"");
        }

        @Test
        @DisplayName("id inconnu : XML retourné à l'identique")
        void unknownIdReturnsXmlUnchanged() {
            String out = XmlMutator.setAttributeById(
                    XML, "inexistant", "text", "X");

            assertThat(out).isEqualTo(XML);
        }

        @Test
        @DisplayName("arguments null : XML retourné à l'identique")
        void nullArgumentsReturnXmlUnchanged() {
            assertThat(XmlMutator.setAttributeById(null, "label", "text", "X"))
                    .isNull();
            assertThat(XmlMutator.setAttributeById(XML, null, "text", "X"))
                    .isEqualTo(XML);
            assertThat(XmlMutator.setAttributeById(XML, "label", null, "X"))
                    .isEqualTo(XML);
        }

        @Test
        @DisplayName("la valeur peut contenir des guillemets simples")
        void handlesSingleQuotesInsideValue() {
            String out = XmlMutator.setAttributeById(
                    XML, "label", "text", "L'été");

            assertThat(out).contains("android:text=\"L&apos;été\"");
        }
    }

    @Nested
    @DisplayName("getAttributeById")
    class GetAttribute {

        @Test
        @DisplayName("lit la valeur d'un attribut existant")
        void readsExistingAttributeValue() {
            String value = XmlMutator.getAttributeById(XML, "label", "text");

            assertThat(value).isEqualTo("Bonjour");
        }

        @Test
        @DisplayName("retourne null si l'attribut est absent")
        void returnsNullWhenAttributeMissing() {
            String value = XmlMutator.getAttributeById(XML, "submit", "text");

            assertThat(value).isNull();
        }

        @Test
        @DisplayName("retourne null si l'id est inconnu")
        void returnsNullWhenIdUnknown() {
            String value = XmlMutator.getAttributeById(XML, "inexistant", "text");

            assertThat(value).isNull();
        }

        @Test
        @DisplayName("retourne null sur arguments null")
        void returnsNullOnNullArguments() {
            assertThat(XmlMutator.getAttributeById(null, "label", "text")).isNull();
            assertThat(XmlMutator.getAttributeById(XML, null, "text")).isNull();
            assertThat(XmlMutator.getAttributeById(XML, "label", null)).isNull();
        }

        @Test
        @DisplayName("ne lit que l'attribut du tag porteur de l'id")
        void readsOnlyTheTagOwningTheId() {
            // « label » précède « submit » : la largeur de submit ne doit
            // pas être lue pour label.
            String labelWidth = XmlMutator.getAttributeById(
                    XML, "label", "layout_width");

            assertThat(labelWidth).isEqualTo("wrap_content");
        }
    }

    @Nested
    @DisplayName("findTagOffsetForId")
    class FindOffset {

        @Test
        @DisplayName("pointe sur le '<' du tag porteur de l'id")
        void pointsAtOpeningAngleBracket() {
            int offset = XmlMutator.findTagOffsetForId(XML, "label");

            assertThat(offset).isGreaterThanOrEqualTo(0);
            assertThat(XML.charAt(offset)).isEqualTo('<');
            // Le tag ouvert à cet offset doit contenir l'id recherché.
            assertThat(XML.substring(offset, XML.indexOf('>', offset) + 1))
                    .contains("android:id=\"@+id/label\"");
        }

        @Test
        @DisplayName("retourne -1 pour un id inconnu ou null")
        void returnsMinusOneForUnknownOrNull() {
            assertThat(XmlMutator.findTagOffsetForId(XML, "inexistant")).isEqualTo(-1);
            assertThat(XmlMutator.findTagOffsetForId(null, "label")).isEqualTo(-1);
            assertThat(XmlMutator.findTagOffsetForId(XML, null)).isEqualTo(-1);
        }

        @Test
        @DisplayName("reconnaît aussi la forme @id/ sans +")
        void recognizesIdWithoutPlusForm() {
            int offset = XmlMutator.findTagOffsetForId(XML, "submit");

            assertThat(offset).isGreaterThanOrEqualTo(0);
            assertThat(XML.charAt(offset)).isEqualTo('<');
            assertThat(XML.substring(offset, XML.indexOf('>', offset) + 1))
                    .contains("android:id=\"@id/submit\"");
        }
    }
}
