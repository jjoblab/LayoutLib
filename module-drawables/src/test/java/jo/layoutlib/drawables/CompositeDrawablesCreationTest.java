package jo.layoutlib.drawables;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import jo.layoutlib.resources.DimensionConverter;
import jo.layoutlib.resources.ResourceResolverImpl;
import jo.layoutlib.resources.ResourceTable;
import jo.layoutlib.resources.ResourceQualifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de création effective des drawables composites par
 * {@link DrawableResolverImpl} (ADR 0116 — P0/L3).
 *
 * <p>Concerne {@code <layer-list>}, {@code <ripple>}, {@code <inset>} et
 * {@code <bitmap>}, auparavant retournés {@code null}
 * (DrawableResolverImpl v2.2.0). En JVM (mockable android.jar), les
 * constructeurs de LayerDrawable/RippleDrawable/InsetDrawable ne lèvent pas :
 * on vérifie la structure du résultat. Les enfants utilisent des couleurs et
 * des shapes inline pour rester indépendants de {@code Resources} ; le
 * décodage de fichiers image reste hors périmètre JVM (null attendu).</p>
 *
 * @author jo@Dev
 * @since 3.0
 */
@DisplayName("DrawableResolverImpl — création des drawables composites (P0/L3)")
class CompositeDrawablesCreationTest {

    private static final String NS =
            "xmlns:android=\"http://schemas.android.com/apk/res/android\"";

    private static ResourceResolverImpl resolverWithColors() {
        ResourceTable table = new ResourceTable();
        table.putColor("fond_sombre", "#FF6750A4", ResourceQualifier.DEFAULT);
        table.putColor("fond_clair", "#FFF2EDF7", ResourceQualifier.DEFAULT);
        return new ResourceResolverImpl(table);
    }

    /** Resolver avec convertisseur 2x — comme la preview CodeIDE. */
    private static DrawableResolverImpl compositeResolver() {
        return new DrawableResolverImpl(resolverWithColors(),
                new DimensionConverter(2f, 2f, 320f));
    }

    @Nested
    @DisplayName("Détection de type (parseConfig)")
    class TypeDetection {

        @Test
        @DisplayName("détecte un layer-list")
        void detectsLayerList() {
            Object config = compositeResolver().parseConfig(
                    "<layer-list " + NS + "/>");
            assertThat(config).isInstanceOf(LayerListConfig.class);
        }

        @Test
        @DisplayName("détecte un ripple")
        void detectsRipple() {
            Object config = compositeResolver().parseConfig(
                    "<ripple " + NS + " android:color=\"#FF4081\"/>");
            assertThat(config).isInstanceOf(RippleConfig.class);
        }

        @Test
        @DisplayName("détecte un inset")
        void detectsInset() {
            Object config = compositeResolver().parseConfig(
                    "<inset " + NS + "/>");
            assertThat(config).isInstanceOf(InsetConfig.class);
        }

        @Test
        @DisplayName("détecte un bitmap")
        void detectsBitmap() {
            Object config = compositeResolver().parseConfig(
                    "<bitmap " + NS + "/>");
            assertThat(config).isInstanceOf(BitmapConfig.class);
        }
    }

    @Nested
    @DisplayName("<layer-list> → LayerDrawable")
    class LayerListCreation {

        @Test
        @DisplayName("des items @color/ produisent un LayerDrawable")
        void createsLayerDrawableFromColorRefs() {
            Drawable drawable = compositeResolver().parse(
                    "<layer-list " + NS + ">\n"
                            + "  <item android:drawable=\"@color/fond_sombre\"/>\n"
                            + "  <item android:left=\"8dp\" android:top=\"8dp\"\n"
                            + "        android:drawable=\"@color/fond_clair\"/>\n"
                            + "</layer-list>", null);

            assertThat(drawable).isInstanceOf(LayerDrawable.class);
        }

        @Test
        @DisplayName("des items littéraux (#hex) fonctionnent aussi")
        void createsLayerDrawableFromLiterals() {
            Drawable drawable = new DrawableResolverImpl().parse(
                    "<layer-list " + NS + ">\n"
                            + "  <item android:drawable=\"#FFFF0000\"/>\n"
                            + "  <item android:drawable=\"#8000FF00\"/>\n"
                            + "</layer-list>", null);

            assertThat(drawable).isInstanceOf(LayerDrawable.class);
        }

