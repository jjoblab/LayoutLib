package jo.layoutlib.inflater.bridge.bars;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jo.layoutlib.themes.ThemeResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires JVM des décisions PURES du chrome (v3.5.0, PE13 ADR
 * 0137 D1/D4) : les PRÉSENCES des barres (dérivation du thème +
 * surcharges) et les HAUTEURS ({@link DimensChrome}, garde-fou G1).
 * L'assemblage des vues (partie Android) reste couvert par les tests
 * Robolectric de CodeIDE — philosophie du module inchangée.
 */
@DisplayName("ChromeSysteme — présences dérivées et hauteurs (v3.5.0)")
class ChromeSystemeDecisionsTest {

    /** Stub de ThemeResolver : ?attr/?android:attr → valeur posée (chaîne). */
    private static class StubThemeResolver implements ThemeResolver {
        final Map<String, Object> attrs = new HashMap<>();
        Float dimensionAtt;
        boolean nuit;

        @Override
        public void setTheme(String themeName) {
        }

        @Override
        public Object resolveAttr(String reference) {
            return attrs.get(reference);
        }

        @Override
        public Integer getColorAttr(String attrName) {
            return null;
        }

        @Override
        public Float getDimensionAttr(String attrName) {
            return dimensionAtt;
        }

        @Override
        public void setNightMode(boolean nightMode) {
            this.nuit = nightMode;
        }

        @Override
        public boolean isNightMode() {
            return nuit;
        }

        @Override
        public String getParentStyle(String styleName) {
            return null;
        }

        @Override
        public String getStyleAttribute(String styleName, String attrName) {
            return null;
        }

        @Override
        public void clearCache() {
        }

        void attrFenetre(String nom, boolean valeur) {
            attrs.put("?android:attr/" + nom, Boolean.toString(valeur));
        }
    }

    private static ConfigChrome configNue() {
        return new ConfigChrome.Builder().build();
    }

    // ── D1 : présences DÉRIVÉES du thème (correctif C1/ADR 0135 conservé) ──

    @Test
    @DisplayName("présences : par défaut, status bar + action bar + barre de navigation")
    void presencesParDefaut() {
        ChromeSysteme.Presences p = ChromeSysteme.presences(null, configNue());
        assertThat(p.avecStatusBar).isTrue();
        assertThat(p.avecActionBar).isTrue();
        assertThat(p.barreNavigation).isTrue();
        assertThat(p.poseDecor).isTrue();
    }

    @Test
    @DisplayName("présences : windowFullscreen retire la status bar")
    void windowFullscreenRetireStatusBar() {
        StubThemeResolver resolver = new StubThemeResolver();
        resolver.attrFenetre("windowFullscreen", true);
        ChromeSysteme.Presences p = ChromeSysteme.presences(resolver, configNue());
        assertThat(p.avecStatusBar).isFalse();
        // L'action bar reste — le décor vit toujours.
        assertThat(p.poseDecor).isTrue();
    }

    @Test
    @DisplayName("présences : windowNoTitle retire l'action bar (miroir fil noBar)")
    void windowNoTitleRetireActionBar() {
        StubThemeResolver resolver = new StubThemeResolver();
        resolver.attrFenetre("windowNoTitle", true);
        resolver.attrFenetre("windowActionBar", false);
        ChromeSysteme.Presences p = ChromeSysteme.presences(resolver, configNue());
        assertThat(p.avecActionBar).isFalse();
        assertThat(p.avecStatusBar).isTrue();
    }

    @Test
    @DisplayName("présences : windowNoTitle seul coupe déjà l'action bar (&& court-circuite le défaut)")
    void windowNoTitleSeulCoupeActionBar() {
        // Miroir du transport exact : avecActionBar = !windowNoTitle &&
        // windowActionBar(défaut vrai) — windowNoTitle=true suffit à couper
        // (le && court-circuite le défaut Material), comportement historique.
        StubThemeResolver resolver = new StubThemeResolver();
        resolver.attrFenetre("windowNoTitle", true);
        ChromeSysteme.Presences p = ChromeSysteme.presences(resolver, configNue());
        assertThat(p.avecActionBar).isFalse();
        // windowNoTitle=false NU : l'action bar reste (défaut vrai).
        StubThemeResolver standard = new StubThemeResolver();
        standard.attrFenetre("windowNoTitle", false);
        assertThat(ChromeSysteme.presences(standard, configNue()).avecActionBar).isTrue();
    }

    @Test
    @DisplayName("présences : plein écran SANS barres → contenu rendu tel quel (miroir v0.133.2)")
    void pleinEcranSansBarresRetourneContenu() {
        StubThemeResolver resolver = new StubThemeResolver();
        resolver.attrFenetre("windowFullscreen", true);
        resolver.attrFenetre("windowNoTitle", true);
        resolver.attrFenetre("windowActionBar", false);
        ChromeSysteme.Presences p = ChromeSysteme.presences(resolver, configNue());
        assertThat(p.avecStatusBar).isFalse();
        assertThat(p.avecActionBar).isFalse();
        assertThat(p.poseDecor).isFalse();
    }

