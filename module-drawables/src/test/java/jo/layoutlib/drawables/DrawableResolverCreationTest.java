package jo.layoutlib.drawables;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jo.layoutlib.resources.DimensionConverter;
import jo.layoutlib.resources.ResourceResolverImpl;
import jo.layoutlib.resources.ResourceTable;
import jo.layoutlib.resources.ResourceQualifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de création effective de drawables par
 * {@link DrawableResolverImpl} (point 4 : complétion des TODO selector /
 * vector).
 *
 * <p>En JVM (mockable android.jar), les constructeurs de
 * {@code StateListDrawable} / {@code ColorDrawable} ne lèvent pas : on peut
 * vérifier la structure du résultat. Les enfants sont des couleurs
 * (via {@code @color/} et littérales) pour rester indépendants de
 * {@code Resources}.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
@DisplayName("DrawableResolverImpl — création de StateListDrawable et replis")
class DrawableResolverCreationTest {

    private static ResourceResolverImpl resolverWithColors() {
        ResourceTable table = new ResourceTable();
        table.putColor("pressed_bg", "#FF6750A4", ResourceQualifier.DEFAULT);
        table.putColor("normal_bg", "#FFFFFFFF", ResourceQualifier.DEFAULT);
        return new ResourceResolverImpl(table);
    }

    @Test
    @DisplayName("un selector avec enfants @color/ produit un StateListDrawable")
    void createsSelectorFromColorReferences() {
        ResourceResolverImpl resolver = resolverWithColors();
        DrawableResolverImpl drawableResolver =
                new DrawableResolverImpl(resolver, new DimensionConverter(2f, 2f, 320f));

        Drawable drawable = drawableResolver.parse(
                "<selector xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                        + "  <item android:state_pressed=\"true\"\n"
                        + "        android:drawable=\"@color/pressed_bg\"/>\n"
                        + "  <item android:drawable=\"@color/normal_bg\"/>\n"
                        + "</selector>", null);

        assertThat(drawable).isInstanceOf(StateListDrawable.class);
    }

    @Test
    @DisplayName("un selector avec enfants littéraux (#hex) fonctionne aussi")
    void createsSelectorFromLiteralColors() {
        DrawableResolverImpl drawableResolver = new DrawableResolverImpl();

        Drawable drawable = drawableResolver.parse(
                "<selector xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                        + "  <item android:state_checked=\"true\"\n"
                        + "        android:drawable=\"#FFFF0000\"/>\n"
                        + "  <item android:drawable=\"#FF00FF00\"/>\n"
                        + "</selector>", null);

        assertThat(drawable).isInstanceOf(StateListDrawable.class);
    }

    @Test
    @DisplayName("un item non résolvable est ignoré, pas d'exception")
    void unresolvableItemIsIgnored() {
        ResourceResolverImpl resolver = resolverWithColors();
        DrawableResolverImpl drawableResolver =
                new DrawableResolverImpl(resolver, null);

        // Le 1er item est introuvable, le 2e existe → selector avec 1 état
        Drawable drawable = drawableResolver.parse(
                "<selector xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                        + "  <item android:state_pressed=\"true\"\n"
                        + "        android:drawable=\"@color/inconnu\"/>\n"
                        + "  <item android:drawable=\"@color/normal_bg\"/>\n"
                        + "</selector>", null);

        assertThat(drawable).isInstanceOf(StateListDrawable.class);
    }

    @Test
    @DisplayName("un selector sans aucun item résolvable retourne null")
    void selectorWithNoResolvableItemReturnsNull() {
        ResourceResolverImpl resolver = resolverWithColors();
        DrawableResolverImpl drawableResolver =
                new DrawableResolverImpl(resolver, null);

        Drawable drawable = drawableResolver.parse(
                "<selector xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                        + "  <item android:state_pressed=\"true\"\n"
                        + "        android:drawable=\"@color/inconnu\"/>\n"
                        + "</selector>", null);

        assertThat(drawable).isNull();
    }

    @Test
    @DisplayName("resolve(\"@color/x\") retourne un ColorDrawable")
    void resolvesColorReferenceToColorDrawable() {
        ResourceResolverImpl resolver = resolverWithColors();
        DrawableResolverImpl drawableResolver = new DrawableResolverImpl(resolver, null);

        Drawable drawable = drawableResolver.resolve("@color/normal_bg", null);

        assertThat(drawable).isInstanceOf(ColorDrawable.class);
    }

    @Test
    @DisplayName("un vector en JVM (pas de Resources réel) retourne null sans lever")
    void vectorOnJvmReturnsNullSafely() {
        DrawableResolverImpl drawableResolver = new DrawableResolverImpl();

        // context null → pas de Resources → null, pas d'exception
        Drawable drawable = drawableResolver.parse(
                "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    android:width=\"24dp\" android:height=\"24dp\"\n"
                        + "    android:viewportWidth=\"24\" android:viewportHeight=\"24\">\n"
                        + "  <path android:fillColor=\"#FF000000\" android:pathData=\"M0,0h24v24h-24z\"/>\n"
                        + "</vector>", null);

        assertThat(drawable).isNull();
    }

    @Test
    @DisplayName("un shape produit toujours un GradientDrawable (non-régression)")
    void shapeStillProducesGradientDrawable() {
        DrawableResolverImpl drawableResolver =
                new DrawableResolverImpl(null, new DimensionConverter(2f, 2f, 320f));

        Drawable drawable = drawableResolver.parse(
                "<shape xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    android:shape=\"rectangle\">\n"
                        + "  <solid android:color=\"#FF6750A4\"/>\n"
                        + "  <corners android:radius=\"8dp\"/>\n"
                        + "</shape>", null);

        assertThat(drawable).isInstanceOf(android.graphics.drawable.GradientDrawable.class);
    }
}
