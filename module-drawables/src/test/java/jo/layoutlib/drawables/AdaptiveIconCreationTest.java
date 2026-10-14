package jo.layoutlib.drawables;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import jo.layoutlib.resources.DimensionConverter;
import jo.layoutlib.resources.ResourceResolverImpl;
import jo.layoutlib.resources.ResourceTable;
import jo.layoutlib.resources.ResourceQualifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de création effective des icônes adaptatives par
 * {@link DrawableResolverImpl} (ADR 0116 — P4/L6).
 *
 * <p>En JVM (mockable android.jar), {@code Build.VERSION.SDK_INT} vaut 0 :
 * c'est le repli {@link LayerDrawable} (superposition background/foreground)
 * qui est exercé — la branche {@code AdaptiveIconDrawable} (API 26+/33+) ne
 * s'exécute que sur appareil. Les constructeurs ne lèvent pas en JVM : on
 * vérifie la structure du résultat.</p>
 *
 * @author jo@Dev
 * @since 3.0
 */
@DisplayName("DrawableResolverImpl — création des <adaptive-icon> (P4/L6)")
class AdaptiveIconCreationTest {

    private static final String NS =
            "xmlns:android=\"http://schemas.android.com/apk/res/android\"";

    private static ResourceResolverImpl resolverWithColors() {
        ResourceTable table = new ResourceTable();
        table.putColor("ic_launcher_bg", "#FF6750A4", ResourceQualifier.DEFAULT);
        return new ResourceResolverImpl(table);
    }

    /** Resolver avec convertisseur 2x — comme la preview CodeIDE. */
    private static DrawableResolverImpl adaptiveResolver() {
        return new DrawableResolverImpl(resolverWithColors(),
                new DimensionConverter(2f, 2f, 320f));
    }

    @Nested
    @DisplayName("Détection de type (parseConfig)")
    class TypeDetection {

        @Test
        @DisplayName("détecte un adaptive-icon")
        void detectsAdaptiveIcon() {
            Object config = adaptiveResolver().parseConfig(
                    "<adaptive-icon " + NS + "/>");
            assertThat(config).isInstanceOf(AdaptiveIconConfig.class);
        }
    }

    @Nested
    @DisplayName("Création (repli LayerDrawable en JVM)")
    class Creation {

        @Test
        @DisplayName("couches @color/ référencées produisent un drawable superposé")
        void createsFromColorRefs() {
            Drawable drawable = adaptiveResolver().parse(
                    "<adaptive-icon " + NS + ">\n"
                            + "  <background android:drawable=\"@color/ic_launcher_bg\"/>\n"
                            + "  <foreground android:drawable=\"@color/ic_launcher_bg\"/>\n"
                            + "</adaptive-icon>", null);
            assertThat(drawable).isInstanceOf(LayerDrawable.class);
        }

        @Test
        @DisplayName("couches littérales (#hex) fonctionnent aussi")
        void createsFromLiterals() {
            Drawable drawable = new DrawableResolverImpl().parse(
                    "<adaptive-icon " + NS + ">\n"
                            + "  <background android:drawable=\"#FF6750A4\"/>\n"
                            + "  <foreground android:drawable=\"#FF000000\"/>\n"
                            + "</adaptive-icon>", null);
            assertThat(drawable).isInstanceOf(LayerDrawable.class);
        }

        @Test
        @DisplayName("couche inline <color> en background fonctionne")
        void createsFromInlineColor() {
            Drawable drawable = new DrawableResolverImpl().parse(
                    "<adaptive-icon " + NS + ">\n"
                            + "  <background>\n"
                            + "    <color android:color=\"#FF6750A4\"/>\n"
                            + "  </background>\n"
                            + "  <foreground android:drawable=\"#FF000000\"/>\n"
                            + "</adaptive-icon>", null);
            assertThat(drawable).isInstanceOf(LayerDrawable.class);
        }

        @Test
        @DisplayName("couche inline <shape> en foreground fonctionne")
        void createsFromInlineShape() {
            Drawable drawable = new DrawableResolverImpl().parse(
                    "<adaptive-icon " + NS + ">\n"
                            + "  <background android:drawable=\"#FF6750A4\"/>\n"
                            + "  <foreground>\n"
                            + "    <shape android:shape=\"oval\">\n"
                            + "      <solid android:color=\"#FFFF0000\"/>\n"
                            + "    </shape>\n"
                            + "  </foreground>\n"
                            + "</adaptive-icon>", null);
            assertThat(drawable).isInstanceOf(LayerDrawable.class);
        }

        @Test
        @DisplayName("un adaptive-icon vide retourne null (placeholder)")
        void emptyAdaptiveIconReturnsNull() {
            Drawable drawable = adaptiveResolver().parse(
                    "<adaptive-icon " + NS + "/>", null);
            assertThat(drawable).isNull();
        }

        @Test
        @DisplayName("des couches non résolvables retournent null (placeholder)")
        void unresolvableLayersReturnNull() {
            Drawable drawable = new DrawableResolverImpl().parse(
                    "<adaptive-icon " + NS + ">\n"
                            + "  <background android:drawable=\"@drawable/inconnu\"/>\n"
                            + "  <foreground android:drawable=\"@drawable/autre\"/>\n"
                            + "</adaptive-icon>", null);
            assertThat(drawable).isNull();
        }

        @Test
        @DisplayName("une seule couche résolvable produit quand même un drawable")
        void singleResolvableLayerStillDraws() {
            Drawable drawable = adaptiveResolver().parse(
                    "<adaptive-icon " + NS + ">\n"
                            + "  <background android:drawable=\"@color/ic_launcher_bg\"/>\n"
                            + "  <foreground android:drawable=\"@drawable/inconnu\"/>\n"
                            + "</adaptive-icon>", null);
            assertThat(drawable).isInstanceOf(LayerDrawable.class);
        }

        @Test
        @DisplayName("la couche monochrome seule produit un drawable")
        void monochromeAloneStillDraws() {
            Drawable drawable = new DrawableResolverImpl().parse(
                    "<adaptive-icon " + NS + ">\n"
                            + "  <monochrome android:drawable=\"#FF000000\"/>\n"
                            + "</adaptive-icon>", null);
            assertThat(drawable).isInstanceOf(LayerDrawable.class);
        }
    }
}
