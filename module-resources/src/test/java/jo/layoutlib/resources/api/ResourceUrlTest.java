package jo.layoutlib.resources.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires du {@link ResourceUrl}.
 *
 * @author jo@Dev
 */
@DisplayName("ResourceUrl — parsing de références @type/name")
class ResourceUrlTest {

    @Nested
    @DisplayName("Références normales")
    class NormalReferences {

        @Test
        @DisplayName("parse @color/foo")
        void shouldParseNormalReference() {
            ResourceUrl url = ResourceUrl.parse("@color/foo");
            assertThat(url).isNotNull();
            assertThat(url.getType()).isEqualTo(ResourceType.COLOR);
            assertThat(url.getName()).isEqualTo("foo");
            assertThat(url.getNamespace()).isEqualTo(ResourceNamespace.RES_AUTO);
            assertThat(url.isCreate()).isFalse();
            assertThat(url.isThemeAttr()).isFalse();
        }

        @Test
        @DisplayName("parse @android:color/holo_red")
        void shouldParseFrameworkReference() {
            ResourceUrl url = ResourceUrl.parse("@android:color/holo_red");
            assertThat(url).isNotNull();
            assertThat(url.getType()).isEqualTo(ResourceType.COLOR);
            assertThat(url.getName()).isEqualTo("holo_red");
            assertThat(url.getNamespace()).isEqualTo(ResourceNamespace.ANDROID);
            assertThat(url.isFramework()).isTrue();
        }

        @Test
        @DisplayName("parse @layout/main")
        void shouldParseLayoutReference() {
            ResourceUrl url = ResourceUrl.parse("@layout/main");
            assertThat(url).isNotNull();
            assertThat(url.getType()).isEqualTo(ResourceType.LAYOUT);
            assertThat(url.getName()).isEqualTo("main");
        }

        @Test
        @DisplayName("parse @drawable/icon")
        void shouldParseDrawableReference() {
            ResourceUrl url = ResourceUrl.parse("@drawable/icon");
            assertThat(url.getType()).isEqualTo(ResourceType.DRAWABLE);
            assertThat(url.getName()).isEqualTo("icon");
        }
    }

    @Nested
    @DisplayName("Création d'id")
    class CreateId {

        @Test
        @DisplayName("parse @+id/button comme création")
        void shouldParseCreateId() {
            ResourceUrl url = ResourceUrl.parse("@+id/button");
            assertThat(url).isNotNull();
            assertThat(url.getType()).isEqualTo(ResourceType.ID);
            assertThat(url.getName()).isEqualTo("button");
            assertThat(url.isCreate()).isTrue();
        }

        @Test
        @DisplayName("parse @id/button comme référence normale")
        void shouldParseIdReference() {
            ResourceUrl url = ResourceUrl.parse("@id/button");
            assertThat(url.isCreate()).isFalse();
            assertThat(url.getType()).isEqualTo(ResourceType.ID);
        }
    }

    @Nested
    @DisplayName("Attributs de thème")
    class ThemeAttributes {

        @Test
        @DisplayName("parse ?attr/colorPrimary")
        void shouldParseThemeAttr() {
            ResourceUrl url = ResourceUrl.parse("?attr/colorPrimary");
            assertThat(url).isNotNull();
            assertThat(url.isThemeAttr()).isTrue();
            assertThat(url.getType()).isEqualTo(ResourceType.ATTR);
            assertThat(url.getName()).isEqualTo("colorPrimary");
        }

        @Test
        @DisplayName("parse ?android:attr/windowBackground")
        void shouldParseFrameworkThemeAttr() {
            ResourceUrl url = ResourceUrl.parse("?android:attr/windowBackground");
            assertThat(url).isNotNull();
            assertThat(url.isThemeAttr()).isTrue();
            assertThat(url.isFramework()).isTrue();
            assertThat(url.getName()).isEqualTo("windowBackground");
        }

        @Test
        @DisplayName("parse ?colorPrimary (format court)")
        void shouldParseShortThemeAttr() {
            ResourceUrl url = ResourceUrl.parse("?colorPrimary");
            assertThat(url).isNotNull();
            assertThat(url.isThemeAttr()).isTrue();
            assertThat(url.getName()).isEqualTo("colorPrimary");
        }
    }

    @Nested
    @DisplayName("Cas d'erreur")
    class ErrorCases {

        @Test
        @DisplayName("retourne null pour null")
        void shouldReturnNullForNull() {
            assertThat(ResourceUrl.parse(null)).isNull();
        }

        @Test
        @DisplayName("retourne null pour vide")
        void shouldReturnNullForEmpty() {
            assertThat(ResourceUrl.parse("")).isNull();
        }

        @Test
        @DisplayName("retourne null pour une chaîne sans @ ni ?")
        void shouldReturnNullForNonReference() {
            assertThat(ResourceUrl.parse("hello")).isNull();
            assertThat(ResourceUrl.parse("#FF0000")).isNull();
        }

        @Test
        @DisplayName("retourne null pour un type inconnu")
        void shouldReturnNullForUnknownType() {
            assertThat(ResourceUrl.parse("@unknowntype/foo")).isNull();
        }

        @Test
        @DisplayName("retourne null pour un nom vide")
        void shouldReturnNullForEmptyName() {
            assertThat(ResourceUrl.parse("@color/")).isNull();
        }
    }

    @Nested
    @DisplayName("toString")
    class ToString {

        @Test
        @DisplayName("produit la bonne chaîne pour @color/foo")
        void shouldProduceCorrectString() {
            ResourceUrl url = ResourceUrl.parse("@color/foo");
            assertThat(url.toString()).isEqualTo("@color/foo");
        }

        @Test
        @DisplayName("produit @android:color/ pour framework")
        void shouldProduceFrameworkString() {
            ResourceUrl url = ResourceUrl.parse("@android:color/foo");
            assertThat(url.toString()).isEqualTo("@android:color/foo");
        }

        @Test
        @DisplayName("produit @+id/ pour création")
        void shouldProduceCreateString() {
            ResourceUrl url = ResourceUrl.parse("@+id/foo");
            assertThat(url.toString()).isEqualTo("@+id/foo");
        }
    }
}
