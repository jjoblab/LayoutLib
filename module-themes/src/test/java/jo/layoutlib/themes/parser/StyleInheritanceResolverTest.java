package jo.layoutlib.themes.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jo.layoutlib.themes.StyleDefinition;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires du {@link StyleInheritanceResolver}.
 *
 * @author jo@Dev
 */
@DisplayName("StyleInheritanceResolver — chaîne d'héritage")
class StyleInheritanceResolverTest {

    private StyleInheritanceResolver resolver;
    private Map<String, StyleDefinition> styles;

    @BeforeEach
    void setUp() {
        resolver = new StyleInheritanceResolver();
        styles = new HashMap<>();

        // Theme.MyApp.Dark → Theme.MyApp → Theme.Material3.DayNight
        styles.put("Theme.Material3.DayNight",
                new StyleDefinition("Theme.Material3.DayNight", null, true));
        styles.put("Theme.MyApp",
                new StyleDefinition("Theme.MyApp", "Theme.Material3.DayNight", true));
        styles.put("Theme.MyApp.Dark",
                new StyleDefinition("Theme.MyApp.Dark", "Theme.MyApp", true));
    }

    @Nested
    @DisplayName("Résolution de chaîne")
    class ChainResolution {

        @Test
        @DisplayName("résout la chaîne complète")
        void shouldResolveFullChain() {
            List<String> chain = resolver.resolveChain("Theme.MyApp.Dark", styles);
            // Theme.MyApp.Dark → Theme.MyApp → Theme.Material3.DayNight → Theme.Material3 (implicite)
            assertThat(chain).contains(
                    "Theme.MyApp.Dark", "Theme.MyApp", "Theme.Material3.DayNight");
        }

        @Test
        @DisplayName("résout un style sans parent explicite (implicite par point)")
        void shouldResolveStyleWithoutExplicitParent() {
            // Theme.Material3.DayNight n'a pas de parent explicite, mais a un parent implicite Theme.Material3
            List<String> chain = resolver.resolveChain("Theme.Material3.DayNight", styles);
            assertThat(chain).startsWith("Theme.Material3.DayNight");
        }

        @Test
        @DisplayName("retourne liste vide pour un style null")
        void shouldReturnEmptyForNullStyle() {
            assertThat(resolver.resolveChain(null, styles)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Parent implicite")
    class ImplicitParent {

        @Test
        @DisplayName("calcule le parent implicite par point")
        void shouldComputeImplicitParent() {
            assertThat(resolver.computeImplicitParent("Theme.MyApp.Dark"))
                    .isEqualTo("Theme.MyApp");
        }

        @Test
        @DisplayName("retourne null pour un nom sans point")
        void shouldReturnNullForNoDot() {
            assertThat(resolver.computeImplicitParent("Theme")).isNull();
        }
    }

    @Nested
    @DisplayName("Parent explicite")
    class ExplicitParent {

        @Test
        @DisplayName("résout @style/Theme.Foo")
        void shouldResolveStyleReference() {
            assertThat(resolver.resolveParentName("@style/Theme.Foo"))
                    .isEqualTo("Theme.Foo");
        }

        @Test
        @DisplayName("résout @android:style/Theme.Foo")
        void shouldResolveAndroidStyleReference() {
            assertThat(resolver.resolveParentName("@android:style/Theme.Foo"))
                    .isEqualTo("Theme.Foo");
        }

        @Test
        @DisplayName("retourne tel quel pour un nom sans préfixe")
        void shouldReturnAsIsForNoPrefix() {
            assertThat(resolver.resolveParentName("Theme.Foo"))
                    .isEqualTo("Theme.Foo");
        }
    }

    @Nested
    @DisplayName("Recherche")
    class Lookup {

        @Test
        @DisplayName("inheritsFrom retourne true pour un parent direct")
        void shouldReturnTrueForDirectParent() {
            assertThat(resolver.inheritsFrom("Theme.MyApp.Dark", "Theme.MyApp", styles))
                    .isTrue();
        }

        @Test
        @DisplayName("inheritsFrom retourne true pour un parent indirect")
        void shouldReturnTrueForIndirectParent() {
            assertThat(resolver.inheritsFrom("Theme.MyApp.Dark",
                    "Theme.Material3.DayNight", styles)).isTrue();
        }

        @Test
        @DisplayName("findRootStyle retourne le style racine")
        void shouldReturnRootStyle() {
            // La racine de Theme.MyApp.Dark est Theme.Material3 (parent implicite de Theme.Material3.DayNight)
            String root = resolver.findRootStyle("Theme.MyApp.Dark", styles);
            assertThat(root).isIn("Theme.Material3.DayNight", "Theme.Material3");
        }
    }
}
