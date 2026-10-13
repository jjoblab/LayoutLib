package jo.layoutlib.drawables;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import jo.layoutlib.resources.DimensionConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitaires du {@link LayerListParser} (ADR 0116 — P0/L3).
 *
 * <p>Valide le parsing des {@code <layer-list>} : ordre et nombre de calques,
 * références {@code android:drawable}, insets convertis en pixels, capture du
 * XML imbriqué (shape/selector inline), et cas limites.</p>
 *
 * @author jo@Dev
 * @since 3.0
 */
@DisplayName("LayerListParser — parsing des <layer-list>")
class LayerListParserTest {

    private static final String NS =
            "xmlns:android=\"http://schemas.android.com/apk/res/android\"";

    private LayerListParser parser;

    @BeforeEach
    void setUp() {
        // densité 2x : 16dp → 32px
        parser = new LayerListParser(new DimensionConverter(2f, 2f, 320f));
    }

    @Nested
    @DisplayName("Structure des calques")
    class LayerStructure {

        @Test
        @DisplayName("parse un layer-list vide en config vide")
        void parsesEmptyLayerList() {
            LayerListConfig config = parser.parse("<layer-list " + NS + "/>");
            assertThat(config).isNotNull();
            assertThat(config.getItemCount()).isZero();
        }

        @Test
        @DisplayName("conserve l'ordre des items (indice 0 = calque le plus bas)")
        void keepsItemOrder() {
            LayerListConfig config = parser.parse(
                    "<layer-list " + NS + ">\n"
                            + "  <item android:drawable=\"@drawable/fond\"/>\n"
                            + "  <item android:drawable=\"@drawable/avant\"/>\n"
                            + "</layer-list>");
            assertThat(config.getItemCount()).isEqualTo(2);
            assertThat(config.getItems().get(0).getDrawableRef())
                    .isEqualTo("@drawable/fond");
            assertThat(config.getItems().get(1).getDrawableRef())
                    .isEqualTo("@drawable/avant");
        }

        @Test
        @DisplayName("lit la référence android:drawable de chaque item")
        void readsDrawableReferences() {
            LayerListConfig config = parser.parse(
                    "<layer-list " + NS + ">\n"
                            + "  <item android:drawable=\"@color/fond\"/>\n"
                            + "</layer-list>");
            assertThat(config.getItems().get(0).getDrawableRef())
                    .isEqualTo("@color/fond");
            assertThat(config.getItems().get(0).hasDrawableSource()).isTrue();
        }

        @Test
        @DisplayName("consomme l'id déclaratif sans l'interpréter")
        void readsItemId() {
            LayerListConfig config = parser.parse(
                    "<layer-list " + NS + ">\n"
                            + "  <item android:id=\"@+id/calque_fond\""
                            + " android:drawable=\"#FFFFFFFF\"/>\n"
                            + "</layer-list>");
            assertThat(config.getItems().get(0).getId())
                    .isEqualTo("@+id/calque_fond");
        }
    }

    @Nested
    @DisplayName("Insets par calque")
    class LayerInsets {

        @Test
        @DisplayName("convertit les insets dp en pixels via le convertisseur")
        void convertsDpInsetsToPixels() {
            LayerListConfig config = parser.parse(
                    "<layer-list " + NS + ">\n"
                            + "  <item android:left=\"16dp\" android:top=\"8dp\"\n"
                            + "        android:right=\"16dp\" android:bottom=\"8dp\"\n"
                            + "        android:drawable=\"#FFFF0000\"/>\n"
                            + "</layer-list>");
            LayerListConfig.LayerItem item = config.getItems().get(0);
            assertThat(item.getInsetLeft()).isEqualTo(32f);
            assertThat(item.getInsetTop()).isEqualTo(16f);
            assertThat(item.getInsetRight()).isEqualTo(32f);
            assertThat(item.getInsetBottom()).isEqualTo(16f);
            assertThat(item.hasAnyInset()).isTrue();
        }

        @Test
        @DisplayName("laisse les insets absents à null")
        void missingInsetsStayNull() {
            LayerListConfig config = parser.parse(
                    "<layer-list " + NS + ">\n"
                            + "  <item android:drawable=\"#FFFF0000\"/>\n"
                            + "</layer-list>");
            LayerListConfig.LayerItem item = config.getItems().get(0);
            assertThat(item.getInsetLeft()).isNull();
            assertThat(item.getInsetTop()).isNull();
            assertThat(item.getInsetRight()).isNull();
            assertThat(item.getInsetBottom()).isNull();
            assertThat(item.hasAnyInset()).isFalse();
        }

