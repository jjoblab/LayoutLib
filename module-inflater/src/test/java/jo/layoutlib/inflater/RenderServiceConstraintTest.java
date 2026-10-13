package jo.layoutlib.inflater;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests du câblage {@code RenderService → BridgeInflater → ConstraintSolver}
 * (ADR 0116 — P2/L9).
 *
 * <p>Le pipeline doit, après la passe measure/layout standard, résoudre les
 * contraintes des enfants des ViewGroups dont les vues portent des
 * attributs {@code app:layout_constraint*}. En JVM (android.jar mockable),
 * {@code View.measure()}/{@code layout()} sont des no-ops : ce test valide
 * le <em>câblage</em> (capture propagée, résolution exécutée sans erreur,
 * non-régression des rendus sans contraintes) — la justesse des positions
 * est couverte par {@code ConstraintSolverCassowaryTest} (module-layout)
 * et par les tests instrumentés sur device.</p>
 *
 * @author jo@Dev
 * @since 3.0
 */
@DisplayName("RenderService — câblage ConstraintSolver (P2/L9)")
class RenderServiceConstraintTest {

    private static final String NS_ANDROID =
            "xmlns:android=\"http://schemas.android.com/apk/res/android\"";
    private static final String NS_APP =
            "xmlns:app=\"http://schemas.android.com/apk/res-auto\"";

    private Context context;
    private RenderService service;

    @BeforeEach
    void setUp() {
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.density = 2f;
        metrics.scaledDensity = 2f;
        metrics.xdpi = 320f;
        Resources resources = mock(Resources.class);
        when(resources.getDisplayMetrics()).thenReturn(metrics);
        context = mock(Context.class);
        when(context.getResources()).thenReturn(resources);
        service = new RenderService(context);
    }

    /** Invoque doRender() directement (sans Handler), retourne la racine rendue. */
    private View renderNow(String xml) throws Exception {
        AtomicReference<View> rootRef = new AtomicReference<>();
        service.setRenderCallback(new RenderService.RenderCallback() {
            @Override
            public void onRenderSuccess(View rootView, long timeMs, int viewCount,
                                        int width, int height) {
                rootRef.set(rootView);
            }

            @Override
            public void onRenderError(String message, Throwable cause) {
                throw new AssertionError("Rendu en erreur : " + message, cause);
            }
        });
        Field pendingXml = RenderService.class.getDeclaredField("pendingXml");
        pendingXml.setAccessible(true);
        pendingXml.set(service, xml);
        Method doRender = RenderService.class.getDeclaredMethod("doRender");
        doRender.setAccessible(true);
        doRender.invoke(service);
        return rootRef.get();
    }

    @Test
    @DisplayName("un rendu avec contraintes exécute la résolution sans erreur")
    void renderWithConstraintsDoesNotFail() {
        String xml =
                "<FrameLayout " + NS_ANDROID + " " + NS_APP
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\">\n"
                        + "  <View android:id=\"@+id/a\"\n"
                        + "        android:layout_width=\"100dp\"\n"
                        + "        android:layout_height=\"50dp\"\n"
                        + "        app:layout_constraintStart_toStartOf=\"parent\"\n"
                        + "        app:layout_constraintTop_toTopOf=\"parent\"/>\n"
                        + "  <View android:id=\"+id/b\"\n"
                        + "        android:layout_width=\"0dp\"\n"
                        + "        android:layout_height=\"50dp\"\n"
                        + "        app:layout_constraintStart_toEndOf=\"@id/a\"\n"
                        + "        app:layout_constraintEnd_toEndOf=\"parent\"/>\n"
                        + "</FrameLayout>";

        assertThatCode(() -> {
            View root = renderNow(xml);
            assertThat(root).isInstanceOf(ViewGroup.class);
        }).doesNotThrowAnyException();

        // La capture est disponible après le rendu (consommée par le solver)
        assertThat(service.getInflater().hasConstraintAttributes()).isTrue();
        assertThat(service.getInflater().getConstraintAttributes()).hasSize(2);
    }

    @Test
    @DisplayName("un rendu avec chaîne + guideline + ratio passe le pipeline")
    void renderWithChainGuidelineRatio() throws Exception {
        String xml =
                "<FrameLayout " + NS_ANDROID + " " + NS_APP
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\">\n"
                        + "  <View android:id=\"+id/guide\"\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:orientation=\"vertical\"\n"
                        + "        app:layout_constraintGuide_percent=\"0.5\"/>\n"
                        + "  <View android:id=\"+id/gauche\"\n"
                        + "        android:layout_width=\"100dp\"\n"
                        + "        android:layout_height=\"100dp\"\n"
                        + "        app:layout_constraintEnd_toStartOf=\"@id/guide\"/>\n"
                        + "  <View android:id=\"+id/droite\"\n"
                        + "        android:layout_width=\"100dp\"\n"
                        + "        android:layout_height=\"0dp\"\n"
                        + "        app:layout_constraintStart_toStartOf=\"@id/guide\"\n"
                        + "        app:layout_constraintDimensionRatio=\"1:1\"/>\n"
                        + "</FrameLayout>";

        View root = renderNow(xml);
        assertThat(root).isInstanceOf(ViewGroup.class);
        // NB : getChildCount() est un no-op en JVM mockable — on valide le
        // câblage via la capture (3 vues contraintes enregistrées)
        Map<View, Map<String, String>> attrs =
                service.getInflater().getConstraintAttributes();
        assertThat(attrs).hasSize(3); // guideline + 2 vues
    }

    @Test
    @DisplayName("un rendu sans contraintes reste inchangé (non-régression)")
    void renderWithoutConstraintsUnchanged() throws Exception {
        String xml =
                "<LinearLayout " + NS_ANDROID + " " + NS_APP
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\"\n"
                        + "    android:orientation=\"vertical\">\n"
                        + "  <View android:layout_width=\"100dp\"\n"
                        + "        android:layout_height=\"50dp\"/>\n"
                        + "</LinearLayout>";

        View root = renderNow(xml);
        assertThat(root).isInstanceOf(ViewGroup.class);
        assertThat(service.getInflater().hasConstraintAttributes()).isFalse();
    }

    @Test
    @DisplayName("un XML avec contraintes incohérentes ne casse pas le rendu")
    void renderWithBrokenConstraintsFallsBack() throws Exception {
        String xml =
                "<FrameLayout " + NS_ANDROID + " " + NS_APP
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\">\n"
                        + "  <View android:id=\"+id/x\"\n"
                        + "        android:layout_width=\"100dp\"\n"
                        + "        android:layout_height=\"50dp\"\n"
                        + "        app:layout_constraintStart_toEndOf=\"@id/y\"/>\n"
                        + "  <View android:id=\"+id/y\"\n"
                        + "        android:layout_width=\"100dp\"\n"
                        + "        android:layout_height=\"50dp\"\n"
                        + "        app:layout_constraintStart_toEndOf=\"@id/x\"/>\n"
                        + "</FrameLayout>";

        // Contraintes circulaires — repli legacy, jamais d'exception.
        // NB : getChildCount() est un no-op en JVM mockable — la capture
        // prouve que les 2 vues ont traversé le pipeline.
        View root = renderNow(xml);
        assertThat(root).isInstanceOf(ViewGroup.class);
        assertThat(service.getInflater().getConstraintAttributes()).hasSize(2);
    }
}
