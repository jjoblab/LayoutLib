package jo.layoutlib.inflater;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.View;

import jo.layoutlib.resources.DimensionConverter;
import jo.layoutlib.resources.ResourceQualifier;
import jo.layoutlib.resources.ResourceResolverImpl;
import jo.layoutlib.resources.ResourceTable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * v3.4.7 — non-régression « @dimen/ dans les LayoutParams » : un
 * {@code layout_width}, {@code layout_height}, {@code layout_margin*} ou
 * {@code padding*} portant une RÉFÉRENCE {@code @dimen/…} doit se résoudre
 * via le {@code ResourceResolver} (miroir du correctif v3.4.4 qui ne
 * couvrait que le chemin {@code AttributeApplier}).
 *
 * <p>Avant v3.4.7, {@code BridgeInflater.parseDimension} passait la
 * référence telle quelle à l'analyse littérale → {@code Float.parseFloat}
 * levait {@code NumberFormatException} → {@code InflateException
 * « Dimension invalide »} qui tuait TOUT le rendu (retour utilisateur
 * CodeIDE : {@code @dimen/spacing_sm} → « rendu échec »).</p>
 *
 * <p>Les lectures passent par RÉFLEXION (patron de
 * {@code RenderServiceWiringTest}) : {@code parseDimension} est privé, et
 * les LayoutParams des vues ne sont pas observables sous la JVM de test
 * (android.jar mockable). Le fichier compile tel quel contre le code
 * d'avant le correctif — et échoue alors à l'exécution
 * ({@code InflateException}), prouvant la régression.</p>
 *
 * @author jo@Dev
 * @since 3.4.7
 */
@DisplayName("BridgeInflater — références @dimen/ dans les dimensions (v3.4.7)")
class BridgeInflaterDimenReferenceTest {

    private Context context;
    private BridgeInflater inflater;

    @BeforeEach
    void setUp() {
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.density = 2.75f;
        metrics.scaledDensity = 2.75f;
        metrics.xdpi = 420f;
        android.content.res.Resources resources = mock(android.content.res.Resources.class);
        when(resources.getDisplayMetrics()).thenReturn(metrics);
        // Repli natif neutralisé : getIdentifier rend toujours 0 pour
        // forcer le chemin ResourceResolver.
        when(resources.getIdentifier(anyString(), anyString(), anyString())).thenReturn(0);
        context = mock(Context.class);
        when(context.getResources()).thenReturn(resources);

        DimensionConverter converter = new DimensionConverter(2.75f, 2.75f, 420f);
        ResourceTable table = new ResourceTable();
        table.putDimen("spacing_sm", "8dp", ResourceQualifier.DEFAULT);
        table.putDimen("card_corner", "12dp", ResourceQualifier.DEFAULT);
        ResourceResolverImpl resolver = new ResourceResolverImpl(table);
        resolver.setDimensionConverter(converter);

        inflater = new BridgeInflater(context, new ViewTagRegistry(false));
        inflater.setResourceResolver(resolver);
    }

    /** Invoque BridgeInflater.parseDimension (privé) par réflexion. */
    private int parseDimension(String valeur) throws Exception {
        java.lang.reflect.Method methode =
                BridgeInflater.class.getDeclaredMethod("parseDimension", String.class);
        methode.setAccessible(true);
        return (Integer) methode.invoke(inflater, valeur);
    }

    @Test
    @DisplayName("parseDimension(@dimen/spacing_sm) — résolu par le résolveur")
    void referenceResolue() throws Exception {
        // 8dp × 2.75 = 22px.
        assertThat(parseDimension("@dimen/spacing_sm")).isEqualTo(Math.round(8f * 2.75f));
        assertThat(parseDimension("@dimen/card_corner")).isEqualTo(Math.round(12f * 2.75f));
    }

    @Test
    @DisplayName("parseDimension(@dimen/ inconnu) — dégrade à 0, jamais d'exception")
    void referenceInconnueDégradeAZero() throws Exception {
        assertThat(parseDimension("@dimen/inexistante")).isEqualTo(0);
    }

    @Test
    @DisplayName("parseDimension(16dp littéral) — chemin nominal inchangé")
    void littéralInchangé() throws Exception {
        assertThat(parseDimension("16dp")).isEqualTo(Math.round(16f * 2.75f));
        assertThat(parseDimension("24dp")).isEqualTo(Math.round(24f * 2.75f));
        assertThat(parseDimension("100px")).isEqualTo(100);
    }

    @Test
    @DisplayName("inflate avec layout_margin=@dimen/ — plus d'InflateException")
    void inflationAvecReferenceNeCrashPlus() {
        assertThatCode(() -> {
            View racine = inflater.inflate(
                    "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    android:layout_width=\"match_parent\"\n"
                            + "    android:layout_height=\"wrap_content\">\n"
                            + "  <FrameLayout\n"
                            + "      android:layout_width=\"@dimen/spacing_sm\"\n"
                            + "      android:layout_height=\"wrap_content\"\n"
                            + "      android:layout_marginTop=\"@dimen/card_corner\"/>\n"
                            + "</LinearLayout>");
            assertThat(racine).isNotNull();
        }).doesNotThrowAnyException();
    }
}
