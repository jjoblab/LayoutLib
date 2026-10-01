package jo.layoutlib.themes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Représentation d'un style ou thème Android.
 *
 * <p>Un style est un ensemble de paires (nom d'attribut, valeur) avec un nom,
 * un parent optionnel, et un flag indiquant s'il s'agit d'un thème. Les
 * valeurs sont stockées sous forme de chaînes brutes — la conversion en
 * couleur, dimension, etc. se fait au moment de la résolution via les
 * modules Resources et Drawables.</p>
 *
 * <h2>Exemple XML parsé</h2>
 * <pre>{@code
 * <style name="Theme.MyApp" parent="Theme.Material3.DayNight">
 *     <item name="colorPrimary">@color/purple_500</item>
 *     <item name="colorSecondary">#FF625B71</item>
 *     <item name="android:windowBackground">@drawable/bg</item>
 * </style>
 * }</pre>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class StyleDefinition {

    /** Nom pleinement qualifié du style (ex. {@code "Theme.MyApp"}). */
    private final String name;

    /** Nom du style parent, ou {@code null} si pas de parent. */
    private String parent;

    /** Indique si c'est un thème (vs un style simple). */
    private final boolean isTheme;

    /** Map ordonnée des attributs (nom → valeur brute). */
    private final Map<String, String> attributes = new LinkedHashMap<>();

    /**
     * Construit un nouveau style.
     *
     * @param name    nom du style
     * @param parent  nom du parent, ou {@code null}
     * @param isTheme {@code true} si c'est un thème
     */
    public StyleDefinition(String name, String parent, boolean isTheme) {
        if (name == null || name.isEmpty()) {
            throw new ThemeException("Nom de style vide", name);
        }
        this.name = name;
        this.parent = parent;
        this.isTheme = isTheme;
    }

    /**
     * Ajoute ou remplace un attribut du style.
     *
     * @param attrName nom de l'attribut (ex. {@code "colorPrimary"} ou
     *                 {@code "android:windowBackground"})
     * @param value    valeur brute (ex. {@code "@color/purple_500"},
     *                 {@code "#FF6750A4"}, {@code "16dp"})
     */
    public void setAttribute(String attrName, String value) {
        if (attrName == null || attrName.isEmpty()) {
            return;
        }
        attributes.put(attrName, value);
    }

    /**
     * Récupère la valeur d'un attribut défini directement dans ce style
     * (sans résolution d'héritage).
     *
     * @param attrName nom de l'attribut
     * @return la valeur, ou {@code null} si non définie
     */
    public String getAttribute(String attrName) {
        return attributes.get(attrName);
    }

    /**
     * Indique si le style définit directement un attribut.
     *
     * @param attrName nom de l'attribut
     * @return {@code true} si l'attribut est défini
     */
    public boolean hasAttribute(String attrName) {
        return attributes.containsKey(attrName);
    }

    /**
     * @return une copie de la map des attributs
     */
    public Map<String, String> getAttributes() {
        return new LinkedHashMap<>(attributes);
    }

    /**
     * @return le nom du style
     */
    public String getName() {
        return name;
    }

    /**
     * @return le nom du parent, ou {@code null}
     */
    public String getParent() {
        return parent;
    }

    /**
     * Définit le parent du style.
     *
     * @param parent nom du parent
     */
    public void setParent(String parent) {
        this.parent = parent;
    }

    /**
     * @return {@code true} si c'est un thème
     */
    public boolean isTheme() {
        return isTheme;
    }

    /**
     * @return le nombre d'attributs définis directement
     */
    public int getAttributeCount() {
        return attributes.size();
    }

    @Override
    public String toString() {
        return "StyleDefinition{name=" + name
                + ", parent=" + parent
                + ", isTheme=" + isTheme
                + ", attrs=" + attributes.size() + "}";
    }
}
