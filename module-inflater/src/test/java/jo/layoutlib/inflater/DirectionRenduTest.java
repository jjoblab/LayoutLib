package jo.layoutlib.inflater;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * v3.7.0 — la DIRECTION DE MISE EN PAGE du rendu (étude PE14 § 2.4-5, S8
 * du plan) : le qualificateur
 * ({@link DirectionRendu}, miroir {@code LayoutDirectionQualifier}), la
 * passe de RÉSOLUTION des marges relatives (hors fenêtre, personne ne
 * déclenche {@code requestLayout} — le moteur fait la passe
 * lui-même) et le seam des marges RELATIVES de {@link BridgeInflater}.
 *
 * <p>Android.jar mockable : {@code setMarginStart} (comme tout setter de
 * vue) est un no-op invisible — les tests passent par des MOCKS Mockito
 * (patron « stubs/verify » établi par
 * {@code BridgeInflaterDimenReferenceTest} ; les constantes
 * {@code View.LAYOUT_DIRECTION_*} sont {@code static final}, RÉELLES même
 * mockables).</p>
 *
 * @author jo@Dev
 * @since 3.7.0
 */
@DisplayName("DirectionRendu — le qualificateur RTL + la résolution (v3.7.0, S8)")
class DirectionRenduTest {

    private BridgeInflater inflater;

    @BeforeEach
    void setUp() {
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.density = 1f;
        metrics.scaledDensity = 1f;
        metrics.xdpi = 160f;
        Resources resources = mock(Resources.class);
        when(resources.getDisplayMetrics()).thenReturn(metrics);
        Context context = mock(Context.class);
        when(context.getResources()).thenReturn(resources);
        inflater = new BridgeInflater(context, new ViewTagRegistry(false));
    }

    // ── La TABLE android:layoutDirection → constante View (pure). ──

    @Test
    @DisplayName("directionDepuisValeur — la table AOSP stricte")
    void shouldMapLayoutDirectionValues() {
        assertThat(DirectionRendu.directionDepuisValeur("ltr"))
                .isEqualTo(View.LAYOUT_DIRECTION_LTR);
        assertThat(DirectionRendu.directionDepuisValeur("rtl"))
                .isEqualTo(View.LAYOUT_DIRECTION_RTL);
        assertThat(DirectionRendu.directionDepuisValeur("locale"))
                .isEqualTo(View.LAYOUT_DIRECTION_LOCALE);
        assertThat(DirectionRendu.directionDepuisValeur("inherit"))
                .isEqualTo(View.LAYOUT_DIRECTION_INHERIT);
        // Toute valeur inconnue (ou absente) retombe sur INHERIT — jamais
        // un échec.
        assertThat(DirectionRendu.directionDepuisValeur("n'importe"))
                .isEqualTo(View.LAYOUT_DIRECTION_INHERIT);
        assertThat(DirectionRendu.directionDepuisValeur(null))
                .isEqualTo(View.LAYOUT_DIRECTION_INHERIT);
        // Les espaces parasites sont tolérés (trim miroir des lecteurs d'attr).
        assertThat(DirectionRendu.directionDepuisValeur(" rtl "))
                .isEqualTo(View.LAYOUT_DIRECTION_RTL);
    }

    // ── La PASSE DE RÉSOLUTION (hors fenêtre : le moteur déclenche lui-même). ──

    @Test
    @DisplayName("resoudre — chaque vue résout ses LayoutParams contre sa direction")
    void shouldResolveLayoutParamsAgainstDirection() {
        // Racine (ViewGroup) avec un enfant, chacun portant ses LP.
        ViewGroup.MarginLayoutParams lpRacine = mock(ViewGroup.MarginLayoutParams.class);
        ViewGroup.MarginLayoutParams lpEnfant = mock(ViewGroup.MarginLayoutParams.class);
        View enfant = mock(View.class);
        when(enfant.getLayoutParams()).thenReturn(lpEnfant);
        when(enfant.getLayoutDirection()).thenReturn(View.LAYOUT_DIRECTION_RTL);

        ViewGroup racine = mock(ViewGroup.class);
        when(racine.getLayoutParams()).thenReturn(lpRacine);
        when(racine.getLayoutDirection()).thenReturn(View.LAYOUT_DIRECTION_RTL);
        when(racine.getChildCount()).thenReturn(1);
        when(racine.getChildAt(0)).thenReturn(enfant);

        DirectionRendu.appliquer(racine, DirectionRendu.INDEFINI);

        // Le qualificateur INDEFINI ne pose RIEN — la résolution passe quand même.
        verify(racine, never()).setLayoutDirection(anyInt());
        verify(lpRacine).resolveLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        verify(lpEnfant).resolveLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    }

