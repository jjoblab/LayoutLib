package jo.layoutlib.inflater;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jo.layoutlib.drawables.DrawableResolver;
import jo.layoutlib.resources.DimensionConverter;
import jo.layoutlib.resources.ResourceResolverImpl;
import jo.layoutlib.resources.ResourceTable;
import jo.layoutlib.resources.ResourceQualifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests de la résolution {@code @drawable/} et {@code @array/} dans
 * {@link AttributeApplier} (point 4 : branchement de module-drawables et du
 * ResourceResolver).
 *
 * <p>Le {@link DrawableResolver} du projet est simulé par un stub : on teste
 * ici le câblage de l'applier (priorité au résolveur, repli natif sûr),
 * pas le parsing XML des drawables lui-même (couvert par les tests du
 * module-drawables).</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
@DisplayName("AttributeApplier — résolution @drawable/ et @array/")
class AttributeApplierDrawableTest {

    private Context context;
    private DimensionConverter converter;

    /** Stub de DrawableResolver : renvoie toujours le même drawable. */
    private static class StubDrawableResolver implements DrawableResolver {
        final Map<String, Drawable> drawables = new HashMap<>();
        int resolveCalls;

        @Override
        public Drawable resolve(String reference, Context context) {
            resolveCalls++;
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
    @DisplayName("resolveDrawable — DrawableResolver d'abord, repli sûr")
    class ResolveDrawable {

        @Test
        @DisplayName("passe par le DrawableResolver connecté")
        void usesDrawableResolverFirst() {
            StubDrawableResolver drawableResolver = new StubDrawableResolver();
            ColorDrawable expected = new ColorDrawable(0xFF6750A4);
            drawableResolver.drawables.put("@drawable/card_bg", expected);
            AttributeApplier applier = new AttributeApplier(context, converter);
            applier.setDrawableResolver(drawableResolver);

            Drawable result = applier.resolveDrawable("@drawable/card_bg");

            assertThat(result).isSameAs(expected);
            assertThat(drawableResolver.resolveCalls).isEqualTo(1);
        }

        @Test
        @DisplayName("retourne null (sans lever) si le résolveur ne connaît pas la référence")
        void returnsNullWhenResolverMisses() {
            StubDrawableResolver drawableResolver = new StubDrawableResolver();
            AttributeApplier applier = new AttributeApplier(context, converter);
            applier.setDrawableResolver(drawableResolver);

            // Résolveur raté + Resources natives mockées (getIdentifier → 0)
            // → null, pas d'exception
            assertThat(applier.resolveDrawable("@drawable/inconnu")).isNull();
        }

        @Test
        @DisplayName("une exception du résolveur est attrapée et journalisée")
        void catchesResolverExceptions() {
            DrawableResolver exploding = new DrawableResolver() {
                @Override
                public Drawable resolve(String reference, Context context) {
                    throw new IllegalStateException("boom");
                }

                @Override
                public Drawable parse(String xml, Context context) {
                    return null;
                }

                @Override
                public void clearCache() {
                }
            };
            AttributeApplier applier = new AttributeApplier(context, converter);
            applier.setDrawableResolver(exploding);

            // Ne doit PAS lever : repli silencieux
            assertThat(applier.resolveDrawable("@drawable/whatever")).isNull();
        }

        @Test
        @DisplayName("null est ignoré sans appel au résolveur")
        void ignoresNullReference() {
            StubDrawableResolver drawableResolver = new StubDrawableResolver();
            AttributeApplier applier = new AttributeApplier(context, converter);
            applier.setDrawableResolver(drawableResolver);

            assertThat(applier.resolveDrawable(null)).isNull();
            assertThat(drawableResolver.resolveCalls).isEqualTo(0);
        }

        @Test
        @DisplayName("une couleur littérale est déléguée au résolveur (ColorDrawable)")
        void delegatesLiteralColorsToResolver() {
            StubDrawableResolver drawableResolver = new StubDrawableResolver();
            ColorDrawable color = new ColorDrawable(0xFFFF0000);
            drawableResolver.drawables.put("#FF0000", color);
            AttributeApplier applier = new AttributeApplier(context, converter);
            applier.setDrawableResolver(drawableResolver);

            // Le DrawableResolver sait aussi gérer les couleurs littérales
            // (@color/, #hex) — il est donc consulté en premier pour toute
            // valeur de drawable.
            assertThat(applier.resolveDrawable("#FF0000")).isSameAs(color);
            assertThat(drawableResolver.resolveCalls).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("resolveStringArray — @array/ via le ResourceResolver")
    class ResolveStringArray {

        @Test
        @DisplayName("résout @array/ depuis la table du projet")
        void resolvesArrayFromTable() {
            ResourceTable table = new ResourceTable();
            table.putStringArray("planets",
                    Arrays.asList("Mercure", "Vénus", "Terre"),
                    ResourceQualifier.DEFAULT);
            ResourceResolverImpl resolver = new ResourceResolverImpl(table);

            AttributeApplier applier = new AttributeApplier(context, converter, resolver);

            List<String> values = applier.resolveStringArray("@array/planets");

            assertThat(values).containsExactly("Mercure", "Vénus", "Terre");
        }

        @Test
        @DisplayName("retourne null (sans lever) pour une référence inconnue")
        void returnsNullForUnknownReference() {
            AttributeApplier applier =
                    new AttributeApplier(context, converter,
                            new ResourceResolverImpl(new ResourceTable()));

            assertThat(applier.resolveStringArray("@array/inconnu")).isNull();
            assertThat(applier.resolveStringArray("@string/x")).isNull();
            assertThat(applier.resolveStringArray(null)).isNull();
        }
    }

    @Nested
    @DisplayName("Câblage RenderService → AttributeApplier")
    class RenderServiceWiring {

        @Test
        @DisplayName("setDrawableResolver atteint l'applier du RenderService")
        void drawableResolverReachesApplier() {
            Context realLikeContext = Contexts.displayMetricsContext();
            RenderService service = new RenderService(realLikeContext);
            StubDrawableResolver drawableResolver = new StubDrawableResolver();

            service.setDrawableResolver(drawableResolver);

            AttributeApplier applier = service.getInflater().getAttributeApplier();
            assertThat(applier).isNotNull();
            assertThat(applier.getDrawableResolver()).isSameAs(drawableResolver);
        }

        @Test
        @DisplayName("un rebuild (setDimensionConverter) conserve le drawableResolver")
        void rebuildKeepsDrawableResolver() {
            Context realLikeContext = Contexts.displayMetricsContext();
            RenderService service = new RenderService(realLikeContext);
            StubDrawableResolver drawableResolver = new StubDrawableResolver();
            service.setDrawableResolver(drawableResolver);

            // Rebuild de l'applier : le résolveur de drawables doit survivre
            service.setDimensionConverter(new DimensionConverter(3f, 3f, 480f));

            assertThat(service.getInflater().getAttributeApplier()
                    .getDrawableResolver()).isSameAs(drawableResolver);
        }
    }

    /** Fabrique un Context mocké avec DisplayMetrics (utilisé par le ctor de RenderService). */
    static final class Contexts {
        static Context displayMetricsContext() {
            android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
            metrics.density = 2.75f;
            metrics.scaledDensity = 2.75f;
            metrics.xdpi = 420f;
            android.content.res.Resources resources =
                    mock(android.content.res.Resources.class);
            org.mockito.Mockito.when(resources.getDisplayMetrics())
                    .thenReturn(metrics);
            Context context = mock(Context.class);
            org.mockito.Mockito.when(context.getResources()).thenReturn(resources);
            return context;
        }

        private Contexts() {
        }
    }
}
