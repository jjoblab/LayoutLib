package jo.layoutlib.themes;

/**
 * Résolveur de thèmes et styles Android.
 *
 * <p>Cette interface est le contrat que doit implémenter le Module 4
 * (Themes). Elle résout les références de type {@code ?attr/colorPrimary}
 * et applique les styles hérités à une vue.</p>
 *
 * <h2>Fonctionnalités attendues</h2>
 * <ul>
 *   <li>Parsing de {@code themes.xml} et {@code styles.xml}</li>
 *   <li>Construction de la chaîne d'héritage
 *       ({@code Theme.MyApp → Theme.Material3.DayNight → ...})</li>
 *   <li>Résolution {@code ?attr/} et {@code ?android:attr/}</li>
 *   <li>Support DayNight ({@code values-night/themes.xml})</li>
 *   <li>Résolution récursive via ResourceResolver (Module 2) et
 *       DrawableResolver (Module 3)</li>
 * </ul>
 *
 * <p>Référence layoutlib original :
 * {@code com.android.layoutlib.bridge.impl.BridgeContext.obtainStyledAttributes()}.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public interface ThemeResolver {

    /**
     * Définit le thème courant.
     *
     * @param themeName nom du thème (ex. {@code Theme.Material3.DayNight})
     */
    void setTheme(String themeName);

    /**
     * Résout une référence {@code ?attr/foo} ou {@code ?android:attr/foo}.
     *
     * @param reference la référence
     * @return la valeur résolue (couleur ARGB, chemin drawable, etc.), ou
     *         {@code null} si non définie dans le thème
     */
    Object resolveAttr(String reference);

    /**
     * Récupère la valeur d'un attribut color du thème.
     *
     * @param attrName nom de l'attribut (ex. {@code colorPrimary})
     * @return la valeur ARGB, ou {@code null}
     */
    Integer getColorAttr(String attrName);

    /**
     * Récupère la valeur d'un attribut dimension du thème.
     *
     * @param attrName nom de l'attribut (ex. {@code buttonHeight})
     * @return la valeur en pixels, ou {@code null}
     */
    Float getDimensionAttr(String attrName);

    /**
     * Active ou désactive le mode nuit.
     *
     * @param nightMode {@code true} pour le mode nuit
     */
    void setNightMode(boolean nightMode);

    /**
     * Récupère le nom du parent direct d'un style.
     *
     * @param styleName nom du style
     * @return le nom du parent, ou {@code null} si pas de parent
     */
    String getParentStyle(String styleName);

    /**
     * Vide le cache des résolutions.
     */
    void clearCache();
}
