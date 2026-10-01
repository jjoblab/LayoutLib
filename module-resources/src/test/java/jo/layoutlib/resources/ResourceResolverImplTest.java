package jo.layoutlib.resources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitaires du {@link ResourceResolverImpl}.
 *
 * <p>Valide la résolution des références {@code @color/}, {@code @string/},
 * {@code @dimen/}, {@code @integer/}, {@code @bool/}, le support des
 * qualifiers (jour/nuit, orientation, API), les références chainables,
 * et la gestion des resources manquantes.</p>
 *
 * @author jo@Dev
 */
@DisplayName("ResourceResolverImpl — résolution des resources")
class ResourceResolverImplTest {

    private ResourceTable table;
    private ResourceResolverImpl resolver;

    @BeforeEach
    void setUp() {
        table = new ResourceTable();
        // Population initiale
        ResourceFileParser parser = new ResourceFileParser(table, ResourceQualifier.DEFAULT);
        parser.parse("<resources>"
                + "<color name=\"red\">#FF0000</color>"
                + "<color name=\"primary\">@color/purple_500</color>"
                + "<color name=\"purple_500\">#FF6750A4</color>"
                + "<string name=\"hello\">Hello World</string>"
                + "<string name=\"greeting\">@string/hello</string>"
                + "<dimen name=\"margin\">16dp</dimen>"
                + "<integer name=\"max\">42</integer>"
                + "<bool name=\"enabled\">true</bool>"
                + "</resources>");

        // Variantes en mode nuit
        ResourceQualifier nightQ = new ResourceQualifier(true, false, 1);
        ResourceFileParser nightParser = new ResourceFileParser(table, nightQ);
        nightParser.parse("<resources>"
                + "<color name=\"primary\">#FFD0BCFF</color>"
                + "<string name=\"hello\">Bonsoir</string>"
                + "</resources>");

        resolver = new ResourceResolverImpl(table);
        // density=2.0, fontScale=1.0
        resolver.setDimensionConverter(new DimensionConverter(2.0f, 1.0f, 320f));
    }

    @Nested
    @DisplayName("Résolution de couleurs")
    class ColorResolution {

        @Test
        @DisplayName("résout une couleur simple")
        void shouldResolveSimpleColor() {
            assertThat(resolver.getColor("@color/red")).isEqualTo(0xFFFF0000);
        }

        @Test
        @DisplayName("résout une couleur chainable")
        void shouldResolveChainableColor() {
            // primary → purple_500 → #FF6750A4
            assertThat(resolver.getColor("@color/primary")).isEqualTo(0xFF6750A4);
        }

        @Test
        @DisplayName("retourne null pour une couleur manquante")
        void shouldReturnNullForMissingColor() {
            assertThat(resolver.getColor("@color/nonexistent")).isNull();
        }

        @Test
        @DisplayName("retourne null pour une référence null ou vide")
        void shouldReturnNullForNullOrEmpty() {
            assertThat(resolver.getColor(null)).isNull();
            assertThat(resolver.getColor("")).isNull();
        }
    }

    @Nested
    @DisplayName("Résolution de chaînes")
    class StringResolution {

        @Test
        @DisplayName("résout une chaîne simple")
        void shouldResolveSimpleString() {
            assertThat(resolver.getString("@string/hello")).isEqualTo("Hello World");
        }

        @Test
        @DisplayName("résout une chaîne chainable")
        void shouldResolveChainableString() {
            assertThat(resolver.getString("@string/greeting")).isEqualTo("Hello World");
        }

        @Test
        @DisplayName("retourne la chaîne brute si ce n'est pas une référence")
        void shouldReturnRawStringIfNotReference() {
            assertThat(resolver.getString("literal text")).isEqualTo("literal text");
        }
    }

    @Nested
    @DisplayName("Résolution de dimensions")
    class DimenResolution {

        @Test
        @DisplayName("résout une dimension en pixels")
        void shouldResolveDimenInPixels() {
            // 16dp × 2.0 = 32.0
            assertThat(resolver.getDimension("@dimen/margin")).isEqualTo(32.0f);
        }

        @Test
        @DisplayName("retourne null pour une dimension manquante")
        void shouldReturnNullForMissingDimen() {
            assertThat(resolver.getDimension("@dimen/nonexistent")).isNull();
        }
    }

    @Nested
    @DisplayName("Résolution d'entiers et booléens")
    class IntegerAndBoolResolution {

        @Test
        @DisplayName("résout un entier")
        void shouldResolveInteger() {
            assertThat(resolver.getInteger("@integer/max")).isEqualTo(42);
        }

