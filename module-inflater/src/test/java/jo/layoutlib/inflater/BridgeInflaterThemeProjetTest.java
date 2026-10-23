package jo.layoutlib.inflater;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import jo.layoutlib.resources.DimensionConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * v3.6.0 — thème prévisualisé appliqué aux vues (étude PE14 § 2.4-2, S7
 * du plan) : {@code BridgeInflater.setThemeProjet(resid)} fait
 * instancier chaque vue sous un {@link ContextThemeWrapper} portant le
 * thème projet — les styles PAR DÉFAUT des familles (buttonStyle,
 * editTextStyle…) se résolvent contre le thème prévisualisé, pas celui
 * de l'hôte.
 *
 * <p>Stratégie JVM (patron {@code BridgeInflaterFactory2Test}) : la
 * Factory2 CAPTURE le contexte passé à chaque création — c'est le seul
 * point observable sous android.jar mockable, et c'est exactement le
 * contexte d'instanciation qu'on veut vérifier.</p>
 *
 * @author jo@Dev
 * @since 3.6.0
 */
@DisplayName("BridgeInflater — thème projet appliqué aux instanciations (v3.6.0, S7)")
class BridgeInflaterThemeProjetTest {

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
        inflater.setAttributeApplier(
                new AttributeApplier(context, new DimensionConverter(2f, 2f, 320f)));
    }

    /** Factory de capture : mémorise le contexte de CHAQUE vue créée. */
    private static final class CaptureContextes implements LayoutInflater.Factory2 {

        private final List<Context> contextes = new ArrayList<>();

        @Override
        public View onCreateView(View parent, String name, Context ctx,
                                 AttributeSet attrs) {
            contextes.add(ctx);
            return null;
        }

        @Override
        public View onCreateView(String name, Context ctx, AttributeSet attrs) {
            return null;
        }
    }

    @Test
    @DisplayName("sans thème projet — contexte nominal (comportement inchangé)")
    void sansThemeContexteNominal() {
        CaptureContextes capture = new CaptureContextes();
        inflater.setFactory2(capture);

        inflater.inflate("<LinearLayout " + NS + "\n"
                + "    android:layout_width=\"wrap_content\"\n"
                + "    android:layout_height=\"wrap_content\">\n"
                + "  <TextView\n"
                + "      android:layout_width=\"wrap_content\"\n"
                + "      android:layout_height=\"wrap_content\" />\n"
                + "</LinearLayout>");

        assertThat(capture.contextes).isNotEmpty();
        assertThat(capture.contextes).allSatisfy(ctx -> assertThat(ctx).isSameAs(context));
    }

    @Test
    @DisplayName("thème projet posé — TOUTES les vues sous ContextThemeWrapper")
    void themePoseContextesEnveloppes() {
        inflater.setThemeProjet(42);
        assertThat(inflater.getThemeProjet()).isEqualTo(42);
        CaptureContextes capture = new CaptureContextes();
        inflater.setFactory2(capture);

        inflater.inflate("<LinearLayout " + NS + "\n"
                + "    android:layout_width=\"wrap_content\"\n"
                + "    android:layout_height=\"wrap_content\">\n"
                + "  <Button\n"
                + "      android:layout_width=\"wrap_content\"\n"
                + "      android:layout_height=\"wrap_content\" />\n"
                + "</LinearLayout>");

        assertThat(capture.contextes).isNotEmpty();
        assertThat(capture.contextes).allSatisfy(ctx ->
                assertThat(ctx).isInstanceOf(ContextThemeWrapper.class));
    }

    @Test
    @DisplayName("retour à 0 — le contexte nominal revient (repli hôte)")
    void retourAZero() {
        inflater.setThemeProjet(42);
        inflater.setThemeProjet(0);
        CaptureContextes capture = new CaptureContextes();
        inflater.setFactory2(capture);

        inflater.inflate("<TextView " + NS + "\n"
                + "    android:layout_width=\"wrap_content\"\n"
                + "    android:layout_height=\"wrap_content\" />");

        assertThat(capture.contextes).allSatisfy(ctx -> assertThat(ctx).isSameAs(context));
    }
}