    @Test
    @DisplayName("présences : decor=false coupe TOUT (l'équivalent positif du ForceNoDecor)")
    void decorCoupeTout() {
        StubThemeResolver resolver = new StubThemeResolver();
        ChromeSysteme.Presences p = ChromeSysteme.presences(
                resolver, new ConfigChrome.Builder().decor(false).build());
        assertThat(p.poseDecor).isFalse();
    }

    // ── D1 : les SURCHARGES utilisateur gagnent sur le thème ────────────

    @Test
    @DisplayName("surcharges : la surcharge status bar force la présence")
    void surchargeStatusBarForce() {
        StubThemeResolver resolver = new StubThemeResolver();
        resolver.attrFenetre("windowFullscreen", true);
        ConfigChrome config =
                new ConfigChrome.Builder().surchargeStatusBar(Boolean.TRUE).build();
        ChromeSysteme.Presences p = ChromeSysteme.presences(resolver, config);
        assertThat(p.avecStatusBar).isTrue();
        // L'inverse : surcharge FALSE retire la barre d'un thème standard.
        ConfigChrome retrait =
                new ConfigChrome.Builder().surchargeStatusBar(Boolean.FALSE).build();
        assertThat(ChromeSysteme.presences(null, retrait).avecStatusBar).isFalse();
    }

    @Test
    @DisplayName("surcharges : la surcharge barre de navigation remplace le défaut ON")
    void surchargeNavigationBarRemplaceDefaut() {
        ConfigChrome eteinte =
                new ConfigChrome.Builder().surchargeNavigationBar(Boolean.FALSE).build();
        assertThat(ChromeSysteme.presences(null, eteinte).barreNavigation).isFalse();
        ConfigChrome allumee =
                new ConfigChrome.Builder().surchargeNavigationBar(Boolean.TRUE).build();
        assertThat(ChromeSysteme.presences(null, allumee).barreNavigation).isTrue();
        // Sans surcharge : défaut ON (miroir S.nav de la maquette).
        assertThat(ChromeSysteme.presences(null, configNue()).barreNavigation).isTrue();
    }

    // ── D4 : les HAUTEURS (garde-fou G1 — repli 24, pas 25) ─────────────

    @Test
    @DisplayName("DimensChrome : replis 24/48/56 dp (maquette — le repli AOSP serait 25, G1)")
    void replisHauteurs() {
        assertThat(DimensChrome.STATUS_BAR_HAUTEUR_DP).isEqualTo(24);
        assertThat(DimensChrome.BARRE_NAVIGATION_HAUTEUR_DP).isEqualTo(48);
        assertThat(DimensChrome.ACTION_BAR_HAUTEUR_DP).isEqualTo(56);
    }

    @Test
    @DisplayName("DimensChrome : actionBarSize déclaré au thème remplace le repli (px → dp)")
    void actionBarSizeDuTheme() {
        StubThemeResolver resolver = new StubThemeResolver();
        // 56 dp à densité 2,75 = 154 px déclarés → relus 56 dp.
        resolver.dimensionAtt = 154f;
        assertThat(DimensChrome.actionBarHauteurDp(resolver, 2.75f)).isEqualTo(56);
        // 64 dp à densité 2 : 128 px déclarés → 64 dp.
        resolver.dimensionAtt = 128f;
        assertThat(DimensChrome.actionBarHauteurDp(resolver, 2f)).isEqualTo(64);
    }

    @Test
    @DisplayName("DimensChrome : sans thème ni attribut → repli 56 dp")
    void actionBarRepliSansTheme() {
        assertThat(DimensChrome.actionBarHauteurDp(null, 2.75f)).isEqualTo(56);
        StubThemeResolver resolver = new StubThemeResolver();
        resolver.dimensionAtt = null;
        assertThat(DimensChrome.actionBarHauteurDp(resolver, 2.75f)).isEqualTo(56);
        // Valeur négative ou nulle : repli (défense).
        resolver.dimensionAtt = 0f;
        assertThat(DimensChrome.actionBarHauteurDp(resolver, 2.75f)).isEqualTo(56);
        // Densité nulle : repli (défense).
        resolver.dimensionAtt = 154f;
        assertThat(DimensChrome.actionBarHauteurDp(resolver, 0f)).isEqualTo(56);
    }

    // ── D3 : les INSETS explicites ──────────────────────────────────────

    @Test
    @DisplayName("InsetsDecor : champs portés, aucun() est nul partout")
    void insetsDecorChamps() {
        InsetsDecor insets = new InsetsDecor(66, 132, 0, 0);
        assertThat(insets.haut).isEqualTo(66);
        assertThat(insets.bas).isEqualTo(132);
        assertThat(insets.gauche).isZero();
        assertThat(insets.droite).isZero();
        InsetsDecor aucun = InsetsDecor.aucun();
        assertThat(aucun.haut).isZero();
        assertThat(aucun.bas).isZero();
        assertThat(aucun.gauche).isZero();
        assertThat(aucun.droite).isZero();
    }
}
