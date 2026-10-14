package jo.layoutlib.inflater;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.view.View;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.Map;

import jo.layoutlib.drawables.DrawableResolver;
import jo.layoutlib.resources.DimensionConverter;
import jo.layoutlib.themes.ThemeResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests de la résolution {@code ?attr/} vers des drawables dans
 * {@link AttributeApplier} (ADR 0116 — L7).
 *
 * <p>Avant L7, {@code resolveThemeAttr()} ne conservait que les couleurs
 * ({@code Integer}) : un attribut de thème valant {@code @drawable/…}
 * (ex. {@code ?attr/selectableItemBackground}) était silencieusement jeté.
 * La nouvelle chaîne {@code resolveThemeDrawableAttr()} suit la référence
 * jusqu'au drawable et l'applique aux background/src/foreground.</p>
 *
 * <p>Le ThemeResolver et le DrawableResolver sont simulés par des stubs :
 * on teste le câblage de l'applier (couleur d'abord, drawable ensuite,
 * repli silencieux), pas le parsing XML (couvert par module-drawables /
 * module-themes).</p>
 *
 * @author jo@Dev
 * @since 3.1
 */
@DisplayName("AttributeApplier — ?attr/ vers drawables (L7)")
class AttributeApplierThemeDrawableTest {

    private Context context;
    private DimensionConverter converter;

    /** Stub de ThemeResolver : ?attr/name → valeur (réf String, Integer…). */
    private static class StubThemeResolver implements ThemeResolver {
        final Map<String, Object> attrs = new HashMap<>();
        int resolveCalls;

        @Override
        public void setTheme(String themeName) {
        }

        @Override
        public Object resolveAttr(String reference) {
            resolveCalls++;
            return attrs.get(reference);
        }

        @Override
        public Integer getColorAttr(String attrName) {
            return null;
        }

        @Override
        public Float getDimensionAttr(String attrName) {
            return null;
        }

        @Override
        public void setNightMode(boolean nightMode) {
        }

        @Override
        public String getParentStyle(String styleName) {
            return null;
        }

        @Override
        public String getStyleAttribute(String styleName, String attrName) {
            return null;
        }

        @Override
        public void clearCache() {
        }
    }

    /** Stub de DrawableResolver : renvoie les drawables enregistrés. */
    private static class StubDrawableResolver implements DrawableResolver {
        final Map<String, Drawable> drawables = new HashMap<>();

        @Override
        public Drawable resolve(String reference, Context context) {
            return drawables.get(reference);
        }

        @Override
        public Drawable parse(String xml, Context context) {
            return null;
        }

        @Override
        public void clearCache() {
        }
    }

    @BeforeEach
    void setUp() {
        context = mock(Context.class);
        converter = new DimensionConverter(2.75f, 2.75f, 420f);
    }

    @Nested
    @DisplayName("resolveThemeDrawableAttr — suivre ?attr/ jusqu'au drawable")
    class ResolveThemeDrawableAttr {

        @Test
        @DisplayName("?attr/ dont la valeur du thème est @drawable/ → drawable résolu")
        void resolvesThemeAttrToProjectDrawable() {
            StubThemeResolver themeResolver = new StubThemeResolver();
            themeResolver.attrs.put("?attr/selectableItemBackground",
                    "@drawable/ripple_sombre");
            StubDrawableResolver drawableResolver = new StubDrawableResolver();
            ColorDrawable expected = new ColorDrawable(0xFF6750A4);
            drawableResolver.drawables.put("@drawable/ripple_sombre", expected);
            AttributeApplier applier = new AttributeApplier(context, converter);
            applier.setThemeResolver(themeResolver);
            applier.setDrawableResolver(drawableResolver);

            Drawable result = applier.resolveThemeDrawableAttr(
                    "?attr/selectableItemBackground");

            assertThat(result).isSameAs(expected);
        }

        @Test
        @DisplayName("?android:attr/ est accepté (attribut framework)")
        void acceptsFrameworkAttrNamespace() {
            StubThemeResolver themeResolver = new StubThemeResolver();
            themeResolver.attrs.put("?android:attr/colorBackground",
                    "@drawable/fond_neutre");
            StubDrawableResolver drawableResolver = new StubDrawableResolver();
            ColorDrawable expected = new ColorDrawable(0xFFF2EDF7);
            drawableResolver.drawables.put("@drawable/fond_neutre", expected);
            AttributeApplier applier = new AttributeApplier(context, converter);
            applier.setThemeResolver(themeResolver);
            applier.setDrawableResolver(drawableResolver);

            Drawable result = applier.resolveThemeDrawableAttr(
                    "?android:attr/colorBackground");

            assertThat(result).isSameAs(expected);
        }

