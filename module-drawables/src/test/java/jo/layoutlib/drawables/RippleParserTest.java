package jo.layoutlib.drawables;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitaires du {@link RippleParser} (ADR 0116 — P0/L3).
 *
 * <p>Valide le parsing des {@code <ripple>} : couleur obligatoire,
 * discrimination contenu/masque ({@code android:id/mask}), capture des
 * enfants inline, et cas limites.</p>
 *
 * @author jo@Dev
 * @since 3.0
 */
@DisplayName("RippleParser — parsing des <ripple>")
class RippleParserTest {

    private static final String NS =
            "xmlns:android=\"http://schemas.android.com/apk/res/android\"";

    private RippleParser parser;

    @BeforeEach
    void setUp() {
        parser = new RippleParser();
    }

    @Nested
    @DisplayName("Couleur du ripple")
    class RippleColor {

        @Test
        @DisplayName("lit la couleur ARGB obligatoire")
        void readsMandatoryColor() {
            RippleConfig config = parser.parse(
                    "<ripple " + NS + " android:color=\"#FF4081\"/>");
            assertThat(config.hasColor()).isTrue();
            assertThat(config.getColor()).isEqualTo(0xFFFF4081);
        }

        @Test
        @DisplayName("une couleur 6 chiffres est complétée en alpha opaque")
        void readsShortColor() {
            RippleConfig config = parser.parse(
                    "<ripple " + NS + " android:color=\"#4081FF\"/>");
            assertThat(config.hasColor()).isTrue();
            assertThat(config.getColor()).isEqualTo(0xFF4081FF);
        }

        @Test
        @DisplayName("sans couleur, la config est invalide (hasColor = false)")
        void missingColorIsInvalid() {
            RippleConfig config = parser.parse("<ripple " + NS + "/>");
            assertThat(config.hasColor()).isFalse();
            assertThat(config.getColor()).isNull();
        }

        @Test
        @DisplayName("une couleur invalide est tolérée (null, pas d'exception)")
        void invalidColorIsTolerated() {
            RippleConfig config = parser.parse(
                    "<ripple " + NS + " android:color=\"pas_une_couleur\"/>");
            assertThat(config.hasColor()).isFalse();
        }
    }

    @Nested
    @DisplayName("Contenu et masque")
    class ContentAndMask {

        @Test
        @DisplayName("un item sans id/mask est le contenu")
        void itemWithoutMaskIdIsContent() {
            RippleConfig config = parser.parse(
                    "<ripple " + NS + " android:color=\"#FF4081\">\n"
                            + "  <item android:drawable=\"@drawable/fond\"/>\n"
                            + "</ripple>");
            assertThat(config.getContent()).isNotNull();
            assertThat(config.getMask()).isNull();
            assertThat(config.getContent().getDrawableRef())
                    .isEqualTo("@drawable/fond");
        }

        @Test
        @DisplayName("un item avec id @android:id/mask est le masque")
        void itemWithMaskIdIsMask() {
            RippleConfig config = parser.parse(
                    "<ripple " + NS + " android:color=\"#FF4081\">\n"
                            + "  <item android:id=\"@android:id/mask\">\n"
                            + "    <color android:color=\"#FFFFFF\"/>\n"
                            + "  </item>\n"
                            + "</ripple>");
            assertThat(config.getMask()).isNotNull();
            assertThat(config.getContent()).isNull();
            assertThat(config.getMask().isMask()).isTrue();
            assertThat(config.getMask().getNestedXml())
                    .startsWith("<color").endsWith("</color>");
        }

        @Test
        @DisplayName("contenu et masque cohabitent, le masque n'écrase pas le contenu")
        void contentAndMaskCoexist() {
            RippleConfig config = parser.parse(
                    "<ripple " + NS + " android:color=\"#FF4081\">\n"
                            + "  <item android:id=\"@android:id/mask\">\n"
                            + "    <color android:color=\"#FFFFFF\"/>\n"
                            + "  </item>\n"
                            + "  <item android:drawable=\"@drawable/fond\"/>\n"
                            + "</ripple>");
            assertThat(config.getMask()).isNotNull();
            assertThat(config.getContent()).isNotNull();
            assertThat(config.getContent().getDrawableRef())
                    .isEqualTo("@drawable/fond");
        }

        @Test
        @DisplayName("seul le premier item non-masque devient le contenu")
        void onlyFirstNonMaskItemIsContent() {
            RippleConfig config = parser.parse(
                    "<ripple " + NS + " android:color=\"#FF4081\">\n"
                            + "  <item android:drawable=\"@drawable/premier\"/>\n"
                            + "  <item android:drawable=\"@drawable/second\"/>\n"
                            + "</ripple>");
            assertThat(config.getContent().getDrawableRef())
                    .isEqualTo("@drawable/premier");
        }

        @Test
        @DisplayName("capture un shape inline comme contenu")
        void capturesInlineShapeContent() {
            RippleConfig config = parser.parse(
                    "<ripple " + NS + " android:color=\"#FF4081\">\n"
                            + "  <item>\n"
                            + "    <shape android:shape=\"rectangle\">\n"
                            + "      <solid android:color=\"#FF6750A4\"/>\n"
                            + "    </shape>\n"
                            + "  </item>\n"
                            + "</ripple>");
            assertThat(config.getContent().getDrawableRef()).isNull();
            assertThat(config.getContent().getNestedXml())
                    .startsWith("<shape").endsWith("</shape>");
            assertThat(config.getContent().hasDrawableSource()).isTrue();
        }
    }

    @Nested
    @DisplayName("Cas limites")
    class EdgeCases {

        @Test
        @DisplayName("XML null lève une DrawableException")
        void nullXmlThrows() {
            assertThatThrownBy(() -> parser.parse(null))
                    .isInstanceOf(DrawableException.class);
        }

        @Test
        @DisplayName("XML vide lève une DrawableException")
        void emptyXmlThrows() {
            assertThatThrownBy(() -> parser.parse(""))
                    .isInstanceOf(DrawableException.class);
        }

        @Test
        @DisplayName("XML corrompu lève une DrawableException")
        void malformedXmlThrows() {
            assertThatThrownBy(() -> parser.parse(
                    "<ripple " + NS + " android:color=\"#FF4081\"><item"))
                    .isInstanceOf(DrawableException.class);
        }
    }
}
