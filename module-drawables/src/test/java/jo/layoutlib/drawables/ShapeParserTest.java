package jo.layoutlib.drawables;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitaires du {@link ShapeParser}.
 *
 * @author jo@Dev
 */
@DisplayName("ShapeParser — parsing des <shape>")
class ShapeParserTest {

    private ShapeParser parser;

    @BeforeEach
    void setUp() {
        parser = new ShapeParser(null);  // sans convertisseur (raw float)
    }

    @Nested
    @DisplayName("Parsing de base")
    class BasicParsing {

        @Test
        @DisplayName("parse une shape rectangle par défaut")
        void shouldParseDefaultRectangle() {
            ShapeConfig config = parser.parse(
                    "<shape xmlns:android=\"http://schemas.android.com/apk/res/android\"/>");
            assertThat(config.getShapeType()).isEqualTo(ShapeType.RECTANGLE);
        }

        @Test
        @DisplayName("parse une shape oval")
        void shouldParseOval() {
            ShapeConfig config = parser.parse(
                    "<shape xmlns:android=\"http://schemas.android.com/apk/res/android\" "
                            + "android:shape=\"oval\"/>");
            assertThat(config.getShapeType()).isEqualTo(ShapeType.OVAL);
        }

        @Test
        @DisplayName("parse une shape line")
        void shouldParseLine() {
            ShapeConfig config = parser.parse(
                    "<shape android:shape=\"line\" "
                            + "xmlns:android=\"http://schemas.android.com/apk/res/android\"/>");
            assertThat(config.getShapeType()).isEqualTo(ShapeType.LINE);
        }

        @Test
        @DisplayName("parse une shape ring")
        void shouldParseRing() {
            ShapeConfig config = parser.parse(
                    "<shape android:shape=\"ring\" "
                            + "xmlns:android=\"http://schemas.android.com/apk/res/android\"/>");
            assertThat(config.getShapeType()).isEqualTo(ShapeType.RING);
        }
    }

    @Nested
    @DisplayName("Solid color")
    class SolidColor {

        @Test
        @DisplayName("parse un solid color")
        void shouldParseSolidColor() {
            ShapeConfig config = parser.parse(
                    "<shape xmlns:android=\"http://schemas.android.com/apk/res/android\">"
                            + "<solid android:color=\"#FF6750A4\"/>"
                            + "</shape>");
            assertThat(config.getSolidColor()).isEqualTo(0xFF6750A4);
        }
    }

    @Nested
    @DisplayName("Gradient")
    class Gradient {

        @Test
        @DisplayName("parse un gradient simple")
        void shouldParseSimpleGradient() {
            ShapeConfig config = parser.parse(
                    "<shape xmlns:android=\"http://schemas.android.com/apk/res/android\">"
                            + "<gradient android:startColor=\"#FF0000\" "
                            + "android:endColor=\"#0000FF\" android:angle=\"90\"/>"
                            + "</shape>");
            assertThat(config.getGradientStartColor()).isEqualTo(0xFFFF0000);
            assertThat(config.getGradientEndColor()).isEqualTo(0xFF0000FF);
            assertThat(config.getGradientAngle()).isEqualTo(90);
            assertThat(config.hasGradient()).isTrue();
        }

        @Test
        @DisplayName("parse un gradient radial")
        void shouldParseRadialGradient() {
            ShapeConfig config = parser.parse(
                    "<shape xmlns:android=\"http://schemas.android.com/apk/res/android\">"
                            + "<gradient android:type=\"radial\" android:gradientRadius=\"100\"/>"
                            + "</shape>");
            assertThat(config.getGradientType()).isEqualTo(GradientType.RADIAL);
            assertThat(config.getGradientRadius()).isEqualTo(100f);
        }
    }

    @Nested
    @DisplayName("Corners")
    class Corners {

        @Test
        @DisplayName("parse un radius global")
        void shouldParseGlobalRadius() {
            ShapeConfig config = parser.parse(
                    "<shape xmlns:android=\"http://schemas.android.com/apk/res/android\">"
                            + "<corners android:radius=\"8\"/>"
                            + "</shape>");
            assertThat(config.getCornerRadius()).isEqualTo(8f);
            assertThat(config.hasCorners()).isTrue();
        }

