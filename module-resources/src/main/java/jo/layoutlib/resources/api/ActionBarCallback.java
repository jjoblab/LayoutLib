package jo.layoutlib.resources.api;

/**
 * Configuration de l'ACTION BAR du décor, miroir réduit du
 * {@code ActionBarCallback} de l'AOSP layoutlib-api (PE13, ADR 0137 D6).
 *
 * <p>Chez la référence, TOUT le contenu de l'action bar vient de la session
 * via ce callback — jamais de l'initiative du moteur : flèche retour
 * ({@code HomeButtonStyle.SHOW_HOME_AS_UP}), sous-titre, menus du projet
 * ({@code getMenuIds()}), besoin du popup de débordement
 * ({@code isOverflowPopupNeeded()}). Le moteur jjoblab rend une action bar
 * FICTICE maquette : il consomme la même configuration, aux DÉFAUTS
 * MAQUETTE (pas de flèche, pas de menu, pas de sous-titre) — l'hôte peut
 * ouvrir chaque capacité sans toucher au moteur.</p>
 *
 * <p>La vraie action bar AppCompat du projet (réflexion
 * {@code WindowDecorActionBar} + vrais {@code menu.xml}) reste HORS
 * périmètre (fragilité réflexive, écart maquette — étude PE13 §3.5).</p>
 *
 * <p>Ancienne coquille (v1.0, nom/valeur sans sémantique) : code mort
 * vérifié, remplacée par le contrat réel.</p>
 *
 * @author jo@Dev
 * @since 3.5.0
 */
public class ActionBarCallback {

    /** Callback par défaut : tout éteint — l'action bar maquette (titre seul). */
    private static final ActionBarCallback PAR_DEFAUT = new ActionBarCallback(false, false, null);

    /** Flèche RETOUR à gauche du titre (miroir SHOW_HOME_AS_UP de la référence). */
    private final boolean homeAsUp;

    /** Bouton de DÉBORDEMENT (⋮) à droite — le popup reste simulé par la vue. */
    private final boolean popupDebordement;

    /** Sous-titre éventuel sous le titre (miroir {@code getSubTitle()}). */
    private final String sousTitre;

    /**
     * Construit le callback.
     *
     * @param homeAsUp          {@code true} = flèche retour à gauche
     * @param popupDebordement  {@code true} = bouton ⋮ à droite
     * @param sousTitre         le sous-titre, ou {@code null}
     */
    public ActionBarCallback(boolean homeAsUp, boolean popupDebordement, String sousTitre) {
        this.homeAsUp = homeAsUp;
        this.popupDebordement = popupDebordement;
        this.sousTitre = sousTitre;
    }

    /**
     * @return le callback PAR DÉFAUT (action bar maquette : titre seul)
     */
    public static ActionBarCallback parDefaut() {
        return PAR_DEFAUT;
    }

    /**
     * @return {@code true} si la flèche retour est posée
     */
    public boolean isHomeAsUp() {
        return homeAsUp;
    }

    /**
     * @return {@code true} si le bouton de débordement est posé
     */
    public boolean isPopupDebordement() {
        return popupDebordement;
    }

    /**
     * @return le sous-titre, ou {@code null}
     */
    public String getSousTitre() {
        return sousTitre;
    }
}
