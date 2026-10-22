package jo.layoutlib.inflater;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * v3.5.1 — ISOLEMENT D'ÉCHEC AU NŒUD (étude PE14 § 3.3-C, S4 du plan § 6) :
 * « aujourd'hui un seul mauvais nœud tue l'aperçu entier
 * (RenduLayoutLib.kt:272-283) : la référence, elle, isole les vues en échec
 * (Bridge.createMockView) et rend LE RESTE ».
 *
 * <p>Un parent qui REFUSE l'ajout (AdapterView.addView lève
 * UnsupportedOperationException pour TOUT enfant — dépôt d'un enfant dans
 * une ListView tapé au source) n'entraîne PLUS l'échec du rendu ENTIER : le
 * nœud fautif reste DÉTACHÉ et journalisé, le reste du document vit. La
 * vue détaille déjà le document côté éditeur (problème
 * ENFANTS_ADAPTATEUR de CodeIDE, v0.171.0).</p>
 *
 * <p>Le test reproduit le refus par un parent Mockito dont
 * {@code addView(View, LayoutParams)} LÈVE — l'android.jar mockable ne
 * peut pas le faire (les méthodes y sont des no-op). Avant v3.5.1,
 * l'exception remontait au catch global : InflateException pour TOUT le
 * document — le premier test échoue donc sur le code d'avant le
 * correctif, prouvant la régression.</p>
 *
 * @author jo@Dev
 * @since 3.5.1
 */
@DisplayName("BridgeInflater — un nœud refusé n'échoue plus le rendu entier (v3.5.1, étude § 3.3-C)")
class BridgeInflaterIsolationNoeudTest {

    private static final String NS_ANDROID =
            "xmlns:android=\"http://schemas.android.com/apk/res/android\"";

    private static final String DOCUMENT =
            "<FrameLayout " + NS_ANDROID + "\n"
                    + "    android:layout_width=\"match_parent\"\n"
                    + "    android:layout_height=\"match_parent\">\n"
                    + "  <TextView android:id=\"+id/nil\"\n"
                    + "      android:layout_width=\"wrap_content\"\n"
                    + "      android:layout_height=\"wrap_content\"/>\n"
                    + "</FrameLayout>";

    private BridgeInflater inflater;

    @BeforeEach
    void setUp() {
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.density = 2f;
        metrics.scaledDensity = 2f;
        metrics.xdpi = 320f;
        Resources resources = mock(Resources.class);
        when(resources.getDisplayMetrics()).thenReturn(metrics);
        Context context = mock(Context.class);
        when(context.getResources()).thenReturn(resources);
        when(context.getPackageName()).thenReturn("jo.test");
        inflater = new BridgeInflater(context, new ViewTagRegistry(false));
    }

    @Test
    @DisplayName("un parent qui REFUSE l'ajout n'entraîne plus l'échec du rendu entier")
    void parentRefusantNeTuePlusLeRendu() {
        ViewGroup parent = mock(ViewGroup.class);
        doThrow(new UnsupportedOperationException(
                "addView(View) is not supported in AdapterView"))
                .when(parent).addView(any(View.class), any(ViewGroup.LayoutParams.class));

        // Avant v3.5.1 : l'exception remontait au catch global →
        // InflateException pour TOUT le document (plus d'aperçu du tout).
        assertThatCode(() -> {
            View racine = inflater.inflate(DOCUMENT, parent);
            assertThat(racine).isNotNull();
            assertThat(racine).isInstanceOf(ViewGroup.class);
        }).doesNotThrowAnyException();
        // Le parent a bien ÉTÉ consulté (le refus est isolé, pas contourné).
        verify(parent).addView(any(View.class), any(ViewGroup.LayoutParams.class));
    }

    @Test
    @DisplayName("un parent qui accepte attache normalement (contrôle)")
    void parentAcceptantAttacheNormalement() {
        ViewGroup parent = mock(ViewGroup.class);
        View racine = inflater.inflate(DOCUMENT, parent);
        assertThat(racine).isNotNull();
        verify(parent).addView(any(View.class), any(ViewGroup.LayoutParams.class));
    }
}
