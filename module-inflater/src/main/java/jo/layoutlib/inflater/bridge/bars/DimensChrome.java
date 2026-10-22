package jo.layoutlib.inflater.bridge.bars;

import jo.layoutlib.themes.ThemeResolver;

/**
 * La SOURCE DE VÉRITÉ des hauteurs du chrome système (PE13, ADR 0137 D4).
 *
 * <p>Avant PE13, les hauteurs vivaient en CONSTANTES DUPPLIQUÉES côté IDE
 * ({@code ChromeSysteme} CodeIDE : 24/56/48) ET côté frère (stubs
 * {@code bars/*}). Désormais le MOTEUR les possède — miroir du
 * {@code Layout.Builder} AOSP qui lit
 * {@code getFrameworkAttrDimension("status_bar_height", 25)} : « le moteur ne
 * porte AUCUNE constante IDE ; chaque taille vient des ressources ».</p>
 *
 * <p><strong>Garde-fou G1</strong> (étude PE13 §5) : le REPLI du moteur de
 * référence vaut 25 dp (status bar) — nous gardons 24 dp, la valeur de la
 * MAQUETTE {@code apercu-xml.html} ({@code .sbar}). L'action bar lit
 * {@code actionBarSize} du thème QUAND l'attribut y est déclaré (repli
 * 56 dp) ; tout écart futur (dossier res framework embarqué) se tranchera
 * avec le propriétaire, jamais silencieusement.</p>
 *
 * @author jo@Dev
 * @since 3.5.0
 */
public final class DimensChrome {

    /** Hauteur de la status bar en dp (maquette {@code .sbar} ; repli AOSP : 25 — G1). */
    public static final int STATUS_BAR_HAUTEUR_DP = 24;

    /** Hauteur de la barre de navigation en dp (maquette {@code .nbar}). */
    public static final int BARRE_NAVIGATION_HAUTEUR_DP = 48;

    /** Hauteur de l'action bar en dp quand le thème ne déclare pas {@code actionBarSize}. */
    public static final int ACTION_BAR_HAUTEUR_DP = 56;

    private DimensChrome() {
        // Classe utilitaire pure.
    }

    /**
     * La hauteur de l'action bar en dp — {@code actionBarSize} du thème si
     * l'attribut est déclaré (px convertis en dp), repli 56 dp (maquette
     * {@code .abar}, miroir du {@code Layout.Builder} AOSP qui lit
     * {@code actionBarSize} avec repli).
     *
     * @param themeResolver le résolveur de thèmes, ou {@code null} (repli)
     * @param densite       la densité du rendu (dp → px) — celle du PROFIL
     * @return la hauteur en dp
     */
    public static int actionBarHauteurDp(ThemeResolver themeResolver, float densite) {
        if (themeResolver == null || densite <= 0f) {
            return ACTION_BAR_HAUTEUR_DP;
        }
        Float px = themeResolver.getDimensionAttr("actionBarSize");
        if (px == null || px <= 0f) {
            return ACTION_BAR_HAUTEUR_DP;
        }
        return Math.round(px / densite);
    }
}
