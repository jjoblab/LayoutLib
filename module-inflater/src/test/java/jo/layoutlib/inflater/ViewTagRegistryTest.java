package jo.layoutlib.inflater;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitaires du {@link ViewTagRegistry}.
 *
 * <p>Valide la résolution des tags XML courts et longs vers les classes
 * Design et natives Android.</p>
 *
 * @author jo@Dev
 */
@DisplayName("ViewTagRegistry — résolution des tags XML")
class ViewTagRegistryTest {

    private ViewTagRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ViewTagRegistry(true);
    }

    @Nested
    @DisplayName("Résolution des tags courts")
    class ShortTagResolution {

        @Test
        @DisplayName("résout TextView vers TextViewDesign quand preferDesign=true")
        void shouldResolveTextViewToDesign() {
            String className = registry.resolveClassName("TextView");
            assertThat(className)
                    .isEqualTo("jo.layoutlib.design.widgets.TextViewDesign");
        }

        @Test
        @DisplayName("résout Button vers ButtonDesign")
        void shouldResolveButtonToDesign() {
            String className = registry.resolveClassName("Button");
            assertThat(className)
                    .isEqualTo("jo.layoutlib.design.buttons.ButtonDesign");
        }

        @Test
        @DisplayName("résout LinearLayout vers LinearLayoutDesign")
        void shouldResolveLinearLayoutToDesign() {
            String className = registry.resolveClassName("LinearLayout");
            assertThat(className)
                    .isEqualTo("jo.layoutlib.design.layouts.LinearLayoutDesign");
        }

        @Test
        @DisplayName("résout View vers ViewDesign")
        void shouldResolveViewToDesign() {
            String className = registry.resolveClassName("View");
            assertThat(className)
                    .isEqualTo("jo.layoutlib.design.widgets.ViewDesign");
        }

        @Test
        @DisplayName("retourne la classe native quand preferDesign=false")
        void shouldReturnNativeClassWhenNotPreferDesign() {
            ViewTagRegistry nativeRegistry = new ViewTagRegistry(false);
            String className = nativeRegistry.resolveClassName("TextView");
            assertThat(className).isEqualTo("android.widget.TextView");
        }
    }

    @Nested
    @DisplayName("Résolution des tags pleinement qualifiés")
    class QualifiedTagResolution {

        @Test
        @DisplayName("retourne tel quel un tag pleinement qualifié AndroidX")
        void shouldReturnAndroidXTagAsIs() {
            String tag = "androidx.cardview.widget.CardView";
            assertThat(registry.resolveClassName(tag)).isEqualTo(tag);
        }

        @Test
        @DisplayName("retourne tel quel un tag pleinement qualifié Material")
        void shouldReturnMaterialTagAsIs() {
            String tag = "com.google.android.material.button.MaterialButton";
            assertThat(registry.resolveClassName(tag)).isEqualTo(tag);
        }

        @Test
        @DisplayName("retourne tel quel un tag pleinement qualifié ConstraintLayout")
        void shouldReturnConstraintLayoutTagAsIs() {
            String tag = "androidx.constraintlayout.widget.ConstraintLayout";
            assertThat(registry.resolveClassName(tag)).isEqualTo(tag);
        }
    }

    @Nested
    @DisplayName("Tags spéciaux")
    class SpecialTags {

        @Test
        @DisplayName("retourne null pour <include>")
        void shouldReturnNullForInclude() {
            assertThat(registry.resolveClassName("include")).isNull();
        }

        @Test
        @DisplayName("retourne null pour <merge>")
        void shouldReturnNullForMerge() {
            assertThat(registry.resolveClassName("merge")).isNull();
        }

        @Test
        @DisplayName("retourne null pour <requestFocus>")
        void shouldReturnNullForRequestFocus() {
            assertThat(registry.resolveClassName("requestFocus")).isNull();
        }

        @Test
        @DisplayName("identifie <include> comme tag spécial")
        void shouldIdentifyIncludeAsSpecial() {
            assertThat(registry.isSpecialTag("include")).isTrue();
        }

        @Test
        @DisplayName("identifie <merge> comme tag spécial")
        void shouldIdentifyMergeAsSpecial() {
            assertThat(registry.isSpecialTag("merge")).isTrue();
        }

        @Test
        @DisplayName("ne considère pas <TextView> comme tag spécial")
        void shouldNotConsiderTextViewAsSpecial() {
            assertThat(registry.isSpecialTag("TextView")).isFalse();
        }
    }

    @Nested
    @DisplayName("Tags inconnus")
    class UnknownTags {

        @Test
        @DisplayName("tente la résolution dans android.widget pour un nom avec majuscule")
        void shouldTryWidgetPackageForUnknownTag() {
            String className = registry.resolveClassName("UnknownView");
            assertThat(className).isEqualTo("android.widget.UnknownView");
        }

        @Test
        @DisplayName("lève InflateException pour un tag null")
        void shouldThrowForNullTag() {
            assertThatThrownBy(() -> registry.resolveClassName(null))
                    .isInstanceOf(InflateException.class);
        }

        @Test
        @DisplayName("lève InflateException pour un tag vide")
        void shouldThrowForEmptyTag() {
            assertThatThrownBy(() -> registry.resolveClassName(""))
                    .isInstanceOf(InflateException.class);
        }
    }

    @Nested
    @DisplayName("Méthodes utilitaires")
    class UtilityMethods {

        @Test
        @DisplayName("hasDesignClass retourne true pour TextView")
        void shouldReturnTrueForTextViewDesignClass() {
            assertThat(registry.hasDesignClass("TextView")).isTrue();
        }

        @Test
        @DisplayName("hasDesignClass retourne false pour un tag inconnu")
        void shouldReturnFalseForUnknownDesignClass() {
            assertThat(registry.hasDesignClass("FooBarBaz")).isFalse();
        }

        @Test
        @DisplayName("getDesignClassMappings retourne une map non vide")
        void shouldReturnNonEmptyDesignMap() {
            assertThat(registry.getDesignClassMappings()).isNotEmpty();
        }

        @Test
        @DisplayName("getNativeClassMappings contient TextView")
        void shouldContainTextViewInNativeMap() {
            assertThat(registry.getNativeClassMappings()).containsKey("TextView");
        }

        @Test
        @DisplayName("register ajoute un nouveau mapping")
        void shouldRegisterNewMapping() {
            registry.register("MyCustomView",
                    "com.example.MyCustomView", "widgets.ViewDesign");
            assertThat(registry.resolveClassName("MyCustomView"))
                    .isEqualTo("jo.layoutlib.design.widgets.ViewDesign");
        }
    }
}
