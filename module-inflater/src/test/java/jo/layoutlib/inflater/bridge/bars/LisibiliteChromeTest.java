package jo.layoutlib.inflater.bridge.bars;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires JVM de {@link LisibiliteChrome} (v3.5.0, PE13 ADR 0137
 * D6) — la partie PURE de la lisibilité du chrome : luminance Rec. 709,
 * bascule blanc ↔ {@code #0C2264} au seuil 0,5, honoration explicite de
 * {@code windowLightStatusBar}. Transport des assertions du
 * {@code ChromeSystemeTest} historique de CodeIDE (v0.166.5), désormais
 * côté moteur.
 */
@DisplayName("LisibiliteChrome — luminance Rec. 709 et couleur du contenu (v3.5.0)")
class LisibiliteChromeTest {

    /** 0,2126 R + 0,7152 V + 0,0722 B — Rec. 709 comme Color.luminance. */
    @Test
    @DisplayName("luminanceRelative : blanc 1, noir 0, rouge pur 0,2126")
    void luminanceRelativeRec709() {
        assertThat(LisibiliteChrome.luminanceRelative(0xFFFFFFFF)).isEqualTo(1.0);
        assertThat(LisibiliteChrome.luminanceRelative(0xFF000000)).isEqualTo(0.0);
        assertThat(LisibiliteChrome.luminanceRelative(0xFFFF0000)).isEqualTo(0.2126);
    }

    /** Miroir --s-onpri : le contenu sombre nuit vaut #0C2264, le clair blanc. */
    @Test
    @DisplayName("couleurContenu : blanc sur fond sombre, #0C2264 sur fond clair")
    void couleurContenuParLuminance() {
        assertThat(LisibiliteChrome.couleurContenu(0xFF2B2F7A)).isEqualTo(0xFFFFFFFF);
        assertThat(LisibiliteChrome.couleurContenu(0xFF43559E)).isEqualTo(0xFFFFFFFF);
        assertThat(LisibiliteChrome.couleurContenu(0xFFBAC4FF)).isEqualTo(0xFF0C2264);
        assertThat(LisibiliteChrome.couleurContenu(0xFFFFFFFF)).isEqualTo(0xFF0C2264);
    }

    /** Le seuil de bascule vaut 0,5 — à la limite, le fond « clair » reçoit le sombre. */
    @Test
    @DisplayName("couleurContenu : la bascule se fait au seuil 0,5 (strictement au-dessus)")
    void couleurContenuAuSeuil() {
        // Luminance EXACTEMENT au-delà de 0,5 (128,128,128 ≈ 0,502) : sombre.
        assertThat(LisibiliteChrome.couleurContenu(0xFF808080)).isEqualTo(0xFF0C2264);
        // Luminance 63,63,63 ≈ 0,247 (en dessous) : blanc.
        assertThat(LisibiliteChrome.couleurContenu(0xFF3F3F3F)).isEqualTo(0xFFFFFFFF);
    }

    /**
     * Étape 5 : l'attribut explicite GAGNE sur la luminance — true →
     * contenu sombre, false → contenu clair, null (absent) → luminance.
     */
    @Test
    @DisplayName("couleurContenu : windowLightStatusBar explicite gagne sur la luminance")
    void windowLightStatusBarExplicite() {
        int fondSombre = 0xFF2B2F7A;
        int fondClair = 0xFFBAC4FF;
        // true (icônes sombres) — même sur fond sombre.
        assertThat(LisibiliteChrome.couleurContenu(fondSombre, Boolean.TRUE))
                .isEqualTo(0xFF0C2264);
        // false (icônes claires) — même sur fond clair.
        assertThat(LisibiliteChrome.couleurContenu(fondClair, Boolean.FALSE))
                .isEqualTo(0xFFFFFFFF);
        // null (thème muet) — la luminance décide.
        assertThat(LisibiliteChrome.couleurContenu(fondSombre, null)).isEqualTo(0xFFFFFFFF);
        assertThat(LisibiliteChrome.couleurContenu(fondClair, null)).isEqualTo(0xFF0C2264);
    }
}
