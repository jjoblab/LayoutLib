package jo.layoutlib.resources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de résolution des resources par dimensions d'écran (ADR 0116 — L1).
 *
 * <p>Valide le basculement {@code values/} → {@code values-sw600dp/} via
 * {@link ResourceResolverImpl#setSmallestScreenWidth(int)} : sélection
 * tablette, repli automatique sur {@code values/} quand la resource
 * manque, écart des dossiers tablette pour la config téléphone (fin du
 * écrasement non déterministe), seuils {@code w/h…dp}, composition avec
 * les autres setters, et scan réel d'un {@code res/} multi-écrans.</p>
 *
 * @author jo@Dev
 * @since 3.1
 */
@DisplayName("ResourceResolverImpl — dimensions d'écran sw/w/h (L1)")
class ResourceResolverImplScreenSizeTest {

    private ResourceTable table;
    private ResourceResolverImpl resolver;

    @BeforeEach
    void setUp() {
        table = new ResourceTable();

        // values/ — défaut téléphone
        ResourceFileParser defParser = new ResourceFileParser(table, ResourceQualifier.DEFAULT);
        defParser.parse("<resources>"
                + "<dimen name=\"marge\">16dp</dimen>"
                + "<dimen name=\"uniquement_defaut\">8dp</dimen>"
                + "<bool name=\"two_pane\">false</bool>"
                + "</resources>");

        // values-sw600dp/ — tablette
        ResourceFileParser sw600Parser = new ResourceFileParser(
                table, new ResourceQualifier(false, false, 1, null, null, 600, 0, 0));
        sw600Parser.parse("<resources>"
                + "<dimen name=\"marge\">24dp</dimen>"
                + "<bool name=\"two_pane\">true</bool>"
                + "</resources>");

        // values-sw720dp/ — grande tablette
        ResourceFileParser sw720Parser = new ResourceFileParser(
                table, new ResourceQualifier(false, false, 1, null, null, 720, 0, 0));
        sw720Parser.parse("<resources>"
                + "<dimen name=\"marge\">32dp</dimen>"
                + "</resources>");

        // values-w600dp/ — largeur disponible
        ResourceFileParser w600Parser = new ResourceFileParser(
                table, new ResourceQualifier(false, false, 1, null, null, 0, 600, 0));
        w600Parser.parse("<resources>"
                + "<dimen name=\"marge\">28dp</dimen>"
                + "</resources>");

        // values-h720dp/ — hauteur disponible
        ResourceFileParser h720Parser = new ResourceFileParser(
                table, new ResourceQualifier(false, false, 1, null, null, 0, 0, 720));
        h720Parser.parse("<resources>"
                + "<dimen name=\"marge\">20dp</dimen>"
                + "</resources>");

        resolver = new ResourceResolverImpl(table);
        resolver.setDimensionConverter(new DimensionConverter(2.0f, 1.0f, 320f));
    }

    @Nested
    @DisplayName("Sélection tablette (smallest width)")
    class SmallestWidthSelection {

        @Test
        @DisplayName("sans dimension, la valeur téléphone est retournée (repli universel)")
        void phoneDefaultWithoutDimension() {
            assertThat(resolver.getDimension("@dimen/marge")).isEqualTo(32.0f);
            assertThat(resolver.getBoolean("@bool/two_pane")).isFalse();
        }

        @Test
        @DisplayName("setSmallestScreenWidth(600) sélectionne le dossier tablette")
        void tabletSelectionAt600() {
            resolver.setSmallestScreenWidth(600);
            assertThat(resolver.getDimension("@dimen/marge")).isEqualTo(48.0f);
            assertThat(resolver.getBoolean("@bool/two_pane")).isTrue();
        }

        @Test
        @DisplayName("setSmallestScreenWidth(720) sélectionne la grande tablette")
        void bigTabletSelectionAt720() {
            resolver.setSmallestScreenWidth(720);
            assertThat(resolver.getDimension("@dimen/marge")).isEqualTo(64.0f);
        }

        @Test
        @DisplayName("au-delà de 720, la plus proche dimension compatible gagne")
        void beyond720FallsBackToClosest() {
            resolver.setSmallestScreenWidth(1024);
            assertThat(resolver.getDimension("@dimen/marge")).isEqualTo(64.0f);
        }

        @Test
        @DisplayName("repli automatique sur values/ quand la resource tablette manque")
        void fallbackToDefaultWhenMissing() {
            resolver.setSmallestScreenWidth(600);
            assertThat(resolver.getDimension("@dimen/uniquement_defaut")).isEqualTo(16.0f);
        }

        @Test
        @DisplayName("setSmallestScreenWidth(0) désélectionne les dossiers tablette")
        void resetToPhone() {
            resolver.setSmallestScreenWidth(600);
            assertThat(resolver.getDimension("@dimen/marge")).isEqualTo(48.0f);
            resolver.setSmallestScreenWidth(0);
            assertThat(resolver.getDimension("@dimen/marge")).isEqualTo(32.0f);
        }

        @Test
        @DisplayName("cache invalidé par le changement de dimension")
        void cacheInvalidated() {
            assertThat(resolver.getDimension("@dimen/marge")).isEqualTo(32.0f);
            resolver.setSmallestScreenWidth(600);
            assertThat(resolver.getDimension("@dimen/marge")).isEqualTo(48.0f);
        }
    }

    @Nested
    @DisplayName("Seuils w/h (largeur/hauteur disponible)")
    class WidthHeightSelection {

        @Test
        @DisplayName("setScreenWidth(600) sélectionne values-w600dp")
        void widthSelection() {
            resolver.setScreenWidth(600);
            assertThat(resolver.getDimension("@dimen/marge")).isEqualTo(56.0f);
        }

        @Test
        @DisplayName("setScreenWidth(599) écarte le dossier (seuil non atteint)")
        void widthBelowThreshold() {
            resolver.setScreenWidth(599);
            assertThat(resolver.getDimension("@dimen/marge")).isEqualTo(32.0f);
        }

        @Test
        @DisplayName("setScreenHeight(720) sélectionne values-h720dp")
        void heightSelection() {
            resolver.setScreenHeight(720);
            assertThat(resolver.getDimension("@dimen/marge")).isEqualTo(40.0f);
        }
    }

    @Nested
    @DisplayName("Composition avec les autres setters")
    class Composition {

        @Test
        @DisplayName("setNightMode après setSmallestScreenWidth préserve la dimension")
        void nightModePreservesSmallestWidth() {
            resolver.setSmallestScreenWidth(600);
            resolver.setNightMode(true);
            assertThat(resolver.getCurrentQualifier().getSmallestScreenWidth())
                    .isEqualTo(600);
            assertThat(resolver.getDimension("@dimen/marge")).isEqualTo(48.0f);
        }

        @Test
        @DisplayName("setLanguage après setSmallestScreenWidth préserve la dimension")
        void languagePreservesSmallestWidth() {
            resolver.setSmallestScreenWidth(720);
            resolver.setLanguage("fr");
            assertThat(resolver.getCurrentQualifier().getSmallestScreenWidth())
                    .isEqualTo(720);
            assertThat(resolver.getDimension("@dimen/marge")).isEqualTo(64.0f);
        }

        @Test
        @DisplayName("setApiLevel après setSmallestScreenWidth préserve la dimension")
        void apiLevelPreservesSmallestWidth() {
            resolver.setSmallestScreenWidth(600);
            resolver.setApiLevel(31);
            assertThat(resolver.getCurrentQualifier().getSmallestScreenWidth())
                    .isEqualTo(600);
        }
    }

    @Nested
    @DisplayName("Scan réel d'un res/ multi-écrans")
    class RealScan {

        @TempDir
        File resDir;

        private ResourceResolverImpl scanRes() throws IOException {
            Path values = Files.createDirectory(resDir.toPath().resolve("values"));
            Files.write(values.resolve("dimens.xml"),
                    ("<resources><dimen name=\"marge\">16dp</dimen></resources>")
                            .getBytes(StandardCharsets.UTF_8));
            Path sw600 = Files.createDirectory(resDir.toPath().resolve("values-sw600dp"));
            Files.write(sw600.resolve("dimens.xml"),
                    ("<resources><dimen name=\"marge\">24dp</dimen></resources>")
                            .getBytes(StandardCharsets.UTF_8));
            ResourceResolverImpl scanned = new ResourceResolverImpl(resDir.getAbsolutePath());
            scanned.setDimensionConverter(new DimensionConverter(2.0f, 1.0f, 320f));
            return scanned;
        }

        @Test
        @DisplayName("le scan classe values-sw600dp avec son qualifier (plus de collision DEFAULT)")
        void scanClassifiesSw600() throws IOException {
            ResourceResolverImpl scanned = scanRes();
            scanned.setSmallestScreenWidth(600);
            assertThat(scanned.getDimension("@dimen/marge")).isEqualTo(48.0f);
        }

        @Test
        @DisplayName("config téléphone : le dossier tablette ne concourt pas (fin du écrasement)")
        void phoneConfigIgnoresTabletFolder() throws IOException {
            ResourceResolverImpl scanned = scanRes();
            // Avant L1, l'ordre du listFiles décidaient entre 16dp et 24dp ;
            // désormais 16dp est déterministe
            assertThat(scanned.getDimension("@dimen/marge")).isEqualTo(32.0f);
        }
    }
}
