package jo.layoutlib.themes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitaires du {@link ThemeFileParser}.
 *
 * @author jo@Dev
 */
@DisplayName("ThemeFileParser — parsing de themes.xml et styles.xml")
class ThemeFileParserTest {

    private ThemeFileParser parser;
    private Map<String, StyleDefinition> map;

    @BeforeEach
    void setUp() {
        parser = new ThemeFileParser();
        map = new HashMap<>();
    }

    @Nested
    @DisplayName("Parsing de styles")
    class StylesParsing {

        @Test
        @DisplayName("parse un style simple")
        void shouldParseSimpleStyle() {
            parser.parseStyles(
                    "<resources>"
                            + "<style name=\"MyStyle\">"
                            + "<item name=\"android:textColor\">#FFFFFFFF</item>"
                            + "</style>"
                            + "</resources>", map);
            assertThat(map).containsKey("MyStyle");
            assertThat(map.get("MyStyle").getAttribute("android:textColor"))
                    .isEqualTo("#FFFFFFFF");
        }

        @Test
        @DisplayName("parse un style avec parent explicite")
        void shouldParseStyleWithExplicitParent() {
            parser.parseStyles(
                    "<resources>"
                            + "<style name=\"MyStyle\" parent=\"ParentStyle\"/>"
                            + "</resources>", map);
            assertThat(map.get("MyStyle").getParent()).isEqualTo("ParentStyle");
        }

        @Test
        @DisplayName("calcule le parent implicite à partir du nom")
        void shouldComputeImplicitParent() {
            parser.parseStyles(
                    "<resources>"
                            + "<style name=\"Theme.MyApp.Dark\"/>"
                            + "</resources>", map);
            assertThat(map.get("Theme.MyApp.Dark").getParent()).isEqualTo("Theme.MyApp");
        }

        @Test
        @DisplayName("parent=@null désactive l'héritage")
        void shouldDisableInheritanceWithNullParent() {
            parser.parseStyles(
                    "<resources>"
                            + "<style name=\"MyStyle\" parent=\"@null\"/>"
                            + "</resources>", map);
            assertThat(map.get("MyStyle").getParent()).isNull();
        }

        @Test
        @DisplayName("parse plusieurs items dans un style")
        void shouldParseMultipleItems() {
            parser.parseStyles(
                    "<resources>"
                            + "<style name=\"MyStyle\">"
                            + "<item name=\"colorPrimary\">#FF6750A4</item>"
                            + "<item name=\"colorSecondary\">#FF625B71</item>"
                            + "<item name=\"android:textColor\">#FFFFFFFF</item>"
                            + "</style>"
                            + "</resources>", map);
            StyleDefinition style = map.get("MyStyle");
            assertThat(style.getAttributeCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("ignore les styles sans attribut name")
        void shouldIgnoreStylesWithoutName() {
            parser.parseStyles(
                    "<resources><style><item name=\"x\">y</item></style></resources>", map);
            assertThat(map).isEmpty();
        }
    }

    @Nested
    @DisplayName("Parsing de thèmes")
    class ThemesParsing {

        @Test
        @DisplayName("parse un thème")
        void shouldParseTheme() {
            parser.parseThemes(
                    "<resources>"
                            + "<style name=\"Theme.MyApp\" parent=\"Theme.Material3.DayNight\">"
                            + "<item name=\"colorPrimary\">@color/purple_500</item>"
                            + "</style>"
                            + "</resources>", map);
            assertThat(map.get("Theme.MyApp")).isNotNull();
            assertThat(map.get("Theme.MyApp").isTheme()).isTrue();
        }
    }

    @Nested
    @DisplayName("Cas limites")
    class EdgeCases {

        @Test
        @DisplayName("retourne 0 pour un XML vide")
        void shouldReturnZeroForEmptyXml() {
            assertThat(parser.parseStyles("", map)).isEqualTo(0);
            assertThat(parser.parseStyles(null, map)).isEqualTo(0);
        }

        @Test
        @DisplayName("parent implicite null pour un nom sans point")
        void shouldReturnNullParentForNoDotName() {
            parser.parseStyles("<resources><style name=\"MyStyle\"/></resources>", map);
            assertThat(map.get("MyStyle").getParent()).isNull();
        }
    }
}