        @Test
        @DisplayName("parse des radius individuels")
        void shouldParseIndividualRadii() {
            ShapeConfig config = parser.parse(
                    "<shape xmlns:android=\"http://schemas.android.com/apk/res/android\">"
                            + "<corners android:topLeftRadius=\"4\" "
                            + "android:topRightRadius=\"8\" "
                            + "android:bottomLeftRadius=\"12\" "
                            + "android:bottomRightRadius=\"16\"/>"
                            + "</shape>");
            float[] radii = config.getCornerRadii();
            assertThat(radii).containsExactly(4f, 8f, 16f, 12f);
        }
    }

    @Nested
    @DisplayName("Stroke")
    class Stroke {

        @Test
        @DisplayName("parse un stroke simple")
        void shouldParseSimpleStroke() {
            ShapeConfig config = parser.parse(
                    "<shape xmlns:android=\"http://schemas.android.com/apk/res/android\">"
                            + "<stroke android:width=\"2\" android:color=\"#FFFFFFFF\"/>"
                            + "</shape>");
            assertThat(config.getStrokeWidth()).isEqualTo(2f);
            assertThat(config.getStrokeColor()).isEqualTo(0xFFFFFFFF);
            assertThat(config.hasStroke()).isTrue();
        }

        @Test
        @DisplayName("parse un stroke dashed")
        void shouldParseDashedStroke() {
            ShapeConfig config = parser.parse(
                    "<shape xmlns:android=\"http://schemas.android.com/apk/res/android\">"
                            + "<stroke android:width=\"2\" android:color=\"#FFFFFFFF\" "
                            + "android:dashWidth=\"4\" android:dashGap=\"2\"/>"
                            + "</shape>");
            assertThat(config.getStrokeDashWidth()).isEqualTo(4f);
            assertThat(config.getStrokeDashGap()).isEqualTo(2f);
        }
    }

    @Nested
    @DisplayName("Padding")
    class Padding {

        @Test
        @DisplayName("parse un padding")
        void shouldParsePadding() {
            ShapeConfig config = parser.parse(
                    "<shape xmlns:android=\"http://schemas.android.com/apk/res/android\">"
                            + "<padding android:left=\"4\" android:top=\"8\" "
                            + "android:right=\"4\" android:bottom=\"8\"/>"
                            + "</shape>");
            assertThat(config.getPaddingLeft()).isEqualTo(4f);
            assertThat(config.getPaddingTop()).isEqualTo(8f);
            assertThat(config.getPaddingRight()).isEqualTo(4f);
            assertThat(config.getPaddingBottom()).isEqualTo(8f);
            assertThat(config.hasPadding()).isTrue();
        }
    }

    @Nested
    @DisplayName("Cas d'erreur")
    class ErrorCases {

        @Test
        @DisplayName("rejette un XML null")
        void shouldRejectNullXml() {
            assertThatThrownBy(() -> parser.parse(null))
                    .isInstanceOf(DrawableException.class);
        }

        @Test
        @DisplayName("rejette un XML vide")
        void shouldRejectEmptyXml() {
            assertThatThrownBy(() -> parser.parse(""))
                    .isInstanceOf(DrawableException.class);
        }

        @Test
        @DisplayName("rejette un type de shape inconnu")
        void shouldRejectUnknownShapeType() {
            assertThatThrownBy(() -> parser.parse(
                    "<shape android:shape=\"hexagon\" "
                            + "xmlns:android=\"http://schemas.android.com/apk/res/android\"/>"))
                    .isInstanceOf(DrawableException.class);
        }
    }

    @Nested
    @DisplayName("Méthodes utilitaires")
    class UtilityMethods {

        @Test
        @DisplayName("hasGradient retourne false sans gradient")
        void shouldReturnFalseForNoGradient() {
            ShapeConfig config = new ShapeConfig();
            assertThat(config.hasGradient()).isFalse();
        }

        @Test
        @DisplayName("hasStroke retourne false sans stroke")
        void shouldReturnFalseForNoStroke() {
            ShapeConfig config = new ShapeConfig();
            assertThat(config.hasStroke()).isFalse();
        }
    }
}
