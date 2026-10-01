package jo.layoutlib.inflater;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitaires du {@link XmlPreprocessor}.
 *
 * <p>Ces tests valident les transformations lexicales appliquées avant le
 * parsing : suppression de {@code xmlns:tools}, conversion des attributs
 * {@code tools:*}, conversion RTL ({@code *Start} → {@code *Left},
 * {@code *End} → {@code *Right}).</p>
 *
 * @author jo@Dev
 */
@DisplayName("XmlPreprocessor — normalisation du XML avant parsing")
class XmlPreprocessorTest {

    @Nested
    @DisplayName("Suppression de xmlns:tools")
    class StripXmlnsTools {

        @Test
        @DisplayName("supprime xmlns:tools quand présent")
        void shouldStripXmlnsToolsWhenPresent() {
            String xml = "<LinearLayout xmlns:tools=\"http://schemas.android.com/tools\"/>";
            String result = XmlPreprocessor.preprocess(xml);
            assertThat(result).doesNotContain("xmlns:tools");
        }

        @Test
        @DisplayName("ne modifie pas un XML sans xmlns:tools")
        void shouldNotModifyWhenNoXmlnsTools() {
            String xml = "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"/>";
            String result = XmlPreprocessor.preprocess(xml);
            assertThat(result).isEqualTo(xml);
        }
    }

    @Nested
    @DisplayName("Conversion des attributs tools:*")
    class ConvertToolsAttributes {

        @Test
        @DisplayName("convertit tools:text → android:text")
        void shouldConvertToolsText() {
            String xml = "<TextView tools:text=\"Hello World\"/>";
            String result = XmlPreprocessor.preprocess(xml);
            assertThat(result).contains("android:text=\"Hello World\"");
            assertThat(result).doesNotContain("tools:text");
        }

        @Test
        @DisplayName("convertit tools:visibility → android:visibility")
        void shouldConvertToolsVisibility() {
            String xml = "<View tools:visibility=\"gone\"/>";
            String result = XmlPreprocessor.preprocess(xml);
            assertThat(result).contains("android:visibility=\"gone\"");
        }

        @Test
        @DisplayName("convertit tools:src → android:src")
        void shouldConvertToolsSrc() {
            String xml = "<ImageView tools:src=\"@drawable/icon\"/>";
            String result = XmlPreprocessor.preprocess(xml);
            assertThat(result).contains("android:src=\"@drawable/icon\"");
        }

        @Test
        @DisplayName("ne convertit pas les attributs tools non listés (tools:context par exemple)")
        void shouldNotConvertUnknownToolsAttrs() {
            String xml = "<LinearLayout tools:context=\".MainActivity\"/>";
            String result = XmlPreprocessor.preprocess(xml);
            assertThat(result).contains("tools:context");
        }
    }

    @Nested
    @DisplayName("Conversion RTL → LTR")
    class ConvertRtl {

        @Test
        @DisplayName("convertit layout_marginStart → layout_marginLeft")
        void shouldConvertMarginStart() {
            String xml = "<View layout_marginStart=\"16dp\"/>";
            String result = XmlPreprocessor.preprocess(xml);
            assertThat(result).contains("layout_marginLeft=\"16dp\"");
            assertThat(result).doesNotContain("layout_marginStart");
        }

        @Test
        @DisplayName("convertit layout_marginEnd → layout_marginRight")
        void shouldConvertMarginEnd() {
            String xml = "<View layout_marginEnd=\"8dp\"/>";
            String result = XmlPreprocessor.preprocess(xml);
            assertThat(result).contains("layout_marginRight=\"8dp\"");
            assertThat(result).doesNotContain("layout_marginEnd");
        }

        @Test
        @DisplayName("convertit paddingStart → paddingLeft")
        void shouldConvertPaddingStart() {
            String xml = "<View paddingStart=\"4dp\"/>";
            String result = XmlPreprocessor.preprocess(xml);
            assertThat(result).contains("paddingLeft=\"4dp\"");
        }

        @Test
        @DisplayName("convertit paddingEnd → paddingRight")
        void shouldConvertPaddingEnd() {
            String xml = "<View paddingEnd=\"4dp\"/>";
            String result = XmlPreprocessor.preprocess(xml);
            assertThat(result).contains("paddingRight=\"4dp\"");
        }
    }

    @Nested
    @DisplayName("Validation XML bien formé")
    class ValidateWellFormed {

        @Test
        @DisplayName("accepte un XML bien formé")
        void shouldAcceptWellFormedXml() {
            String xml = "<TextView xmlns:android=\"http://schemas.android.com/apk/res/android\" android:text=\"Hello\"/>";
            XmlPreprocessor.validateWellFormed(xml);
            // Pas d'exception attendue
        }

        @Test
        @DisplayName("rejette un XML malformé")
        void shouldRejectMalformedXml() {
            String xml = "<TextView><TextView";  // tag mal fermé
            assertThatThrownBy(() -> XmlPreprocessor.validateWellFormed(xml))
                    .isInstanceOf(InflateException.class)
                    .hasMessageContaining("XML malformé");
        }
    }

    @Nested
    @DisplayName("Cas limites")
    class EdgeCases {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("lève IllegalArgumentException pour null, vide ou whitespace")
        void shouldRejectNullOrEmpty(String input) {
            assertThatThrownBy(() -> XmlPreprocessor.preprocess(input))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("préserve les attributs non concernés")
        void shouldPreserveOtherAttributes() {
            String xml = "<TextView xmlns:android=\"http://schemas.android.com/apk/res/android\" android:text=\"Hello\" android:textSize=\"16sp\"/>";
            String result = XmlPreprocessor.preprocess(xml);
            assertThat(result).contains("android:text=\"Hello\"");
            assertThat(result).contains("android:textSize=\"16sp\"");
        }
    }

    @ParameterizedTest
    @CsvSource({
            "tools:text, android:text",
            "tools:visibility, android:visibility",
            "tools:layout_width, android:layout_width",
            "tools:layout_height, android:layout_height"
    })
    @DisplayName("conversion paramétrée tools → android")
    void parameterizedToolsConversion(String toolsAttr, String androidAttr) {
        String xml = "<View " + toolsAttr + "=\"value\"/>";
        String result = XmlPreprocessor.preprocess(xml);
        assertThat(result).contains(androidAttr + "=\"value\"");
    }
}
