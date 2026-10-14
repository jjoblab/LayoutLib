package jo.layoutlib.drawables;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import jo.layoutlib.resources.DimensionConverter;
import jo.layoutlib.resources.ResourceResolverImpl;
import jo.layoutlib.resources.ResourceTable;
import jo.layoutlib.resources.ResourceQualifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Tests de création effective des animated-vectors par
 * {@link DrawableResolverImpl} (ADR 0116 — P5/L5).
 *
 * <p>En JVM (mockable android.jar), {@code Drawable.createFromXml()} retourne
 * null ({@code returnDefaultValues}) : c'est la résolution LayoutLib (stratégie
 * 2 — première frame = drawable sous-jacent) qui est exercée. Un vrai
 * {@code AnimatedVectorDrawable} natif ne s'obtient que sur appareil dont les
 * Resources résolvent {@code @anim/} et {@code @drawable/}.</p>
 *
 * @author jo@Dev
 * @since 3.1
 */
@DisplayName("DrawableResolverImpl — création des <animated-vector> (P5/L5)")
class AnimatedVectorCreationTest {

    private static final String NS =
            "xmlns:android=\"http://schemas.android.com/apk/res/android\"";

    private static ResourceResolverImpl resolverWithColors() {
        ResourceTable table = new ResourceTable();
        table.putColor("accent", "#FF6750A4", ResourceQualifier.DEFAULT);
        return new ResourceResolverImpl(table);
    }

    /** Resolver avec convertisseur 2x — comme la preview CodeIDE. */
    private static DrawableResolverImpl animatedResolver() {
        return new DrawableResolverImpl(resolverWithColors(),
                new DimensionConverter(2f, 2f, 320f));
    }

    @Nested
    @DisplayName("Détection de type (parseConfig)")
    class TypeDetection {

        @Test
        @DisplayName("détecte un animated-vector")
        void detectsAnimatedVector() {
            Object config = animatedResolver().parseConfig(
                    "<animated-vector " + NS + " android:drawable=\"@drawable/i\"/>");
            assertThat(config).isInstanceOf(AnimatedVectorConfig.class);
        }

        @Test
        @DisplayName("détecte un animated-vector vide et produit une config vide")
        void detectsEmptyAnimatedVector() {
            Object config = animatedResolver().parseConfig(
                    "<animated-vector " + NS + "/>");
            assertThat(config).isInstanceOf(AnimatedVectorConfig.class);
            assertThat(((AnimatedVectorConfig) config).hasDrawable()).isFalse();
        }
    }

    @Nested
    @DisplayName("Création — première frame (résolution LayoutLib en JVM)")
    class Creation {

        @Test
        @DisplayName("vector sous-jacent @color/ référencé produit sa première frame")
        void createsFromColorRef() {
            Drawable drawable = animatedResolver().parse(
                    "<animated-vector " + NS + " android:drawable=\"@color/accent\">\n"
                            + "  <target android:animation=\"@anim/a\" android:name=\"p\"/>\n"
                            + "</animated-vector>", null);
            assertThat(drawable).isInstanceOf(ColorDrawable.class);
        }

        @Test
        @DisplayName("vector sous-jacent littéral (#hex) fonctionne aussi")
        void createsFromLiteral() {
            Drawable drawable = new DrawableResolverImpl().parse(
                    "<animated-vector " + NS + " android:drawable=\"#FF6750A4\"/>", null);
            assertThat(drawable).isInstanceOf(ColorDrawable.class);
        }

        @Test
        @DisplayName("drawable inline embarqué (forme API 21-23) produit la frame")
        void createsFromInlineDrawable() {
            Drawable drawable = new DrawableResolverImpl().parse(
                    "<animated-vector " + NS + ">\n"
                            + "  <color android:color=\"#FF6750A4\"/>\n"
                            + "</animated-vector>", null);
            assertThat(drawable).isInstanceOf(ColorDrawable.class);
        }

        @Test
        @DisplayName("un vector inline en JVM retourne null SANS exception (createFromXml indisponible)")
        void inlineVectorReturnsNullInJvmWithoutException() {
            assertThatCode(() -> {
                Drawable drawable = new DrawableResolverImpl().parse(
                        "<animated-vector " + NS + ">\n"
                                + "  <vector android:width=\"24dp\" android:height=\"24dp\"\n"
                                + "          android:viewportWidth=\"24\"\n"
                                + "          android:viewportHeight=\"24\">\n"
                                + "    <path android:fillColor=\"#FF000000\"\n"
                                + "          android:pathData=\"M4,4 L20,20\"/>\n"
                                + "  </vector>\n"
                                + "</animated-vector>", null);
                assertThat(drawable).isNull();
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("un animated-vector sans vector désigné retourne null (placeholder)")
        void noDrawableReturnsNull() {
            Drawable drawable = animatedResolver().parse(
                    "<animated-vector " + NS + ">\n"
                            + "  <target android:animation=\"@anim/a\" android:name=\"p\"/>\n"
                            + "</animated-vector>", null);
            assertThat(drawable).isNull();
        }

        @Test
        @DisplayName("une référence non résolvable retourne null (placeholder)")
        void unresolvableRefReturnsNull() {
            Drawable drawable = animatedResolver().parse(
                    "<animated-vector " + NS
                            + " android:drawable=\"@drawable/inconnu\"/>", null);
            assertThat(drawable).isNull();
        }

        @Test
        @DisplayName("les cibles <target> sont ignorées : seule la frame est rendue")
        void targetsDoNotAffectFirstFrame() {
            Drawable withTargets = animatedResolver().parse(
                    "<animated-vector " + NS + " android:drawable=\"@color/accent\">\n"
                            + "  <target android:animation=\"@anim/manquante\" android:name=\"x\"/>\n"
                            + "</animated-vector>", null);
            Drawable withoutTargets = animatedResolver().parse(
                    "<animated-vector " + NS
                            + " android:drawable=\"@color/accent\"/>", null);
            assertThat(withTargets).isInstanceOf(ColorDrawable.class);
            assertThat(withoutTargets).isInstanceOf(ColorDrawable.class);
        }

        @Test
        @DisplayName("un XML invalide retourne null sans exception (placeholder)")
        void invalidXmlReturnsNull() {
            Drawable drawable = new DrawableResolverImpl().parse(
                    "<animated-vector " + NS + ">", null);
            assertThat(drawable).isNull();
        }

        @Test
        @DisplayName("un contexte JVM mockable ne casse pas la création (createFromXml → null → repli)")
        void mockContextFallsBackSilently() {
            // context non-null mais Resources mockables (getResources() → null,
            // createFromXml → null) → repli résolution LayoutLib sans exception
            android.content.Context context =
                    org.mockito.Mockito.mock(android.content.Context.class);
            Drawable drawable = animatedResolver().parse(
                    "<animated-vector " + NS
                            + " android:drawable=\"#FF000000\"/>",
                    context);
            assertThat(drawable).isInstanceOf(ColorDrawable.class);
        }
    }
}
