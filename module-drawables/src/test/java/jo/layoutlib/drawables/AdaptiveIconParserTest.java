package jo.layoutlib.drawables;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitaires du {@link AdaptiveIconParser} (ADR 0116 — P4/L6).
 *
 * <p>Valide le parsing des {@code <adaptive-icon>} : les trois couches
 * ({@code background}/{@code foreground}/{@code monochrome}) par référence
 * ou par XML inline, les enfants inconnus ignorés, et les cas limites.</p>
 *
 * @author jo@Dev
 * @since 3.0
 */
@DisplayName("AdaptiveIconParser — parsing des <adaptive-icon>")
class AdaptiveIconParserTest {

    private static final String NS =
            "xmlns:android=\"http://schemas.android.com/apk/res/android\"";

    private AdaptiveIconParser parser;

    @BeforeEach
    void setUp() {
        parser = new AdaptiveIconParser();
    }

    @Nested
    @DisplayName("Couches par référence")
    class ReferenceLayers {

        @Test
        @DisplayName("capture les références background/foreground/monochrome")
        void capturesAllThreeLayerRefs() {
            AdaptiveIconConfig config = parser.parse(
                    "<adaptive-icon " + NS + ">\n"
                            + "  <background android:drawable=\"@color/ic_launcher_bg\"/>\n"
                            + "  <foreground android:drawable=\"@drawable/ic_launcher_fg\"/>\n"
                            + "  <monochrome android:drawable=\"@drawable/ic_launcher_mono\"/>\n"
                            + "</adaptive-icon>");
            assertThat(config.getBackgroundRef()).isEqualTo("@color/ic_launcher_bg");
            assertThat(config.getForegroundRef()).isEqualTo("@drawable/ic_launcher_fg");
            assertThat(config.getMonochromeRef()).isEqualTo("@drawable/ic_launcher_mono");
            assertThat(config.hasAnyLayer()).isTrue();
        }

        @Test
        @DisplayName("background et foreground seuls (format API 26-32)")
        void capturesTwoLayerRefs() {
            AdaptiveIconConfig config = parser.parse(
                    "<adaptive-icon " + NS + ">\n"
                            + "  <background android:drawable=\"#FF6750A4\"/>\n"
                            + "  <foreground android:drawable=\"@mipmap/logo\"/>\n"
                            + "</adaptive-icon>");
            assertThat(config.getBackgroundRef()).isEqualTo("#FF6750A4");
            assertThat(config.getForegroundRef()).isEqualTo("@mipmap/logo");
            assertThat(config.getMonochromeRef()).isNull();
            assertThat(config.hasAnyLayer()).isTrue();
        }

        @Test
        @DisplayName("une couche manquante laisse la config à null pour cette couche")
        void missingLayerStaysNull() {
            AdaptiveIconConfig config = parser.parse(
                    "<adaptive-icon " + NS + ">\n"
                            + "  <background android:drawable=\"@color/fond\"/>\n"
                            + "</adaptive-icon>");
            assertThat(config.getBackgroundRef()).isEqualTo("@color/fond");
            assertThat(config.getForegroundRef()).isNull();
            assertThat(config.getForegroundXml()).isNull();
        }
    }

    @Nested
    @DisplayName("Couches inline")
    class InlineLayers {

        @Test
        @DisplayName("capture un drawable inline <color> en background")
        void capturesInlineColorBackground() {
            AdaptiveIconConfig config = parser.parse(
                    "<adaptive-icon " + NS + ">\n"
                            + "  <background>\n"
                            + "    <color android:color=\"#FF6750A4\"/>\n"
                            + "  </background>\n"
                            + "  <foreground android:drawable=\"@drawable/logo\"/>\n"
                            + "</adaptive-icon>");
            assertThat(config.getBackgroundRef()).isNull();
            assertThat(config.getBackgroundXml()).contains("<color");
            assertThat(config.getBackgroundXml()).contains("#FF6750A4");
        }

        @Test
        @DisplayName("capture un drawable inline <vector> en foreground")
        void capturesInlineVectorForeground() {
            AdaptiveIconConfig config = parser.parse(
                    "<adaptive-icon " + NS + ">\n"
                            + "  <background android:drawable=\"@color/fond\"/>\n"
                            + "  <foreground>\n"
                            + "    <vector android:width=\"24dp\" android:height=\"24dp\"\n"
                            + "            android:viewportWidth=\"24\" android:viewportHeight=\"24\">\n"
                            + "      <path android:fillColor=\"#FF000000\" android:pathData=\"M4,4\"/>\n"
                            + "    </vector>\n"
                            + "  </foreground>\n"
                            + "</adaptive-icon>");
            assertThat(config.getForegroundRef()).isNull();
            assertThat(config.getForegroundXml()).startsWith("<vector");
            assertThat(config.getForegroundXml()).contains("pathData");
        }

        @Test
        @DisplayName("la référence et l'inline sont capturés ensemble (réf prioritaire)")
        void capturesRefAndInlineTogether() {
            AdaptiveIconConfig config = parser.parse(
                    "<adaptive-icon " + NS + ">\n"
                            + "  <background android:drawable=\"@color/fond\">\n"
                            + "    <color android:color=\"#FFFF0000\"/>\n"
                            + "  </background>\n"
                            + "</adaptive-icon>");
            assertThat(config.getBackgroundRef()).isEqualTo("@color/fond");
            assertThat(config.getBackgroundXml()).contains("#FFFF0000");
        }
    }

    @Nested
    @DisplayName("Cas limites")
    class EdgeCases {

        @Test
        @DisplayName("un enfant inconnu est ignoré sans exception")
        void unknownChildIsIgnored() {
            AdaptiveIconConfig config = parser.parse(
                    "<adaptive-icon " + NS + ">\n"
                            + "  <background android:drawable=\"@color/fond\"/>\n"
                            + "  <layer_inconnu android:drawable=\"@drawable/x\"/>\n"
                            + "</adaptive-icon>");
            assertThat(config.getBackgroundRef()).isEqualTo("@color/fond");
            assertThat(config.hasAnyLayer()).isTrue();
        }

        @Test
        @DisplayName("un adaptive-icon vide n'a aucune couche")
        void emptyAdaptiveIconHasNoLayer() {
            AdaptiveIconConfig config = parser.parse(
                    "<adaptive-icon " + NS + "/>");
            assertThat(config.hasAnyLayer()).isFalse();
        }

        @Test
        @DisplayName("XML vide lève DrawableException")
        void emptyXmlThrows() {
            assertThatThrownBy(() -> parser.parse("  "))
                    .isInstanceOf(DrawableException.class);
        }

        @Test
        @DisplayName("XML null lève DrawableException")
        void nullXmlThrows() {
            assertThatThrownBy(() -> parser.parse(null))
                    .isInstanceOf(DrawableException.class);
        }
    }
}
