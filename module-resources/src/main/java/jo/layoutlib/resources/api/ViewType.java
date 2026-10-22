package jo.layoutlib.resources.api;

/**
 * Le MARQUAGE d'une vue SYSTÈME du décor de rendu, miroir du
 * {@code ViewType} de l'AOSP layoutlib-api (PE13, ADR 0137 D5).
 *
 * <p>Le moteur de rendu possède le chrome système ; à chaque rendu il produit
 * DEUX arbres d'infos — le CONTENU de l'utilisateur et le DÉCOR. Chaque nœud
 * du décor porte son {@code ViewType}, exactement comme le
 * {@code SystemViewInfo} de la référence : l'IDE peut exclure le chrome de
 * l'arbre des composants et, plus tard, y réintroduire une section
 * « système » interprétable (les menus de l'action bar y sont marqués chez la
 * référence pour exactement cet usage).</p>
 *
 * <p>Ancienne coquille (v1.0, nom/valeur sans sémantique) : code mort
 * vérifié, remplacé par l'enum.</p>
 *
 * @author jo@Dev
 * @since 3.5.0
 */
public enum ViewType {

    /** Nœud système de type inconnu (repli défensif). */
    UNKNOWN,

    /** Vue de l'APPLICATION (conteneurs du décor qui ne sont pas des barres). */
    APPLICATION,

    /** La STATUS BAR du décor. */
    STATUS_BAR,

    /** La BARRE DE NAVIGATION du décor (trois boutons ou pilule gestuelle). */
    NAVIGATION_BAR,

    /** L'ACTION BAR factice du décor. */
    ACTION_BAR,

    /** Le MENU de l'action bar (les items d'action — miroir AOSP). */
    ACTION_BAR_MENU,

    /** Le BOUTON de débordement (⋮) de l'action bar — miroir AOSP. */
    ACTION_BAR_OVERFLOW,

    /** Le POPUP de débordement ouvert — miroir AOSP. */
    ACTION_BAR_OVERFLOW_MENU
}