        @Test
        @DisplayName("une valeur Integer (couleur) ne devient pas un drawable ici → null")
        void integerColorYieldsNull() {
            StubThemeResolver themeResolver = new StubThemeResolver();
            themeResolver.attrs.put("?attr/colorPrimary", 0xFF6750A4);
            AttributeApplier applier = new AttributeApplier(context, converter);
            applier.setThemeResolver(themeResolver);

            // Les couleurs restent le domaine de resolveThemeAttr ; ici on
            // ne fabrique pas de ColorDrawable (pas de Resources réel en JVM)
            assertThat(applier.resolveThemeDrawableAttr("?attr/colorPrimary"))
                    .isNull();
        }

        @Test
        @DisplayName("attribut absent du thème → null sans exception")
        void unknownAttrReturnsNull() {
            AttributeApplier applier = new AttributeApplier(context, converter);
            applier.setThemeResolver(new StubThemeResolver());

            assertThat(applier.resolveThemeDrawableAttr("?attr/inexistant"))
                    .isNull();
        }

        @Test
        @DisplayName("référence null ou vide → null")
        void nullRefReturnsNull() {
            AttributeApplier applier = new AttributeApplier(context, converter);
            applier.setThemeResolver(new StubThemeResolver());

            assertThat(applier.resolveThemeDrawableAttr(null)).isNull();
            assertThat(applier.resolveThemeDrawableAttr("")).isNull();
        }

        @Test
        @DisplayName("sans ThemeResolver connecté → null (fallback natif inert en JVM)")
        void noThemeResolverReturnsNull() {
            AttributeApplier applier = new AttributeApplier(context, converter);

            // mock(Context) : getResources() → null → repli natif court-circuité
            assertThat(applier.resolveThemeDrawableAttr("?attr/selectableItemBackground"))
                    .isNull();
        }

        @Test
        @DisplayName("une exception du ThemeResolver est attrapée → null")
        void themeResolverExceptionIsCaught() {
            ThemeResolver exploding = new StubThemeResolver() {
                @Override
                public Object resolveAttr(String reference) {
                    throw new IllegalStateException("boom");
                }
            };
            AttributeApplier applier = new AttributeApplier(context, converter);
            applier.setThemeResolver(exploding);

            assertThat(applier.resolveThemeDrawableAttr("?attr/whatever")).isNull();
        }
    }

    @Nested
    @DisplayName("applyBackground — ?attr/ couleur ou drawable")
    class ApplyBackground {

        @Test
        @DisplayName("?attr/ vers un drawable → setBackground(drawable)")
        void appliesThemeDrawableAsBackground() {
            StubThemeResolver themeResolver = new StubThemeResolver();
            themeResolver.attrs.put("?attr/selectableItemBackground",
                    "@drawable/ripple_sombre");
            StubDrawableResolver drawableResolver = new StubDrawableResolver();
            ColorDrawable expected = new ColorDrawable(0xFF6750A4);
            drawableResolver.drawables.put("@drawable/ripple_sombre", expected);
            AttributeApplier applier = new AttributeApplier(context, converter);
            applier.setThemeResolver(themeResolver);
            applier.setDrawableResolver(drawableResolver);
            View view = mock(View.class);

            applier.applyBackground(view, "?attr/selectableItemBackground");

            verify(view).setBackground(expected);
        }

        @Test
        @DisplayName("?attr/ vers une couleur → setBackground(ColorDrawable) (régression)")
        void themeColorStillAppliesColorDrawable() {
            StubThemeResolver themeResolver = new StubThemeResolver();
            themeResolver.attrs.put("?attr/colorPrimary", 0xFF6750A4);
            AttributeApplier applier = new AttributeApplier(context, converter);
            applier.setThemeResolver(themeResolver);
            View view = mock(View.class);

            applier.applyBackground(view, "?attr/colorPrimary");

            ArgumentCaptor<Drawable> captor = ArgumentCaptor.forClass(Drawable.class);
            verify(view).setBackground(captor.capture());
            assertThat(captor.getValue()).isInstanceOf(ColorDrawable.class);
        }

        @Test
        @DisplayName("?attr/ non résolvable → aucun background, pas d'exception")
        void unresolvedThemeAttrLeavesBackgroundUntouched() {
            AttributeApplier applier = new AttributeApplier(context, converter);
            applier.setThemeResolver(new StubThemeResolver());
            View view = mock(View.class);

            applier.applyBackground(view, "?attr/inconnu");

            verify(view, never()).setBackground(any(Drawable.class));
        }
    }