    @Test
    @DisplayName("appliquer — le qualificateur se pose sur la RACINE seulement")
    void shouldApplyQualifierOnRootOnly() {
        View racine = mock(View.class);
        DirectionRendu.appliquer(racine, View.LAYOUT_DIRECTION_RTL);
        verify(racine).setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    }

    @Test
    @DisplayName("appliquer — racine null : aucun lever")
    void shouldTolerateNullRoot() {
        assertThatCode(() -> DirectionRendu.appliquer(null, View.LAYOUT_DIRECTION_RTL))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("resoudre — un LP qui lève n'arrête pas la marche (isolement au nœud)")
    void shouldIsolateResolveFailures() {
        View vueSansLp = mock(View.class);
        when(vueSansLp.getLayoutParams())
                .thenThrow(new RuntimeException("mock : LP indisponible"));
        assertThatCode(() -> DirectionRendu.appliquer(vueSansLp, DirectionRendu.INDEFINI))
                .doesNotThrowAnyException();
    }

    // ── Le SEAM des marges RELATIVES de BridgeInflater. ──

    @Test
    @DisplayName("marges relatives — setMarginStart/setMarginEnd avec la valeur PARSE")
    void shouldApplyRelativeMargins() {
        ViewGroup.MarginLayoutParams lp = mock(ViewGroup.MarginLayoutParams.class);
        // Densité 1f (setUp) : 16dp → 16, 8dp → 8.
        inflater.appliquerMargesRelatives(lp, "16dp", "8dp");
        verify(lp).setMarginStart(16);
        verify(lp).setMarginEnd(8);
    }

    @Test
    @DisplayName("marges relatives — absentes : aucun setter appelé")
    void shouldSkipMissingRelativeMargins() {
        ViewGroup.MarginLayoutParams lp = mock(ViewGroup.MarginLayoutParams.class);
        inflater.appliquerMargesRelatives(lp, null, null);
        verify(lp, never()).setMarginStart(anyInt());
        verify(lp, never()).setMarginEnd(anyInt());
    }

    // ── Le CÂBLAGE inflate : la passe de direction ne casse jamais un rendu. ──

    @Test
    @DisplayName("inflate — marges relatives + layoutDirection par vue : aucun lever")
    void shouldInflateRtlXmlWithoutCrash() {
        String xml = "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\""
                + " android:layout_width=\"match_parent\""
                + " android:layout_height=\"match_parent\""
                + " android:layoutDirection=\"rtl\">"
                + "<TextView android:layout_width=\"wrap_content\""
                + " android:layout_height=\"wrap_content\""
                + " android:paddingStart=\"16dp\""
                + " android:layout_marginStart=\"16dp\"/>"
                + "</LinearLayout>";
        assertThatCode(() -> inflater.inflate(xml)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("qualificateur — le set/get round-trip du rendu")
    void shouldRoundTripQualifier() {
        assertThat(inflater.getLayoutDirectionRendu()).isEqualTo(DirectionRendu.INDEFINI);
        inflater.setLayoutDirectionRendu(View.LAYOUT_DIRECTION_RTL);
        assertThat(inflater.getLayoutDirectionRendu())
                .isEqualTo(View.LAYOUT_DIRECTION_RTL);
        inflater.setLayoutDirectionRendu(DirectionRendu.INDEFINI);
        assertThat(inflater.getLayoutDirectionRendu()).isEqualTo(DirectionRendu.INDEFINI);
    }
}
