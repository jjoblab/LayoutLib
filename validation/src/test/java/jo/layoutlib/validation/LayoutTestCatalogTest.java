package jo.layoutlib.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires du {@link LayoutTestCatalog}.
 *
 * @author jo@Dev
 */
@DisplayName("LayoutTestCatalog — catalogue des 50 layouts")
class LayoutTestCatalogTest {

    @Nested
    @DisplayName("Catalogue complet")
    class FullCatalog {

        @Test
        @DisplayName("contient exactement 50 tests")
        void shouldContainExactly50Tests() {
            List<LayoutTestCase> tests = LayoutTestCatalog.getAllTests();
            assertThat(tests).hasSize(50);
        }

        @Test
        @DisplayName("tous les tests ont un nom non vide")
        void allTestsShouldHaveName() {
            List<LayoutTestCase> tests = LayoutTestCatalog.getAllTests();
            for (LayoutTestCase test : tests) {
                assertThat(test.getName()).isNotEmpty();
            }
        }

        @Test
        @DisplayName("tous les tests ont un XML non vide")
        void allTestsShouldHaveXml() {
            for (LayoutTestCase test : LayoutTestCatalog.getAllTests()) {
                assertThat(test.getXml()).isNotEmpty();
            }
        }

        @Test
        @DisplayName("tous les tests ont une catégorie")
        void allTestsShouldHaveCategory() {
            for (LayoutTestCase test : LayoutTestCatalog.getAllTests()) {
                assertThat(test.getCategory()).isNotEmpty();
            }
        }

        @Test
        @DisplayName("les noms sont uniques")
        void namesShouldBeUnique() {
            List<LayoutTestCase> tests = LayoutTestCatalog.getAllTests();
            long uniqueCount = tests.stream()
                    .map(LayoutTestCase::getName)
                    .distinct()
                    .count();
            assertThat(uniqueCount).isEqualTo(50);
        }
    }

    @Nested
    @DisplayName("Catégories")
    class Categories {

        @Test
        @DisplayName("getCategories retourne 8 catégories")
        void shouldReturn8Categories() {
            List<String> cats = LayoutTestCatalog.getCategories();
            assertThat(cats).hasSize(8);
            assertThat(cats).contains(
                    "basic", "nested", "resources", "drawables",
                    "theme", "special", "material", "complex");
        }

        @Test
        @DisplayName("la catégorie 'basic' contient 10 tests")
        void basicShouldContain10Tests() {
            List<LayoutTestCase> tests = LayoutTestCatalog.getAllTests();
            long basicCount = tests.stream()
                    .filter(t -> "basic".equals(t.getCategory()))
                    .count();
            assertThat(basicCount).isEqualTo(10);
        }

        @Test
        @DisplayName("la catégorie 'complex' contient 5 tests")
        void complexShouldContain5Tests() {
            long complexCount = LayoutTestCatalog.getAllTests().stream()
                    .filter(t -> "complex".equals(t.getCategory()))
                    .count();
            assertThat(complexCount).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("Contenu XML")
    class XmlContent {

        @Test
        @DisplayName("le premier test est un TextView simple")
        void firstTestShouldBeTextView() {
            LayoutTestCase first = LayoutTestCatalog.getAllTests().get(0);
            assertThat(first.getXml()).contains("TextView");
            assertThat(first.getXml()).contains("Hello");
        }

        @Test
        @DisplayName("le test de login contient EditText et Button")
        void loginTestShouldContainEditTextAndButton() {
            LayoutTestCase login = LayoutTestCatalog.getAllTests().stream()
                    .filter(t -> t.getName().contains("login"))
                    .findFirst()
                    .orElse(null);
            assertThat(login).isNotNull();
            assertThat(login.getXml()).contains("EditText");
            assertThat(login.getXml()).contains("Button");
            assertThat(login.getXml()).contains("Sign In");
        }

        @Test
        @DisplayName("le test dashboard contient une ScrollView")
        void dashboardTestShouldContainScrollView() {
            LayoutTestCase dashboard = LayoutTestCatalog.getAllTests().stream()
                    .filter(t -> t.getName().contains("dashboard"))
                    .findFirst()
                    .orElse(null);
            assertThat(dashboard).isNotNull();
            assertThat(dashboard.getXml()).contains("ScrollView");
        }
    }
}
