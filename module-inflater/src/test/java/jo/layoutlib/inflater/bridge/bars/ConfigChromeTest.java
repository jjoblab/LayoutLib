package jo.layoutlib.inflater.bridge.bars;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jo.layoutlib.inflater.ViewInfoCollector;
import jo.layoutlib.inflater.bridge.impl.SystemViewInfo;
import jo.layoutlib.resources.api.ActionBarCallback;
import jo.layoutlib.resources.api.ViewType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires JVM des NOUVEAUX CONTRATS du chrome (v3.5.0, PE13 ADR
 * 0137) : {@link ConfigChrome} (config de session + défauts maquette),
 * {@link ActionBarCallback}/{@link ViewType} (sémantiques réelles) et
 * {@link SystemViewInfo} (marquage du décor). Les holders sont purs —
 * constructibles en JVM sans Robolectric.
 */
@DisplayName("ConfigChrome, ActionBarCallback, ViewType et SystemViewInfo (v3.5.0)")
class ConfigChromeTest {

    @Test
    @DisplayName("builder : les DÉFAUTS MAQUETTE — décor, PADDE, trois boutons, sans surcharge")
    void defautsMaquette() {
        ConfigChrome config = new ConfigChrome.Builder().build();
        assertThat(config.decor).isTrue();
        assertThat(config.appLabel).isNull();
        assertThat(config.surchargeStatusBar).isNull();
        assertThat(config.surchargeNavigationBar).isNull();
        assertThat(config.modePlacement).isEqualTo(ModePlacementDecor.PADDE_PAR_DEFAUT);
        assertThat(config.modeNavigation).isEqualTo(ModeNavigation.TROIS_BOUTONS);
        assertThat(config.actionBarCallback.isHomeAsUp()).isFalse();
        assertThat(config.actionBarCallback.isPopupDebordement()).isFalse();
        assertThat(config.actionBarCallback.getSousTitre()).isNull();
    }

    @Test
    @DisplayName("builder : les surcharges et capacités posées sont portées")
    void surchargesPortees() {
        ConfigChrome config = new ConfigChrome.Builder()
                .decor(false)
                .appLabel("Ma vue")
                .surchargeStatusBar(Boolean.TRUE)
                .surchargeNavigationBar(Boolean.FALSE)
                .modePlacement(ModePlacementDecor.SURIMPRESSION)
                .modeNavigation(ModeNavigation.GESTUELLE)
                .actionBarCallback(new ActionBarCallback(true, true, "Sous-titre"))
                .build();
        assertThat(config.decor).isFalse();
        assertThat(config.appLabel).isEqualTo("Ma vue");
        assertThat(config.surchargeStatusBar).isTrue();
        assertThat(config.surchargeNavigationBar).isFalse();
        assertThat(config.modePlacement).isEqualTo(ModePlacementDecor.SURIMPRESSION);
        assertThat(config.modeNavigation).isEqualTo(ModeNavigation.GESTUELLE);
        assertThat(config.actionBarCallback.isHomeAsUp()).isTrue();
        assertThat(config.actionBarCallback.isPopupDebordement()).isTrue();
        assertThat(config.actionBarCallback.getSousTitre()).isEqualTo("Sous-titre");
    }

    @Test
    @DisplayName("builder : nulls défensifs — PADDE, trois boutons, callback par défaut")
    void nullsDefensifs() {
        ConfigChrome config = new ConfigChrome.Builder()
                .modePlacement(null)
                .modeNavigation(null)
                .actionBarCallback(null)
                .build();
        assertThat(config.modePlacement).isEqualTo(ModePlacementDecor.PADDE_PAR_DEFAUT);
        assertThat(config.modeNavigation).isEqualTo(ModeNavigation.TROIS_BOUTONS);
        assertThat(config.actionBarCallback).isSameAs(ActionBarCallback.parDefaut());
    }

    @Test
    @DisplayName("ActionBarCallback : le singleton par défaut est TOUT ÉTEINT")
    void callbackParDefaut() {
        ActionBarCallback defaut = ActionBarCallback.parDefaut();
        assertThat(defaut.isHomeAsUp()).isFalse();
        assertThat(defaut.isPopupDebordement()).isFalse();
        assertThat(defaut.getSousTitre()).isNull();
        // Le singleton est IMMUTABLE par usage — deux appels rendent le MÊME.
        assertThat(ActionBarCallback.parDefaut()).isSameAs(defaut);
    }

    @Test
    @DisplayName("ViewType : le marquage complet du décor (miroir AOSP)")
    void viewTypeMarquage() {
        assertThat(ViewType.values()).containsExactlyInAnyOrder(
                ViewType.UNKNOWN, ViewType.APPLICATION, ViewType.STATUS_BAR,
                ViewType.NAVIGATION_BAR, ViewType.ACTION_BAR, ViewType.ACTION_BAR_MENU,
                ViewType.ACTION_BAR_OVERFLOW, ViewType.ACTION_BAR_OVERFLOW_MENU);
    }

    /** Une ViewInfo témoin, constructible en JVM (holder pur). */
    private ViewInfoCollector.ViewInfo temoin() {
        List<ViewInfoCollector.ViewInfo> enfants = new ArrayList<>();
        return new ViewInfoCollector.ViewInfo(
                "android.widget.TextView", "TextView", 42, "titre",
                0, 66, 1080, 132, 1080, 66,
                16, 8, 16, 8, 0, 0, 0, 0,
                0, 2, false, enfants);
    }

    @Test
    @DisplayName("SystemViewInfo : copie INTÉGRALE de l'info source + marquage")
    void systemViewInfoCopie() {
        ViewInfoCollector.ViewInfo source = temoin();
        SystemViewInfo systeme = new SystemViewInfo(source, ViewType.STATUS_BAR);
        assertThat(systeme.className).isEqualTo("android.widget.TextView");
        assertThat(systeme.simpleName).isEqualTo("TextView");
        assertThat(systeme.id).isEqualTo(42);
        assertThat(systeme.idName).isEqualTo("titre");
        assertThat(systeme.left).isZero();
        assertThat(systeme.top).isEqualTo(66);
        assertThat(systeme.right).isEqualTo(1080);
        assertThat(systeme.bottom).isEqualTo(132);
        assertThat(systeme.width).isEqualTo(1080);
        assertThat(systeme.height).isEqualTo(66);
        assertThat(systeme.depth).isEqualTo(2);
        assertThat(systeme.getViewType()).isEqualTo(ViewType.STATUS_BAR);
        assertThat(systeme.toString()).contains("STATUS_BAR");
    }

    @Test
    @DisplayName("SystemViewInfo : marquage null → UNKNOWN (repli défensif)")
    void systemViewInfoRepliUnknown() {
        SystemViewInfo systeme = new SystemViewInfo(temoin(), null);
        assertThat(systeme.getViewType()).isEqualTo(ViewType.UNKNOWN);
    }
}