        @Test
        @DisplayName("résout un booléen")
        void shouldResolveBoolean() {
            assertThat(resolver.getBoolean("@bool/enabled")).isTrue();
        }
    }

    @Nested
    @DisplayName("Qualifiers (mode nuit)")
    class QualifiersNightMode {

        @Test
        @DisplayName("récupère la valeur jour par défaut")
        void shouldReturnDayValueByDefault() {
            assertThat(resolver.getColor("@color/primary")).isEqualTo(0xFF6750A4);
        }

        @Test
        @DisplayName("récupère la valeur nuit après setNightMode(true)")
        void shouldReturnNightValueAfterNightMode() {
            resolver.setNightMode(true);
            assertThat(resolver.getColor("@color/primary")).isEqualTo(0xFFD0BCFF);
        }

        @Test
        @DisplayName("récupère la valeur jour après setNightMode(false)")
        void shouldReturnDayValueAfterDayMode() {
            resolver.setNightMode(true);
            resolver.setNightMode(false);
            assertThat(resolver.getColor("@color/primary")).isEqualTo(0xFF6750A4);
        }

        @Test
        @DisplayName("invalide le cache après changement de mode")
        void shouldInvalidateCacheAfterModeChange() {
            // Première résolution (jour)
            assertThat(resolver.getString("@string/hello")).isEqualTo("Hello World");
            // Mode nuit
            resolver.setNightMode(true);
            // Doit re-résoudre et trouver "Bonsoir"
            assertThat(resolver.getString("@string/hello")).isEqualTo("Bonsoir");
        }
    }

    @Nested
    @DisplayName("Cache LRU")
    class CacheBehavior {

        @Test
        @DisplayName("cache les résolutions réussies")
        void shouldCacheSuccessfulResolutions() {
            Integer first = resolver.getColor("@color/red");
            Integer second = resolver.getColor("@color/red");
            assertThat(second).isSameAs(first);
        }

        @Test
        @DisplayName("clearCache vide tous les caches")
        void shouldClearAllCaches() {
            resolver.getColor("@color/red");
            resolver.getString("@string/hello");
            resolver.clearCache();
            // Pas d'erreur, juste vérifie que clear ne lève pas
            assertThat(resolver.getColor("@color/red")).isEqualTo(0xFFFF0000);
        }
    }

    @Nested
    @DisplayName("Layouts et drawables (sans dossier res/)")
    class LayoutAndDrawableWithoutFolder {

        @Test
        @DisplayName("getLayout retourne null sans dossier res/")
        void shouldReturnNullForLayoutWithoutFolder() {
            assertThat(resolver.getLayout("@layout/foo")).isNull();
        }

        @Test
        @DisplayName("getDrawablePath retourne null sans dossier res/")
        void shouldReturnNullForDrawableWithoutFolder() {
            assertThat(resolver.getDrawablePath("@drawable/foo")).isNull();
        }
    }

    @Nested
    @DisplayName("Constructeur depuis dossier")
    class ConstructorFromFolder {

        @Test
        @DisplayName("rejette un dossier inexistant")
        void shouldRejectNonExistentFolder() {
            assertThatThrownBy(() -> new ResourceResolverImpl("/nonexistent/path/res"))
                    .isInstanceOf(ResourceException.class);
        }
    }

    @Nested
    @DisplayName("Configuration du qualifier")
    class QualifierConfiguration {

        @Test
        @DisplayName("setLandscape change le qualifier courant")
        void shouldChangeQualifierOnLandscape() {
            resolver.setLandscape(true);
            assertThat(resolver.getCurrentQualifier().isLandscape()).isTrue();
        }

        @Test
        @DisplayName("setApiLevel change le qualifier courant")
        void shouldChangeQualifierOnApiLevel() {
            resolver.setApiLevel(31);
            assertThat(resolver.getCurrentQualifier().getApiLevel()).isEqualTo(31);
        }
    }

    @Nested
    @DisplayName("Statistiques")
    class Statistics {

        @Test
        @DisplayName("getColorCount retourne le nombre de couleurs chargées")
        void shouldReturnColorCount() {
            assertThat(resolver.getColorCount()).isEqualTo(3); // red, primary, purple_500
        }

        @Test
        @DisplayName("getStringCount retourne le nombre de chaînes chargées")
        void shouldReturnStringCount() {
            assertThat(resolver.getStringCount()).isEqualTo(2); // hello, greeting
        }

        @Test
        @DisplayName("getDimenCount retourne le nombre de dimensions chargées")
        void shouldReturnDimenCount() {
            assertThat(resolver.getDimenCount()).isEqualTo(1);
        }
    }
}
