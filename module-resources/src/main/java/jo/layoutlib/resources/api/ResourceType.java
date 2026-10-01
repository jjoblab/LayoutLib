package jo.layoutlib.resources.api;

/**
 * Type de resource Android, inspiré de
 * {@code com.android.resources.ResourceType} de l'AOSP.
 *
 * <p>Enumère tous les types de resources gérés par le framework Android.
 * Chaque type correspond à un sous-dossier de {@code res/} et à un
 * préfixe de référence {@code @type/}.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public enum ResourceType {

    /** Animations par propriétés (res/animator/). */
    ANIMATOR("animator"),
    /** Animations tween (res/anim/). */
    ANIM("anim"),
    /** Tableau de resources (res/array/). */
    ARRAY("array"),
    /** Attribut custom (res/attr/). */
    ATTR("attr"),
    /** Booleen (res/values/bools.xml). */
    BOOL("bool"),
    /** Couleur (res/values/colors.xml ou res/color/). */
    COLOR("color"),
    /** Drawable (res/drawable/). */
    DRAWABLE("drawable"),
    /** Dimension (res/values/dimens.xml). */
    DIMEN("dimen"),
    /** Font (res/font/). */
    FONT("font"),
    /** Fraction. */
    FRACTION("fraction"),
    /** ID (res/values/ids.xml). */
    ID("id"),
    /** Entier (res/values/integers.xml). */
    INTEGER("integer"),
    /** Layout (res/layout/). */
    LAYOUT("layout"),
    /** Menu (res/menu/). */
    MENU("menu"),
    /** Mipmap (res/mipmap/). */
    MIPMAP("mipmap"),
    /** Pluriels (res/values/strings.xml {@code <plurals>}). */
    PLURALS("plurals"),
    /** Resource brute (res/raw/). */
    RAW("raw"),
    /** Chaîne (res/values/strings.xml). */
    STRING("string"),
    /** Style (res/values/styles.xml). */
    STYLE("style"),
    /** Styleable (res/values/attrs.xml {@code <declare-styleable>}). */
    STYLEABLE("styleable"),
    /** Theme (res/values/themes.xml). */
    THEME("theme"),
    /** Transition (res/transition/). */
    TRANSITION("transition"),
    /** XML (res/xml/). */
    XML("xml");

    /** Nom du type tel qu'il apparaît dans les références {@code @type/}. */
    private final String name;

    ResourceType(String name) {
        this.name = name;
    }

    /**
     * @return le nom du type (ex. {@code "color"})
     */
    public String getName() {
        return name;
    }

    /**
     * Récupère le ResourceType depuis son nom.
     *
     * @param name le nom du type
     * @return l'enum, ou {@code null} si inconnu
     */
    public static ResourceType fromName(String name) {
        if (name == null) {
            return null;
        }
        for (ResourceType type : values()) {
            if (type.name.equals(name)) {
                return type;
            }
        }
        return null;
    }

    /**
     * @return le nom du dossier de resources associé (ex. {@code "values"} pour COLOR)
     */
    public String getFolderName() {
        switch (this) {
            case COLOR:
            case STRING:
            case DIMEN:
            case INTEGER:
            case BOOL:
            case ATTR:
            case STYLE:
            case STYLEABLE:
            case THEME:
            case PLURALS:
            case ARRAY:
            case ID:
                return "values";
            default:
                return name;
        }
    }
}
