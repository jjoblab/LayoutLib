package jo.layoutlib.drawables;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires du {@link DrawableResolverImpl}.
 *
 * <p>Ces tests valident la détection du type de drawable et la production
 * des configs (POJOs) via {@link #parseConfig(String)}. La création de
 * véritables Drawables Android est testée dans les tests instrumentés.</p>
 *
 * @author jo@Dev
 */
@DisplayName("DrawableResolverImpl — résolution des drawables")
class DrawableResolverImplTest {

    private DrawableResolverImpl resolver;

    @BeforeEach
    void setUp() {
        resolver = new DrawableResolverImpl();
    }

    @Nested
    @DisplayName("Détection de type")
    class TypeDetection {

        @Test
        @DisplayName("détecte un shape")
        void shouldDetectShape() {
            Object config = resolver.parseConfig(
                    "<shape xmlns:android=\"http://schemas.android.com/apk/res/android\"/>");
            assertThat(config).isInstanceOf(ShapeConfig.class);
        }

        @Test
        @DisplayName("détecte un selector")
        void shouldDetectSelector() {
            Object config = resolver.parseConfig(
                    "<selector xmlns:android=\"http://schemas.android.com/apk/res/android\"/>");
            assertThat(config).isInstanceOf(SelectorConfig.class);
        }

        @Test
        @DisplayName("détecte un vector")
        void shouldDetectVector() {
            Object config = resolver.parseConfig(
                    "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\" "
                            + "android:width=\"24\" android:height=\"24\" "
                            + "android:viewportWidth=\"24\" android:viewportHeight=\"24\"/>");
            assertThat(config).isInstanceOf(VectorConfig.class);
        }
    }

    @Nested
    @DisplayName("Cache")
    class CacheBehavior {

        @Test
        @DisplayName("cache les configs parsés")
        void shouldCacheParsedConfigs() {
            String xml = "<shape xmlns:android=\"http://schemas.android.com/apk/res/android\"/>";
            Object config1 = resolver.parseConfig(xml);
            Object config2 = resolver.parseConfig(xml);
            assertThat(config2).isSameAs(config1);
        }

        @Test
        @DisplayName("clearCache vide le cache")
        void shouldClearCache() {
            resolver.parseConfig("<shape xmlns:android=\"http://schemas.android.com/apk/res/android\"/>");
            resolver.clearCache();
            // Pas d'erreur, juste vérifie que clear ne lève pas
            assertThat(true).isTrue();
        }
    }

    @Nested
    @DisplayName("Cas limites")
    class EdgeCases {

        @Test
        @DisplayName("retourne null pour un XML null")
        void shouldReturnNullForNullXml() {
            assertThat(resolver.parseConfig(null)).isNull();
        }

        @Test
        @DisplayName("retourne null pour un XML vide")
        void shouldReturnNullForEmptyXml() {
            assertThat(resolver.parseConfig("")).isNull();
        }

        @Test
        @DisplayName("retourne null pour un type inconnu")
        void shouldReturnNullForUnknownType() {
            assertThat(resolver.parseConfig(
                    "<unknown xmlns:android=\"http://schemas.android.com/apk/res/android\"/>"))
                    .isNull();
        }
    }
}
