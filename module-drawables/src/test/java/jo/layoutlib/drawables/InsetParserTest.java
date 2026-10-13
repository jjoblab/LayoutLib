package jo.layoutlib.drawables;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import jo.layoutlib.resources.DimensionConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitaires du {@link InsetParser} (ADR 0116 — P0/L3).
 *
 * <p>Valide le parsing des {@code <inset>} : les quatre insets
 * ({@code insetLeft/insetTop/insetRight/insetBottom}) convertis en pixels,
 * le drawable encapsulé (référence ou enfant inline), et cas limites.</p>
 *
 * @author jo@Dev
 * @since 3.0
 */
@DisplayName("InsetParser — parsing des <inset>")
class InsetParserTest {

    private static final String NS =
            "xmlns:android=\"http://schemas.android.com/apk/res/android\"";

    private InsetParser parser;

    @BeforeEach
    void setUp() {
        // densité 2x : 8dp → 16px
        parser = new InsetParser(new DimensionConverter(2f, 2f, 320f));
    }

    @Nested
    @DisplayName("Insets")
    class Insets {

        @Test
        @DisplayName("convertit les quatre insets dp en pixels")
        void convertsAllFourInsets() {
            InsetConfig config = parser.parse(
                    "<inset " + NS + " android:drawable=\"@drawable/fond\"\n"
                            + "       android:insetLeft=\"8dp\""
                            + " android:insetTop=\"4dp\"\n"
                            + "       android:insetRight=\"8dp\""
                            + " android:insetBottom=\"4dp\"/>");
            assertThat(config.getInsetLeft()).isEqualTo(16f);
            assertThat(config.getInsetTop()).isEqualTo(8f);
            assertThat(config.getInsetRight()).isEqualTo(16f);
            assertThat(config.getInsetBottom()).isEqualTo(8f);
            assertThat(config.hasAnyInset()).isTrue();
        }

        @Test
        @DisplayName("laisse les insets absents à null")
        void missingInsetsStayNull() {
            InsetConfig config = parser.parse(
                    "<inset " + NS + " android:drawable=\"@drawable/fond\"/>");
            assertThat(config.getInsetLeft()).isNull();
            assertThat(config.getInsetTop()).isNull();
            assertThat(config.getInsetRight()).isNull();
            assertThat(config.getInsetBottom()).isNull();
            assertThat(config.hasAnyInset()).isFalse();
        }

        @Test
        @DisplayName("parse un inset isolé sans convertir les autres")
        void parsesSingleInset() {
            InsetConfig config = parser.parse(
                    "<inset " + NS + " android:drawable=\"@drawable/fond\"\n"
                            + "       android:insetLeft=\"10dp\"/>");
            assertThat(config.getInsetLeft()).isEqualTo(20f);
            assertThat(config.getInsetTop()).isNull();
        }

        @Test
        @DisplayName("sans convertisseur, la valeur numérique reste brute")
        void noConverterKeepsRawValue() {
            InsetParser rawParser = new InsetParser(null);
            InsetConfig config = rawParser.parse(
                    "<inset " + NS + " android:drawable=\"@drawable/fond\"\n"
                            + "       android:insetLeft=\"10dp\"/>");
            assertThat(config.getInsetLeft()).isEqualTo(10f);
        }
    }

    @Nested
    @DisplayName("Drawable encapsulé")
    class EncapsulatedDrawable {

        @Test
        @DisplayName("lit la référence android:drawable")
        void readsDrawableReference() {
            InsetConfig config = parser.parse(
                    "<inset " + NS + " android:drawable=\"@color/fond\"/>");
            assertThat(config.getDrawableRef()).isEqualTo("@color/fond");
            assertThat(config.getNestedXml()).isNull();
            assertThat(config.hasDrawableSource()).isTrue();
        }

        @Test
        @DisplayName("capture un enfant inline (shape)")
        void capturesInlineChild() {
            InsetConfig config = parser.parse(
                    "<inset " + NS + ">\n"
                            + "  <shape android:shape=\"rectangle\">\n"
                            + "    <solid android:color=\"#FF6750A4\"/>\n"
                            + "  </shape>\n"
                            + "</inset>");
            assertThat(config.getDrawableRef()).isNull();
            assertThat(config.getNestedXml())
                    .startsWith("<shape")
                    .contains("xmlns:android=")
                    .endsWith("</shape>");
            assertThat(config.hasDrawableSource()).isTrue();
        }

        @Test
        @DisplayName("un inset vide n'encapsule rien")
        void emptyInsetHasNoSource() {
            InsetConfig config = parser.parse("<inset " + NS + "/>");
            assertThat(config.hasDrawableSource()).isFalse();
        }

        @Test
        @DisplayName("référence et enfant inline peuvent coexister"
                + " (priorité à la résolution)")
        void referenceAndInlineCoexist() {
            // Le parser capture tout ; la priorité de la référence est
            // appliquée par resolveLayerChild (réf d'abord), comme le
            // framework où android:drawable l'emporte sur l'enfant
            InsetConfig config = parser.parse(
                    "<inset " + NS + " android:drawable=\"@drawable/fond\">\n"
                            + "  <color android:color=\"#FF000000\"/>\n"
                            + "</inset>");
            assertThat(config.getDrawableRef()).isEqualTo("@drawable/fond");
            assertThat(config.getNestedXml())
                    .startsWith("<color").endsWith("</color>");
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
            assertThatThrownBy(() -> parser.parse("  "))
                    .isInstanceOf(DrawableException.class);
        }

        @Test
        @DisplayName("XML corrompu lève une DrawableException")
        void malformedXmlThrows() {
            assertThatThrownBy(() -> parser.parse(
                    "<inset " + NS + " android:drawable=\"@drawable/fond\""))
                    .isInstanceOf(DrawableException.class);
        }
    }
}
