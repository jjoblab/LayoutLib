package jo.layoutlib.inflater.bridge.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitaires du {@link DynamicIdMap}.
 *
 * @author jo@Dev
 */
@DisplayName("DynamicIdMap — génération d'ids uniques")
class DynamicIdMapTest {

    private DynamicIdMap map;

    @BeforeEach
    void setUp() {
        map = new DynamicIdMap();
    }

    @Nested
    @DisplayName("Génération d'ids")
    class IdGeneration {

        @Test
        @DisplayName("génère un id unique pour une nouvelle clé")
        void shouldGenerateIdForNewKey() {
            int id = map.getId("button_save");
            assertThat(id).isGreaterThanOrEqualTo(DynamicIdMap.ID_START);
        }

        @Test
        @DisplayName("retourne le même id pour la même clé")
        void shouldReturnSameIdForSameKey() {
            int id1 = map.getId("button_save");
            int id2 = map.getId("button_save");
            assertThat(id2).isEqualTo(id1);
        }

        @Test
        @DisplayName("génère des ids différents pour des clés différentes")
        void shouldGenerateDifferentIdsForDifferentKeys() {
            int id1 = map.getId("button_a");
            int id2 = map.getId("button_b");
            assertThat(id2).isNotEqualTo(id1);
        }

        @Test
        @DisplayName("incrémente séquentiellement")
        void shouldIncrementSequentially() {
            int id1 = map.getId("a");
            int id2 = map.getId("b");
            int id3 = map.getId("c");
            assertThat(id2).isEqualTo(id1 + 1);
            assertThat(id3).isEqualTo(id2 + 1);
        }
    }

    @Nested
    @DisplayName("Recherche")
    class Lookup {

        @Test
        @DisplayName("getKey retourne la clé pour un id existant")
        void shouldReturnKeyForExistingId() {
            int id = map.getId("button_save");
            assertThat(map.getKey(id)).isEqualTo("button_save");
        }

        @Test
        @DisplayName("getKey retourne null pour un id inexistant")
        void shouldReturnNullForUnknownId() {
            assertThat(map.getKey(99999)).isNull();
        }

        @Test
        @DisplayName("hasId retourne true pour une clé existante")
        void shouldReturnTrueForExistingKey() {
            map.getId("button_save");
            assertThat(map.hasId("button_save")).isTrue();
            assertThat(map.hasId("unknown")).isFalse();
        }
    }

    @Nested
    @DisplayName("Gestion")
    class Management {

        @Test
        @DisplayName("size retourne le nombre d'ids")
        void shouldReturnSize() {
            map.getId("a");
            map.getId("b");
            assertThat(map.size()).isEqualTo(2);
        }

        @Test
        @DisplayName("clear vide la map et réinitialise le compteur")
        void shouldClearMap() {
            map.getId("a");
            map.clear();
            assertThat(map.size()).isEqualTo(0);
            int newId = map.getId("a");
            assertThat(newId).isEqualTo(DynamicIdMap.ID_START);
        }

        @Test
        @DisplayName("peekNextId retourne le prochain id sans le consommer")
        void shouldPeekNextId() {
            int peeked = map.peekNextId();
            int actual = map.getId("a");
            assertThat(actual).isEqualTo(peeked);
        }

        @Test
        @DisplayName("rejette une clé null ou vide")
        void shouldRejectNullOrEmptyKey() {
            assertThatThrownBy(() -> map.getId(null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> map.getId(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
