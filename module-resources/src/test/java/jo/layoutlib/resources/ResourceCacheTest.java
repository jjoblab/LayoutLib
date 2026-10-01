package jo.layoutlib.resources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires du {@link ResourceCache}.
 *
 * @author jo@Dev
 */
@DisplayName("ResourceCache — cache LRU")
class ResourceCacheTest {

    private ResourceCache<String, String> cache;

    @BeforeEach
    void setUp() {
        cache = new ResourceCache<>(3);  // petite capacité pour tester LRU
    }

    @Nested
    @DisplayName("Opérations de base")
    class BasicOperations {

        @Test
        @DisplayName("put et get fonctionnent")
        void shouldPutAndGet() {
            cache.put("key1", "value1");
            assertThat(cache.get("key1")).isEqualTo("value1");
        }

        @Test
        @DisplayName("get retourne null pour une clé absente")
        void shouldReturnNullForMissingKey() {
            assertThat(cache.get("nonexistent")).isNull();
        }

        @Test
        @DisplayName("put écrase la valeur existante")
        void shouldOverwriteExistingValue() {
            cache.put("key1", "value1");
            cache.put("key1", "value2");
            assertThat(cache.get("key1")).isEqualTo("value2");
        }
    }

    @Nested
    @DisplayName("Politique LRU")
    class LruPolicy {

        @Test
        @DisplayName("évince l'entrée la plus ancienne quand capacité dépassée")
        void shouldEvictOldestWhenCapacityExceeded() {
            cache.put("k1", "v1");
            cache.put("k2", "v2");
            cache.put("k3", "v3");
            cache.put("k4", "v4");  // k1 doit être évincé
            assertThat(cache.get("k1")).isNull();
            assertThat(cache.get("k4")).isEqualTo("v4");
        }

        @Test
        @DisplayName("l'accès à une entrée la maintient dans le cache")
        void shouldKeepAccessedEntry() {
            cache.put("k1", "v1");
            cache.put("k2", "v2");
            cache.get("k1");  // k1 récemment utilisée
            cache.put("k3", "v3");
            cache.put("k4", "v4");  // k2 doit être évincé (pas k1)
            assertThat(cache.get("k1")).isEqualTo("v1");
            assertThat(cache.get("k2")).isNull();
        }
    }

    @Nested
    @DisplayName("Méthode peek")
    class PeekMethod {

        @Test
        @DisplayName("peek ne modifie pas l'ordre LRU")
        void shouldNotModifyLruOrder() {
            cache.put("k1", "v1");
            cache.put("k2", "v2");
            cache.peek("k1");  // ne marque pas comme récente
            cache.put("k3", "v3");
            cache.put("k4", "v4");  // k1 doit être évincé
            assertThat(cache.get("k1")).isNull();
        }
    }

    @Nested
    @DisplayName("Capacité")
    class Capacity {

        @Test
        @DisplayName("getMaxCapacity retourne la capacité configurée")
        void shouldReturnMaxCapacity() {
            assertThat(cache.getMaxCapacity()).isEqualTo(3);
        }

        @Test
        @DisplayName("la capacité par défaut est 512")
        void defaultCapacityShouldBe512() {
            ResourceCache<String, String> defaultCache = new ResourceCache<>();
            assertThat(defaultCache.getMaxCapacity()).isEqualTo(512);
        }

        @Test
        @DisplayName("rejette une capacité <= 0")
        void shouldRejectZeroOrNegativeCapacity() {
            try {
                new ResourceCache<String, String>(0);
                throw new AssertionError("Devrait lever une exception");
            } catch (IllegalArgumentException expected) {
                // OK
            }
        }
    }
}
