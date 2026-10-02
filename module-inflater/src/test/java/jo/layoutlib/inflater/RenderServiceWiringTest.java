package jo.layoutlib.inflater;

import android.content.Context;
import android.util.DisplayMetrics;
import android.content.res.Resources;
import android.view.View;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import jo.layoutlib.resources.DimensionConverter;
import jo.layoutlib.resources.ResourceResolverImpl;
import jo.layoutlib.resources.ResourceTable;
import jo.layoutlib.resources.ResourceQualifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests du câblage {@code RenderService → BridgeInflater → AttributeApplier
 * → ResourceResolver}.
 *
 * <p>Régression du bug historique : {@code MainActivity.setupRenderService()}
 * appelait {@code setDimensionConverter(...)} <em>avant</em>
 * {@code setResourceResolver(...)}. L'{@link AttributeApplier} était alors
 * construit avec un résolveur {@code null} et ne le recevait jamais :
 * {@code @color/}, {@code @string/} et {@code @dimen/} ne passaient jamais
 * par le {@code ResourceResolver}.</p>
 *
 * <p>Ces tests garantissent que le résultat est <strong>indépendant de
 * l'ordre des setters</strong>.</p>
 *
 * <p>Toutes les lectures d'état interne (champs {@code resourceResolver},
 * {@code attributeApplier}, {@code dimensionConverter}) et les invocations
 * des méthodes {@code resolveColor/String/Dimension} passent par réflexion,
 * de sorte que ce fichier compile tel quel contre le code d'avant le
 * correctif — et échoue alors à l'exécution, prouvant la régression.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
@DisplayName("RenderService — câblage converter/résolveur → AttributeApplier")
class RenderServiceWiringTest {

    private Context context;
    private DimensionConverter converter;
    private ResourceResolverImpl resolver;

    @BeforeEach
    void setUp() {
        // Contexte mocké : RenderService lit density/scaledDensity/xdpi au
        // constructeur. DisplayMetrics est un POJO réel.
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.density = 2.75f;
        metrics.scaledDensity = 2.75f;
        metrics.xdpi = 420f;
        Resources resources = mock(Resources.class);
        when(resources.getDisplayMetrics()).thenReturn(metrics);
        context = mock(Context.class);
        when(context.getResources()).thenReturn(resources);

        converter = new DimensionConverter(2.75f, 2.75f, 420f);

        ResourceTable table = new ResourceTable();
        table.putColor("primary", "#FF6750A4", ResourceQualifier.DEFAULT);
        table.putString("app_name", "Layout Editor", ResourceQualifier.DEFAULT);
        table.putDimen("margin_large", "16dp", ResourceQualifier.DEFAULT);
        resolver = new ResourceResolverImpl(table);
        resolver.setDimensionConverter(converter);
    }

    // ------------------------------------------------------------------
    // Accès réflexifs (compilent avant ET après le correctif)
    // ------------------------------------------------------------------

    /** Lit le champ privé {@code resourceResolver} d'un AttributeApplier. */
    private static Object resolverOf(AttributeApplier applier) throws Exception {
        return fieldOf(applier, "resourceResolver");
    }

    /** Lit le champ privé {@code attributeApplier} d'un BridgeInflater. */
    private static AttributeApplier applierOf(BridgeInflater inflater) throws Exception {
        return (AttributeApplier) fieldOf(inflater, "attributeApplier");
    }

    /** Lit le champ privé {@code dimensionConverter} d'un AttributeApplier. */
    private static Object converterOf(AttributeApplier applier) throws Exception {
        return fieldOf(applier, "dimensionConverter");
    }

