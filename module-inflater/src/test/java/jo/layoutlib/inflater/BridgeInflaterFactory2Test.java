package jo.layoutlib.inflater;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Tests du hook {@code LayoutInflater.Factory2} du {@link BridgeInflater}
 * (ADR 0116 — P6/L10).
 *
 * <p>Sémantique AOSP vérifiée : la factory intercepte la création des vues
 * standard avant la création par défaut, un retour null laisse la création
 * par défaut, les tags spéciaux ne passent pas par la factory, une factory
 * qui lève interrompt l'inflation, et une seule factory peut être définie.</p>
 *
 * @author jo@Dev
 * @since 3.1
 */
@DisplayName("BridgeInflater — hook LayoutInflater.Factory2 (P6/L10)")
class BridgeInflaterFactory2Test {

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
        org.mockito.Mockito.when(resources.getDisplayMetrics())
                .thenReturn(metrics);
        context = mock(Context.class);
        org.mockito.Mockito.when(context.getResources()).thenReturn(resources);
        inflater = new BridgeInflater(context, new ViewTagRegistry(false));
    }

    /** Factory2 de test : journalise les appels, substitue sur demande. */
    private static final class JournalisingFactory implements LayoutInflater.Factory2 {
        final List<String> calls = new ArrayList<>();
        final View substitution;

        JournalisingFactory(View substitution) {
            this.substitution = substitution;
        }

        @Override
        public View onCreateView(View parent, String name, Context ctx,
                                 AttributeSet attrs) {
            calls.add(name);
            return substitution;
        }

        @Override
        public View onCreateView(String name, Context ctx, AttributeSet attrs) {
            calls.add(name);
            return substitution;
        }
    }

    @Nested
    @DisplayName("Substitution de vues")
    class Substitution {

        @Test
        @DisplayName("la vue fournie par la factory devient la racine du rendu")
        void factoryViewBecomesRoot() {
            View substituee = new FrameLayout(context);
            inflater.setFactory2(new JournalisingFactory(substituee));

            View root = inflater.inflate(
                    "<FrameLayout " + NS
                            + " android:layout_width=\"match_parent\"\n"
                            + " android:layout_height=\"match_parent\"/>");

            assertThat(root).isSameAs(substituee);
            assertThat(inflater.getFactoryViewCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("la factory est consultée pour chaque vue de l'arbre")
        void factoryCalledForEachView() {
            View substituee = new FrameLayout(context);
            JournalisingFactory factory = new JournalisingFactory(substituee);
            inflater.setFactory2(factory);

            inflater.inflate(
                    "<LinearLayout " + NS
                            + " android:layout_width=\"match_parent\"\n"
                            + " android:layout_height=\"match_parent\">\n"
                            + "  <TextView android:layout_width=\"wrap_content\"\n"
                            + "            android:layout_height=\"wrap_content\"/>\n"
                            + "  <Button android:layout_width=\"wrap_content\"\n"
                            + "         android:layout_height=\"wrap_content\"/>\n"
                            + "</LinearLayout>");

            assertThat(factory.calls).containsExactly(
                    "LinearLayout", "TextView", "Button");
            assertThat(inflater.getFactoryViewCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("la factory reçoit le contexte de l'inflater et un AttributeSet null")
        void factoryReceivesContextAndNullAttrs() {
            List<Object> received = new ArrayList<>();
            LayoutInflater.Factory2 factory = new LayoutInflater.Factory2() {
                @Override
                public View onCreateView(View parent, String name, Context ctx,
                                         AttributeSet attrs) {
                    received.add(ctx);
                    received.add(attrs);
                    return null;
                }

                @Override
                public View onCreateView(String name, Context ctx,
                                         AttributeSet attrs) {
                    return null;
                }
            };
            inflater.setFactory2(factory);

            inflater.inflate(
                    "<View " + NS + " android:layout_width=\"wrap_content\"\n"
                            + " android:layout_height=\"wrap_content\"/>");

            assertThat(received.get(0)).isSameAs(context);
            assertThat(received.get(1)).isNull();
        }
    }

    @Nested
    @DisplayName("Repli sur la création par défaut")
    class Fallback {

        @Test
        @DisplayName("un retour null de la factory laisse la création par défaut")
        void nullReturnFallsBackToDefault() {
            inflater.setFactory2(new JournalisingFactory(null));

            View root = inflater.inflate(
                    "<FrameLayout " + NS
                            + " android:layout_width=\"match_parent\"\n"
                            + " android:layout_height=\"match_parent\"/>");

            assertThat(root).isInstanceOf(FrameLayout.class);
            assertThat(inflater.getFactoryViewCount()).isZero();
        }

        @Test
        @DisplayName("sans factory, l'inflation est strictement inchangée")
        void noFactoryMeansDefaultBehavior() {
            View root = inflater.inflate(
                    "<LinearLayout " + NS
                            + " android:layout_width=\"match_parent\"\n"
                            + " android:layout_height=\"match_parent\"/>");
            assertThat(root).isInstanceOf(android.widget.LinearLayout.class);
            assertThat(inflater.getFactory2()).isNull();
            assertThat(inflater.getFactoryViewCount()).isZero();
        }
    }

    @Nested
    @DisplayName("Contrat AOSP")
    class AospContract {

        @Test
        @DisplayName("les tags spéciaux ne passent pas par la factory")
        void specialTagsBypassFactory() {
            JournalisingFactory factory =
                    new JournalisingFactory(new FrameLayout(context));
            inflater.setFactory2(factory);

            // <merge> et <requestFocus> : tags spéciaux sans création de vue
            View root = inflater.inflate(
                    "<merge " + NS + ">\n"
                            + "  <requestFocus/>\n"
                            + "</merge>",
                    new FrameLayout(context));

            assertThat(factory.calls).isEmpty();
            assertThat(inflater.getFactoryViewCount()).isZero();
        }

        @Test
        @DisplayName("une factory qui lève interrompt l'inflation (InflateException)")
        void throwingFactoryInterruptsInflation() {
            LayoutInflater.Factory2 factory = new LayoutInflater.Factory2() {
                @Override
                public View onCreateView(View parent, String name, Context ctx,
                                         AttributeSet attrs) {
                    throw new IllegalStateException("boom");
                }

                @Override
                public View onCreateView(String name, Context ctx,
                                         AttributeSet attrs) {
                    throw new IllegalStateException("boom");
                }
            };
            inflater.setFactory2(factory);

            assertThatThrownBy(() -> inflater.inflate(
                    "<View " + NS + " android:layout_width=\"wrap_content\"\n"
                            + " android:layout_height=\"wrap_content\"/>"))
                    .isInstanceOf(InflateException.class)
                    .hasMessageContaining("Factory2");
        }

        @Test
        @DisplayName("une seconde factory lève IllegalStateException (contrat AOSP)")
        void secondFactoryThrows() {
            inflater.setFactory2(new JournalisingFactory(null));
            assertThatThrownBy(() ->
                    inflater.setFactory2(new JournalisingFactory(null)))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("le hook est propagé aux <include> (compteur d'enfant)")
        void factoryPropagatesToIncludes() {
            // Parent avec <include> résolu via un ResourceResolver de test :
            // <tmp>/res/layout/enfant.xml
            java.nio.file.Path res;
            try {
                res = java.nio.file.Files.createTempDirectory("res");
                java.nio.file.Files.createDirectories(res.resolve("layout"));
                java.nio.file.Files.write(res.resolve("layout").resolve("enfant.xml"),
                        ("<TextView " + NS
                                + " android:layout_width=\"wrap_content\"\n"
                                + " android:layout_height=\"wrap_content\"/>")
                                .getBytes());
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
            jo.layoutlib.resources.ResourceResolverImpl resolver =
                    new jo.layoutlib.resources.ResourceResolverImpl(res.toString());
            inflater.setResourceResolver(resolver);
            inflater.setFactory2(new JournalisingFactory(new FrameLayout(context)));

            View root = inflater.inflate(
                    "<FrameLayout " + NS
                            + " android:layout_width=\"match_parent\"\n"
                            + " android:layout_height=\"match_parent\">\n"
                            + "  <include layout=\"@layout/enfant\"/>\n"
                            + "</FrameLayout>");

            assertThat(root).isInstanceOf(FrameLayout.class);
            // FrameLayout racine + TextView de l'<include> : les deux substitués,
            // le compteur du parent inclut celui de l'inflater enfant
            assertThat(inflater.getFactoryViewCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Câblage RenderService")
    class RenderServiceWiring {

        @Test
        @DisplayName("RenderService.setFactory2 forward vers l'inflater")
        void renderServiceForwardsFactory() {
            RenderService service = new RenderService(context);
            LayoutInflater.Factory2 factory = new JournalisingFactory(null);

            service.setFactory2(factory);

            assertThat(service.getFactory2()).isSameAs(factory);
            assertThat(service.getInflater().getFactory2()).isSameAs(factory);
        }
    }
}
