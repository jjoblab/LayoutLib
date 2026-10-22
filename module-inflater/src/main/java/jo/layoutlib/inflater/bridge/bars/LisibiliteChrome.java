package jo.layoutlib.inflater.bridge.bars;

/**
 * La LISIBILITÉ du contenu du chrome système (PE13 — transport à l'identique
 * de {@code LisibiliteChrome} v0.166.5 côté CodeIDE, ADR 0137 D6).
 *
 * <p>La couleur du contenu suit la LUMINANCE du fond résolu — blanc sur fond
 * sombre, sombre sur fond clair (nuit : le primaire clair {@code #BAC4FF}
 * reçoit le sombre {@code #0C2264}, miroir {@code --s-onpri} de la maquette
 * {@code apercu-xml.html} ; jour : le blanc sur {@code #43559E}). Classe
 * PURE — aucun appel au framework (arithmétique de bits pour les canaux, la
 * classe est testable en JVM comme le reste du module) ; le seuil de
 * bascule vaut 0,5.</p>
 *
 * <p>PE13 étape 5 : {@link #couleurContenu(int, Boolean)} honore en plus
 * l'attribut {@code windowLightStatusBar} EXPLICITE du thème quand il est
 * déclaré ({@code true} → contenu sombre, {@code false} → contenu clair,
 * absent → luminance). Le moteur de référence v32 ne connaît pas cet
 * attribut (icônes à couleurs fixes par version) — la luminance reste le
 * comportement par défaut, l'attribut est une AMÉLIORATION assumée.</p>
 *
 * @author jo@Dev
 * @since 3.5.0
 */
public final class LisibiliteChrome {

    /** Le contenu CLAIR (blanc — maquette {@code .sbar}/{@code .abar} jour). */
    private static final int CONTENU_CLAIR = 0xFFFFFFFF;

    /** Le contenu SOMBRE (miroir {@code --s-onpri} nuit {@code #0C2264}). */
    private static final int CONTENU_SOMBRE = 0xFF0C2264;

    /** Le seuil de bascule (fond moitié claire → contenu sombre). */
    private static final double SEUIL_LUMINANCE = 0.5;

    private LisibiliteChrome() {
        // Classe utilitaire pure.
    }

    /**
     * La couleur du contenu pour un fond donné (blanc ↔ sombre), par
     * LUMINANCE du fond.
     *
     * @param fond la couleur ARGB du fond
     * @return la couleur lisible du contenu
     */
    public static int couleurContenu(int fond) {
        return couleurContenu(fond, null);
    }

    /**
     * La couleur du contenu pour un fond donné, en HONORANT
     * {@code windowLightStatusBar} quand le thème le déclare.
     *
     * @param fond                 la couleur ARGB du fond
     * @param windowLightStatusBar la valeur EXPLICITE de l'attribut, ou
     *                             {@code null} si le thème ne le déclare
     *                             pas (→ luminance)
     * @return la couleur lisible du contenu
     */
    public static int couleurContenu(int fond, Boolean windowLightStatusBar) {
        if (windowLightStatusBar != null) {
            return windowLightStatusBar ? CONTENU_SOMBRE : CONTENU_CLAIR;
        }
        if (luminanceRelative(fond) > SEUIL_LUMINANCE) {
            return CONTENU_SOMBRE;
        }
        return CONTENU_CLAIR;
    }

    /**
     * La luminance perçue relative d'une couleur (0..1) — Rec. 709 comme
     * {@code Color.luminance} de la plateforme, en calcul PUR (testable
     * JVM) : 0,2126 R + 0,7152 V + 0,0722 B.
     *
     * @param couleur la couleur ARGB
     * @return la luminance relative
     */
    public static double luminanceRelative(int couleur) {
        double rouge = canalRouge(couleur) / 255.0;
        double vert = canalVert(couleur) / 255.0;
        double bleu = canalBleu(couleur) / 255.0;
        return 0.2126 * rouge + 0.7152 * vert + 0.0722 * bleu;
    }

    private static int canalRouge(int couleur) {
        return (couleur >> 16) & 0xFF;
    }

    private static int canalVert(int couleur) {
        return (couleur >> 8) & 0xFF;
    }

    private static int canalBleu(int couleur) {
        return couleur & 0xFF;
    }
}