    private static Object fieldOf(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    /**
     * Invoque {@code resolveColor/resolveString/resolveDimension} (signature
     * {@code (String)})", privées ou package-private selon la version.
     */
    @SuppressWarnings("unchecked")
    private static <T> T invokeResolve(AttributeApplier applier, String method, String ref) {
        try {
            Method m = AttributeApplier.class.getDeclaredMethod(method, String.class);
            m.setAccessible(true);
            return (T) m.invoke(applier, ref);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Méthode introuvable : " + method, e);
        }
    }

    // ------------------------------------------------------------------
    // 1. Ordre des setters — échoue sur le code d'avant le correctif
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Ordre des setters (régression bug ordre d'appel)")
    class SetterOrder {

        @Test
        @DisplayName("setDimensionConverter puis setResourceResolver (ordre de MainActivity)")
        void converterThenResolver() throws Exception {
            RenderService service = new RenderService(context);

            // Ordre historique de MainActivity.setupRenderService() :
            service.setDimensionConverter(converter);
            service.setResourceResolver(resolver);

            AttributeApplier applier = applierOf(service.getInflater());
            assertThat(applier).isNotNull();
            // AVANT correctif : null (l'applier n'était jamais mis à jour)
            assertThat(resolverOf(applier)).isSameAs(resolver);

            // …et l'aplier résout bien @color/ via le résolveur :
            Integer color = invokeResolve(applier, "resolveColor", "@color/primary");
            assertThat(color).isEqualTo(0xFF6750A4);
        }

        @Test
        @DisplayName("setResourceResolver puis setDimensionConverter (ordre inverse)")
        void resolverThenConverter() throws Exception {
            RenderService service = new RenderService(context);

            service.setResourceResolver(resolver);
            service.setDimensionConverter(converter);

            AttributeApplier applier = applierOf(service.getInflater());
            assertThat(applier).isNotNull();
            // Le remplacement du convertisseur ne doit pas écraser le résolveur
            assertThat(resolverOf(applier)).isSameAs(resolver);
            assertThat(converterOf(applier)).isSameAs(converter);
        }

        @Test
        @DisplayName("setResourceResolver seul suffit (sans setDimensionConverter)")
        void resolverOnly() throws Exception {
            RenderService service = new RenderService(context);

            service.setResourceResolver(resolver);

            AttributeApplier applier = applierOf(service.getInflater());
            assertThat(applier).isNotNull();
            assertThat(resolverOf(applier)).isSameAs(resolver);
        }

        @Test
        @DisplayName("le résolveur est aussi propagé via BridgeInflater seul")
        void inflaterPropagatesToApplier() throws Exception {
            BridgeInflater inflater = new BridgeInflater(context, new ViewTagRegistry(false));
            AttributeApplier applier = new AttributeApplier(context, converter);
            inflater.setAttributeApplier(applier);

            inflater.setResourceResolver(resolver);

            assertThat(inflater.getResourceResolver()).isSameAs(resolver);
            assertThat(resolverOf(applier)).isSameAs(resolver);
        }
    }

    // ------------------------------------------------------------------
    // 2. Non-régression : résolution via le ResourceResolver
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Résolution via le ResourceResolver (non-régression)")
    class ResolutionThroughResolver {

        @Test
        @DisplayName("resolveColor passe par le ResourceResolver")
        void resolvesColorViaResolver() {
            AttributeApplier applier = new AttributeApplier(context, converter, resolver);

            Integer color = invokeResolve(applier, "resolveColor", "@color/primary");

            assertThat(color).isEqualTo(0xFF6750A4);
        }

        @Test
        @DisplayName("resolveString passe par le ResourceResolver")
        void resolvesStringViaResolver() {
            AttributeApplier applier = new AttributeApplier(context, converter, resolver);

            String value = invokeResolve(applier, "resolveString", "@string/app_name");

            assertThat(value).isEqualTo("Layout Editor");
        }

        @Test
        @DisplayName("resolveDimension passe par le ResourceResolver (16dp × 2.75)")
        void resolvesDimensionViaResolver() {
            AttributeApplier applier = new AttributeApplier(context, converter, resolver);

            Integer pixels = invokeResolve(applier, "resolveDimension", "@dimen/margin_large");

            assertThat(pixels).isEqualTo(Math.round(16f * 2.75f));
        }
    }

    // ------------------------------------------------------------------
    // 3. Cycle de vie — release()
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Cycle de vie (release)")
    class Lifecycle {

        @Test
        @DisplayName("release est idempotent et coupe le callback")
        void releaseIsIdempotent() {
            RenderService service = new RenderService(context);
            service.setRenderCallback(new RenderService.RenderCallback() {
                @Override
                public void onRenderSuccess(View root, long timeMs, int viewCount,
                                            int width, int height) {
                }

                @Override
                public void onRenderError(String message, Throwable cause) {
                }
            });

            service.release();
            service.release(); // idempotent

            assertThat(service.isReleased()).isTrue();
        }

        @Test
        @DisplayName("requestRender après release est ignoré sans lever")
        void requestRenderAfterReleaseIsIgnored() {
            RenderService service = new RenderService(context);
            service.release();

            // Ne doit pas lever (activity en cours de destruction)
            service.requestRender("<TextView/>");
            service.requestImmediateRender("<TextView/>");
        }

        @Test
        @DisplayName("les setters après release lèvent IllegalStateException")
        void settersAfterReleaseThrow() {
            RenderService service = new RenderService(context);
            service.release();

            org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                    () -> service.setResourceResolver(resolver));
            org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                    () -> service.setDimensionConverter(converter));
        }
    }
}
