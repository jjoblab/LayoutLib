package jo.layoutlib.themes;

import android.content.Context;
import android.content.res.Resources;
import android.view.ContextThemeWrapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * v3.6.0 — tests du {@link ThemeContexteResolver} (étude PE14 § 2.4-2,
 * S7 du plan) : la marche de la chaîne d'héritage du thème projet doit
 * trouver l'ancêtre RÉSOLVABLE (thème de bibliothèque embarquée ou du
 * framework) — l'hôte seul ne connaît pas {@code Theme.MonApp}, mais il
 * connaît son parent {@code Theme.Material3.DayNight} (resources des
 * bibliothèques mergées dans l'hôte).
 *
 * <p>Le lookup {@code getIdentifier} est MOCKÉ : le comportement
 * « Theme.MonApp introuvable / Theme.Material3.DayNight trouvé » est
 * exactement celui d'un appareil.</p>
 *
 * @author jo@Dev
 * @since 3.6.0
 */
@DisplayName("ThemeContexteResolver — résid du thème projet résoluble (v3.6.0, S7)")
class ThemeContexteResolverTest {

    private ThemeResolverImpl resolver;
    private Context contexte;
    private Resources resources;

    @BeforeEach
    void setUp() {
        resolver = new ThemeResolverImpl();
        resolver.registerThemesFile(
                "<resources>"
                        + "<style name=\"Theme.Material3.DayNight\" />"
                        + "<style name=\"Theme.MonApp\" parent=\"Theme.Material3.DayNight\">"
                        + "<item name=\"colorPrimary\">#FF123456</item>"
                        + "</style>"
                        + "<style name=\"Theme.Orphelin\" parent=\"Theme.NullePart\" />"
                        + "</resources>");

        resources = mock(Resources.class);
        when(resources.getIdentifier(anyString(), anyString(), anyString()))
                .thenReturn(0);
        contexte = mock(Context.class);
        when(contexte.getResources()).thenReturn(resources);
        when(contexte.getPackageName()).thenReturn("com.exemple.hote");
    }

    private void identifiantPour(String nom, int resid) {
        when(resources.getIdentifier(nom, "style", "com.exemple.hote"))
                .thenReturn(resid);
    }

    @Nested
    @DisplayName("resIdResolvable — la chaîne d'héritage se marche")
    class ResId {

        @Test
        @DisplayName("thème projet résoluble DANS l'hôte (Material embarqué)")
        void resolvableDirectement() {
            identifiantPour("Theme.Material3.DayNight", 77);
            assertThat(ThemeContexteResolver.resIdResolvable(
                    contexte, resolver, "Theme.Material3.DayNight")).isEqualTo(77);
        }

        @Test
        @DisplayName("thème métier — le PARENT résoluble gagne")
        void parentResolvable() {
            identifiantPour("Theme.MonApp", 0);
            identifiantPour("Theme.Material3.DayNight", 91);
            assertThat(ThemeContexteResolver.resIdResolvable(
                    contexte, resolver, "Theme.MonApp")).isEqualTo(91);
        }

        @Test
        @DisplayName("les préfixes @style/ et @android:style/ partent")
        void prefixesRetires() {
            identifiantPour("Theme.MonApp", 33);
            assertThat(ThemeContexteResolver.resIdResolvable(
                    contexte, resolver, "@style/Theme.MonApp")).isEqualTo(33);
        }

        @Test
        @DisplayName("chaîne morte — 0 (repli hôte, jamais d'échec)")
        void chaineMorte() {
            assertThat(ThemeContexteResolver.resIdResolvable(
                    contexte, resolver, "Theme.Orphelin")).isEqualTo(0);
        }

        @Test
        @DisplayName("cycle dans l'héritage — 0 sans boucle infinie")
        void cycle() {
            resolver.registerThemesFile(
                    "<resources>"
                            + "<style name=\"A\" parent=\"B\" />"
                            + "<style name=\"B\" parent=\"A\" />"
                            + "</resources>");
            assertThat(ThemeContexteResolver.resIdResolvable(
                    contexte, resolver, "A")).isEqualTo(0);
        }

        @Test
        @DisplayName("nom null ou vide — 0")
        void nomVide() {
            assertThat(ThemeContexteResolver.resIdResolvable(
                    contexte, resolver, null)).isZero();
            assertThat(ThemeContexteResolver.resIdResolvable(
                    contexte, resolver, "")).isZero();
        }
    }

    @Nested
    @DisplayName("envelopper — ContextThemeWrapper ou repli identité")
    class Envelopper {

        @Test
        @DisplayName("resid non nul — le contexte est enveloppé")
        void enveloppe() {
            Context enveloppe = ThemeContexteResolver.envelopper(contexte, 91);
            // NB : getBaseContext() est un stub android.jar (rend null
            // sous JVM) — seule l'INSTANCE du wrapper est observable.
            assertThat(enveloppe).isInstanceOf(ContextThemeWrapper.class);
        }

        @Test
        @DisplayName("resid 0 — repli IDENTITÉ (contexte hôte tel quel)")
        void identite() {
            assertThat(ThemeContexteResolver.envelopper(contexte, 0))
                    .isSameAs(contexte);
        }

        @Test
        @DisplayName("contexte null — null (garde-fou)")
        void contexteNull() {
            assertThat(ThemeContexteResolver.envelopper(null, 91)).isNull();
        }
    }
}