    @Nested
    @DisplayName("applyImageSrc — android:src=\"?attr/…\"")
    class ApplyImageSrc {

        @Test
        @DisplayName("?attr/ vers un drawable → setImageDrawable")
        void appliesThemeDrawableAsSrc() {
            StubThemeResolver themeResolver = new StubThemeResolver();
            themeResolver.attrs.put("?attr/icThemed", "@drawable/ic_accueil");
            StubDrawableResolver drawableResolver = new StubDrawableResolver();
            ColorDrawable expected = new ColorDrawable(0xFF000000);
            drawableResolver.drawables.put("@drawable/ic_accueil", expected);
            AttributeApplier applier = new AttributeApplier(context, converter);
            applier.setThemeResolver(themeResolver);
            applier.setDrawableResolver(drawableResolver);
            ImageView iv = mock(ImageView.class);

            applier.applyImageSrc(iv, "?attr/icThemed");

            verify(iv).setImageDrawable(expected);
        }

        @Test
        @DisplayName("@drawable/ direct reste fonctionnel (régression)")
        void directDrawableRefStillWorks() {
            StubDrawableResolver drawableResolver = new StubDrawableResolver();
            ColorDrawable expected = new ColorDrawable(0xFF00FF00);
            drawableResolver.drawables.put("@drawable/logo", expected);
            AttributeApplier applier = new AttributeApplier(context, converter);
            applier.setDrawableResolver(drawableResolver);
            ImageView iv = mock(ImageView.class);

            applier.applyImageSrc(iv, "@drawable/logo");

            verify(iv).setImageDrawable(expected);
        }

        @Test
        @DisplayName("?attr/ non résolvable → aucune image, pas d'exception")
        void unresolvedThemeAttrLeavesSrcUntouched() {
            AttributeApplier applier = new AttributeApplier(context, converter);
            applier.setThemeResolver(new StubThemeResolver());
            ImageView iv = mock(ImageView.class);

            applier.applyImageSrc(iv, "?attr/inconnu");

            verify(iv, never()).setImageDrawable(any(Drawable.class));
        }
    }

    @Nested
    @DisplayName("applyForeground — ?attr/ et @drawable/")
    class ApplyForeground {

        @Test
        @DisplayName("?attr/ vers un drawable → setForeground sur FrameLayout")
        void appliesThemeDrawableAsForeground() {
            StubThemeResolver themeResolver = new StubThemeResolver();
            themeResolver.attrs.put("?attr/overlay", "@drawable/voile");
            StubDrawableResolver drawableResolver = new StubDrawableResolver();
            ColorDrawable expected = new ColorDrawable(0x80000000);
            drawableResolver.drawables.put("@drawable/voile", expected);
            AttributeApplier applier = new AttributeApplier(context, converter);
            applier.setThemeResolver(themeResolver);
            applier.setDrawableResolver(drawableResolver);
            FrameLayout frame = mock(FrameLayout.class);

            applier.applyForeground(frame, "?attr/overlay");

            verify(frame).setForeground(expected);
        }

        @Test
        @DisplayName("@drawable/ direct → setForeground sur FrameLayout")
        void appliesDirectDrawableAsForeground() {
            StubDrawableResolver drawableResolver = new StubDrawableResolver();
            ColorDrawable expected = new ColorDrawable(0xFF112233);
            drawableResolver.drawables.put("@drawable/bordure", expected);
            AttributeApplier applier = new AttributeApplier(context, converter);
            applier.setDrawableResolver(drawableResolver);
            FrameLayout frame = mock(FrameLayout.class);

            applier.applyForeground(frame, "@drawable/bordure");

            verify(frame).setForeground(expected);
        }

        @Test
        @DisplayName("vue non-FrameLayout → attribut consommé sans bruit")
        void nonFrameLayoutIsIgnoredSilently() {
            StubThemeResolver themeResolver = new StubThemeResolver();
            themeResolver.attrs.put("?attr/overlay", "@drawable/voile");
            StubDrawableResolver drawableResolver = new StubDrawableResolver();
            drawableResolver.drawables.put("@drawable/voile",
                    new ColorDrawable(0x80000000));
            AttributeApplier applier = new AttributeApplier(context, converter);
            applier.setThemeResolver(themeResolver);
            applier.setDrawableResolver(drawableResolver);
            LinearLayout layout = mock(LinearLayout.class);

            applier.applyForeground(layout, "?attr/overlay");

            verify(layout, never()).setForeground(any(Drawable.class));
        }
    }
}
