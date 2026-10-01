package jo.layoutlib.resources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires du {@link ResourceTable}.
 *
 * @author jo@Dev
 */
@DisplayName("ResourceTable — stockage des resources")
class ResourceTableTest {

    private ResourceTable table;

    @BeforeEach
    void setUp() {
        table = new ResourceTable();
    }

    @Nested
    @DisplayName("Stockage et récupération")
    class StorageAndRetrieval {

        @Test
        @DisplayName("stocke et récupère une couleur")
        void shouldStoreAndRetrieveColor() {
            table.putColor("red", "#FFFF0000", ResourceQualifier.DEFAULT);
            assertThat(table.getColor("red", ResourceQualifier.DEFAULT))
                    .isEqualTo("#FFFF0000");
        }

        @Test
        @DisplayName("stocke et récupère une chaîne")
        void shouldStoreAndRetrieveString() {
            table.putString("hello", "Hello", ResourceQualifier.DEFAULT);
            assertThat(table.getString("hello", ResourceQualifier.DEFAULT))
                    .isEqualTo("Hello");
        }

        @Test
        @DisplayName("stocke et récupère une dimension")
        void shouldStoreAndRetrieveDimen() {
            table.putDimen("margin", "16dp", ResourceQualifier.DEFAULT);
            assertThat(table.getDimen("margin", ResourceQualifier.DEFAULT))
                    .isEqualTo("16dp");
        }

        @Test
        @DisplayName("stocke et récupère un entier")
        void shouldStoreAndRetrieveInteger() {
            table.putInteger("max", 42, ResourceQualifier.DEFAULT);
            assertThat(table.getInteger("max", ResourceQualifier.DEFAULT))
                    .isEqualTo(42);
        }

        @Test
        @DisplayName("stocke et récupère un booléen")
        void shouldStoreAndRetrieveBoolean() {
            table.putBoolean("enabled", true, ResourceQualifier.DEFAULT);
            assertThat(table.getBoolean("enabled", ResourceQualifier.DEFAULT))
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("Résolution par qualifier")
    class QualifierBasedResolution {

        @Test
        @DisplayName("retourne la valeur par défaut si pas de variantes")
        void shouldReturnDefaultValueIfNoVariants() {
            table.putColor("primary", "#FF6750A4", ResourceQualifier.DEFAULT);
            ResourceQualifier nightTarget = new ResourceQualifier(true, false, 1);
            // Pas de variante nuit → retourne la valeur DEFAULT (compatible)
            assertThat(table.getColor("primary", nightTarget)).isEqualTo("#FF6750A4");
        }

        @Test
        @DisplayName("retourne la variante nuit en mode nuit")
        void shouldReturnNightVariantInNightMode() {
            table.putColor("primary", "#FF6750A4", ResourceQualifier.DEFAULT);
            ResourceQualifier nightQ = new ResourceQualifier(true, false, 1);
            table.putColor("primary", "#FFD0BCFF", nightQ);

            ResourceQualifier nightTarget = new ResourceQualifier(true, false, 1);
            assertThat(table.getColor("primary", nightTarget)).isEqualTo("#FFD0BCFF");
        }

        @Test
        @DisplayName("retourne la variante API 31 si target API >= 31")
        void shouldReturnV31VariantForApi31Target() {
            table.putColor("bg", "#FF000000", ResourceQualifier.DEFAULT);
            table.putColor("bg", "#FFFFFFFF", new ResourceQualifier(false, false, 31));

            ResourceQualifier target31 = new ResourceQualifier(false, false, 31);
            assertThat(table.getColor("bg", target31)).isEqualTo("#FFFFFFFF");

            ResourceQualifier target24 = new ResourceQualifier(false, false, 24);
            assertThat(table.getColor("bg", target24)).isEqualTo("#FF000000");
        }
    }

    @Nested
    @DisplayName("Méthodes utilitaires")
    class UtilityMethods {

        @Test
        @DisplayName("hasColor retourne true pour une couleur existante")
        void shouldReturnTrueForExistingColor() {
            table.putColor("red", "#FFFF0000", ResourceQualifier.DEFAULT);
            assertThat(table.hasColor("red")).isTrue();
            assertThat(table.hasColor("blue")).isFalse();
        }

        @Test
        @DisplayName("colorCount retourne le nombre de couleurs")
        void shouldReturnColorCount() {
            table.putColor("red", "#FFFF0000", ResourceQualifier.DEFAULT);
            table.putColor("blue", "#FF0000FF", ResourceQualifier.DEFAULT);
            assertThat(table.colorCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("clear vide toutes les tables")
        void shouldClearAllTables() {
            table.putColor("red", "#FFFF0000", ResourceQualifier.DEFAULT);
            table.putString("hello", "Hello", ResourceQualifier.DEFAULT);
            table.clear();
            assertThat(table.colorCount()).isEqualTo(0);
            assertThat(table.stringCount()).isEqualTo(0);
        }
    }
}