        @Test
        @DisplayName("un shape inline est résolu récursivement par calque")
        void createsLayerDrawableWithInlineShape() {
            Drawable drawable = compositeResolver().parse(
                    "<layer-list " + NS + ">\n"
                            + "  <item>\n"
                            + "    <shape android:shape=\"rectangle\">\n"
                            + "      <solid android:color=\"#FF6750A4\"/>\n"
                            + "    </shape>\n"
                            + "  </item>\n"
                            + "  <item android:drawable=\"@color/fond_clair\"/>\n"
                            + "</layer-list>", null);

            assertThat(drawable).isInstanceOf(LayerDrawable.class);
        }

        @Test
        @DisplayName("un calque non résolvable est ignoré, pas d'exception")
        void unresolvableLayerIsSkipped() {
            Drawable drawable = compositeResolver().parse(
                    "<layer-list " + NS + ">\n"
                            + "  <item android:drawable=\"@color/inconnu\"/>\n"
                            + "  <item android:drawable=\"@color/fond_clair\"/>\n"
                            + "</layer-list>", null);

            assertThat(drawable).isInstanceOf(LayerDrawable.class);
        }

        @Test
        @DisplayName("aucun calque résolvable → null")
        void noResolvableLayerReturnsNull() {
            Drawable drawable = compositeResolver().parse(
                    "<layer-list " + NS + ">\n"
                            + "  <item android:drawable=\"@color/inconnu\"/>\n"
                            + "</layer-list>", null);

            assertThat(drawable).isNull();
        }

        @Test
        @DisplayName("un layer-list sans aucun item → null")
        void emptyLayerListReturnsNull() {
            Drawable drawable = compositeResolver().parse(
                    "<layer-list " + NS + "/>", null);

            assertThat(drawable).isNull();
        }
    }

    @Nested
    @DisplayName("<ripple> → RippleDrawable")
    class RippleCreation {

        @Test
        @DisplayName("couleur + contenu + masque produisent un RippleDrawable")
        void createsRippleDrawable() {
            Drawable drawable = compositeResolver().parse(
                    "<ripple " + NS + " android:color=\"#FF4081\">\n"
                            + "  <item android:id=\"@android:id/mask\">\n"
                            + "    <color android:color=\"#FFFFFF\"/>\n"
                            + "  </item>\n"
                            + "  <item android:drawable=\"@color/fond_sombre\"/>\n"
                            + "</ripple>", null);

            assertThat(drawable).isInstanceOf(RippleDrawable.class);
        }

        @Test
        @DisplayName("un ripple couleur seule (pas de contenu) est valide")
        void createsRippleWithoutContent() {
            Drawable drawable = compositeResolver().parse(
                    "<ripple " + NS + " android:color=\"#FF4081\"/>", null);

            assertThat(drawable).isInstanceOf(RippleDrawable.class);
        }

        @Test
        @DisplayName("un contenu inline shape est résolu")
        void createsRippleWithInlineShapeContent() {
            Drawable drawable = compositeResolver().parse(
                    "<ripple " + NS + " android:color=\"#FF4081\">\n"
                            + "  <item>\n"
                            + "    <shape android:shape=\"rectangle\">\n"
                            + "      <solid android:color=\"#FFF2EDF7\"/>\n"
                            + "    </shape>\n"
                            + "  </item>\n"
                            + "</ripple>", null);

            assertThat(drawable).isInstanceOf(RippleDrawable.class);
        }

        @Test
        @DisplayName("sans couleur, le ripple retourne null (aucun effet visible)")
        void rippleWithoutColorReturnsNull() {
            Drawable drawable = compositeResolver().parse(
                    "<ripple " + NS + "/>", null);

            assertThat(drawable).isNull();
        }

        @Test
        @DisplayName("une couleur invalide est tolérée → null")
        void rippleWithInvalidColorReturnsNull() {
            Drawable drawable = compositeResolver().parse(
                    "<ripple " + NS + " android:color=\"pas_une_couleur\"/>", null);

            assertThat(drawable).isNull();
        }
    }

    @Nested
    @DisplayName("<inset> → InsetDrawable")
    class InsetCreation {

        @Test
        @DisplayName("un enfant @color avec insets produit un InsetDrawable")
        void createsInsetDrawableFromColorRef() {
            Drawable drawable = compositeResolver().parse(
                    "<inset " + NS + " android:drawable=\"@color/fond_sombre\"\n"
                            + "       android:insetLeft=\"8dp\""
                            + " android:insetTop=\"4dp\"\n"
                            + "       android:insetRight=\"8dp\""
                            + " android:insetBottom=\"4dp\"/>", null);

            assertThat(drawable).isInstanceOf(InsetDrawable.class);
        }

