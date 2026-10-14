package jo.layoutlib.inflater;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.View;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests de la consommation de {@code android:animateLayoutChanges} par le
 * {@link BridgeInflater} (ADR 0116 — P6/L11).
 *
 * <p>En JVM (mockable android.jar), {@code getLayoutTransition()} retourne
 * null même après {@code setLayoutTransition()} ({@code returnDefaultValues}) :
 * l'installation est donc vérifiée via le compteur d'introspection
 * {@link BridgeInflater#getAppliedLayoutTransitionCount()} — sur appareil, le
 * getter natif confirme le {@code LayoutTransition} réel. L'exigence
 * principale reste : jamais d'exception, attribut consommé.</p>
 *
 * @author jo@Dev
 * @since 3.1
 */
@DisplayName("BridgeInflater — animateLayoutChanges / LayoutTransition (P6/L11)")
class BridgeInflaterLayoutTransitionTest {

    private static final String NS =
            "xmlns:android=\"http://schemas.android.com/apk/res/android\"";

    private Context context;
    private BridgeInflater inflater;

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
        inflater = new BridgeInflater(context, new ViewTagRegistry(false));
    }

    @Nested
    @DisplayName("Installation du LayoutTransition")
    class Installation {

        @Test
        @DisplayName("animateLayoutChanges=\"true\" sur un ViewGroup est consommé")
        void trueOnViewGroupIsConsumed() {
            View root = inflater.inflate(
                    "<LinearLayout " + NS
                            + " android:animateLayoutChanges=\"true\"\n"
                            + " android:layout_width=\"match_parent\"\n"
                            + " android:layout_height=\"match_parent\"/>");
            assertThat(root).isInstanceOf(android.view.ViewGroup.class);
            assertThat(inflater.getAppliedLayoutTransitionCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("deux ViewGroups animés sont comptés")
        void twoAnimatedGroupsAreCounted() {
            inflater.inflate(
                    "<LinearLayout " + NS
                            + " android:layout_width=\"match_parent\"\n"
                            + " android:layout_height=\"match_parent\">\n"
                            + "  <FrameLayout android:animateLayoutChanges=\"true\"\n"
                            + "               android:layout_width=\"match_parent\"\n"
                            + "               android:layout_height=\"wrap_content\"/>\n"
                            + "</LinearLayout>");
            // Racine sans attribut + enfant avec : 1 seul LayoutTransition
            assertThat(inflater.getAppliedLayoutTransitionCount()).isEqualTo(1);

            inflater = new BridgeInflater(context, new ViewTagRegistry(false));
            inflater.inflate(
                    "<LinearLayout " + NS
                            + " android:animateLayoutChanges=\"true\"\n"
                            + " android:layout_width=\"match_parent\"\n"
                            + " android:layout_height=\"match_parent\">\n"
                            + "  <FrameLayout android:animateLayoutChanges=\"true\"\n"
                            + "               android:layout_width=\"match_parent\"\n"
                            + "               android:layout_height=\"wrap_content\"/>\n"
                            + "</LinearLayout>");
            assertThat(inflater.getAppliedLayoutTransitionCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("le rendu complet via RenderService ne lève jamais")
        void fullRenderNeverThrows() {
            RenderService service = new RenderService(context);
            String xml =
                    "<LinearLayout " + NS
                            + " android:animateLayoutChanges=\"true\"\n"
                            + " android:layout_width=\"match_parent\"\n"
                            + " android:layout_height=\"match_parent\">\n"
                            + "  <TextView android:layout_width=\"wrap_content\"\n"
                            + "            android:layout_height=\"wrap_content\"/>\n"
                            + "</LinearLayout>";
            assertThatCode(() -> renderNow(service, xml)).doesNotThrowAnyException();
            assertThat(service.getInflater()
                    .getAppliedLayoutTransitionCount()).isEqualTo(1);
        }
    }

    /** Invoque doRender() directement (sans Handler), pattern
     * RenderServiceConstraintTest. */
    private static void renderNow(RenderService service, String xml)
            throws Exception {
        Field pendingXml = RenderService.class.getDeclaredField("pendingXml");
        pendingXml.setAccessible(true);
        pendingXml.set(service, xml);
        Method doRender = RenderService.class.getDeclaredMethod("doRender");
        doRender.setAccessible(true);
        doRender.invoke(service);
    }

    @Nested
    @DisplayName("Attribut consommé sans effet")
    class ConsumedWithoutEffect {

        @Test
        @DisplayName("animateLayoutChanges=\"false\" est consommé sans installation")
        void falseIsConsumedWithoutInstallation() {
            inflater.inflate(
                    "<LinearLayout " + NS
                            + " android:animateLayoutChanges=\"false\"\n"
                            + " android:layout_width=\"match_parent\"\n"
                            + " android:layout_height=\"match_parent\"/>");
            assertThat(inflater.getAppliedLayoutTransitionCount()).isZero();
        }

        @Test
        @DisplayName("sans attribut, rien n'est installé")
        void noAttributeMeansNothingInstalled() {
            inflater.inflate(
                    "<LinearLayout " + NS
                            + " android:layout_width=\"match_parent\"\n"
                            + " android:layout_height=\"match_parent\"/>");
            assertThat(inflater.getAppliedLayoutTransitionCount()).isZero();
        }

        @Test
        @DisplayName("sur une vue simple (non ViewGroup), l'attribut est consommé sans effet ni exception")
        void simpleViewConsumesWithoutEffect() {
            View root = inflater.inflate(
                    "<TextView " + NS
                            + " android:animateLayoutChanges=\"true\"\n"
                            + " android:layout_width=\"wrap_content\"\n"
                            + " android:layout_height=\"wrap_content\"/>");
            assertThat(root).isNotInstanceOf(android.view.ViewGroup.class);
            assertThat(inflater.getAppliedLayoutTransitionCount()).isZero();
        }
    }
}