        @Test
        @DisplayName("parse les insets partiels (left seul)")
        void parsesPartialInsets() {
            LayerListConfig config = parser.parse(
                    "<layer-list " + NS + ">\n"
                            + "  <item android:left=\"10dp\""
                            + " android:drawable=\"#FFFF0000\"/>\n"
                            + "</layer-list>");
            LayerListConfig.LayerItem item = config.getItems().get(0);
            assertThat(item.getInsetLeft()).isEqualTo(20f);
            assertThat(item.getInsetTop()).isNull();
            assertThat(item.hasAnyInset()).isTrue();
        }
    }

    @Nested
    @DisplayName("Enfants inline")
    class InlineChildren {

        @Test
        @DisplayName("capture le XML d'un shape imbriqué")
        void capturesNestedShapeXml() {
            LayerListConfig config = parser.parse(
                    "<layer-list " + NS + ">\n"
                            + "  <item>\n"
                            + "    <shape android:shape=\"rectangle\">\n"
                            + "      <solid android:color=\"#FF6750A4\"/>\n"
                            + "    </shape>\n"
                            + "  </item>\n"
                            + "</layer-list>");
            LayerListConfig.LayerItem item = config.getItems().get(0);
            assertThat(item.getDrawableRef()).isNull();
            assertThat(item.getNestedXml()).isNotNull();
            assertThat(item.hasDrawableSource()).isTrue();
            // Le fragment capturé est re-sérialisé avec le namespace déclaré
            assertThat(item.getNestedXml())
                    .startsWith("<shape")
                    .contains("xmlns:android=")
                    .contains("android:color=\"#FF6750A4\"")
                    .endsWith("</shape>");
        }

        @Test
        @DisplayName("le fragment capturé se re-parse en GradientDrawable config")
        void capturedFragmentReparses() {
            LayerListConfig config = parser.parse(
                    "<layer-list " + NS + ">\n"
                            + "  <item>\n"
                            + "    <shape android:shape=\"oval\">\n"
                            + "      <solid android:color=\"#FF0000FF\"/>\n"
                            + "    </shape>\n"
                            + "  </item>\n"
                            + "</layer-list>");
            String nested = config.getItems().get(0).getNestedXml();
            ShapeParser shapeParser = new ShapeParser(null);
            ShapeConfig shapeConfig = shapeParser.parse(nested);
            assertThat(shapeConfig).isNotNull();
        }

        @Test
        @DisplayName("un item auto-fermant sans drawable ni enfant est non résolvable")
        void emptyItemHasNoSource() {
            LayerListConfig config = parser.parse(
                    "<layer-list " + NS + ">\n"
                            + "  <item/>\n"
                            + "</layer-list>");
            LayerListConfig.LayerItem item = config.getItems().get(0);
            assertThat(item.hasDrawableSource()).isFalse();
        }

        @Test
        @DisplayName("mélange références et enfants inline dans la même pile")
        void mixesRefsAndInlineChildren() {
            LayerListConfig config = parser.parse(
                    "<layer-list " + NS + ">\n"
                            + "  <item android:drawable=\"@drawable/fond\"/>\n"
                            + "  <item android:top=\"4dp\">\n"
                            + "    <color android:color=\"#80FFFFFF\"/>\n"
                            + "  </item>\n"
                            + "</layer-list>");
            assertThat(config.getItemCount()).isEqualTo(2);
            assertThat(config.getItems().get(0).getDrawableRef())
                    .isEqualTo("@drawable/fond");
            assertThat(config.getItems().get(1).getNestedXml())
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
            assertThatThrownBy(() -> parser.parse("   "))
                    .isInstanceOf(DrawableException.class);
        }

        @Test
        @DisplayName("XML corrompu lève une DrawableException")
        void malformedXmlThrows() {
            assertThatThrownBy(() -> parser.parse("<layer-list " + NS + "><item"))
                    .isInstanceOf(DrawableException.class);
        }

        @Test
        @DisplayName("sans convertisseur, les dimensions restent numériques brutes")
        void noConverterFallsBackToRawFloat() {
            LayerListParser rawParser = new LayerListParser(null);
            LayerListConfig config = rawParser.parse(
                    "<layer-list " + NS + ">\n"
                            + "  <item android:left=\"16dp\""
                            + " android:drawable=\"#FFFF0000\"/>\n"
                            + "</layer-list>");
            assertThat(config.getItems().get(0).getInsetLeft()).isEqualTo(16f);
        }
    }
}