        @Test
        @DisplayName("un enfant littéral fonctionne aussi")
        void createsInsetDrawableFromLiteral() {
            Drawable drawable = new DrawableResolverImpl().parse(
                    "<inset " + NS + " android:drawable=\"#FFFF0000\"\n"
                            + "       android:insetLeft=\"16dp\"/>", null);

            assertThat(drawable).isInstanceOf(InsetDrawable.class);
        }

        @Test
        @DisplayName("un shape inline est encapsulé")
        void createsInsetDrawableWithInlineShape() {
            Drawable drawable = compositeResolver().parse(
                    "<inset " + NS + " android:insetLeft=\"8dp\">\n"
                            + "  <shape android:shape=\"oval\">\n"
                            + "    <solid android:color=\"#FF6750A4\"/>\n"
                            + "  </shape>\n"
                            + "</inset>", null);

            assertThat(drawable).isInstanceOf(InsetDrawable.class);
        }

        @Test
        @DisplayName("sans drawable encapsulé → null (rien à encadrer)")
        void insetWithoutDrawableReturnsNull() {
            Drawable drawable = compositeResolver().parse(
                    "<inset " + NS + " android:insetLeft=\"8dp\"/>", null);

            assertThat(drawable).isNull();
        }

        @Test
        @DisplayName("un enfant non résolvable → null sans exception")
        void insetWithUnresolvableChildReturnsNull() {
            Drawable drawable = compositeResolver().parse(
                    "<inset " + NS + " android:drawable=\"@color/inconnu\"/>", null);

            assertThat(drawable).isNull();
        }
    }

    @Nested
    @DisplayName("<bitmap> → ColorDrawable / BitmapDrawable")
    class BitmapCreation {

        @Test
        @DisplayName("une source couleur littérale produit un ColorDrawable")
        void createsColorDrawableFromLiteralSource() {
            Drawable drawable = new DrawableResolverImpl().parse(
                    "<bitmap " + NS + " android:src=\"#FF6750A4\"/>", null);

            assertThat(drawable).isInstanceOf(ColorDrawable.class);
        }

        @Test
        @DisplayName("une source @color/ est résolue en ColorDrawable")
        void createsColorDrawableFromColorRef() {
            Drawable drawable = compositeResolver().parse(
                    "<bitmap " + NS + " android:src=\"@color/fond_sombre\"/>", null);

            assertThat(drawable).isInstanceOf(ColorDrawable.class);
        }

        @Test
        @DisplayName("sans source → null")
        void bitmapWithoutSourceReturnsNull() {
            Drawable drawable = compositeResolver().parse(
                    "<bitmap " + NS + "/>", null);

            assertThat(drawable).isNull();
        }

        @Test
        @DisplayName("une source @drawable/ image en JVM (pas de Resources)"
                + " retourne null sans lever")
        void imageSourceOnJvmReturnsNullSafely() {
            // Sans fichier réel ni Resources, le décodage échoue silencieusement
            Drawable drawable = compositeResolver().parse(
                    "<bitmap " + NS + " android:src=\"@drawable/introuvable\"/>", null);

            assertThat(drawable).isNull();
        }
    }

    @Nested
    @DisplayName("Non-régressions")
    class NonRegressions {

        @Test
        @DisplayName("un shape nu produit toujours un GradientDrawable")
        void shapeStillWorks() {
            Drawable drawable = new DrawableResolverImpl(null,
                    new DimensionConverter(2f, 2f, 320f)).parse(
                    "<shape " + NS + " android:shape=\"rectangle\">\n"
                            + "  <solid android:color=\"#FF6750A4\"/>\n"
                            + "</shape>", null);

            assertThat(drawable).isInstanceOf(GradientDrawable.class);
        }

        @Test
        @DisplayName("un layer-list d'un seul calque couleur reste un LayerDrawable")
        void singleLayerLayerList() {
            Drawable drawable = compositeResolver().parse(
                    "<layer-list " + NS + ">\n"
                            + "  <item android:drawable=\"@color/fond_sombre\"/>\n"
                            + "</layer-list>", null);

            assertThat(drawable).isInstanceOf(LayerDrawable.class);
        }
    }
}
