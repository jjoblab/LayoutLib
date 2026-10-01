package jo.layoutlib.drawables;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitaires du {@link VectorParser}.
 *
 * @author jo@Dev
 */
@DisplayName("VectorParser — parsing des <vector>")
class VectorParserTest {

    private VectorParser parser;

    @BeforeEach
    void setUp() {
        parser = new VectorParser(null);
    }

    @Nested
    @DisplayName("Parsing de base")
    class BasicParsing {

        @Test
        @DisplayName("parse les dimensions et viewport")
        void shouldParseDimensionsAndViewport() {
            VectorConfig config = parser.parse(
                    "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\" "
                            + "android:width=\"24\" android:height=\"24\" "
                            + "android:viewportWidth=\"24\" android:viewportHeight=\"24\"/>");
            assertThat(config.getWidth()).isEqualTo(24f);
            assertThat(config.getHeight()).isEqualTo(24f);
            assertThat(config.getViewportWidth()).isEqualTo(24f);
            assertThat(config.getViewportHeight()).isEqualTo(24f);
        }

        @Test
        @DisplayName("parse le tint")
        void shouldParseTint() {
            VectorConfig config = parser.parse(
                    "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\" "
                            + "android:width=\"24\" android:height=\"24\" "
                            + "android:viewportWidth=\"24\" android:viewportHeight=\"24\" "
                            + "android:tint=\"#FF000000\"/>");
            assertThat(config.getTint()).isEqualTo(0xFF000000);
        }

        @Test
        @DisplayName("parse l'alpha")
        void shouldParseAlpha() {
            VectorConfig config = parser.parse(
                    "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\" "
                            + "android:width=\"24\" android:height=\"24\" "
                            + "android:viewportWidth=\"24\" android:viewportHeight=\"24\" "
                            + "android:alpha=\"0.5\"/>");
            assertThat(config.getAlpha()).isEqualTo(0.5f);
        }
    }

    @Nested
    @DisplayName("Paths")
    class Paths {

        @Test
        @DisplayName("parse un path simple")
        void shouldParseSimplePath() {
            VectorConfig config = parser.parse(
                    "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\" "
                            + "android:width=\"24\" android:height=\"24\" "
                            + "android:viewportWidth=\"24\" android:viewportHeight=\"24\">"
                            + "<path android:fillColor=\"#FF000000\" "
                            + "android:pathData=\"M3,3 L21,21\"/>"
                            + "</vector>");
            assertThat(config.getNodes()).hasSize(1);
            VectorConfig.PathNode path = (VectorConfig.PathNode) config.getNodes().get(0);
            assertThat(path.getFillColor()).isEqualTo(0xFF000000);
            assertThat(path.getPathData()).isEqualTo("M3,3 L21,21");
        }

        @Test
        @DisplayName("parse un path avec stroke")
        void shouldParsePathWithStroke() {
            VectorConfig config = parser.parse(
                    "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\" "
                            + "android:width=\"24\" android:height=\"24\" "
                            + "android:viewportWidth=\"24\" android:viewportHeight=\"24\">"
                            + "<path android:strokeColor=\"#FFFFFFFF\" "
                            + "android:strokeWidth=\"2\" "
                            + "android:pathData=\"M3,3 L21,21\"/>"
                            + "</vector>");
            VectorConfig.PathNode path = (VectorConfig.PathNode) config.getNodes().get(0);
            assertThat(path.getStrokeColor()).isEqualTo(0xFFFFFFFF);
            assertThat(path.getStrokeWidth()).isEqualTo(2f);
        }
    }

    @Nested
    @DisplayName("Groupes")
    class Groups {

        @Test
        @DisplayName("parse un groupe avec rotation")
        void shouldParseGroupWithRotation() {
            VectorConfig config = parser.parse(
                    "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\" "
                            + "android:width=\"24\" android:height=\"24\" "
                            + "android:viewportWidth=\"24\" android:viewportHeight=\"24\">"
                            + "<group android:name=\"rotation\" android:rotation=\"45\" "
                            + "android:pivotX=\"12\" android:pivotY=\"12\">"
                            + "<path android:pathData=\"M3,3 L21,21\"/>"
                            + "</group>"
                            + "</vector>");
            assertThat(config.getNodes()).hasSize(1);
            VectorConfig.GroupNode group = (VectorConfig.GroupNode) config.getNodes().get(0);
            assertThat(group.getName()).isEqualTo("rotation");
            assertThat(group.getRotation()).isEqualTo(45f);
            assertThat(group.getPivotX()).isEqualTo(12f);
            assertThat(group.getPivotY()).isEqualTo(12f);
            assertThat(group.getChildren()).hasSize(1);
        }

        @Test
        @DisplayName("parse un groupe avec scale et translate")
        void shouldParseGroupWithScaleAndTranslate() {
            VectorConfig config = parser.parse(
                    "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\" "
                            + "android:width=\"24\" android:height=\"24\" "
                            + "android:viewportWidth=\"24\" android:viewportHeight=\"24\">"
                            + "<group android:scaleX=\"2\" android:scaleY=\"0.5\" "
                            + "android:translateX=\"4\" android:translateY=\"8\">"
                            + "<path android:pathData=\"M3,3 L21,21\"/>"
                            + "</group>"
                            + "</vector>");
            VectorConfig.GroupNode group = (VectorConfig.GroupNode) config.getNodes().get(0);
            assertThat(group.getScaleX()).isEqualTo(2f);
            assertThat(group.getScaleY()).isEqualTo(0.5f);
            assertThat(group.getTranslateX()).isEqualTo(4f);
            assertThat(group.getTranslateY()).isEqualTo(8f);
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
    }
}
