package jo.layoutlib.themes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires du {@link ThemeResolverImpl}.
 *
 * @author jo@Dev
 */
@DisplayName("ThemeResolverImpl — résolution de thèmes")
class ThemeResolverImplTest {

    private ThemeResolverImpl resolver;

    @BeforeEach
    void setUp() {
        resolver = new ThemeResolverImpl();

        // Thème jour
        resolver.registerThemesFile(
                "<resources>"
                        + "<style name=\"Theme.Material3.DayNight\">"
                        + "<item name=\"colorPrimary\">#FF6750A4</item>"
                        + "<item name=\"colorOnPrimary\">#FFFFFFFF</item>"
                        + "<item name=\"android:windowBackground\">#FFFFFFFF</item>"
                        + "</style>"
                        + "<style name=\"Theme.MyApp\" parent=\"Theme.Material3.DayNight\">"
                        + "<item name=\"colorPrimary\">@color/my_primary</item>"
                        + "</style>"
                        + "</resources>");

        // Thème nuit
        resolver.registerThemesFile(
                "<resources>"
                        + "<style name=\"Theme.Material3.DayNight\">"
                        + "<item name=\"colorPrimary\">#FFD0BCFF</item>"
                        + "</style>"
                        + "</resources>", true);

        resolver.setTheme("Theme.MyApp");
    }

    @Nested
    @DisplayName("Résolution d'attributs")
    class AttrResolution {

        @Test
        @DisplayName("résout un attribut défini dans le thème courant")
        void shouldResolveAttrFromCurrentTheme() {
            // colorPrimary est défini dans Theme.MyApp comme @color/my_primary
            // Sans ResourceResolver, la valeur @color/my_primary est retournée telle quelle
            Object value = resolver.resolveAttr("?attr/colorPrimary");
            assertThat(value).isEqualTo("@color/my_primary");
        }

        @Test
        @DisplayName("résout un attribut hérité du parent")
        void shouldResolveInheritedAttr() {
            Object value = resolver.resolveAttr("?attr/colorOnPrimary");
            assertThat(value).isEqualTo(-1);  // 0xFFFFFFFF as signed int
        }

        @Test
        @DisplayName("résout un attribut android: du parent")
        void shouldResolveAndroidAttr() {
            Object value = resolver.resolveAttr("?android:attr/windowBackground");
            assertThat(value).isEqualTo(-1);  // 0xFFFFFFFF as signed int
        }

        @Test
        @DisplayName("retourne null pour un attribut non défini")
        void shouldReturnNullForUndefinedAttr() {
            assertThat(resolver.resolveAttr("?attr/nonexistent")).isNull();
        }

        @Test
        @DisplayName("retourne null pour null ou vide")
        void shouldReturnNullForNullOrEmpty() {
            assertThat(resolver.resolveAttr(null)).isNull();
            assertThat(resolver.resolveAttr("")).isNull();
        }
    }

    @Nested
    @DisplayName("Résolution typée")
    class TypedResolution {

        @Test
        @DisplayName("getColorAttr retourne un Integer pour une couleur")
        void shouldReturnIntegerForColor() {
            // Le parent définit colorOnPrimary = #FFFFFFFF
            Integer color = resolver.getColorAttr("colorOnPrimary");
            assertThat(color).isEqualTo(0xFFFFFFFF);
        }

        @Test
        @DisplayName("getColorAttr retourne null pour un attribut non couleur")
        void shouldReturnNullForNonColorAttr() {
            // colorPrimary dans Theme.MyApp est @color/my_primary (string, pas résolu)
            assertThat(resolver.getColorAttr("colorPrimary")).isNull();
        }
    }

    @Nested
    @DisplayName("Mode nuit")
    class NightMode {

        @Test
        @DisplayName("récupère la valeur jour par défaut")
        void shouldReturnDayValueByDefault() {
            // Utilisons un thème sans override dans Theme.MyApp
            resolver.setTheme("Theme.Material3.DayNight");
            Object value = resolver.resolveAttr("?attr/colorPrimary");
            assertThat(value).isEqualTo(0xFF6750A4);  // valeur jour
        }

        @Test
        @DisplayName("récupère la valeur nuit après setNightMode(true)")
        void shouldReturnNightValueAfterNightMode() {
            resolver.setTheme("Theme.Material3.DayNight");
            resolver.setNightMode(true);
            Object value = resolver.resolveAttr("?attr/colorPrimary");
            assertThat(value).isEqualTo(0xFFD0BCFF);  // valeur nuit
        }

        @Test
        @DisplayName("invalide le cache après changement de mode")
        void shouldInvalidateCache() {
            resolver.setTheme("Theme.Material3.DayNight");
            resolver.resolveAttr("?attr/colorPrimary");
            resolver.setNightMode(true);
            Object value = resolver.resolveAttr("?attr/colorPrimary");
            assertThat(value).isEqualTo(0xFFD0BCFF);
        }
    }

    @Nested
    @DisplayName("Héritage")
    class Inheritance {

        @Test
        @DisplayName("getParentStyle retourne le parent direct")
        void shouldReturnDirectParent() {
            assertThat(resolver.getParentStyle("Theme.MyApp"))
                    .isEqualTo("Theme.Material3.DayNight");
        }

        @Test
        @DisplayName("getInheritanceChain construit la chaîne complète")
        void shouldBuildFullChain() {
            // Theme.MyApp → Theme.Material3.DayNight → Theme.Material3 (implicite)
            assertThat(resolver.getInheritanceChain("Theme.MyApp"))
                    .contains("Theme.MyApp", "Theme.Material3.DayNight");
        }

        @Test
        @DisplayName("getParentStyle retourne null pour un style inexistant")
        void shouldReturnNullForUnknownStyle() {
            assertThat(resolver.getParentStyle("Unknown")).isNull();
        }
    }

    @Nested
    @DisplayName("Configuration")
    class Configuration {

        @Test
        @DisplayName("setTheme change le thème courant")
        void shouldChangeCurrentTheme() {
            resolver.setTheme("Theme.Material3.DayNight");
            assertThat(resolver.getCurrentTheme()).isEqualTo("Theme.Material3.DayNight");
        }

        @Test
        @DisplayName("setNightMode change le flag")
        void shouldChangeNightMode() {
            resolver.setNightMode(true);
            assertThat(resolver.isNightMode()).isTrue();
        }
    }

    @Nested
    @DisplayName("Statistiques")
    class Statistics {

        @Test
        @DisplayName("getStyleCount retourne le nombre de styles")
        void shouldReturnStyleCount() {
            // Theme.Material3.DayNight + Theme.MyApp = 2
            assertThat(resolver.getStyleCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("getNightStyleCount retourne le nombre de styles nuit")
        void shouldReturnNightStyleCount() {
            assertThat(resolver.getNightStyleCount()).isEqualTo(1);
        }
    }
}
