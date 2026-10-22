package jo.layoutlib.inflater.bridge.bars;

/**
 * Le mode de CONTENU de la barre de navigation du décor (PE13, ADR 0137 D6).
 *
 * <p>La zone garde SA HAUTEUR (48 dp, maquette {@code .nbar}) dans les deux
 * modes — seul le contenu change : les trois glyphes boutons de la maquette
 * ou la PILULE gestuelle ({@link NavigationHandle}) d'Android 10+. Le mode
 * gestuel est une CAPACITÉ moteur aux DÉFAUTS MAQUETTE (trois boutons) :
 * aucune UI CodeIDE ne le branche aujourd'hui (hors planche v6), le
 * branchement d'un réglage reste un chantier séparé.</p>
 *
 * @author jo@Dev
 * @since 3.5.0
 */
public enum ModeNavigation {

    /** Les TROIS boutons de la maquette {@code .nbar} (retour, accueil, applications). */
    TROIS_BOUTONS,

    /** La PILULE gestuelle ({@link NavigationHandle}, 48×4 dp). */
    GESTUELLE
}
